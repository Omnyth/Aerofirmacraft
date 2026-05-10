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
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Mixin(targets = "net.dries007.tfc.world.TFCChunkGenerator", remap = false)
public abstract class TFCChunkGeneratorMixin {
    private static final ResourceKey<Biome> TFC_OCEAN_BIOME_KEY = ResourceKey.create(
            Registries.BIOME,
            ResourceLocation.fromNamespaceAndPath("tfc", "ocean")
    );

    private static final int EXPECTED_DIMENSION_MIN_Y = -128;

    // Old TFC terrain floor. TFC keeps normal terrain generation here and upward.
    private static final int OLD_TFC_MIN_Y = -64;

    // Physical lower ocean band.
    private static final int LOWER_OCEAN_WATER_TOP_Y = OLD_TFC_MIN_Y - 1; // -65

    // Biome cells are 4 blocks tall. This includes the biome cell covering Y=-64..-61.
    private static final int LOWER_OCEAN_BIOME_TOP_Y = OLD_TFC_MIN_Y + 3; // -61

    private static final int LOWER_BEDROCK_LEVELS = 1;
    private static final int LOWER_CRUST_LEVELS = 8;

    private static final int DETAILED_LOG_LIMIT = 8;
    private static final int SUMMARY_LOG_INTERVAL = 512;

    private static final AtomicBoolean AFC_DIMENSION_SANITY_LOGGED = new AtomicBoolean(false);

    private static final AtomicInteger AFC_PRE_NOISE_BIOME_ASSIGN_COUNT = new AtomicInteger();
    private static final AtomicInteger AFC_LOWER_BAND_FILL_COUNT = new AtomicInteger();

    private static final AtomicLong AFC_TOTAL_PRE_NOISE_TFC_OCEAN_CELLS = new AtomicLong();
    private static final AtomicLong AFC_TOTAL_POST_NOISE_TFC_OCEAN_CELLS = new AtomicLong();
    private static final AtomicLong AFC_TOTAL_LOWER_CRUST_BLOCKS = new AtomicLong();
    private static final AtomicLong AFC_TOTAL_LOWER_WATER_BLOCKS = new AtomicLong();

