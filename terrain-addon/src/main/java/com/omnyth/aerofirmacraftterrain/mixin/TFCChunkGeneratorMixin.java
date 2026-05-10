package com.omnyth.aerofirmacraftterrain.mixin;

import com.omnyth.aerofirmacraftterrain.AerofirmacraftTerrain;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Mixin(targets = "net.dries007.tfc.world.TFCChunkGenerator", remap = false)
public abstract class TFCChunkGeneratorMixin {
    private static final Object AFC_TARGET_LOCK = new Object();

    private static final Set<Long> AFC_TRANSFORMED_CHUNKS = ConcurrentHashMap.newKeySet();
    private static final List<PendingChunk> AFC_PENDING_CHUNKS = new ArrayList<>();

    private static final AtomicInteger AFC_TRANSFORM_COUNT = new AtomicInteger();
    private static final AtomicInteger AFC_SKIP_COUNT = new AtomicInteger();

    private static boolean afc$targetSelected = false;
    private static int afc$targetChunkX = 0;
    private static int afc$targetChunkZ = 0;

    private static final int GLOBAL_OCEAN_TOP_Y = 0;
    private static final int LAND_TRIGGER_SURFACE_Y = 66;

    private static final int BASE_LAND_MASS_THICKNESS = 44;
    private static final int THICKNESS_VARIATION = 10;

    private static final int REGION_RADIUS_CHUNKS = 2;
    private static final int MAX_TRANSFORMED_CHUNKS = 25;
    private static final int SKIP_LOG_LIMIT = 32;
    private static final int PENDING_CHUNK_LIMIT = 96;

    @Inject(method = "fillFromNoise", at = @At("RETURN"))
    private void afc$regionTransform5x5(
            final Blender blender,
            final RandomState randomState,
            final StructureManager structureManager,
            final ChunkAccess chunk,
            final CallbackInfoReturnable<CompletableFuture<ChunkAccess>> cir
    ) {
        final CompletableFuture<ChunkAccess> future = cir.getReturnValue();

        if (future == null) {
            AerofirmacraftTerrain.LOGGER.warn(
                    "AFC region 5x5: fillFromNoise returned null future for chunkX={} chunkZ={}",
                    chunk.getPos().x,
                    chunk.getPos().z
            );
            return;
        }

        future.whenComplete((result, throwable) -> {
            if (throwable != null) {
                AerofirmacraftTerrain.LOGGER.error(
                        "AFC region 5x5: fillFromNoise future failed for chunkX={} chunkZ={}",
                        chunk.getPos().x,
                        chunk.getPos().z,
                        throwable
                );
                return;
            }

            final int minY = result.getHeightAccessorForGeneration().getMinBuildHeight();
            final int maxY = result.getHeightAccessorForGeneration().getMaxBuildHeight() - 1;
            final int centerWorldX = result.getPos().getBlockX(8);
            final int centerWorldZ = result.getPos().getBlockZ(8);
            final int centerSurfaceY = findTopNonAirY(result, centerWorldX, centerWorldZ, minY, maxY);

            final List<TransformJob> jobs = claimChunkAndMaybeReplayPending(result, centerSurfaceY);

            for (TransformJob job : jobs) {
                applyFullChunkTransform(job.chunk(), job.centerSurfaceY(), job.index());
            }
        });
    }

    private static List<TransformJob> claimChunkAndMaybeReplayPending(final ChunkAccess chunk, final int centerSurfaceY) {
        final List<TransformJob> jobs = new ArrayList<>();

        synchronized (AFC_TARGET_LOCK) {
            if (!afc$targetSelected) {
                if (centerSurfaceY < LAND_TRIGGER_SURFACE_Y) {
                    rememberPending(chunk, centerSurfaceY);
                    logSkip(chunk, centerSurfaceY, "awaiting_land_target");
                    return jobs;
                }

                afc$targetSelected = true;
                afc$targetChunkX = chunk.getPos().x;
                afc$targetChunkZ = chunk.getPos().z;

                AerofirmacraftTerrain.LOGGER.info(
                        "AFC region 5x5: selected target center chunkX={} chunkZ={} centerSurfaceY={}",
                        afc$targetChunkX,
                        afc$targetChunkZ,
                        centerSurfaceY
                );

                replayPendingChunksInto(jobs);
            }

            claimCurrentChunkInto(chunk, centerSurfaceY, jobs);
        }

        return jobs;
    }

