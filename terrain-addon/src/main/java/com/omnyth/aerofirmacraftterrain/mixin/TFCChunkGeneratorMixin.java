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

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Mixin(targets = "net.dries007.tfc.world.TFCChunkGenerator", remap = false)
public abstract class TFCChunkGeneratorMixin {
    private static final AtomicBoolean AFC_DID_TRANSFORM = new AtomicBoolean(false);
    private static final AtomicInteger AFC_SKIP_COUNT = new AtomicInteger();

    private static final int GLOBAL_OCEAN_TOP_Y = 0;
    private static final int LAND_TRIGGER_SURFACE_Y = 66;

    // Bigger than the test patch so we preserve enough mass for later ore testing.
    private static final int BASE_LAND_MASS_THICKNESS = 44;
    private static final int THICKNESS_VARIATION = 10;

    private static final int SKIP_LOG_LIMIT = 32;

    @Inject(method = "fillFromNoise", at = @At("RETURN"))
    private void afc$fullChunkTransform(
            final Blender blender,
            final RandomState randomState,
            final StructureManager structureManager,
            final ChunkAccess chunk,
            final CallbackInfoReturnable<CompletableFuture<ChunkAccess>> cir
    ) {
        final CompletableFuture<ChunkAccess> future = cir.getReturnValue();

        if (future == null) {
            AerofirmacraftTerrain.LOGGER.warn(
                    "AFC full transform: fillFromNoise returned null future for chunkX={} chunkZ={}",
                    chunk.getPos().x,
                    chunk.getPos().z
            );
            return;
        }

        future.whenComplete((result, throwable) -> {
            if (throwable != null) {
                AerofirmacraftTerrain.LOGGER.error(
                        "AFC full transform: fillFromNoise future failed for chunkX={} chunkZ={}",
                        chunk.getPos().x,
                        chunk.getPos().z,
                        throwable
                );
                return;
            }

            if (AFC_DID_TRANSFORM.get()) {
                return;
            }

            final int minY = result.getHeightAccessorForGeneration().getMinBuildHeight();
            final int maxY = result.getHeightAccessorForGeneration().getMaxBuildHeight() - 1;
            final int centerWorldX = result.getPos().getBlockX(8);
            final int centerWorldZ = result.getPos().getBlockZ(8);
            final int centerSurfaceY = findTopNonAirY(result, centerWorldX, centerWorldZ, minY, maxY);

            if (centerSurfaceY < LAND_TRIGGER_SURFACE_Y) {
                final int skip = AFC_SKIP_COUNT.incrementAndGet();

                if (skip <= SKIP_LOG_LIMIT) {
                    AerofirmacraftTerrain.LOGGER.info(
                            "AFC full transform: skipped chunkX={} chunkZ={} centerSurfaceY={} requiredSurfaceY={}",
                            result.getPos().x,
                            result.getPos().z,
                            centerSurfaceY,
                            LAND_TRIGGER_SURFACE_Y
                    );
                } else if (skip == SKIP_LOG_LIMIT + 1) {
                    AerofirmacraftTerrain.LOGGER.info(
                            "AFC full transform: skip log limit reached. Further skip logs suppressed."
                    );
                }

                return;
            }

            if (!AFC_DID_TRANSFORM.compareAndSet(false, true)) {
                return;
            }

            applyFullChunkTransform(result, centerSurfaceY);
        });
    }

    private static void applyFullChunkTransform(final ChunkAccess chunk, final int centerSurfaceY) {
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
        int centerUndersideY = minY;

        for (int localX = 0; localX < 16; localX++) {
            for (int localZ = 0; localZ < 16; localZ++) {
                final int worldX = chunk.getPos().getBlockX(localX);
                final int worldZ = chunk.getPos().getBlockZ(localZ);

                final int surfaceY = findTopNonAirY(chunk, worldX, worldZ, minY, maxY);

                minSurfaceY = Math.min(minSurfaceY, surfaceY);
                maxSurfaceY = Math.max(maxSurfaceY, surfaceY);

                if (surfaceY < LAND_TRIGGER_SURFACE_Y) {
                    lowColumns++;

                    // Treat lower columns as ocean/shore/lowland-like for this prototype:
                    // clear above the future low ocean layer, but do not do registry checks.
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

                    // Mark every 4th underside column with glowstone so the shape is visible without filling the whole underside.
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

        chunk.setUnsaved(true);

        AerofirmacraftTerrain.LOGGER.info(
                "AFC full transform: applied chunkX={} chunkZ={} centerX={} centerZ={} landLikeColumns={} lowColumns={} airBlocks={} markerBlocks={} surfaceY={}..{} undersideY={}..{} centerSurfaceY={} centerUndersideY={} chunkStatus={} chunkClass={} surfaceTp='/tp @s {} {} {}' undersideTp='/tp @s {} {} {}'",
                chunk.getPos().x,
                chunk.getPos().z,
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
}