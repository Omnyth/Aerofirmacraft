package com.omnyth.aerofirmacraftterrain.mixin;

import com.omnyth.aerofirmacraftterrain.AerofirmacraftTerrain;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Mixin(targets = "net.dries007.tfc.world.TFCChunkGenerator", remap = false)
public abstract class TFCChunkGeneratorMixin {
    private static final Object AFC_TARGET_LOCK = new Object();
    private static final Set<Long> AFC_TRANSFORMED_CHUNKS = ConcurrentHashMap.newKeySet();

    private static final AtomicInteger AFC_TRANSFORM_COUNT = new AtomicInteger();
    private static final AtomicInteger AFC_SKIP_COUNT = new AtomicInteger();
    private static final AtomicInteger AFC_LOCK_ATTEMPT_COUNT = new AtomicInteger();

    private static boolean afc$targetSelected = false;
    private static int afc$targetChunkX = 0;
    private static int afc$targetChunkZ = 0;

    private static final int GLOBAL_OCEAN_TOP_Y = 0;
    private static final int LAND_TRIGGER_SURFACE_Y = 66;

    private static final int BASE_LAND_MASS_THICKNESS = 44;
    private static final int THICKNESS_VARIATION = 10;

    private static final int REGION_RADIUS_CHUNKS = 4;
    private static final int MAX_TRANSFORMED_CHUNKS = 81;

    private static final int SKIP_LOG_LIMIT = 32;
    private static final int LOCK_LOG_LIMIT = 48;

    private static final int OCEAN_CRUST_THICKNESS = 8;

    @Inject(method = "fillFromNoise", at = @At("RETURN"), cancellable = true)
    private void afc$regionTransform9x9FutureChainLockedV3(
            final Blender blender,
            final RandomState randomState,
            final StructureManager structureManager,
            final ChunkAccess chunk,
            final CallbackInfoReturnable<CompletableFuture<ChunkAccess>> cir
    ) {
        final CompletableFuture<ChunkAccess> originalFuture = cir.getReturnValue();

        if (originalFuture == null) {
            AerofirmacraftTerrain.LOGGER.warn(
                    "AFC locked v3: fillFromNoise returned null future for chunkX={} chunkZ={}",
                    chunk.getPos().x,
                    chunk.getPos().z
            );
            return;
        }

        cir.setReturnValue(originalFuture.thenApply(result -> {
            try {
                applyMaybeTransformLocked(result);
            } catch (Throwable throwable) {
                AerofirmacraftTerrain.LOGGER.error(
                        "AFC locked v3: transform failed chunkX={} chunkZ={} chunkClass={} chunkStatus={}",
                        result.getPos().x,
                        result.getPos().z,
                        result.getClass().getName(),
                        result.getPersistedStatus(),
                        throwable
                );
                throw throwable;
            }

            return result;
        }));
    }

    private static void applyMaybeTransformLocked(final ChunkAccess chunk) {
        if (!(chunk instanceof ProtoChunk)) {
            AerofirmacraftTerrain.LOGGER.warn(
                    "AFC locked v3: skipped non-ProtoChunk chunkX={} chunkZ={} chunkClass={} chunkStatus={}",
                    chunk.getPos().x,
                    chunk.getPos().z,
                    chunk.getClass().getName(),
                    chunk.getPersistedStatus()
            );
            return;
        }

        final int lockAttemptIndex = AFC_LOCK_ATTEMPT_COUNT.incrementAndGet();

        if (lockAttemptIndex <= LOCK_LOG_LIMIT) {
            AerofirmacraftTerrain.LOGGER.info(
                    "AFC locked v3: attempting section locks index={} chunkX={} chunkZ={} chunkStatus={}",
                    lockAttemptIndex,
                    chunk.getPos().x,
                    chunk.getPos().z,
                    chunk.getPersistedStatus()
            );
        }

        final Set<LevelChunkSection> lockedSections = new HashSet<>();

        for (LevelChunkSection section : chunk.getSections()) {
            section.acquire();
            lockedSections.add(section);
        }

        if (lockAttemptIndex <= LOCK_LOG_LIMIT) {
            AerofirmacraftTerrain.LOGGER.info(
                    "AFC locked v3: acquired section locks index={} chunkX={} chunkZ={} sectionCount={}",
                    lockAttemptIndex,
                    chunk.getPos().x,
                    chunk.getPos().z,
                    lockedSections.size()
            );
        }

        try {
            final int centerSurfaceY = getCenterSurfaceYUnlocked(chunk);
            final int transformIndex = claimChunkForTransform(chunk, centerSurfaceY);

            if (transformIndex > 0) {
                applyFullChunkTransformUnlocked(chunk, centerSurfaceY, transformIndex);
                chunk.setUnsaved(true);
            }
        } finally {
            for (LevelChunkSection section : lockedSections) {
                section.release();
            }

            if (lockAttemptIndex <= LOCK_LOG_LIMIT) {
                AerofirmacraftTerrain.LOGGER.info(
                        "AFC locked v3: released section locks index={} chunkX={} chunkZ={}",
                        lockAttemptIndex,
                        chunk.getPos().x,
                        chunk.getPos().z
                );
            }
        }
    }

