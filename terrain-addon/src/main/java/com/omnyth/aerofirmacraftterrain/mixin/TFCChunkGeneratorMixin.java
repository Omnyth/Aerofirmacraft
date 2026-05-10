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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Mixin(targets = "net.dries007.tfc.world.TFCChunkGenerator", remap = false)
public abstract class TFCChunkGeneratorMixin {
    private static final ResourceKey<Biome> TFC_OCEAN_BIOME_KEY = ResourceKey.create(
            Registries.BIOME,
            ResourceLocation.fromNamespaceAndPath("tfc", "ocean")
    );

    private static final int LOWER_OCEAN_TOP_Y = 0;
    private static final int LOWER_OCEAN_BOTTOM_Y = -56;

    private static final int DETAILED_LOG_LIMIT = 16;
    private static final int SUMMARY_LOG_INTERVAL = 128;

    private static final AtomicInteger AFC_PRE_NOISE_ASSIGN_COUNT = new AtomicInteger();
    private static final AtomicInteger AFC_POST_NOISE_PROBE_COUNT = new AtomicInteger();

    private static final AtomicLong AFC_TOTAL_PRE_NOISE_TFC_OCEAN_CELLS = new AtomicLong();
    private static final AtomicLong AFC_TOTAL_LOWER_WATER_BLOCKS = new AtomicLong();
    private static final AtomicLong AFC_TOTAL_LOWER_NON_AIR_BLOCKS = new AtomicLong();

