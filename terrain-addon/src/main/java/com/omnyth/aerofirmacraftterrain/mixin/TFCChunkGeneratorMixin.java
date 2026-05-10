package com.omnyth.aerofirmacraftterrain.mixin;

import com.omnyth.aerofirmacraftterrain.AerofirmacraftTerrain;
import com.omnyth.aerofirmacraftterrain.mixin.accessor.LevelChunkSectionAccessor;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Mixin(targets = "net.dries007.tfc.world.TFCChunkGenerator", remap = false)
public abstract class TFCChunkGeneratorMixin {
    private static final ResourceKey<Biome> SKY_GAP_BIOME_KEY = ResourceKey.create(
            Registries.BIOME,
            ResourceLocation.fromNamespaceAndPath(AerofirmacraftTerrain.MODID, "sky_gap")
    );

    private static final ResourceKey<Biome> TFC_OCEAN_BIOME_KEY = ResourceKey.create(
            Registries.BIOME,
            ResourceLocation.fromNamespaceAndPath("tfc", "ocean")
    );

    private static final int EXPECTED_DIMENSION_MIN_Y = -128;
    private static final int OLD_TFC_MIN_Y = -64;
    private static final int LOWER_OCEAN_TOP_Y = OLD_TFC_MIN_Y - 1;

    private static final int LOWER_BEDROCK_LEVELS = 1;
    private static final int LOWER_CRUST_LEVELS = 8;

    private static final int ISLAND_BASE_THICKNESS = 44;
    private static final int ISLAND_THICKNESS_VARIATION = 10;

    private static final int SKY_GAP_NONE = 0;
    private static final int SKY_GAP_DEFINITE_OCEAN = 1;
    private static final int SKY_GAP_COASTAL_EDGE = 2;

    private static final int DETAILED_LOG_LIMIT = 4;
    private static final int SUMMARY_LOG_INTERVAL = 512;

    private static final AtomicBoolean AFC_DIMENSION_SANITY_LOGGED = new AtomicBoolean(false);

    private static final AtomicInteger AFC_LOWER_FILL_COUNT = new AtomicInteger();
    private static final AtomicInteger AFC_SURFACE_TRANSFORM_COUNT = new AtomicInteger();

    private static final AtomicLong AFC_TOTAL_LOWER_CRUST_BLOCKS = new AtomicLong();
    private static final AtomicLong AFC_TOTAL_LOWER_WATER_BLOCKS = new AtomicLong();
    private static final AtomicLong AFC_TOTAL_TFC_OCEAN_BIOME_CELLS = new AtomicLong();

    private static final AtomicLong AFC_TOTAL_SURFACE_AIR_BLOCKS = new AtomicLong();
    private static final AtomicLong AFC_TOTAL_PRESERVED_COLUMNS = new AtomicLong();
    private static final AtomicLong AFC_TOTAL_DEFINITE_OCEAN_SKY_GAP_COLUMNS = new AtomicLong();
    private static final AtomicLong AFC_TOTAL_COASTAL_SKY_GAP_COLUMNS = new AtomicLong();
    private static final AtomicLong AFC_TOTAL_SKY_GAP_BIOME_CELLS = new AtomicLong();