    private static void rememberPending(final ChunkAccess chunk, final int centerSurfaceY) {
        if (AFC_PENDING_CHUNKS.size() >= PENDING_CHUNK_LIMIT) {
            return;
        }

        AFC_PENDING_CHUNKS.add(new PendingChunk(chunk, centerSurfaceY));
    }

    private static void replayPendingChunksInto(final List<TransformJob> jobs) {
        final Iterator<PendingChunk> iterator = AFC_PENDING_CHUNKS.iterator();

        while (iterator.hasNext()) {
            final PendingChunk pending = iterator.next();
            iterator.remove();

            claimCurrentChunkInto(pending.chunk(), pending.centerSurfaceY(), jobs);

            if (AFC_TRANSFORMED_CHUNKS.size() >= MAX_TRANSFORMED_CHUNKS) {
                return;
            }
        }
    }

    private static void claimCurrentChunkInto(
            final ChunkAccess chunk,
            final int centerSurfaceY,
            final List<TransformJob> jobs
    ) {
        if (AFC_TRANSFORMED_CHUNKS.size() >= MAX_TRANSFORMED_CHUNKS) {
            logSkip(chunk, centerSurfaceY, "transform_limit_reached");
            return;
        }

        final int dx = Math.abs(chunk.getPos().x - afc$targetChunkX);
        final int dz = Math.abs(chunk.getPos().z - afc$targetChunkZ);

        if (dx > REGION_RADIUS_CHUNKS || dz > REGION_RADIUS_CHUNKS) {
            logSkip(chunk, centerSurfaceY, "outside_target_5x5");
            return;
        }

        if (!AFC_TRANSFORMED_CHUNKS.add(chunk.getPos().toLong())) {
            return;
        }

        final int index = AFC_TRANSFORM_COUNT.incrementAndGet();
        jobs.add(new TransformJob(chunk, centerSurfaceY, index));
    }

    private static void applyFullChunkTransform(final ChunkAccess chunk, final int centerSurfaceY, final int transformIndex) {
        final int minY = chunk.getHeightAccessorForGeneration().getMinBuildHeight();
        final int maxY = chunk.getHeightAccessorForGeneration().getMaxBuildHeight() - 1;

        final int centerWorldX = chunk.getPos().getBlockX(8);
        final int centerWorldZ = chunk.getPos().getBlockZ(8);

        final BlockState air = Blocks.AIR.defaultBlockState();
        final BlockState glowstone = Blocks.GLOWSTONE.defaultBlockState();

        final BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();

        int landLikeColumns = 0;
        int lowColumns = 0;
        int airBlocks = 0;
        int markerBlocks = 0;

        int minSurfaceY = Integer.MAX_VALUE;
        int maxSurfaceY = Integer.MIN_VALUE;
        int minUndersideY = Integer.MAX_VALUE;
        int maxUndersideY = Integer.MIN_VALUE;
        int centerUndersideY = Integer.MIN_VALUE;

        for (int localX = 0; localX < 16; localX++) {
            for (int localZ = 0; localZ < 16; localZ++) {
                final int worldX = chunk.getPos().getBlockX(localX);
                final int worldZ = chunk.getPos().getBlockZ(localZ);

                final int surfaceY = findTopNonAirY(chunk, worldX, worldZ, minY, maxY);

                minSurfaceY = Math.min(minSurfaceY, surfaceY);
                maxSurfaceY = Math.max(maxSurfaceY, surfaceY);

                if (surfaceY < LAND_TRIGGER_SURFACE_Y) {
                    lowColumns++;

                    final int carveTopY = clamp(surfaceY + 6, GLOBAL_OCEAN_TOP_Y + 1, maxY);

                    for (int y = GLOBAL_OCEAN_TOP_Y + 1; y <= carveTopY; y++) {
                        mutablePos.set(worldX, y, worldZ);

                        if (!chunk.getBlockState(mutablePos).isAir()) {
                            chunk.setBlockState(mutablePos, air, false);
                            airBlocks++;
                        }
                    }
                } else {
                    landLikeColumns++;

                    final int thickness = computeColumnThickness(worldX, worldZ);
                    final int undersideY = clamp(surfaceY - thickness, GLOBAL_OCEAN_TOP_Y + 8, maxY - 1);
                    final int carveTopY = undersideY - 1;

                    minUndersideY = Math.min(minUndersideY, undersideY);
                    maxUndersideY = Math.max(maxUndersideY, undersideY);

                    if (localX == 8 && localZ == 8) {
                        centerUndersideY = undersideY;
                    }

                    if ((localX % 4 == 0) && (localZ % 4 == 0)) {
                        mutablePos.set(worldX, undersideY, worldZ);
                        chunk.setBlockState(mutablePos, glowstone, false);
                        markerBlocks++;
                    }

                    for (int y = GLOBAL_OCEAN_TOP_Y + 1; y <= carveTopY; y++) {
                        mutablePos.set(worldX, y, worldZ);

                        if (!chunk.getBlockState(mutablePos).isAir()) {
                            chunk.setBlockState(mutablePos, air, false);
                            airBlocks++;
                        }
                    }
                }
            }
        }

        if (minUndersideY == Integer.MAX_VALUE) {
            minUndersideY = -9999;
            maxUndersideY = -9999;
        }

        if (centerUndersideY == Integer.MIN_VALUE) {
            centerUndersideY = minUndersideY;
        }

        chunk.setUnsaved(true);

        AerofirmacraftTerrain.LOGGER.info(
                "AFC region 5x5: applied index={} chunkX={} chunkZ={} targetChunkX={} targetChunkZ={} centerX={} centerZ={} landLikeColumns={} lowColumns={} airBlocks={} markerBlocks={} surfaceY={}..{} undersideY={}..{} centerSurfaceY={} centerUndersideY={} chunkStatus={} chunkClass={} surfaceTp='/tp @s {} {} {}' undersideTp='/tp @s {} {} {}'",
                transformIndex,
                chunk.getPos().x,
                chunk.getPos().z,
                afc$targetChunkX,
                afc$targetChunkZ,
                centerWorldX,
                centerWorldZ,
                landLikeColumns,
                lowColumns,
                airBlocks,
                markerBlocks,
                minSurfaceY,
                maxSurfaceY,
                minUndersideY,
                maxUndersideY,
                centerSurfaceY,
                centerUndersideY,
                chunk.getPersistedStatus(),
                chunk.getClass().getName(),
                centerWorldX,
                centerSurfaceY + 16,
                centerWorldZ,
                centerWorldX,
                Math.max(GLOBAL_OCEAN_TOP_Y + 4, centerUndersideY - 8),
                centerWorldZ
        );
    }