    @Inject(method = "createBiomes", at = @At("RETURN"), cancellable = true)
    private void afc$preAssignTfcOceanBelowY0(
            final RandomState randomState,
            final Blender blender,
            final StructureManager structureManager,
            final ChunkAccess chunk,
            final CallbackInfoReturnable<CompletableFuture<ChunkAccess>> cir
    ) {
        final CompletableFuture<ChunkAccess> originalFuture = cir.getReturnValue();

        if (originalFuture == null) {
            AerofirmacraftTerrain.LOGGER.warn(
                    "AFC biome-prenoise v12: createBiomes returned null future for chunkX={} chunkZ={}",
                    chunk.getPos().x,
                    chunk.getPos().z
            );
            return;
        }

        cir.setReturnValue(originalFuture.thenApply(result -> {
            try {
                final Registry<Biome> biomeRegistry = structureManager.registryAccess().registryOrThrow(Registries.BIOME);
                final Holder<Biome> tfcOceanBiome = biomeRegistry.getHolderOrThrow(TFC_OCEAN_BIOME_KEY);

                preAssignLowerOceanBiomeLocked(result, tfcOceanBiome);
            } catch (Throwable throwable) {
                AerofirmacraftTerrain.LOGGER.error(
                        "AFC biome-prenoise v12: pre-noise biome assignment failed chunkX={} chunkZ={} chunkClass={} chunkStatus={}",
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
    private void afc$postNoiseProbeOnly(
            final Blender blender,
            final RandomState randomState,
            final StructureManager structureManager,
            final ChunkAccess chunk,
            final CallbackInfoReturnable<CompletableFuture<ChunkAccess>> cir
    ) {
        final CompletableFuture<ChunkAccess> originalFuture = cir.getReturnValue();

        if (originalFuture == null) {
            AerofirmacraftTerrain.LOGGER.warn(
                    "AFC biome-prenoise v12: fillFromNoise returned null future for chunkX={} chunkZ={}",
                    chunk.getPos().x,
                    chunk.getPos().z
            );
            return;
        }

        cir.setReturnValue(originalFuture.thenApply(result -> {
            try {
                postNoiseProbeLocked(result);
            } catch (Throwable throwable) {
                AerofirmacraftTerrain.LOGGER.error(
                        "AFC biome-prenoise v12: post-noise probe failed chunkX={} chunkZ={} chunkClass={} chunkStatus={}",
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

    private static void preAssignLowerOceanBiomeLocked(
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
            final int minY = chunk.getHeightAccessorForGeneration().getMinBuildHeight();
            final int maxY = chunk.getHeightAccessorForGeneration().getMaxBuildHeight() - 1;

            final int assignedCells = assignTfcOceanBiomeCellsUnlocked(chunk, tfcOceanBiome, minY, maxY);
            final int index = AFC_PRE_NOISE_ASSIGN_COUNT.incrementAndGet();

            AFC_TOTAL_PRE_NOISE_TFC_OCEAN_CELLS.addAndGet(assignedCells);

            if (index <= DETAILED_LOG_LIMIT || index % SUMMARY_LOG_INTERVAL == 0) {
                AerofirmacraftTerrain.LOGGER.info(
                        "AFC biome-prenoise v12: pre-noise assigned index={} chunkX={} chunkZ={} tfcOceanBiomeCells={} totalTfcOceanBiomeCells={} chunkStatus={} chunkClass={}",
                        index,
                        chunk.getPos().x,
                        chunk.getPos().z,
                        assignedCells,
                        AFC_TOTAL_PRE_NOISE_TFC_OCEAN_CELLS.get(),
                        chunk.getPersistedStatus(),
                        chunk.getClass().getName()
                );
            }

            chunk.setUnsaved(true);
        } finally {
            for (LevelChunkSection section : lockedSections) {
                section.release();
            }
        }
    }

    private static void postNoiseProbeLocked(final ChunkAccess chunk) {
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

            final int centerSurfaceY = findTopNonAirY(chunk, 8, 8, minY, maxY);
            final String centerSurfaceBiome = getBiomeIdForColumn(chunk, 8, centerSurfaceY, 8, minY, maxY);
            final String centerLowerBiome = getBiomeIdForColumn(chunk, 8, LOWER_OCEAN_TOP_Y, 8, minY, maxY);

            int lowerWaterBlocks = 0;
            int lowerNonAirBlocks = 0;

            for (int localX = 0; localX < 16; localX++) {
                for (int localZ = 0; localZ < 16; localZ++) {
                    for (int y = Math.max(minY, LOWER_OCEAN_BOTTOM_Y); y <= Math.min(maxY, LOWER_OCEAN_TOP_Y); y++) {
                        final BlockState state = getBlockStateUnlocked(chunk, localX, y, localZ);

                        if (!state.isAir()) {
                            lowerNonAirBlocks++;
                        }

                        if (state.is(Blocks.WATER)) {
                            lowerWaterBlocks++;
                        }
                    }
                }
            }

            AFC_TOTAL_LOWER_WATER_BLOCKS.addAndGet(lowerWaterBlocks);
            AFC_TOTAL_LOWER_NON_AIR_BLOCKS.addAndGet(lowerNonAirBlocks);

            if (index <= DETAILED_LOG_LIMIT || index % SUMMARY_LOG_INTERVAL == 0) {
                AerofirmacraftTerrain.LOGGER.info(
                        "AFC biome-prenoise v12: post-noise probe index={} chunkX={} chunkZ={} centerSurfaceY={} centerSurfaceBiome={} centerLowerBiome={} lowerWaterBlocks={} lowerNonAirBlocks={} totalLowerWaterBlocks={} totalLowerNonAirBlocks={} chunkStatus={} chunkClass={} surfaceTp='/tp @s {} {} {}' lowerTp='/tp @s {} {} {}'",
                        index,
                        chunk.getPos().x,
                        chunk.getPos().z,
                        centerSurfaceY,
                        centerSurfaceBiome,
                        centerLowerBiome,
                        lowerWaterBlocks,
                        lowerNonAirBlocks,
                        AFC_TOTAL_LOWER_WATER_BLOCKS.get(),
                        AFC_TOTAL_LOWER_NON_AIR_BLOCKS.get(),
                        chunk.getPersistedStatus(),
                        chunk.getClass().getName(),
                        chunk.getPos().getBlockX(8),
                        centerSurfaceY + 12,
                        chunk.getPos().getBlockZ(8),
                        chunk.getPos().getBlockX(8),
                        LOWER_OCEAN_TOP_Y + 4,
                        chunk.getPos().getBlockZ(8)
                );
            }
        } finally {
            for (LevelChunkSection section : lockedSections) {
                section.release();
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

    private static int clamp(final int value, final int min, final int max) {
        return Math.max(min, Math.min(max, value));
    }
}