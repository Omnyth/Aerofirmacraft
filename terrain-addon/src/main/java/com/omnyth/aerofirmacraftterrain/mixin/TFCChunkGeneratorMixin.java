package com.omnyth.aerofirmacraftterrain.mixin;

import com.omnyth.aerofirmacraftterrain.AerofirmacraftTerrain;
import com.omnyth.aerofirmacraftterrain.mixin.accessor.LevelChunkSectionAccessor;
import com.omnyth.aerofirmacraftterrain.world.AFCBiomes;
import net.dries007.tfc.world.biome.BiomeExtension;
import net.dries007.tfc.world.biome.TFCBiomes;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.StructureManager;
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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Mixin(targets = "net.dries007.tfc.world.TFCChunkGenerator", remap = false)
public abstract class TFCChunkGeneratorMixin {
    private static final int PLAN_LAND_INTERIOR = 0;
    private static final int PLAN_SKY_COAST = 1;
    private static final int PLAN_LOWER_OCEAN = 2;
    private static final int PLAN_RIVER_OR_PRESERVE = 3;

    private static final int EXPECTED_DIMENSION_MIN_Y = -256;
    private static final int OLD_TFC_MIN_Y = -64;
    private static final int SURFACE_BIOME_SAMPLE_Y = 70;

    private static final int LOWER_OCEAN_MIN_Y = -256;
    private static final int LOWER_OCEAN_WATER_TOP_Y = -193;
    private static final int LOWER_OCEAN_BIOME_TOP_Y = -189;

    private static final int SKY_GAP_BOTTOM_Y = LOWER_OCEAN_WATER_TOP_Y + 1;
    private static final int SKY_GAP_TOP_Y = OLD_TFC_MIN_Y - 1;

    private static final int OCEAN_FLOOR_BASE_Y = -218;
    private static final int OCEAN_FLOOR_MIN_Y = -236;
    private static final int OCEAN_FLOOR_MAX_Y = -204;
    private static final int OCEAN_FLOOR_SOLID_DEPTH = 8;

    private static final int ISLAND_INTERIOR_BOTTOM_BASE_Y = -120;
    private static final int ISLAND_INTERIOR_BOTTOM_MIN_Y = -140;
    private static final int ISLAND_INTERIOR_BOTTOM_MAX_Y = -100;

    private static final int SKY_COAST_BOTTOM_BASE_Y = -88;
    private static final int SKY_COAST_BOTTOM_MIN_Y = -112;
    private static final int SKY_COAST_BOTTOM_MAX_Y = -74;
    private static final int SKY_COAST_RADIUS_BLOCKS = 5;

    private static final int DETAILED_LOG_LIMIT = 8;
    private static final int SUMMARY_LOG_INTERVAL = 2048;

    private static final AtomicBoolean AFC_DIMENSION_SANITY_LOGGED = new AtomicBoolean(false);
    private static final AtomicBoolean AFC_EXTENSION_SANITY_LOGGED = new AtomicBoolean(false);
    private static final AtomicBoolean AFC_SALT_WATER_SANITY_LOGGED = new AtomicBoolean(false);

    private static final AtomicInteger AFC_PRE_NOISE_BIOME_ASSIGN_COUNT = new AtomicInteger();
    private static final AtomicInteger AFC_COLUMN_PLAN_GENERATE_COUNT = new AtomicInteger();

    private static final AtomicLong AFC_TOTAL_PRE_NOISE_LOWER_OCEAN_CELLS = new AtomicLong();
    private static final AtomicLong AFC_TOTAL_POST_NOISE_LOWER_OCEAN_CELLS = new AtomicLong();