    private static int getCenterSurfaceYUnlocked(final ChunkAccess chunk) {
        final int minY = chunk.getHeightAccessorForGeneration().getMinBuildHeight();
        final int maxY = chunk.getHeightAccessorForGeneration().getMaxBuildHeight() - 1;

        return findTopNonAirY(chunk, 8, 8, minY, maxY);
    }

    private static int claimChunkForTransform(final ChunkAccess chunk, final int centerSurfaceY) {
        synchronized (AFC_TARGET_LOCK) {
            if (!afc$targetSelected) {
                if (centerSurfaceY < LAND_TRIGGER_SURFACE_Y) {
                    logSkip(chunk, centerSurfaceY, "awaiting_land_target");
                    return -1;
                }

                afc$targetSelected = true;
                afc$targetChunkX = chunk.getPos().x;
                afc$targetChunkZ = chunk.getPos().z;

                AerofirmacraftTerrain.LOGGER.info(
                        "AFC locked v3: selected target center chunkX={} chunkZ={} centerSurfaceY={}",
                        afc$targetChunkX,
                        afc$targetChunkZ,
                        centerSurfaceY
                );
            }

            final int dx = Math.abs(chunk.getPos().x - afc$targetChunkX);
            final int dz = Math.abs(chunk.getPos().z - afc$targetChunkZ);

            if (dx > REGION_RADIUS_CHUNKS || dz > REGION_RADIUS_CHUNKS) {
                logSkip(chunk, centerSurfaceY, "outside_target_9x9");
                return -1;
            }

            if (AFC_TRANSFORMED_CHUNKS.size() >= MAX_TRANSFORMED_CHUNKS) {
                logSkip(chunk, centerSurfaceY, "transform_limit_reached");
                return -1;
            }

            if (!AFC_TRANSFORMED_CHUNKS.add(chunk.getPos().toLong())) {
                return -1;
            }

            return AFC_TRANSFORM_COUNT.incrementAndGet();
        }
    }

