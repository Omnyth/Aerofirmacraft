package com.omnyth.aerofirmacraftterrain.mixin;

import com.omnyth.aerofirmacraftterrain.AerofirmacraftTerrain;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
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
    private static final AtomicBoolean AFC_DID_LAND_PATCH = new AtomicBoolean(false);
    private static final AtomicInteger AFC_SKIP_LOG_COUNT = new AtomicInteger();

    private static final int GLOBAL_OCEAN_TOP_Y = 0;
    private static final int LAND_MASS_THICKNESS = 14;
    private static final int PATCH_MIN_LOCAL = 4;
    private static final int PATCH_MAX_LOCAL = 11;
    private static final int REQUIRED_LAND_COLUMNS = 24;
    private static final int SKIP_LOG_LIMIT = 20;

    @Inject(method = "fillFromNoise", at = @At("RETURN"))
    private void afc$landPatchPrototype(
            final Blender blender,
            final RandomState randomState,
            final StructureManager structureManager,
            final ChunkAccess chunk,
            final CallbackInfoReturnable<CompletableFuture<ChunkAccess>> cir
    ) {
        final CompletableFuture<ChunkAccess> future = cir.getReturnValue();

        if (future == null) {
            AerofirmacraftTerrain.LOGGER.warn(
                    "AFC land patch: fillFromNoise returned null future for chunkX={} chunkZ={}",
                    chunk.getPos().x,
                    chunk.getPos().z
            );
            return;
        }

        future.whenComplete((result, throwable) -> {
            if (throwable != null) {
                AerofirmacraftTerrain.LOGGER.error(
                        "AFC land patch: fillFromNoise future failed for chunkX={} chunkZ={}",
                        chunk.getPos().x,
                        chunk.getPos().z,
                        throwable
                );
                return;
            }

            if (AFC_DID_LAND_PATCH.get()) {
                return;
            }

            final PatchStats stats = inspectPatch(result);

            if (stats.landColumns < REQUIRED_LAND_COLUMNS) {
                final int skipCount = AFC_SKIP_LOG_COUNT.incrementAndGet();

                if (skipCount <= SKIP_LOG_LIMIT) {
                    AerofirmacraftTerrain.LOGGER.info(
                            "AFC land patch: skipped chunkX={} chunkZ={} landColumns={} fluidColumns={} centerSurfaceBlock={}",
                            result.getPos().x,
                            result.getPos().z,
                            stats.landColumns,
                            stats.fluidColumns,
                            stats.centerSurfaceBlock
                    );
                } else if (skipCount == SKIP_LOG_LIMIT + 1) {
                    AerofirmacraftTerrain.LOGGER.info(
                            "AFC land patch: skip log limit reached. Further skip logs suppressed."
                    );
                }

                return;
            }

            if (!AFC_DID_LAND_PATCH.compareAndSet(false, true)) {
                return;
            }

            applyLandPatch(result, stats);
        });
    }

    private static PatchStats inspectPatch(final ChunkAccess chunk) {
        final int minY = chunk.getHeightAccessorForGeneration().getMinBuildHeight();
        final int maxY = chunk.getHeightAccessorForGeneration().getMaxBuildHeight() - 1;

        final BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();

        final PatchStats stats = new PatchStats();
        stats.minSurfaceY = Integer.MAX_VALUE;
        stats.maxSurfaceY = Integer.MIN_VALUE;
        stats.centerSurfaceY = minY;
        stats.centerSurfaceBlock = "unknown";

        for (int localX = PATCH_MIN_LOCAL; localX <= PATCH_MAX_LOCAL; localX++) {
            for (int localZ = PATCH_MIN_LOCAL; localZ <= PATCH_MAX_LOCAL; localZ++) {
                final int worldX = chunk.getPos().getBlockX(localX);
                final int worldZ = chunk.getPos().getBlockZ(localZ);

                final int surfaceY = findTopNonAirY(chunk, worldX, worldZ, minY, maxY);

                mutablePos.set(worldX, surfaceY, worldZ);
                final BlockState surfaceState = chunk.getBlockState(mutablePos);
                final String surfaceBlockId = BuiltInRegistries.BLOCK.getKey(surfaceState.getBlock()).toString();

                stats.minSurfaceY = Math.min(stats.minSurfaceY, surfaceY);
                stats.maxSurfaceY = Math.max(stats.maxSurfaceY, surfaceY);

                if (isFluidSurface(surfaceBlockId)) {
                    stats.fluidColumns++;
                } else {
                    stats.landColumns++;
                }

                if (localX == 8 && localZ == 8) {
                    stats.centerSurfaceY = surfaceY;
                    stats.centerSurfaceBlock = surfaceBlockId;
                }
            }
        }

        return stats;
    }

    private static void applyLandPatch(final ChunkAccess chunk, final PatchStats stats) {
        final int minY = chunk.getHeightAccessorForGeneration().getMinBuildHeight();
        final int maxY = chunk.getHeightAccessorForGeneration().getMaxBuildHeight() - 1;

        final int centerWorldX = chunk.getPos().getBlockX(8);
        final int centerWorldZ = chunk.getPos().getBlockZ(8);

        final BlockState air = Blocks.AIR.defaultBlockState();
        final BlockState glowstone = Blocks.GLOWSTONE.defaultBlockState();

        final BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();

        int transformedLandColumns = 0;
        int transformedFluidColumns = 0;
        int airBlocks = 0;
        int markerBlocks = 0;

        int minUndersideY = Integer.MAX_VALUE;
        int maxUndersideY = Integer.MIN_VALUE;
        int centerUndersideY = minY;

        for (int localX = PATCH_MIN_LOCAL; localX <= PATCH_MAX_LOCAL; localX++) {
            for (int localZ = PATCH_MIN_LOCAL; localZ <= PATCH_MAX_LOCAL; localZ++) {
                final int worldX = chunk.getPos().getBlockX(localX);
                final int worldZ = chunk.getPos().getBlockZ(localZ);

                final int surfaceY = findTopNonAirY(chunk, worldX, worldZ, minY, maxY);

                mutablePos.set(worldX, surfaceY, worldZ);
                final BlockState surfaceState = chunk.getBlockState(mutablePos);
                final String surfaceBlockId = BuiltInRegistries.BLOCK.getKey(surfaceState.getBlock()).toString();

                if (isFluidSurface(surfaceBlockId)) {
                    transformedFluidColumns++;

                    final int carveTopY = clamp(surfaceY + 6, GLOBAL_OCEAN_TOP_Y + 1, maxY);

                    for (int y = GLOBAL_OCEAN_TOP_Y + 1; y <= carveTopY; y++) {
                        mutablePos.set(worldX, y, worldZ);

                        if (!chunk.getBlockState(mutablePos).isAir()) {
                            chunk.setBlockState(mutablePos, air, false);
                            airBlocks++;
                        }
                    }
                } else {
                    transformedLandColumns++;

                    final int undersideY = clamp(surfaceY - LAND_MASS_THICKNESS, GLOBAL_OCEAN_TOP_Y + 8, maxY - 1);
                    final int carveTopY = undersideY - 1;

                    minUndersideY = Math.min(minUndersideY, undersideY);
                    maxUndersideY = Math.max(maxUndersideY, undersideY);

                    if (localX == 8 && localZ == 8) {
                        centerUndersideY = undersideY;
                    }

                    mutablePos.set(worldX, undersideY, worldZ);
                    chunk.setBlockState(mutablePos, glowstone, false);
                    markerBlocks++;

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
                "AFC land patch: applied chunkX={} chunkZ={} centerX={} centerZ={} landColumns={} fluidColumns={} transformedLandColumns={} transformedFluidColumns={} airBlocks={} markerBlocks={} surfaceY={}..{} undersideY={}..{} centerSurfaceY={} centerUndersideY={} centerSurfaceBlockBefore={} chunkStatus={} chunkClass={} surfaceTp='/tp @s {} {} {}' undersideTp='/tp @s {} {} {}'",
                chunk.getPos().x,
                chunk.getPos().z,
                centerWorldX,
                centerWorldZ,
                stats.landColumns,
                stats.fluidColumns,
                transformedLandColumns,
                transformedFluidColumns,
                airBlocks,
                markerBlocks,
                stats.minSurfaceY,
                stats.maxSurfaceY,
                minUndersideY,
                maxUndersideY,
                stats.centerSurfaceY,
                centerUndersideY,
                stats.centerSurfaceBlock,
                chunk.getPersistedStatus(),
                chunk.getClass().getName(),
                centerWorldX,
                stats.centerSurfaceY + 12,
                centerWorldZ,
                centerWorldX,
                Math.max(GLOBAL_OCEAN_TOP_Y + 4, centerUndersideY - 6),
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

    private static boolean isFluidSurface(final String surfaceBlockId) {
        return surfaceBlockId.startsWith("tfc:fluid/")
                || surfaceBlockId.contains("water");
    }

    private static int clamp(final int value, final int min, final int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static final class PatchStats {
        int landColumns;
        int fluidColumns;
        int minSurfaceY;
        int maxSurfaceY;
        int centerSurfaceY;
        String centerSurfaceBlock;
    }
}