    private static final AtomicLong AFC_TOTAL_WATER_BLOCKS = new AtomicLong();
    private static final AtomicLong AFC_TOTAL_SOLID_FLOOR_BLOCKS = new AtomicLong();
    private static final AtomicLong AFC_TOTAL_ISLAND_SOLID_BLOCKS = new AtomicLong();
    private static final AtomicLong AFC_TOTAL_AIR_BLOCKS = new AtomicLong();
    private static final AtomicLong AFC_TOTAL_SKIPPED_INTERIOR_CHUNKS = new AtomicLong();
    private static final AtomicLong AFC_TOTAL_OCEAN_PLAN_CHUNKS = new AtomicLong();
    private static final AtomicLong AFC_TOTAL_SKY_COAST_PLAN_CHUNKS = new AtomicLong();

    @Inject(method = "createBiomes", at = @At("RETURN"), cancellable = true)
    private void afc$assignLowerOceanBiomesV29(
            final RandomState randomState,
            final Blender blender,
            final StructureManager structureManager,
            final ChunkAccess chunk,
            final CallbackInfoReturnable<CompletableFuture<ChunkAccess>> cir
    ) {
        final CompletableFuture<ChunkAccess> originalFuture = cir.getReturnValue();

        if (originalFuture == null) {
            AerofirmacraftTerrain.LOGGER.warn(
                    "AFC v29 biome: createBiomes returned null future for chunkX={} chunkZ={}",
                    chunk.getPos().x,
                    chunk.getPos().z
            );
            return;
        }

        cir.setReturnValue(originalFuture.thenApply(result -> {
            try {
                final Registry<Biome> biomeRegistry = structureManager.registryAccess().registryOrThrow(Registries.BIOME);
                final Registry<BiomeExtension> extensionRegistry = TFCBiomes.REGISTRY;

                logExtensionSanityOnce(biomeRegistry, extensionRegistry);

                final Holder<Biome> lowerOceanBiome = biomeRegistry.getHolderOrThrow(AFCBiomes.LOWER_OCEAN_BIOME_KEY);

                assignLowerOceanBiomeLocked(result, lowerOceanBiome, true);
            } catch (Throwable throwable) {
                AerofirmacraftTerrain.LOGGER.error(
                        "AFC v29 biome: pre-noise lower_ocean biome assignment failed chunkX={} chunkZ={} chunkClass={} chunkStatus={}",
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

    @Inject(method = "fillFromNoise", at = @At("RETURN"), cancellable = true)
    private void afc$generateColumnPlansV29(
            final Blender blender,
            final RandomState randomState,
            final StructureManager structureManager,
            final ChunkAccess chunk,
            final CallbackInfoReturnable<CompletableFuture<ChunkAccess>> cir
    ) {
        final CompletableFuture<ChunkAccess> originalFuture = cir.getReturnValue();

        if (originalFuture == null) {
            AerofirmacraftTerrain.LOGGER.warn(
                    "AFC v29 column-plan: fillFromNoise returned null future for chunkX={} chunkZ={}",
                    chunk.getPos().x,
                    chunk.getPos().z
            );
            return;
        }

        cir.setReturnValue(originalFuture.thenApply(result -> {
            try {
                final Registry<Biome> biomeRegistry = structureManager.registryAccess().registryOrThrow(Registries.BIOME);
                final Holder<Biome> lowerOceanBiome = biomeRegistry.getHolderOrThrow(AFCBiomes.LOWER_OCEAN_BIOME_KEY);

                assignLowerOceanBiomeLocked(result, lowerOceanBiome, false);
                generateColumnPlanLocked(result);
            } catch (Throwable throwable) {
                AerofirmacraftTerrain.LOGGER.error(
                        "AFC v29 column-plan: generation failed chunkX={} chunkZ={} chunkClass={} chunkStatus={}",
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

    private static void assignLowerOceanBiomeLocked(
            final ChunkAccess chunk,
            final Holder<Biome> lowerOceanBiome,
            final boolean preNoise
    ) {
        if (!(chunk instanceof ProtoChunk)) {
            return;
        }

        final Set<LevelChunkSection> lockedSections = lockSections(chunk);

        try {
            final int minY = chunk.getHeightAccessorForGeneration().getMinBuildHeight();
            final int maxY = chunk.getHeightAccessorForGeneration().getMaxBuildHeight() - 1;

            logDimensionSanityOnce(minY, maxY);

            final int assignedCells = assignLowerOceanBiomeCellsByPlanUnlocked(chunk, lowerOceanBiome, minY, maxY);

            if (preNoise) {
                final int index = AFC_PRE_NOISE_BIOME_ASSIGN_COUNT.incrementAndGet();
                AFC_TOTAL_PRE_NOISE_LOWER_OCEAN_CELLS.addAndGet(assignedCells);

                if (index <= DETAILED_LOG_LIMIT || index % SUMMARY_LOG_INTERVAL == 0) {
                    AerofirmacraftTerrain.LOGGER.info(
                            "AFC v29 biome: index={} chunkX={} chunkZ={} minY={} assignedLowerOceanCells={} totalPreNoiseLowerOceanCells={} status={}",
                            index,
                            chunk.getPos().x,
                            chunk.getPos().z,
                            minY,
                            assignedCells,
                            AFC_TOTAL_PRE_NOISE_LOWER_OCEAN_CELLS.get(),
                            chunk.getPersistedStatus()
                    );
                }
            } else {
                AFC_TOTAL_POST_NOISE_LOWER_OCEAN_CELLS.addAndGet(assignedCells);
            }

            if (assignedCells > 0) {
                chunk.setUnsaved(true);
            }
        } finally {
            unlockSections(lockedSections);
        }
    }

    private static void generateColumnPlanLocked(final ChunkAccess chunk) {
        if (!(chunk instanceof ProtoChunk)) {
            return;
        }

        final Set<LevelChunkSection> lockedSections = lockSections(chunk);

        try {
            final boolean changed = generateColumnPlanUnlocked(chunk);

            if (changed) {
                chunk.setUnsaved(true);
            }
        } finally {
            unlockSections(lockedSections);
        }
    }

    private static boolean generateColumnPlanUnlocked(final ChunkAccess chunk) {
        final int index = AFC_COLUMN_PLAN_GENERATE_COUNT.incrementAndGet();

        final int minY = chunk.getHeightAccessorForGeneration().getMinBuildHeight();
        final int maxY = chunk.getHeightAccessorForGeneration().getMaxBuildHeight() - 1;

        logDimensionSanityOnce(minY, maxY);

        final int[][] plans = buildColumnPlans(chunk, minY, maxY);
        final int[] counts = countPlans(plans);
        final boolean hasOceanOrCoast = counts[0] > 0 || counts[1] > 0;

        if (!hasOceanOrCoast) {
            AFC_TOTAL_SKIPPED_INTERIOR_CHUNKS.incrementAndGet();

            if (index <= DETAILED_LOG_LIMIT || index % SUMMARY_LOG_INTERVAL == 0) {
                final String centerUpperBiome = getBiomeIdForColumn(chunk, 8, SURFACE_BIOME_SAMPLE_Y, 8, minY, maxY);
                final String centerLowerBiome = getBiomeIdForColumn(chunk, 8, LOWER_OCEAN_WATER_TOP_Y, 8, minY, maxY);

                AerofirmacraftTerrain.LOGGER.info(
                        "AFC v29 column-plan: index={} chunkX={} chunkZ={} skippedPureInterior=true plans[lowerOcean={}, skyCoast={}, landInterior={}, preserve={}] centerPlan={} centerUpperBiome={} centerLowerBiome={} totalSkippedInteriorChunks={} status={}",
                        index,
                        chunk.getPos().x,
                        chunk.getPos().z,
                        counts[0],
                        counts[1],
                        counts[2],
                        counts[3],
                        planName(plans[8][8]),
                        centerUpperBiome,
                        centerLowerBiome,
                        AFC_TOTAL_SKIPPED_INTERIOR_CHUNKS.get(),
                        chunk.getPersistedStatus()
                );
            }

            return false;
        }

        final int oceanBottomY = Math.max(minY, LOWER_OCEAN_MIN_Y);
        final int oceanTopY = Math.min(maxY, LOWER_OCEAN_WATER_TOP_Y);
        final int skyGapBottomY = Math.max(minY, SKY_GAP_BOTTOM_Y);
        final int skyGapTopY = Math.min(maxY, SKY_GAP_TOP_Y);

        final BlockState saltWater = getSaltWaterBlockState();
        final BlockState air = Blocks.AIR.defaultBlockState();

        int waterBlocks = 0;
        int solidFloorBlocks = 0;
        int islandSolidBlocks = 0;
        int airBlocks = 0;

        int centerPlan = plans[8][8];
        int centerFloorY = OCEAN_FLOOR_BASE_Y;
        int centerIslandBottomY = ISLAND_INTERIOR_BOTTOM_BASE_Y;

        for (int localX = 0; localX < 16; localX++) {
            for (int localZ = 0; localZ < 16; localZ++) {
                final int plan = plans[localX][localZ];

                if (plan == PLAN_RIVER_OR_PRESERVE) {
                    continue;
                }

                final int worldX = chunk.getPos().getBlockX(localX);
                final int worldZ = chunk.getPos().getBlockZ(localZ);

                if (plan == PLAN_LOWER_OCEAN) {
                    final int floorY = computeOceanFloorY(worldX, worldZ);
                    final BlockState floorState = pickFloorStateUnlocked(chunk, localX, floorY, localZ, minY);

                    if (localX == 8 && localZ == 8) {
                        centerFloorY = floorY;
                    }

                    final int solidBottomY = Math.max(minY, floorY - OCEAN_FLOOR_SOLID_DEPTH + 1);

                    for (int y = solidBottomY; y <= floorY; y++) {
                        final BlockState current = getBlockStateUnlocked(chunk, localX, y, localZ);

                        if (!current.equals(floorState)) {
                            setBlockStateUnlocked(chunk, localX, y, localZ, floorState);
                            solidFloorBlocks++;
                        }
                    }

                    for (int y = oceanBottomY; y <= oceanTopY; y++) {
                        if (y <= floorY) {
                            continue;
                        }

                        if (!getBlockStateUnlocked(chunk, localX, y, localZ).equals(saltWater)) {
                            setBlockStateUnlocked(chunk, localX, y, localZ, saltWater);
                            waterBlocks++;
                        }
                    }

                    for (int y = skyGapBottomY; y <= skyGapTopY; y++) {
                        if (!getBlockStateUnlocked(chunk, localX, y, localZ).equals(air)) {
                            setBlockStateUnlocked(chunk, localX, y, localZ, air);
                            airBlocks++;
                        }
                    }

                    continue;
                }

                final int islandBottomY = computeIslandBottomY(worldX, worldZ, plan);

                if (localX == 8 && localZ == 8) {
                    centerIslandBottomY = islandBottomY;
                }

                final BlockState islandState = pickFloorStateUnlocked(chunk, localX, OLD_TFC_MIN_Y - 1, localZ, minY);

                for (int y = islandBottomY; y <= OLD_TFC_MIN_Y - 1; y++) {
                    final BlockState current = getBlockStateUnlocked(chunk, localX, y, localZ);

                    if (!current.equals(islandState)) {
                        setBlockStateUnlocked(chunk, localX, y, localZ, islandState);
                        islandSolidBlocks++;
                    }
                }

                for (int y = skyGapBottomY; y < islandBottomY; y++) {
                    if (!getBlockStateUnlocked(chunk, localX, y, localZ).equals(air)) {
                        setBlockStateUnlocked(chunk, localX, y, localZ, air);
                        airBlocks++;
                    }
                }
            }
        }

        AFC_TOTAL_WATER_BLOCKS.addAndGet(waterBlocks);
        AFC_TOTAL_SOLID_FLOOR_BLOCKS.addAndGet(solidFloorBlocks);
        AFC_TOTAL_ISLAND_SOLID_BLOCKS.addAndGet(islandSolidBlocks);
        AFC_TOTAL_AIR_BLOCKS.addAndGet(airBlocks);

        final long oceanPlanChunkIndex = counts[0] > 0 ? AFC_TOTAL_OCEAN_PLAN_CHUNKS.incrementAndGet() : AFC_TOTAL_OCEAN_PLAN_CHUNKS.get();
        final long skyCoastPlanChunkIndex = counts[1] > 0 ? AFC_TOTAL_SKY_COAST_PLAN_CHUNKS.incrementAndGet() : AFC_TOTAL_SKY_COAST_PLAN_CHUNKS.get();

        if (index <= DETAILED_LOG_LIMIT || index % SUMMARY_LOG_INTERVAL == 0 || oceanPlanChunkIndex <= 32 || skyCoastPlanChunkIndex <= 32) {
            final String centerUpperBiome = getBiomeIdForColumn(chunk, 8, SURFACE_BIOME_SAMPLE_Y, 8, minY, maxY);
            final String centerLowerBiome = getBiomeIdForColumn(chunk, 8, LOWER_OCEAN_WATER_TOP_Y, 8, minY, maxY);

            final BlockState centerOceanWaterBlock = getBlockStateUnlocked(chunk, 8, clamp(LOWER_OCEAN_WATER_TOP_Y, minY, maxY), 8);
            final BlockState centerOceanFloorBlock = getBlockStateUnlocked(chunk, 8, clamp(centerFloorY, minY, maxY), 8);
            final BlockState centerGapBlock = getBlockStateUnlocked(chunk, 8, clamp(SKY_GAP_BOTTOM_Y, minY, maxY), 8);
            final BlockState centerIslandBottomBlock = getBlockStateUnlocked(chunk, 8, clamp(centerIslandBottomY, minY, maxY), 8);

            AerofirmacraftTerrain.LOGGER.info(
                    "AFC v29 column-plan: index={} chunkX={} chunkZ={} skippedPureInterior=false oceanPlanChunkIndex={} skyCoastPlanChunkIndex={} plans[lowerOcean={}, skyCoast={}, landInterior={}, preserve={}] centerPlan={} centerUpperBiome={} centerLowerBiome={} waterBlocks={} solidFloorBlocks={} islandSolidBlocks={} airBlocks={} totalWaterBlocks={} totalSolidFloorBlocks={} totalIslandSolidBlocks={} totalAirBlocks={} centerFloorY={} centerIslandBottomY={} centerOceanWaterBlock='{}' centerOceanFloorBlock='{}' centerGapBlock='{}' centerIslandBottomBlock='{}' status={} lowerOceanTp='/tp @s {} {} {}' skyGapTp='/tp @s {} {} {}' islandBottomTp='/tp @s {} {} {}'",
                    index,
                    chunk.getPos().x,
                    chunk.getPos().z,
                    oceanPlanChunkIndex,
                    skyCoastPlanChunkIndex,
                    counts[0],
                    counts[1],
                    counts[2],
                    counts[3],
                    planName(centerPlan),
                    centerUpperBiome,
                    centerLowerBiome,
                    waterBlocks,
                    solidFloorBlocks,
                    islandSolidBlocks,
                    airBlocks,
                    AFC_TOTAL_WATER_BLOCKS.get(),
                    AFC_TOTAL_SOLID_FLOOR_BLOCKS.get(),
                    AFC_TOTAL_ISLAND_SOLID_BLOCKS.get(),
                    AFC_TOTAL_AIR_BLOCKS.get(),
                    centerFloorY,
                    centerIslandBottomY,
                    centerOceanWaterBlock,
                    centerOceanFloorBlock,
                    centerGapBlock,
                    centerIslandBottomBlock,
                    chunk.getPersistedStatus(),
                    chunk.getPos().getBlockX(8),
                    LOWER_OCEAN_WATER_TOP_Y + 4,
                    chunk.getPos().getBlockZ(8),
                    chunk.getPos().getBlockX(8),
                    SKY_GAP_BOTTOM_Y + 8,
                    chunk.getPos().getBlockZ(8),
                    chunk.getPos().getBlockX(8),
                    centerIslandBottomY,
                    chunk.getPos().getBlockZ(8)
            );
        }

        return waterBlocks > 0 || solidFloorBlocks > 0 || islandSolidBlocks > 0 || airBlocks > 0;
    }

    private static int assignLowerOceanBiomeCellsByPlanUnlocked(
            final ChunkAccess chunk,
            final Holder<Biome> lowerOceanBiome,
            final int minY,
            final int maxY
    ) {
        int assignedCells = 0;

        final boolean[][] quartOcean = new boolean[4][4];

        for (int quartX = 0; quartX < 4; quartX++) {
            for (int quartZ = 0; quartZ < 4; quartZ++) {
                final int localX = quartX * 4 + 2;
                final int localZ = quartZ * 4 + 2;
                final String upperBiomeId = getBiomeIdForColumn(chunk, localX, SURFACE_BIOME_SAMPLE_Y, localZ, minY, maxY);

                quartOcean[quartX][quartZ] = isOceanLikeBiome(upperBiomeId);
            }
        }

        final LevelChunkSection[] sections = chunk.getSections();

        for (int sectionIndex = 0; sectionIndex < sections.length; sectionIndex++) {
            final LevelChunkSection section = sections[sectionIndex];
            final int sectionMinY = minY + sectionIndex * 16;

            PalettedContainer<Holder<Biome>> mutableBiomes = null;
            boolean changed = false;

            for (int quartY = 0; quartY < 4; quartY++) {
                final int quartBlockY = sectionMinY + quartY * 4;

                if (quartBlockY > LOWER_OCEAN_BIOME_TOP_Y || quartBlockY > maxY) {
                    continue;
                }

                for (int quartX = 0; quartX < 4; quartX++) {
                    for (int quartZ = 0; quartZ < 4; quartZ++) {
                        if (!quartOcean[quartX][quartZ]) {
                            continue;
                        }

                        if (mutableBiomes == null) {
                            mutableBiomes = section.getBiomes().recreate();
                        }

                        mutableBiomes.getAndSetUnchecked(quartX, quartY, quartZ, lowerOceanBiome);
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

    private static int[][] buildColumnPlans(final ChunkAccess chunk, final int minY, final int maxY) {
        final boolean[][] ocean = new boolean[16][16];
        final boolean[][] preserve = new boolean[16][16];
        final int[][] plans = new int[16][16];

        for (int localX = 0; localX < 16; localX++) {
            for (int localZ = 0; localZ < 16; localZ++) {
                final String upperBiomeId = getBiomeIdForColumn(chunk, localX, SURFACE_BIOME_SAMPLE_Y, localZ, minY, maxY);

                ocean[localX][localZ] = isOceanLikeBiome(upperBiomeId);
                preserve[localX][localZ] = isRiverOrPreserveBiome(upperBiomeId);
            }
        }

        for (int localX = 0; localX < 16; localX++) {
            for (int localZ = 0; localZ < 16; localZ++) {
                if (preserve[localX][localZ]) {
                    plans[localX][localZ] = PLAN_RIVER_OR_PRESERVE;
                } else if (ocean[localX][localZ]) {
                    plans[localX][localZ] = PLAN_LOWER_OCEAN;
                } else if (isNearOcean(localX, localZ, ocean)) {
                    plans[localX][localZ] = PLAN_SKY_COAST;
                } else {
                    plans[localX][localZ] = PLAN_LAND_INTERIOR;
                }
            }
        }

        return plans;
    }

    private static int[] countPlans(final int[][] plans) {
        final int[] counts = new int[] {0, 0, 0, 0};

        for (int localX = 0; localX < 16; localX++) {
            for (int localZ = 0; localZ < 16; localZ++) {
                final int plan = plans[localX][localZ];

                if (plan == PLAN_LOWER_OCEAN) {
                    counts[0]++;
                } else if (plan == PLAN_SKY_COAST) {
                    counts[1]++;
                } else if (plan == PLAN_LAND_INTERIOR) {
                    counts[2]++;
                } else {
                    counts[3]++;
                }
            }
        }

        return counts;
    }

    private static boolean isNearOcean(final int localX, final int localZ, final boolean[][] ocean) {
        for (int dx = -SKY_COAST_RADIUS_BLOCKS; dx <= SKY_COAST_RADIUS_BLOCKS; dx++) {
            for (int dz = -SKY_COAST_RADIUS_BLOCKS; dz <= SKY_COAST_RADIUS_BLOCKS; dz++) {
                final int x = localX + dx;
                final int z = localZ + dz;

                if (x < 0 || x > 15 || z < 0 || z > 15) {
                    continue;
                }

                if (ocean[x][z]) {
                    return true;
                }
            }
        }

        return false;
    }

    private static boolean isOceanLikeBiome(final String biomeId) {
        if (biomeId == null) {
            return false;
        }

        final String id = biomeId.toLowerCase();

        if (id.contains("dune_sea") || id.contains("river")) {
            return false;
        }

        return id.equals("tfc:ocean")
                || id.contains(":ocean")
                || id.contains("deep_ocean")
                || id.contains("ocean_reef")
                || id.contains("embayment");
    }

    private static boolean isRiverOrPreserveBiome(final String biomeId) {
        if (biomeId == null) {
            return false;
        }

        final String id = biomeId.toLowerCase();

        return id.contains(":river")
                || id.contains("river_")
                || id.contains("_river")
                || id.contains("dune_sea");
    }

    private static String planName(final int plan) {
        return switch (plan) {
            case PLAN_SKY_COAST -> "SKY_COAST";
            case PLAN_LOWER_OCEAN -> "LOWER_OCEAN";
            case PLAN_RIVER_OR_PRESERVE -> "RIVER_OR_PRESERVE";
            default -> "LAND_INTERIOR";
        };
    }

    private static Set<LevelChunkSection> lockSections(final ChunkAccess chunk) {
        final Set<LevelChunkSection> lockedSections = new HashSet<>();

        for (LevelChunkSection section : chunk.getSections()) {
            section.acquire();
            lockedSections.add(section);
        }

        return lockedSections;
    }

    private static void unlockSections(final Set<LevelChunkSection> lockedSections) {
        for (LevelChunkSection section : lockedSections) {
            section.release();
        }
    }

    private static void logExtensionSanityOnce(
            final Registry<Biome> biomeRegistry,
            final Registry<BiomeExtension> extensionRegistry
    ) {
        if (AFC_EXTENSION_SANITY_LOGGED.compareAndSet(false, true)) {
            final boolean biomePresent = biomeRegistry.getHolder(AFCBiomes.LOWER_OCEAN_BIOME_KEY).isPresent();
            final Optional<BiomeExtension> extension = extensionRegistry.getOptional(AFCBiomes.LOWER_OCEAN_ID);

            if (biomePresent && extension.isPresent()) {
                AerofirmacraftTerrain.LOGGER.info(
                        "AFC v29 extension sanity passed: biome={} extension={} registered=true",
                        AFCBiomes.LOWER_OCEAN_BIOME_KEY.location(),
                        AFCBiomes.LOWER_OCEAN_ID
                );
            } else {
                AerofirmacraftTerrain.LOGGER.warn(
                        "AFC v29 extension sanity failed: biomePresent={} extensionPresent={} biome={} extension={}",
                        biomePresent,
                        extension.isPresent(),
                        AFCBiomes.LOWER_OCEAN_BIOME_KEY.location(),
                        AFCBiomes.LOWER_OCEAN_ID
                );
            }
        }
    }

    private static void logDimensionSanityOnce(final int minY, final int maxY) {
        if (AFC_DIMENSION_SANITY_LOGGED.compareAndSet(false, true)) {
            if (minY != EXPECTED_DIMENSION_MIN_Y) {
                AerofirmacraftTerrain.LOGGER.warn(
                        "AFC v29 dimension sanity: expected minY={} but got minY={}. maxY={} lowerOceanWaterTopY={} lowerOceanBiomeTopY={}",
                        EXPECTED_DIMENSION_MIN_Y,
                        minY,
                        maxY,
                        LOWER_OCEAN_WATER_TOP_Y,
                        LOWER_OCEAN_BIOME_TOP_Y
                );
            } else {
                AerofirmacraftTerrain.LOGGER.info(
                        "AFC v29 dimension sanity passed: minY={} maxY={} lowerOceanWaterTopY={} lowerOceanBiomeTopY={} oldTfcMinY={}",
                        minY,
                        maxY,
                        LOWER_OCEAN_WATER_TOP_Y,
                        LOWER_OCEAN_BIOME_TOP_Y,
                        OLD_TFC_MIN_Y
                );
            }
        }
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

    private static BlockState getSaltWaterBlockState() {
        final ResourceLocation saltWaterId = ResourceLocation.fromNamespaceAndPath("tfc", "salt_water");
        final BlockState state = BuiltInRegistries.FLUID.get(saltWaterId)
                .defaultFluidState()
                .createLegacyBlock();

        if (AFC_SALT_WATER_SANITY_LOGGED.compareAndSet(false, true)) {
            AerofirmacraftTerrain.LOGGER.info(
                    "AFC v29 salt water sanity: fluid={} blockState='{}'",
                    saltWaterId,
                    state
            );
        }

        return state;
    }

    private static BlockState pickFloorStateUnlocked(
            final ChunkAccess chunk,
            final int localX,
            final int sampleY,
            final int localZ,
            final int minY
    ) {
        for (int y = clamp(sampleY, minY, OLD_TFC_MIN_Y - 1); y >= minY; y--) {
            final BlockState state = getBlockStateUnlocked(chunk, localX, y, localZ);

            if (isUsableRockState(state)) {
                return state;
            }
        }

        return BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("tfc", "rock/raw/granite"))
                .defaultBlockState();
    }

    private static boolean isUsableRockState(final BlockState state) {
        if (state.isAir() || !state.getFluidState().isEmpty()) {
            return false;
        }

        final ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock());

        return blockId != null
                && "tfc".equals(blockId.getNamespace())
                && blockId.getPath().startsWith("rock/raw/");
    }

    private static int computeOceanFloorY(final int worldX, final int worldZ) {
        final int broad = centeredNoise(Math.floorDiv(worldX, 24), Math.floorDiv(worldZ, 24), 14);
        final int medium = centeredNoise(Math.floorDiv(worldX, 8), Math.floorDiv(worldZ, 8), 6);
        final int fine = centeredNoise(worldX, worldZ, 3);

        return clamp(OCEAN_FLOOR_BASE_Y + broad + medium + fine, OCEAN_FLOOR_MIN_Y, OCEAN_FLOOR_MAX_Y);
    }

    private static int computeIslandBottomY(final int worldX, final int worldZ, final int plan) {
        final int broad = centeredNoise(Math.floorDiv(worldX, 32), Math.floorDiv(worldZ, 32), 10);
        final int medium = centeredNoise(Math.floorDiv(worldX, 12), Math.floorDiv(worldZ, 12), 5);
        final int fine = centeredNoise(worldX, worldZ, 2);

        if (plan == PLAN_SKY_COAST) {
            return clamp(SKY_COAST_BOTTOM_BASE_Y + broad + medium + fine, SKY_COAST_BOTTOM_MIN_Y, SKY_COAST_BOTTOM_MAX_Y);
        }

        return clamp(ISLAND_INTERIOR_BOTTOM_BASE_Y + broad + medium + fine, ISLAND_INTERIOR_BOTTOM_MIN_Y, ISLAND_INTERIOR_BOTTOM_MAX_Y);
    }

    private static int centeredNoise(final int x, final int z, final int radius) {
        if (radius <= 0) {
            return 0;
        }

        return Math.floorMod(hash(x, z), radius * 2 + 1) - radius;
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
