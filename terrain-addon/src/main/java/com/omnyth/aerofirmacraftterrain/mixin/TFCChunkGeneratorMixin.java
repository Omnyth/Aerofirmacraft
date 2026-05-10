package com.omnyth.aerofirmacraftterrain.mixin;

import com.omnyth.aerofirmacraftterrain.AerofirmacraftTerrain;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.Biome;
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

    // Fallback local-shape heuristic for low columns that are not directly tagged as tfc:river.
    private static final int RIVER_LOW_MIN_Y = 58;
    private static final int RIVER_LOW_MAX_Y = 65;
    private static final int RIVER_NEIGHBOR_RADIUS = 5;
    private static final int RIVER_CARDINAL_HALF_WIDTH = 2;
    private static final int RIVER_MIN_NEARBY_LAND_OPPOSITE = 8;
    private static final int RIVER_MIN_NEARBY_LAND_SURROUNDED = 12;

    private static final int DETAILED_LOG_LIMIT = 16;
    private static final int SUMMARY_LOG_INTERVAL = 128;

    private static final AtomicInteger AFC_TRANSFORM_COUNT = new AtomicInteger();

    private static final AtomicLong AFC_TOTAL_AIR_BLOCKS = new AtomicLong();
    private static final AtomicLong AFC_TOTAL_CRUST_BLOCKS = new AtomicLong();
    private static final AtomicLong AFC_TOTAL_WATER_BLOCKS = new AtomicLong();
    private static final AtomicLong AFC_TOTAL_LAND_COLUMNS = new AtomicLong();
    private static final AtomicLong AFC_TOTAL_RIVER_BIOME_COLUMNS = new AtomicLong();
    private static final AtomicLong AFC_TOTAL_HEURISTIC_PRESERVED_COLUMNS = new AtomicLong();
    private static final AtomicLong AFC_TOTAL_SKY_GAP_COLUMNS = new AtomicLong();

    @Inject(method = "fillFromNoise", at = @At("RETURN"), cancellable = true)
    private void afc$continuousOceanLockedV5TfcRiverBiome(
            final Blender blender,
            final RandomState randomState,
            final StructureManager structureManager,
            final ChunkAccess chunk,
            final CallbackInfoReturnable<CompletableFuture<ChunkAccess>> cir
    ) {
        final CompletableFuture<ChunkAccess> originalFuture = cir.getReturnValue();

        if (originalFuture == null) {
            AerofirmacraftTerrain.LOGGER.warn(
                    "AFC continuous locked v5: fillFromNoise returned null future for chunkX={} chunkZ={}",
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
                        "AFC continuous locked v5: transform failed chunkX={} chunkZ={} chunkClass={} chunkStatus={}",
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
                    "AFC continuous locked v5: skipped non-ProtoChunk chunkX={} chunkZ={} chunkClass={} chunkStatus={}",
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

        final int[][] surfaceMap = new int[16][16];
        final boolean[][] landLikeMap = new boolean[16][16];
        final boolean[][] riverBiomeMap = new boolean[16][16];
        final boolean[][] heuristicPreserveMap = new boolean[16][16];
        final boolean[][] preservedLowMap = new boolean[16][16];

        int landLikeColumns = 0;
        int riverBiomeColumns = 0;
        int heuristicPreservedColumns = 0;
        int skyGapColumns = 0;

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

        // First pass:
        // - write lower ocean/crust
        // - capture surface height and biome classification
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

                surfaceMap[localX][localZ] = surfaceY;
                landLikeMap[localX][localZ] = surfaceY >= LAND_TRIGGER_SURFACE_Y;
                riverBiomeMap[localX][localZ] = isTfcRiverBiomeColumn(chunk, localX, surfaceY, localZ, minY, maxY);

                minSurfaceY = Math.min(minSurfaceY, surfaceY);
                maxSurfaceY = Math.max(maxSurfaceY, surfaceY);

                if (localX == 8 && localZ == 8) {
                    centerSurfaceY = surfaceY;
                }
            }
        }

        // Second pass:
        // - low tfc:river biome columns are preserved directly
        // - local heuristic remains as fallback
        for (int localX = 0; localX < 16; localX++) {
            for (int localZ = 0; localZ < 16; localZ++) {
                if (!landLikeMap[localX][localZ]) {
                    if (riverBiomeMap[localX][localZ]) {
                        preservedLowMap[localX][localZ] = true;
                    } else if (shouldPreserveLowColumn(surfaceMap, landLikeMap, localX, localZ)) {
                        heuristicPreserveMap[localX][localZ] = true;
                        preservedLowMap[localX][localZ] = true;
                    }
                }
            }
        }

        // Third pass:
        // - land and preserved river/lake columns keep their surface, but still get floating underside carving
        // - broad low/ocean columns become sky gaps over the lower ocean
        for (int localX = 0; localX < 16; localX++) {
            for (int localZ = 0; localZ < 16; localZ++) {
                final int surfaceY = surfaceMap[localX][localZ];

                if (landLikeMap[localX][localZ] || preservedLowMap[localX][localZ]) {
                    if (landLikeMap[localX][localZ]) {
                        landLikeColumns++;
                    } else if (riverBiomeMap[localX][localZ]) {
                        riverBiomeColumns++;
                    } else {
                        heuristicPreservedColumns++;
                    }

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
                } else {
                    skyGapColumns++;

                    final int carveTopY = clamp(surfaceY + 6, GLOBAL_OCEAN_TOP_Y + 1, maxY);

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
        AFC_TOTAL_RIVER_BIOME_COLUMNS.addAndGet(riverBiomeColumns);
        AFC_TOTAL_HEURISTIC_PRESERVED_COLUMNS.addAndGet(heuristicPreservedColumns);
        AFC_TOTAL_SKY_GAP_COLUMNS.addAndGet(skyGapColumns);

        if (transformIndex <= DETAILED_LOG_LIMIT) {
            AerofirmacraftTerrain.LOGGER.info(
                    "AFC continuous locked v5: applied index={} chunkX={} chunkZ={} centerX={} centerZ={} landLikeColumns={} riverBiomeColumns={} heuristicPreservedColumns={} skyGapColumns={} airBlocks={} crustBlocks={} waterBlocks={} surfaceY={}..{} undersideY={}..{} oceanCrustTopY={} oceanTopY={} centerSurfaceY={} centerUndersideY={} chunkStatus={} chunkClass={} surfaceTp='/tp @s {} {} {}' oceanTp='/tp @s {} {} {}' undersideTp='/tp @s {} {} {}'",
                    transformIndex,
                    chunk.getPos().x,
                    chunk.getPos().z,
                    centerWorldX,
                    centerWorldZ,
                    landLikeColumns,
                    riverBiomeColumns,
                    heuristicPreservedColumns,
                    skyGapColumns,
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
                    "AFC continuous locked v5 summary: chunks={} latestChunkX={} latestChunkZ={} totalLandColumns={} totalRiverBiomeColumns={} totalHeuristicPreservedColumns={} totalSkyGapColumns={} totalAirBlocks={} totalCrustBlocks={} totalWaterBlocks={} latestStatus={}",
                    transformIndex,
                    chunk.getPos().x,
                    chunk.getPos().z,
                    AFC_TOTAL_LAND_COLUMNS.get(),
                    AFC_TOTAL_RIVER_BIOME_COLUMNS.get(),
                    AFC_TOTAL_HEURISTIC_PRESERVED_COLUMNS.get(),
                    AFC_TOTAL_SKY_GAP_COLUMNS.get(),
                    AFC_TOTAL_AIR_BLOCKS.get(),
                    AFC_TOTAL_CRUST_BLOCKS.get(),
                    AFC_TOTAL_WATER_BLOCKS.get(),
                    chunk.getPersistedStatus()
            );
        }
    }

    private static boolean isTfcRiverBiomeColumn(
            final ChunkAccess chunk,
            final int localX,
            final int surfaceY,
            final int localZ,
            final int minY,
            final int maxY
    ) {
        final int sampleY = clamp(surfaceY, minY, maxY);
        final LevelChunkSection section = chunk.getSection(chunk.getSectionIndex(sampleY));

        final int quartLocalX = (localX >> 2) & 3;
        final int quartLocalY = QuartPos.fromBlock(sampleY) & 3;
        final int quartLocalZ = (localZ >> 2) & 3;

        final Holder<Biome> biomeHolder = section.getNoiseBiome(quartLocalX, quartLocalY, quartLocalZ);

        return biomeHolder.unwrapKey()
                .map(ResourceKey::location)
                .map(id -> "tfc".equals(id.getNamespace()) && "river".equals(id.getPath()))
                .orElse(false);
    }

    private static boolean shouldPreserveLowColumn(
            final int[][] surfaceMap,
            final boolean[][] landLikeMap,
            final int localX,
            final int localZ
    ) {
        final int surfaceY = surfaceMap[localX][localZ];

        if (surfaceY < RIVER_LOW_MIN_Y || surfaceY > RIVER_LOW_MAX_Y) {
            return false;
        }

        int nearbyLand = 0;

        boolean north = false;
        boolean south = false;
        boolean west = false;
        boolean east = false;

        for (int dx = -RIVER_NEIGHBOR_RADIUS; dx <= RIVER_NEIGHBOR_RADIUS; dx++) {
            for (int dz = -RIVER_NEIGHBOR_RADIUS; dz <= RIVER_NEIGHBOR_RADIUS; dz++) {
                if (dx == 0 && dz == 0) {
                    continue;
                }

                final int nx = localX + dx;
                final int nz = localZ + dz;

                if (nx < 0 || nx >= 16 || nz < 0 || nz >= 16) {
                    continue;
                }

                if (!landLikeMap[nx][nz]) {
                    continue;
                }

                nearbyLand++;

                if (dz < 0 && Math.abs(dx) <= RIVER_CARDINAL_HALF_WIDTH) {
                    north = true;
                }

                if (dz > 0 && Math.abs(dx) <= RIVER_CARDINAL_HALF_WIDTH) {
                    south = true;
                }

                if (dx < 0 && Math.abs(dz) <= RIVER_CARDINAL_HALF_WIDTH) {
                    west = true;
                }

                if (dx > 0 && Math.abs(dz) <= RIVER_CARDINAL_HALF_WIDTH) {
                    east = true;
                }
            }
        }

        final boolean oppositeBanks = (north && south) || (west && east);

        int directionCount = 0;
        if (north) directionCount++;
        if (south) directionCount++;
        if (west) directionCount++;
        if (east) directionCount++;

        return (oppositeBanks && nearbyLand >= RIVER_MIN_NEARBY_LAND_OPPOSITE)
                || (directionCount >= 3 && nearbyLand >= RIVER_MIN_NEARBY_LAND_SURROUNDED);
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