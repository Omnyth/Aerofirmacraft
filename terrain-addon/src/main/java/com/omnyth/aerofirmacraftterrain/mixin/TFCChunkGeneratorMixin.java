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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Mixin(targets = "net.dries007.tfc.world.TFCChunkGenerator", remap = false)
public abstract class TFCChunkGeneratorMixin {
    private static final int GLOBAL_OCEAN_TOP_Y = 0;
    private static final int LAND_TRIGGER_SURFACE_Y = 66;

    private static final int BASE_LAND_MASS_THICKNESS = 44;
    private static final int THICKNESS_VARIATION = 10;

    private static final int OCEAN_CRUST_THICKNESS = 8;

    private static final int DETAILED_LOG_LIMIT = 12;
    private static final int SUMMARY_LOG_INTERVAL = 128;

    private static final AtomicInteger AFC_TRANSFORM_COUNT = new AtomicInteger();

    private static final AtomicLong AFC_TOTAL_AIR_BLOCKS = new AtomicLong();
    private static final AtomicLong AFC_TOTAL_CRUST_BLOCKS = new AtomicLong();
    private static final AtomicLong AFC_TOTAL_WATER_BLOCKS = new AtomicLong();
    private static final AtomicLong AFC_TOTAL_LAND_COLUMNS = new AtomicLong();
    private static final AtomicLong AFC_TOTAL_LOW_COLUMNS = new AtomicLong();

    @Inject(method = "fillFromNoise", at = @At("RETURN"), cancellable = true)
    private void afc$continuousOceanLockedV3(
            final Blender blender,
            final RandomState randomState,
            final StructureManager structureManager,
            final ChunkAccess chunk,
            final CallbackInfoReturnable<CompletableFuture<ChunkAccess>> cir
    ) {
        final CompletableFuture<ChunkAccess> originalFuture = cir.getReturnValue();

        if (originalFuture == null) {
            AerofirmacraftTerrain.LOGGER.warn(
                    "AFC continuous locked v3: fillFromNoise returned null future for chunkX={} chunkZ={}",
                    chunk.getPos().x,
                    chunk.getPos().z
            );
            return;
        }

        cir.setReturnValue(originalFuture.thenApply(result -> {
            try {
                applyContinuousTransformLocked(result);
            } catch (Throwable throwable) {
                AerofirmacraftTerrain.LOGGER.error(
                        "AFC continuous locked v3: transform failed chunkX={} chunkZ={} chunkClass={} chunkStatus={}",
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

    private static void applyContinuousTransformLocked(final ChunkAccess chunk) {
        if (!(chunk instanceof ProtoChunk)) {
            AerofirmacraftTerrain.LOGGER.warn(
                    "AFC continuous locked v3: skipped non-ProtoChunk chunkX={} chunkZ={} chunkClass={} chunkStatus={}",
                    chunk.getPos().x,
                    chunk.getPos().z,
                    chunk.getClass().getName(),
                    chunk.getPersistedStatus()
            );
            return;
        }

        final Set<LevelChunkSection> lockedSections = new HashSet<>();

        for (LevelChunkSection section : chunk.getSections()) {
            section.acquire();
            lockedSections.add(section);
        }

        try {
            applyContinuousTransformUnlocked(chunk);
            chunk.setUnsaved(true);
        } finally {
            for (LevelChunkSection section : lockedSections) {
                section.release();
            }
        }
    }

    private static void applyContinuousTransformUnlocked(final ChunkAccess chunk) {
        final int transformIndex = AFC_TRANSFORM_COUNT.incrementAndGet();

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
        int centerSurfaceY = Integer.MIN_VALUE;
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

                if (localX == 8 && localZ == 8) {
                    centerSurfaceY = surfaceY;
                }

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

        if (centerSurfaceY == Integer.MIN_VALUE) {
            centerSurfaceY = minSurfaceY;
        }

        AFC_TOTAL_AIR_BLOCKS.addAndGet(airBlocks);
        AFC_TOTAL_CRUST_BLOCKS.addAndGet(crustBlocks);
        AFC_TOTAL_WATER_BLOCKS.addAndGet(waterBlocks);
        AFC_TOTAL_LAND_COLUMNS.addAndGet(landLikeColumns);
        AFC_TOTAL_LOW_COLUMNS.addAndGet(lowColumns);

        if (transformIndex <= DETAILED_LOG_LIMIT) {
            AerofirmacraftTerrain.LOGGER.info(
                    "AFC continuous locked v3: applied index={} chunkX={} chunkZ={} centerX={} centerZ={} landLikeColumns={} lowColumns={} airBlocks={} crustBlocks={} waterBlocks={} surfaceY={}..{} undersideY={}..{} oceanCrustTopY={} oceanTopY={} centerSurfaceY={} centerUndersideY={} chunkStatus={} chunkClass={} surfaceTp='/tp @s {} {} {}' oceanTp='/tp @s {} {} {}' undersideTp='/tp @s {} {} {}'",
                    transformIndex,
                    chunk.getPos().x,
                    chunk.getPos().z,
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
        } else if (transformIndex % SUMMARY_LOG_INTERVAL == 0) {
            AerofirmacraftTerrain.LOGGER.info(
                    "AFC continuous locked v3 summary: chunks={} latestChunkX={} latestChunkZ={} totalLandColumns={} totalLowColumns={} totalAirBlocks={} totalCrustBlocks={} totalWaterBlocks={} latestStatus={}",
                    transformIndex,
                    chunk.getPos().x,
                    chunk.getPos().z,
                    AFC_TOTAL_LAND_COLUMNS.get(),
                    AFC_TOTAL_LOW_COLUMNS.get(),
                    AFC_TOTAL_AIR_BLOCKS.get(),
                    AFC_TOTAL_CRUST_BLOCKS.get(),
                    AFC_TOTAL_WATER_BLOCKS.get(),
                    chunk.getPersistedStatus()
            );
        }
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