    @Inject(method = "fillFromNoise", at = @At("RETURN"), cancellable = true)
    private void afc$fillLowerOceanOnlyV16(
            final Blender blender,
            final RandomState randomState,
            final StructureManager structureManager,
            final ChunkAccess chunk,
            final CallbackInfoReturnable<CompletableFuture<ChunkAccess>> cir
    ) {
        final CompletableFuture<ChunkAccess> originalFuture = cir.getReturnValue();

        if (originalFuture == null) {
            AerofirmacraftTerrain.LOGGER.warn(
                    "AFC v16 lower: fillFromNoise returned null future for chunkX={} chunkZ={}",
                    chunk.getPos().x,
                    chunk.getPos().z
            );
            return;
        }

        cir.setReturnValue(originalFuture.thenApply(result -> {
            try {
                final Registry<Biome> biomeRegistry = structureManager.registryAccess().registryOrThrow(Registries.BIOME);
                final Holder<Biome> tfcOceanBiome = biomeRegistry.getHolderOrThrow(TFC_OCEAN_BIOME_KEY);

                fillLowerOceanLocked(result, tfcOceanBiome);
            } catch (Throwable throwable) {
                AerofirmacraftTerrain.LOGGER.error(
                        "AFC v16 lower: lower-ocean fill failed chunkX={} chunkZ={} chunkClass={} chunkStatus={}",
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

    @Inject(method = "applyBiomeDecoration", at = @At("RETURN"))
    private void afc$deferredSurfaceTransformV16(
            final WorldGenLevel level,
            final ChunkAccess chunk,
            final StructureManager structureManager,
            final CallbackInfo ci
    ) {
        try {
            final Registry<Biome> biomeRegistry = level.registryAccess().registryOrThrow(Registries.BIOME);
            final Holder<Biome> skyGapBiome = biomeRegistry.getHolderOrThrow(SKY_GAP_BIOME_KEY);

            transformSurfaceAfterFeaturesLocked(chunk, skyGapBiome);
        } catch (Throwable throwable) {
            AerofirmacraftTerrain.LOGGER.error(
                    "AFC v16 surface: deferred transform failed chunkX={} chunkZ={} chunkClass={} chunkStatus={}",
                    chunk.getPos().x,
                    chunk.getPos().z,
                    chunk.getClass().getName(),
                    chunk.getPersistedStatus(),
                    throwable
            );
            throw throwable;
        }
    }

    private static void fillLowerOceanLocked(
            final ChunkAccess chunk,
            final Holder<Biome> tfcOceanBiome
    ) {
        if (!(chunk instanceof ProtoChunk)) {
            return;
        }

        final Set<LevelChunkSection> lockedSections = new HashSet<>();

        for (LevelChunkSection section : chunk.getSections()) {
            section.acquire();
            lockedSections.add(section);
        }

        try {
            fillLowerOceanUnlocked(chunk, tfcOceanBiome);
            chunk.setUnsaved(true);
        } finally {
            for (LevelChunkSection section : lockedSections) {
                section.release();
            }
        }
    }

    private static void transformSurfaceAfterFeaturesLocked(
            final ChunkAccess chunk,
            final Holder<Biome> skyGapBiome
    ) {
        if (!(chunk instanceof ProtoChunk)) {
            return;
        }

        final Set<LevelChunkSection> lockedSections = new HashSet<>();

        for (LevelChunkSection section : chunk.getSections()) {
            section.acquire();
            lockedSections.add(section);
        }

        try {
            transformSurfaceAfterFeaturesUnlocked(chunk, skyGapBiome);
            chunk.setUnsaved(true);
        } finally {
            for (LevelChunkSection section : lockedSections) {
                section.release();
            }
        }
    }

    private static void fillLowerOceanUnlocked(
            final ChunkAccess chunk,
            final Holder<Biome> tfcOceanBiome
    ) {
        final int index = AFC_LOWER_FILL_COUNT.incrementAndGet();

        final int minY = chunk.getHeightAccessorForGeneration().getMinBuildHeight();
        final int maxY = chunk.getHeightAccessorForGeneration().getMaxBuildHeight() - 1;
        final boolean lowerBandAvailable = minY <= LOWER_OCEAN_TOP_Y;

        if (AFC_DIMENSION_SANITY_LOGGED.compareAndSet(false, true)) {
            if (minY != EXPECTED_DIMENSION_MIN_Y) {
                AerofirmacraftTerrain.LOGGER.warn(
                        "AFC v16 dimension sanity: expected minY={} but got minY={}. maxY={} lowerOceanTopY={}",
                        EXPECTED_DIMENSION_MIN_Y,
                        minY,
                        maxY,
                        LOWER_OCEAN_TOP_Y
                );
            } else {
                AerofirmacraftTerrain.LOGGER.info(
                        "AFC v16 dimension sanity passed: minY={} maxY={} lowerOceanTopY={} oldTfcMinY={}",
                        minY,
                        maxY,
                        LOWER_OCEAN_TOP_Y,
                        OLD_TFC_MIN_Y
                );
            }
        }

        final BlockState bedrock = Blocks.BEDROCK.defaultBlockState();
        final BlockState stone = Blocks.STONE.defaultBlockState();
        final BlockState water = Blocks.WATER.defaultBlockState();

        int crustBlocks = 0;
        int waterBlocks = 0;

        final int crustTopY = minY + LOWER_BEDROCK_LEVELS + LOWER_CRUST_LEVELS - 1;
        final int lowerOceanTopY = Math.min(LOWER_OCEAN_TOP_Y, maxY);

        if (lowerBandAvailable) {
            for (int localX = 0; localX < 16; localX++) {
                for (int localZ = 0; localZ < 16; localZ++) {
                    for (int y = minY; y <= lowerOceanTopY; y++) {
                        if (y == minY) {
                            setBlockStateUnlocked(chunk, localX, y, localZ, bedrock);
                            crustBlocks++;
                        } else if (y <= crustTopY) {
                            setBlockStateUnlocked(chunk, localX, y, localZ, stone);
                            crustBlocks++;
                        } else {
                            setBlockStateUnlocked(chunk, localX, y, localZ, water);
                            waterBlocks++;
                        }
                    }
                }
            }
        }

        final int tfcOceanBiomeCells = assignTfcOceanBiomeCellsUnlocked(chunk, tfcOceanBiome, minY, maxY);

        AFC_TOTAL_LOWER_CRUST_BLOCKS.addAndGet(crustBlocks);
        AFC_TOTAL_LOWER_WATER_BLOCKS.addAndGet(waterBlocks);
        AFC_TOTAL_TFC_OCEAN_BIOME_CELLS.addAndGet(tfcOceanBiomeCells);

        if (index <= DETAILED_LOG_LIMIT || index % SUMMARY_LOG_INTERVAL == 0) {
            AerofirmacraftTerrain.LOGGER.info(
                    "AFC v16 lower: index={} chunkX={} chunkZ={} minY={} lowerBand={} crustBlocks={} waterBlocks={} tfcOceanBiomeCells={} totalsCrust={} totalsWater={} totalsTfcOceanBiomeCells={} status={}",
                    index,
                    chunk.getPos().x,
                    chunk.getPos().z,
                    minY,
                    lowerBandAvailable,
                    crustBlocks,
                    waterBlocks,
                    tfcOceanBiomeCells,
                    AFC_TOTAL_LOWER_CRUST_BLOCKS.get(),
                    AFC_TOTAL_LOWER_WATER_BLOCKS.get(),
                    AFC_TOTAL_TFC_OCEAN_BIOME_CELLS.get(),
                    chunk.getPersistedStatus()
            );
        }
    }

    private static void transformSurfaceAfterFeaturesUnlocked(
            final ChunkAccess chunk,
            final Holder<Biome> skyGapBiome
    ) {
        final int index = AFC_SURFACE_TRANSFORM_COUNT.incrementAndGet();

        final int minY = chunk.getHeightAccessorForGeneration().getMinBuildHeight();
        final int maxY = chunk.getHeightAccessorForGeneration().getMaxBuildHeight() - 1;

        final int centerWorldX = chunk.getPos().getBlockX(8);
        final int centerWorldZ = chunk.getPos().getBlockZ(8);

        final BlockState air = Blocks.AIR.defaultBlockState();

        final int[][] surfaceMap = new int[16][16];
        final int[][] skyGapClassMap = new int[16][16];
        final String[][] biomeIdMap = new String[16][16];

        int preservedColumns = 0;
        int definiteOceanSkyGapColumns = 0;
        int coastalSkyGapColumns = 0;
        int skyGapBiomeCells = 0;
        int airBlocks = 0;

        int minSurfaceY = Integer.MAX_VALUE;
        int maxSurfaceY = Integer.MIN_VALUE;
        int minUndersideY = Integer.MAX_VALUE;
        int maxUndersideY = Integer.MIN_VALUE;
        int centerSurfaceY = Integer.MIN_VALUE;
        int centerUndersideY = Integer.MIN_VALUE;

        for (int localX = 0; localX < 16; localX++) {
            for (int localZ = 0; localZ < 16; localZ++) {
                final int surfaceY = findTopNonAirY(chunk, localX, localZ, OLD_TFC_MIN_Y, maxY);
                final String biomeId = getBiomeIdForColumn(chunk, localX, surfaceY, localZ, minY, maxY);
                final int skyGapClass = classifySkyGapBiomeId(biomeId);

                surfaceMap[localX][localZ] = surfaceY;
                biomeIdMap[localX][localZ] = biomeId;
                skyGapClassMap[localX][localZ] = skyGapClass;

                minSurfaceY = Math.min(minSurfaceY, surfaceY);
                maxSurfaceY = Math.max(maxSurfaceY, surfaceY);

                if (localX == 8 && localZ == 8) {
                    centerSurfaceY = surfaceY;
                }
            }
        }

        skyGapBiomeCells = assignSkyGapBiomeCellsUnlocked(chunk, skyGapBiome, skyGapClassMap, minY, maxY);

        for (int localX = 0; localX < 16; localX++) {
            for (int localZ = 0; localZ < 16; localZ++) {
                final int surfaceY = surfaceMap[localX][localZ];
                final int skyGapClass = skyGapClassMap[localX][localZ];

                if (skyGapClass == SKY_GAP_DEFINITE_OCEAN || skyGapClass == SKY_GAP_COASTAL_EDGE) {
                    if (skyGapClass == SKY_GAP_DEFINITE_OCEAN) {
                        definiteOceanSkyGapColumns++;
                    } else {
                        coastalSkyGapColumns++;
                    }

                    final int carveTopY = clamp(surfaceY + 6, OLD_TFC_MIN_Y, maxY);

                    for (int y = OLD_TFC_MIN_Y; y <= carveTopY; y++) {
                        if (!getBlockStateUnlocked(chunk, localX, y, localZ).isAir()) {
                            setBlockStateUnlocked(chunk, localX, y, localZ, air);
                            airBlocks++;
                        }
                    }
                } else {
                    preservedColumns++;

                    final int worldX = chunk.getPos().getBlockX(localX);
                    final int worldZ = chunk.getPos().getBlockZ(localZ);

                    final int thickness = computeColumnThickness(worldX, worldZ);
                    final int undersideY = clamp(surfaceY - thickness, OLD_TFC_MIN_Y + 8, maxY - 1);
                    final int carveTopY = undersideY - 1;

                    minUndersideY = Math.min(minUndersideY, undersideY);
                    maxUndersideY = Math.max(maxUndersideY, undersideY);

                    if (localX == 8 && localZ == 8) {
                        centerUndersideY = undersideY;
                    }

                    for (int y = OLD_TFC_MIN_Y; y <= carveTopY; y++) {
                        if (!getBlockStateUnlocked(chunk, localX, y, localZ).isAir()) {
                            setBlockStateUnlocked(chunk, localX, y, localZ, air);
                            airBlocks++;
                        }
                    }
                }
            }
        }

        if (minUndersideY == Integer.MAX_VALUE) {
            minUndersideY = OLD_TFC_MIN_Y + 4;
            maxUndersideY = OLD_TFC_MIN_Y + 4;
        }

        if (centerUndersideY == Integer.MIN_VALUE) {
            centerUndersideY = minUndersideY;
        }

        if (centerSurfaceY == Integer.MIN_VALUE) {
            centerSurfaceY = minSurfaceY;
        }

        final String centerBiomeId = biomeIdMap[8][8];

        AFC_TOTAL_SURFACE_AIR_BLOCKS.addAndGet(airBlocks);
        AFC_TOTAL_PRESERVED_COLUMNS.addAndGet(preservedColumns);
        AFC_TOTAL_DEFINITE_OCEAN_SKY_GAP_COLUMNS.addAndGet(definiteOceanSkyGapColumns);
        AFC_TOTAL_COASTAL_SKY_GAP_COLUMNS.addAndGet(coastalSkyGapColumns);
        AFC_TOTAL_SKY_GAP_BIOME_CELLS.addAndGet(skyGapBiomeCells);

        if (index <= DETAILED_LOG_LIMIT || index % SUMMARY_LOG_INTERVAL == 0) {
            AerofirmacraftTerrain.LOGGER.info(
                    "AFC v16 surface: index={} chunkX={} chunkZ={} centerBiome={} preserved={} oceanGap={} coastalGap={} skyGapBiomeCells={} airBlocks={} totalPreserved={} totalOceanGap={} totalCoastalGap={} totalSkyGapBiomeCells={} totalAirBlocks={} surfaceY={}..{} undersideY={}..{} status={} surfaceTp='/tp @s {} {} {}' lowerOceanTp='/tp @s {} {} {}'",
                    index,
                    chunk.getPos().x,
                    chunk.getPos().z,
                    centerBiomeId,
                    preservedColumns,
                    definiteOceanSkyGapColumns,
                    coastalSkyGapColumns,
                    skyGapBiomeCells,
                    airBlocks,
                    AFC_TOTAL_PRESERVED_COLUMNS.get(),
                    AFC_TOTAL_DEFINITE_OCEAN_SKY_GAP_COLUMNS.get(),
                    AFC_TOTAL_COASTAL_SKY_GAP_COLUMNS.get(),
                    AFC_TOTAL_SKY_GAP_BIOME_CELLS.get(),
                    AFC_TOTAL_SURFACE_AIR_BLOCKS.get(),
                    minSurfaceY,
                    maxSurfaceY,
                    minUndersideY,
                    maxUndersideY,
                    chunk.getPersistedStatus(),
                    centerWorldX,
                    centerSurfaceY + 16,
                    centerWorldZ,
                    centerWorldX,
                    LOWER_OCEAN_TOP_Y + 4,
                    centerWorldZ
            );
        }
    }

    private static int assignTfcOceanBiomeCellsUnlocked(
            final ChunkAccess chunk,
            final Holder<Biome> tfcOceanBiome,
            final int minY,
            final int maxY
    ) {
        int assignedCells = 0;

        final LevelChunkSection[] sections = chunk.getSections();

        for (int sectionIndex = 0; sectionIndex < sections.length; sectionIndex++) {
            final LevelChunkSection section = sections[sectionIndex];
            final int sectionMinY = minY + sectionIndex * 16;

            PalettedContainer<Holder<Biome>> mutableBiomes = null;
            boolean changed = false;

            for (int quartY = 0; quartY < 4; quartY++) {
                final int quartBlockY = sectionMinY + quartY * 4;

                if (quartBlockY > LOWER_OCEAN_TOP_Y || quartBlockY > maxY) {
                    continue;
                }

                for (int quartX = 0; quartX < 4; quartX++) {
                    for (int quartZ = 0; quartZ < 4; quartZ++) {
                        if (mutableBiomes == null) {
                            mutableBiomes = section.getBiomes().recreate();
                        }

                        mutableBiomes.getAndSetUnchecked(quartX, quartY, quartZ, tfcOceanBiome);
                        changed = true;
                        assignedCells++;
                    }
                }
            }

            if (changed && mutableBiomes != null) {
                ((LevelChunkSectionAccessor) (Object) section).afc$setBiomes(mutableBiomes);
            }
        }

        return assignedCells;
    }

    private static int assignSkyGapBiomeCellsUnlocked(
            final ChunkAccess chunk,
            final Holder<Biome> skyGapBiome,
            final int[][] skyGapClassMap,
            final int minY,
            final int maxY
    ) {
        int assignedCells = 0;

        final LevelChunkSection[] sections = chunk.getSections();

        for (int sectionIndex = 0; sectionIndex < sections.length; sectionIndex++) {
            final LevelChunkSection section = sections[sectionIndex];
            final int sectionMinY = minY + sectionIndex * 16;

            PalettedContainer<Holder<Biome>> mutableBiomes = null;
            boolean changed = false;

            for (int quartY = 0; quartY < 4; quartY++) {
                final int quartBlockY = sectionMinY + quartY * 4;

                if (quartBlockY <= LOWER_OCEAN_TOP_Y || quartBlockY > maxY) {
                    continue;
                }

                for (int quartX = 0; quartX < 4; quartX++) {
                    for (int quartZ = 0; quartZ < 4; quartZ++) {
                        if (!shouldQuartCellBecomeSkyGapBiome(skyGapClassMap, quartX, quartZ)) {
                            continue;
                        }

                        if (mutableBiomes == null) {
                            mutableBiomes = section.getBiomes().recreate();
                        }

                        mutableBiomes.getAndSetUnchecked(quartX, quartY, quartZ, skyGapBiome);
                        changed = true;
                        assignedCells++;
                    }
                }
            }

            if (changed && mutableBiomes != null) {
                ((LevelChunkSectionAccessor) (Object) section).afc$setBiomes(mutableBiomes);
            }
        }

        return assignedCells;
    }

    private static boolean shouldQuartCellBecomeSkyGapBiome(
            final int[][] skyGapClassMap,
            final int quartX,
            final int quartZ
    ) {
        int skyGapColumns = 0;

        final int startX = quartX * 4;
        final int startZ = quartZ * 4;

        for (int dx = 0; dx < 4; dx++) {
            for (int dz = 0; dz < 4; dz++) {
                final int skyGapClass = skyGapClassMap[startX + dx][startZ + dz];

                if (skyGapClass == SKY_GAP_DEFINITE_OCEAN || skyGapClass == SKY_GAP_COASTAL_EDGE) {
                    skyGapColumns++;
                }
            }
        }

        return skyGapColumns >= 8;
    }

    private static int classifySkyGapBiomeId(final String biomeId) {
        return switch (biomeId) {
            case "tfc:ocean",
                 "tfc:ocean_reef",
                 "tfc:deep_ocean",
                 "tfc:deep_ocean_trench" -> SKY_GAP_DEFINITE_OCEAN;

            case "tfc:embayments",
                 "tfc:shore",
                 "tfc:tidal_flats",
                 "tfc:sea_stacks",
                 "tfc:terrace_upper",
                 "tfc:terrace_lower",
                 "tfc:setback_cliffs",
                 "tfc:coastal_dunes",
                 "tfc:rocky_shores",
                 "tfc:shield_volcano_shore",
                 "tfc:old_shield_volcano_shore",
                 "tfc:ice_sheet_oceanic",
                 "tfc:ice_sheet_shore" -> SKY_GAP_COASTAL_EDGE;

            default -> SKY_GAP_NONE;
        };
    }

    private static String getBiomeIdForColumn(
            final ChunkAccess chunk,
            final int localX,
            final int sampleY,
            final int localZ,
            final int minY,
            final int maxY
    ) {
        final int y = clamp(sampleY, minY, maxY);
        final LevelChunkSection section = chunk.getSection(chunk.getSectionIndex(y));

        final int quartLocalX = (localX >> 2) & 3;
        final int quartLocalY = QuartPos.fromBlock(y) & 3;
        final int quartLocalZ = (localZ >> 2) & 3;

        final Holder<Biome> biomeHolder = section.getNoiseBiome(quartLocalX, quartLocalY, quartLocalZ);

        return biomeHolder.unwrapKey()
                .map(key -> key.location().toString())
                .orElse("unknown");
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
        final int noise = Math.floorMod(hash(worldX, worldZ), ISLAND_THICKNESS_VARIATION * 2 + 1) - ISLAND_THICKNESS_VARIATION;
        return ISLAND_BASE_THICKNESS + noise;
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