    @Inject(method = "createBiomes", at = @At("RETURN"), cancellable = true)
    private void afc$assignLowerOceanBiomeByYRangeV18(
            final RandomState randomState,
            final Blender blender,
            final StructureManager structureManager,
            final ChunkAccess chunk,
            final CallbackInfoReturnable<CompletableFuture<ChunkAccess>> cir
    ) {
        final CompletableFuture<ChunkAccess> originalFuture = cir.getReturnValue();

        if (originalFuture == null) {
            AerofirmacraftTerrain.LOGGER.warn(
                    "AFC v18 biome: createBiomes returned null future for chunkX={} chunkZ={}",
                    chunk.getPos().x,
                    chunk.getPos().z
            );
            return;
        }

        cir.setReturnValue(originalFuture.thenApply(result -> {
            try {
                final Registry<Biome> biomeRegistry = structureManager.registryAccess().registryOrThrow(Registries.BIOME);
                final Holder<Biome> tfcOceanBiome = biomeRegistry.getHolderOrThrow(TFC_OCEAN_BIOME_KEY);

                assignLowerOceanBiomeLocked(result, tfcOceanBiome, true);
            } catch (Throwable throwable) {
                AerofirmacraftTerrain.LOGGER.error(
                        "AFC v18 biome: lower-ocean biome assignment failed chunkX={} chunkZ={} chunkClass={} chunkStatus={}",
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
    private void afc$fillLowerOceanBandOnlyV18(
            final Blender blender,
            final RandomState randomState,
            final StructureManager structureManager,
            final ChunkAccess chunk,
            final CallbackInfoReturnable<CompletableFuture<ChunkAccess>> cir
    ) {
        final CompletableFuture<ChunkAccess> originalFuture = cir.getReturnValue();

        if (originalFuture == null) {
            AerofirmacraftTerrain.LOGGER.warn(
                    "AFC v18 lower: fillFromNoise returned null future for chunkX={} chunkZ={}",
                    chunk.getPos().x,
                    chunk.getPos().z
            );
            return;
        }

        cir.setReturnValue(originalFuture.thenApply(result -> {
            try {
                final Registry<Biome> biomeRegistry = structureManager.registryAccess().registryOrThrow(Registries.BIOME);
                final Holder<Biome> tfcOceanBiome = biomeRegistry.getHolderOrThrow(TFC_OCEAN_BIOME_KEY);

                fillLowerOceanBandLocked(result, tfcOceanBiome);
            } catch (Throwable throwable) {
                AerofirmacraftTerrain.LOGGER.error(
                        "AFC v18 lower: lower-ocean band fill failed chunkX={} chunkZ={} chunkClass={} chunkStatus={}",
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
            final Holder<Biome> tfcOceanBiome,
            final boolean preNoise
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
            final int minY = chunk.getHeightAccessorForGeneration().getMinBuildHeight();
            final int maxY = chunk.getHeightAccessorForGeneration().getMaxBuildHeight() - 1;

            logDimensionSanityOnce(minY, maxY);

            final int assignedCells = assignTfcOceanBiomeCellsUnlocked(chunk, tfcOceanBiome, minY, maxY);

            if (preNoise) {
                final int index = AFC_PRE_NOISE_BIOME_ASSIGN_COUNT.incrementAndGet();
                AFC_TOTAL_PRE_NOISE_TFC_OCEAN_CELLS.addAndGet(assignedCells);

                if (index <= DETAILED_LOG_LIMIT || index % SUMMARY_LOG_INTERVAL == 0) {
                    AerofirmacraftTerrain.LOGGER.info(
                            "AFC v18 biome: index={} chunkX={} chunkZ={} minY={} tfcOceanCells={} totalPreNoiseTfcOceanCells={} status={}",
                            index,
                            chunk.getPos().x,
                            chunk.getPos().z,
                            minY,
                            assignedCells,
                            AFC_TOTAL_PRE_NOISE_TFC_OCEAN_CELLS.get(),
                            chunk.getPersistedStatus()
                    );
                }
            } else {
                AFC_TOTAL_POST_NOISE_TFC_OCEAN_CELLS.addAndGet(assignedCells);
            }

            chunk.setUnsaved(true);
        } finally {
            for (LevelChunkSection section : lockedSections) {
                section.release();
            }
        }
    }

    private static void fillLowerOceanBandLocked(
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
            final int index = AFC_LOWER_BAND_FILL_COUNT.incrementAndGet();

            final int minY = chunk.getHeightAccessorForGeneration().getMinBuildHeight();
            final int maxY = chunk.getHeightAccessorForGeneration().getMaxBuildHeight() - 1;

            logDimensionSanityOnce(minY, maxY);

            final boolean lowerBandAvailable = minY <= LOWER_OCEAN_WATER_TOP_Y;

            final BlockState bedrock = Blocks.BEDROCK.defaultBlockState();
            final BlockState stone = Blocks.STONE.defaultBlockState();
            final BlockState water = Blocks.WATER.defaultBlockState();

            int crustBlocks = 0;
            int waterBlocks = 0;

            final int crustTopY = minY + LOWER_BEDROCK_LEVELS + LOWER_CRUST_LEVELS - 1;
            final int lowerOceanWaterTopY = Math.min(LOWER_OCEAN_WATER_TOP_Y, maxY);

            if (lowerBandAvailable) {
                for (int localX = 0; localX < 16; localX++) {
                    for (int localZ = 0; localZ < 16; localZ++) {
                        for (int y = minY; y <= lowerOceanWaterTopY; y++) {
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

            final int postNoiseBiomeCells = assignTfcOceanBiomeCellsUnlocked(chunk, tfcOceanBiome, minY, maxY);

            AFC_TOTAL_LOWER_CRUST_BLOCKS.addAndGet(crustBlocks);
            AFC_TOTAL_LOWER_WATER_BLOCKS.addAndGet(waterBlocks);
            AFC_TOTAL_POST_NOISE_TFC_OCEAN_CELLS.addAndGet(postNoiseBiomeCells);

            if (index <= DETAILED_LOG_LIMIT || index % SUMMARY_LOG_INTERVAL == 0) {
                final String centerBiomeAtWaterSurface = getBiomeIdForColumn(chunk, 8, LOWER_OCEAN_WATER_TOP_Y, 8, minY, maxY);
                final String centerBiomeAtBiomeCap = getBiomeIdForColumn(chunk, 8, LOWER_OCEAN_BIOME_TOP_Y, 8, minY, maxY);

                AerofirmacraftTerrain.LOGGER.info(
                        "AFC v18 lower: index={} chunkX={} chunkZ={} minY={} lowerBand={} waterTopY={} biomeTopY={} centerBiomeWaterTop={} centerBiomeBiomeCap={} crustBlocks={} waterBlocks={} postNoiseTfcOceanCells={} totalCrust={} totalWater={} status={} lowerOceanTp='/tp @s {} {} {}'",
                        index,
                        chunk.getPos().x,
                        chunk.getPos().z,
                        minY,
                        lowerBandAvailable,
                        LOWER_OCEAN_WATER_TOP_Y,
                        LOWER_OCEAN_BIOME_TOP_Y,
                        centerBiomeAtWaterSurface,
                        centerBiomeAtBiomeCap,
                        crustBlocks,
                        waterBlocks,
                        postNoiseBiomeCells,
                        AFC_TOTAL_LOWER_CRUST_BLOCKS.get(),
                        AFC_TOTAL_LOWER_WATER_BLOCKS.get(),
                        chunk.getPersistedStatus(),
                        chunk.getPos().getBlockX(8),
                        LOWER_OCEAN_WATER_TOP_Y + 4,
                        chunk.getPos().getBlockZ(8)
                );
            }

            chunk.setUnsaved(true);
        } finally {
            for (LevelChunkSection section : lockedSections) {
                section.release();
            }
        }
    }

    private static void logDimensionSanityOnce(final int minY, final int maxY) {
        if (AFC_DIMENSION_SANITY_LOGGED.compareAndSet(false, true)) {
            if (minY != EXPECTED_DIMENSION_MIN_Y) {
                AerofirmacraftTerrain.LOGGER.warn(
                        "AFC v18 dimension sanity: expected minY={} but got minY={}. maxY={} lowerOceanWaterTopY={} lowerOceanBiomeTopY={}",
                        EXPECTED_DIMENSION_MIN_Y,
                        minY,
                        maxY,
                        LOWER_OCEAN_WATER_TOP_Y,
                        LOWER_OCEAN_BIOME_TOP_Y
                );
            } else {
                AerofirmacraftTerrain.LOGGER.info(
                        "AFC v18 dimension sanity passed: minY={} maxY={} lowerOceanWaterTopY={} lowerOceanBiomeTopY={} oldTfcMinY={}",
                        minY,
                        maxY,
                        LOWER_OCEAN_WATER_TOP_Y,
                        LOWER_OCEAN_BIOME_TOP_Y,
                        OLD_TFC_MIN_Y
                );
            }
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

                if (quartBlockY > LOWER_OCEAN_BIOME_TOP_Y || quartBlockY > maxY) {
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

    private static int clamp(final int value, final int min, final int max) {
        return Math.max(min, Math.min(max, value));
    }
}