    private static void logSkip(final ChunkAccess chunk, final int centerSurfaceY, final String reason) {
        final int skip = AFC_SKIP_COUNT.incrementAndGet();

        if (skip <= SKIP_LOG_LIMIT) {
            AerofirmacraftTerrain.LOGGER.info(
                    "AFC region 5x5: skipped chunkX={} chunkZ={} centerSurfaceY={} reason={}",
                    chunk.getPos().x,
                    chunk.getPos().z,
                    centerSurfaceY,
                    reason
            );
        } else if (skip == SKIP_LOG_LIMIT + 1) {
            AerofirmacraftTerrain.LOGGER.info(
                    "AFC region 5x5: skip log limit reached. Further skip logs suppressed."
            );
        }
    }

    private static int findTopNonAirY(
            final ChunkAccess chunk,
            final int worldX,
            final int worldZ,
            final int minY,
            final int maxY
    ) {
        final BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();

        for (int y = maxY; y >= minY; y--) {
            mutablePos.set(worldX, y, worldZ);

            if (!chunk.getBlockState(mutablePos).isAir()) {
                return y;
            }
        }

        return minY;
    }

    private static int computeColumnThickness(final int worldX, final int worldZ) {
        final int noise = Math.floorMod(hash(worldX, worldZ), THICKNESS_VARIATION * 2 + 1) - THICKNESS_VARIATION;
        return BASE_LAND_MASS_THICKNESS + noise;
    }

    private static int hash(final int x, final int z) {
        int h = x * 73428767 ^ z * 912367;
        h ^= (h >>> 13);
        h *= 1274126177;
        h ^= (h >>> 16);
        return h;
    }

    private static int clamp(final int value, final int min, final int max) {
        return Math.max(min, Math.min(max, value));
    }

    private record PendingChunk(ChunkAccess chunk, int centerSurfaceY) {
    }

    private record TransformJob(ChunkAccess chunk, int centerSurfaceY, int index) {
    }
}