    private static void applyFullChunkTransformUnlocked(
            final ChunkAccess chunk,
            final int centerSurfaceY,
            final int transformIndex
    ) {
        final int minY = chunk.getHeightAccessorForGeneration().getMinBuildHeight();
        final int maxY = chunk.getHeightAccessorForGeneration().getMaxBuildHeight() - 1;

        final int centerWorldX = chunk.getPos().getBlockX(8);
        final int centerWorldZ = chunk.getPos().getBlockZ(8);

        final BlockState air = Blocks.AIR.defaultBlockState();
        final BlockState bedrock = Blocks.BEDROCK.defaultBlockState();
        final BlockState stone = Blocks.STONE.defaultBlockState();
        final BlockState water = Blocks.WATER.defaultBlockState();

        int landLikeColumns = 0;
        int lowColumns = 0;
        int airBlocks = 0;
        int crustBlocks = 0;
        int waterBlocks = 0;

        int minSurfaceY = Integer.MAX_VALUE;
        int maxSurfaceY = Integer.MIN_VALUE;
        int minUndersideY = Integer.MAX_VALUE;
        int maxUndersideY = Integer.MIN_VALUE;
        int centerUndersideY = Integer.MIN_VALUE;

        final int oceanCrustTopY = minY + OCEAN_CRUST_THICKNESS;

        for (int localX = 0; localX < 16; localX++) {
            for (int localZ = 0; localZ < 16; localZ++) {
                for (int y = minY; y <= oceanCrustTopY; y++) {
                    if (y == minY) {
                        setBlockStateUnlocked(chunk, localX, y, localZ, bedrock);
                    } else {
                        setBlockStateUnlocked(chunk, localX, y, localZ, stone);
                    }

                    crustBlocks++;
                }

                for (int y = oceanCrustTopY + 1; y <= GLOBAL_OCEAN_TOP_Y; y++) {
                    setBlockStateUnlocked(chunk, localX, y, localZ, water);
                    waterBlocks++;
                }

                final int surfaceY = findTopNonAirY(chunk, localX, localZ, minY, maxY);

                minSurfaceY = Math.min(minSurfaceY, surfaceY);
                maxSurfaceY = Math.max(maxSurfaceY, surfaceY);

                if (surfaceY < LAND_TRIGGER_SURFACE_Y) {
                    lowColumns++;

                    final int carveTopY = clamp(surfaceY + 6, GLOBAL_OCEAN_TOP_Y + 1, maxY);

                    for (int y = GLOBAL_OCEAN_TOP_Y + 1; y <= carveTopY; y++) {
                        if (!getBlockStateUnlocked(chunk, localX, y, localZ).isAir()) {
                            setBlockStateUnlocked(chunk, localX, y, localZ, air);
                            airBlocks++;
                        }
                    }
                } else {
                    landLikeColumns++;

                    final int worldX = chunk.getPos().getBlockX(localX);
                    final int worldZ = chunk.getPos().getBlockZ(localZ);

                    final int thickness = computeColumnThickness(worldX, worldZ);
                    final int undersideY = clamp(surfaceY - thickness, GLOBAL_OCEAN_TOP_Y + 8, maxY - 1);
                    final int carveTopY = undersideY - 1;

                    minUndersideY = Math.min(minUndersideY, undersideY);
                    maxUndersideY = Math.max(maxUndersideY, undersideY);

                    if (localX == 8 && localZ == 8) {
                        centerUndersideY = undersideY;
                    }

                    for (int y = GLOBAL_OCEAN_TOP_Y + 1; y <= carveTopY; y++) {
                        if (!getBlockStateUnlocked(chunk, localX, y, localZ).isAir()) {
                            setBlockStateUnlocked(chunk, localX, y, localZ, air);
                            airBlocks++;
                        }
                    }
                }
            }
        }

        if (minUndersideY == Integer.MAX_VALUE) {
            minUndersideY = GLOBAL_OCEAN_TOP_Y + 4;
            maxUndersideY = GLOBAL_OCEAN_TOP_Y + 4;
        }

        if (centerUndersideY == Integer.MIN_VALUE) {
            centerUndersideY = minUndersideY;
        }

        AerofirmacraftTerrain.LOGGER.info(
                "AFC locked v3: applied index={} chunkX={} chunkZ={} targetChunkX={} targetChunkZ={} centerX={} centerZ={} landLikeColumns={} lowColumns={} airBlocks={} crustBlocks={} waterBlocks={} surfaceY={}..{} undersideY={}..{} oceanCrustTopY={} oceanTopY={} centerSurfaceY={} centerUndersideY={} chunkStatus={} chunkClass={} surfaceTp='/tp @s {} {} {}' oceanTp='/tp @s {} {} {}' undersideTp='/tp @s {} {} {}'",
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
                crustBlocks,
                waterBlocks,
                minSurfaceY,
                maxSurfaceY,
                minUndersideY,
                maxUndersideY,
                oceanCrustTopY,
                GLOBAL_OCEAN_TOP_Y,
                centerSurfaceY,
                centerUndersideY,
                chunk.getPersistedStatus(),
                chunk.getClass().getName(),
                centerWorldX,
                centerSurfaceY + 16,
                centerWorldZ,
                centerWorldX,
                GLOBAL_OCEAN_TOP_Y + 4,
                centerWorldZ,
                centerWorldX,
                Math.max(GLOBAL_OCEAN_TOP_Y + 4, centerUndersideY - 8),
                centerWorldZ
        );
    }

    private static BlockState getBlockStateUnlocked(
            final ChunkAccess chunk,
            final int localX,
            final int y,
            final int localZ
    ) {
        final LevelChunkSection section = chunk.getSection(chunk.getSectionIndex(y));
        return section.getBlockState(localX & 15, y & 15, localZ & 15);
    }

    private static void setBlockStateUnlocked(
            final ChunkAccess chunk,
            final int localX,
            final int y,
            final int localZ,
            final BlockState state
    ) {
        final LevelChunkSection section = chunk.getSection(chunk.getSectionIndex(y));
        section.setBlockState(localX & 15, y & 15, localZ & 15, state, false);
    }

    private static void logSkip(final ChunkAccess chunk, final int centerSurfaceY, final String reason) {
        final int skip = AFC_SKIP_COUNT.incrementAndGet();

        if (skip <= SKIP_LOG_LIMIT) {
            AerofirmacraftTerrain.LOGGER.info(
                    "AFC locked v3: skipped chunkX={} chunkZ={} centerSurfaceY={} reason={}",
                    chunk.getPos().x,
                    chunk.getPos().z,
                    centerSurfaceY,
                    reason
            );
        } else if (skip == SKIP_LOG_LIMIT + 1) {
            AerofirmacraftTerrain.LOGGER.info(
                    "AFC locked v3: skip log limit reached. Further skip logs suppressed."
            );
        }
    }

    private static int findTopNonAirY(
            final ChunkAccess chunk,
            final int localX,
            final int localZ,
            final int minY,
            final int maxY
    ) {
        for (int y = maxY; y >= minY; y--) {
            if (!getBlockStateUnlocked(chunk, localX, y, localZ).isAir()) {
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
}