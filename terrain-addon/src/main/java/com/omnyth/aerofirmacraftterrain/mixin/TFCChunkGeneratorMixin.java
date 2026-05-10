package com.omnyth.aerofirmacraftterrain.mixin;

import com.omnyth.aerofirmacraftterrain.AerofirmacraftTerrain;
import com.omnyth.aerofirmacraftterrain.mixin.accessor.LevelChunkSectionAccessor;
import com.omnyth.aerofirmacraftterrain.world.AFCBiomes;
import net.dries007.tfc.world.biome.BiomeExtension;
import net.dries007.tfc.world.biome.TFCBiomes;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.Biome;
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
    private static final int EXPECTED_DIMENSION_MIN_Y = -128;
    private static final int OLD_TFC_MIN_Y = -64;

    // V20 does not place water/stone/bedrock here.
    // It only applies the real custom biome and probes what generation does.
    private static final int LOWER_OCEAN_WATER_TOP_Y = OLD_TFC_MIN_Y - 1; // -65
    private static final int LOWER_OCEAN_BIOME_TOP_Y = OLD_TFC_MIN_Y + 3; // -61

    private static final int DETAILED_LOG_LIMIT = 8;
    private static final int SUMMARY_LOG_INTERVAL = 512;

    private static final AtomicBoolean AFC_DIMENSION_SANITY_LOGGED = new AtomicBoolean(false);
    private static final AtomicBoolean AFC_EXTENSION_SANITY_LOGGED = new AtomicBoolean(false);

    private static final AtomicInteger AFC_PRE_NOISE_BIOME_ASSIGN_COUNT = new AtomicInteger();
    private static final AtomicInteger AFC_POST_NOISE_PROBE_COUNT = new AtomicInteger();

    private static final AtomicLong AFC_TOTAL_PRE_NOISE_LOWER_OCEAN_CELLS = new AtomicLong();
    private static final AtomicLong AFC_TOTAL_POST_NOISE_LOWER_OCEAN_CELLS = new AtomicLong();

    private static final AtomicLong AFC_TOTAL_LOWER_AIR_BLOCKS = new AtomicLong();
    private static final AtomicLong AFC_TOTAL_LOWER_NON_AIR_BLOCKS = new AtomicLong();
    private static final AtomicLong AFC_TOTAL_LOWER_FLUID_BLOCKS = new AtomicLong();

    @Inject(method = "createBiomes", at = @At("RETURN"), cancellable = true)
    private void afc$assignRealLowerOceanCreateBiomesV20(
            final RandomState randomState,
            final Blender blender,
            final StructureManager structureManager,
            final ChunkAccess chunk,
            final CallbackInfoReturnable<CompletableFuture<ChunkAccess>> cir
    ) {
        final CompletableFuture<ChunkAccess> originalFuture = cir.getReturnValue();

        if (originalFuture == null) {
            AerofirmacraftTerrain.LOGGER.warn(
                    "AFC v20 biome: createBiomes returned null future for chunkX={} chunkZ={}",
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
                        "AFC v20 biome: pre-noise lower_ocean biome assignment failed chunkX={} chunkZ={} chunkClass={} chunkStatus={}",
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
    private void afc$probeRealLowerOceanNoBlockFillV20(
            final Blender blender,
            final RandomState randomState,
            final StructureManager structureManager,
            final ChunkAccess chunk,
            final CallbackInfoReturnable<CompletableFuture<ChunkAccess>> cir
    ) {
        final CompletableFuture<ChunkAccess> originalFuture = cir.getReturnValue();

        if (originalFuture == null) {
            AerofirmacraftTerrain.LOGGER.warn(
                    "AFC v20 probe: fillFromNoise returned null future for chunkX={} chunkZ={}",
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
                probeLowerBandLocked(result);
            } catch (Throwable throwable) {
                AerofirmacraftTerrain.LOGGER.error(
                        "AFC v20 probe: lower_ocean probe failed chunkX={} chunkZ={} chunkClass={} chunkStatus={}",
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

        final Set<LevelChunkSection> lockedSections = new HashSet<>();

        for (LevelChunkSection section : chunk.getSections()) {
            section.acquire();
            lockedSections.add(section);
        }

        try {
            final int minY = chunk.getHeightAccessorForGeneration().getMinBuildHeight();
            final int maxY = chunk.getHeightAccessorForGeneration().getMaxBuildHeight() - 1;

            logDimensionSanityOnce(minY, maxY);

            final int assignedCells = assignBiomeCellsUnlocked(chunk, lowerOceanBiome, minY, maxY);

            if (preNoise) {
                final int index = AFC_PRE_NOISE_BIOME_ASSIGN_COUNT.incrementAndGet();
                AFC_TOTAL_PRE_NOISE_LOWER_OCEAN_CELLS.addAndGet(assignedCells);

                if (index <= DETAILED_LOG_LIMIT || index % SUMMARY_LOG_INTERVAL == 0) {
                    AerofirmacraftTerrain.LOGGER.info(
                            "AFC v20 biome: index={} chunkX={} chunkZ={} minY={} lowerOceanCells={} totalPreNoiseLowerOceanCells={} status={}",
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

            chunk.setUnsaved(true);
        } finally {
            for (LevelChunkSection section : lockedSections) {
                section.release();
            }
        }
    }

    private static void probeLowerBandLocked(final ChunkAccess chunk) {
        if (!(chunk instanceof ProtoChunk)) {
            return;
        }

        final Set<LevelChunkSection> lockedSections = new HashSet<>();

        for (LevelChunkSection section : chunk.getSections()) {
            section.acquire();
            lockedSections.add(section);
        }

        try {
            final int index = AFC_POST_NOISE_PROBE_COUNT.incrementAndGet();

            final int minY = chunk.getHeightAccessorForGeneration().getMinBuildHeight();
            final int maxY = chunk.getHeightAccessorForGeneration().getMaxBuildHeight() - 1;

            logDimensionSanityOnce(minY, maxY);

            int lowerAirBlocks = 0;
            int lowerNonAirBlocks = 0;
            int lowerFluidBlocks = 0;

            final int probeBottomY = minY;
            final int probeTopY = Math.min(LOWER_OCEAN_WATER_TOP_Y, maxY);

            for (int localX = 0; localX < 16; localX++) {
                for (int localZ = 0; localZ < 16; localZ++) {
                    for (int y = probeBottomY; y <= probeTopY; y++) {
                        final BlockState state = getBlockStateUnlocked(chunk, localX, y, localZ);

                        if (state.isAir()) {
                            lowerAirBlocks++;
                        } else {
                            lowerNonAirBlocks++;
                        }

                        if (!state.getFluidState().isEmpty()) {
                            lowerFluidBlocks++;
                        }
                    }
                }
            }

            AFC_TOTAL_LOWER_AIR_BLOCKS.addAndGet(lowerAirBlocks);
            AFC_TOTAL_LOWER_NON_AIR_BLOCKS.addAndGet(lowerNonAirBlocks);
            AFC_TOTAL_LOWER_FLUID_BLOCKS.addAndGet(lowerFluidBlocks);

            if (index <= DETAILED_LOG_LIMIT || index % SUMMARY_LOG_INTERVAL == 0) {
                final String centerBiomeAtWaterTop = getBiomeIdForColumn(chunk, 8, LOWER_OCEAN_WATER_TOP_Y, 8, minY, maxY);
                final String centerBiomeAtBiomeCap = getBiomeIdForColumn(chunk, 8, LOWER_OCEAN_BIOME_TOP_Y, 8, minY, maxY);

                final BlockState centerBottomBlock = getBlockStateUnlocked(chunk, 8, minY, 8);
                final BlockState centerMidBlock = getBlockStateUnlocked(chunk, 8, clamp(-96, minY, maxY), 8);
                final BlockState centerTopBlock = getBlockStateUnlocked(chunk, 8, clamp(LOWER_OCEAN_WATER_TOP_Y, minY, maxY), 8);

                AerofirmacraftTerrain.LOGGER.info(
                        "AFC v20 probe: index={} chunkX={} chunkZ={} minY={} probeY={}..{} centerBiomeWaterTop={} centerBiomeBiomeCap={} lowerAirBlocks={} lowerNonAirBlocks={} lowerFluidBlocks={} totalLowerAirBlocks={} totalLowerNonAirBlocks={} totalLowerFluidBlocks={} centerBottomBlock='{}' centerMidBlock='{}' centerTopBlock='{}' status={} lowerBandTp='/tp @s {} {} {}'",
                        index,
                        chunk.getPos().x,
                        chunk.getPos().z,
                        minY,
                        probeBottomY,
                        probeTopY,
                        centerBiomeAtWaterTop,
                        centerBiomeAtBiomeCap,
                        lowerAirBlocks,
                        lowerNonAirBlocks,
                        lowerFluidBlocks,
                        AFC_TOTAL_LOWER_AIR_BLOCKS.get(),
                        AFC_TOTAL_LOWER_NON_AIR_BLOCKS.get(),
                        AFC_TOTAL_LOWER_FLUID_BLOCKS.get(),
                        centerBottomBlock,
                        centerMidBlock,
                        centerTopBlock,
                        chunk.getPersistedStatus(),
                        chunk.getPos().getBlockX(8),
                        LOWER_OCEAN_WATER_TOP_Y + 4,
                        chunk.getPos().getBlockZ(8)
                );
            }
        } finally {
            for (LevelChunkSection section : lockedSections) {
                section.release();
            }
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
                        "AFC v20 extension sanity passed: biome={} extension={} registered=true",
                        AFCBiomes.LOWER_OCEAN_BIOME_KEY.location(),
                        AFCBiomes.LOWER_OCEAN_ID
                );
            } else {
                AerofirmacraftTerrain.LOGGER.warn(
                        "AFC v20 extension sanity failed: biomePresent={} extensionPresent={} biome={} extension={}",
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
                        "AFC v20 dimension sanity: expected minY={} but got minY={}. maxY={} lowerOceanWaterTopY={} lowerOceanBiomeTopY={}",
                        EXPECTED_DIMENSION_MIN_Y,
                        minY,
                        maxY,
                        LOWER_OCEAN_WATER_TOP_Y,
                        LOWER_OCEAN_BIOME_TOP_Y
                );
            } else {
                AerofirmacraftTerrain.LOGGER.info(
                        "AFC v20 dimension sanity passed: minY={} maxY={} lowerOceanWaterTopY={} lowerOceanBiomeTopY={} oldTfcMinY={}",
                        minY,
                        maxY,
                        LOWER_OCEAN_WATER_TOP_Y,
                        LOWER_OCEAN_BIOME_TOP_Y,
                        OLD_TFC_MIN_Y
                );
            }
        }
    }

    private static int assignBiomeCellsUnlocked(
            final ChunkAccess chunk,
            final Holder<Biome> biome,
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

                        mutableBiomes.getAndSetUnchecked(quartX, quartY, quartZ, biome);
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

    private static BlockState getBlockStateUnlocked(
            final ChunkAccess chunk,
            final int localX,
            final int y,
            final int localZ
    ) {
        final LevelChunkSection section = chunk.getSection(chunk.getSectionIndex(y));
        return section.getBlockState(localX & 15, y & 15, localZ & 15);
    }

    private static int clamp(final int value, final int min, final int max) {
        return Math.max(min, Math.min(max, value));
    }
}