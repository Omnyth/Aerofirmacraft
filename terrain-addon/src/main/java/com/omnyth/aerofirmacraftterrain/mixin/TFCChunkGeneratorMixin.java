package com.omnyth.aerofirmacraftterrain.mixin;

import com.omnyth.aerofirmacraftterrain.AerofirmacraftTerrain;
import com.omnyth.aerofirmacraftterrain.world.AFCBiomes;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
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
    @Unique
    private static final int AFC_LOWER_OCEAN_BIOME_TOP_Y = -189;

    @Unique
    private static final AtomicBoolean AFC_V33B_BIOME_LOGGED = new AtomicBoolean(false);

    @Unique
    private static final AtomicInteger AFC_V33B_BIOME_CHUNKS = new AtomicInteger();

    @Unique
    private static final AtomicLong AFC_V33B_TOTAL_BIOME_CELLS = new AtomicLong();

    @Inject(method = "createBiomes", at = @At("RETURN"), cancellable = true)
    private void afc$createLayeredBiomesV33b(
            final RandomState randomState,
            final Blender blender,
            final StructureManager structureManager,
            final ChunkAccess chunk,
            final CallbackInfoReturnable<CompletableFuture<ChunkAccess>> cir
    ) {
        final CompletableFuture<ChunkAccess> originalFuture = cir.getReturnValue();

        if (originalFuture == null) {
            return;
        }

        cir.setReturnValue(originalFuture.thenApply(result -> {
            final Registry<Biome> biomeRegistry = structureManager.registryAccess().registryOrThrow(Registries.BIOME);
            final Holder<Biome> lowerOceanBiome = biomeRegistry.getHolderOrThrow(AFCBiomes.LOWER_OCEAN_BIOME_KEY);

            final int assignedCells = afc$assignLowerOceanBiomeCells(result, lowerOceanBiome);

            if (assignedCells > 0) {
                result.setUnsaved(true);
            }

            final int chunkIndex = AFC_V33B_BIOME_CHUNKS.incrementAndGet();
            final long totalCells = AFC_V33B_TOTAL_BIOME_CELLS.addAndGet(assignedCells);

            if (AFC_V33B_BIOME_LOGGED.compareAndSet(false, true) || chunkIndex <= 8 || chunkIndex % 2048 == 0) {
                AerofirmacraftTerrain.LOGGER.info(
                        "AFC v33b biome layer active: chunkIndex={} chunkX={} chunkZ={} assignedLowerOceanCells={} totalLowerOceanCells={} lowerOceanBiomeTopY={} status={}",
                        chunkIndex,
                        result.getPos().x,
                        result.getPos().z,
                        assignedCells,
                        totalCells,
                        AFC_LOWER_OCEAN_BIOME_TOP_Y,
                        result.getPersistedStatus()
                );
            }

            return result;
        }));
    }

    @Unique
    private static int afc$assignLowerOceanBiomeCells(
            final ChunkAccess chunk,
            final Holder<Biome> lowerOceanBiome
    ) {
        int assignedCells = 0;

        final int minY = chunk.getHeightAccessorForGeneration().getMinBuildHeight();
        final int maxY = chunk.getHeightAccessorForGeneration().getMaxBuildHeight() - 1;
        final LevelChunkSection[] sections = chunk.getSections();
        final Set<LevelChunkSection> lockedSections = new HashSet<>();

        try {
            for (LevelChunkSection section : sections) {
                section.acquire();
                lockedSections.add(section);
            }

            for (int sectionIndex = 0; sectionIndex < sections.length; sectionIndex++) {
                final LevelChunkSection section = sections[sectionIndex];
                final int sectionMinY = minY + sectionIndex * 16;

                @SuppressWarnings("unchecked")
                final PalettedContainer<Holder<Biome>> mutableBiomes =
                        (PalettedContainer<Holder<Biome>>) (Object) section.getBiomes();

                for (int quartY = 0; quartY < 4; quartY++) {
                    final int quartBlockY = sectionMinY + quartY * 4;

                    if (quartBlockY > AFC_LOWER_OCEAN_BIOME_TOP_Y || quartBlockY > maxY) {
                        continue;
                    }

                    for (int quartX = 0; quartX < 4; quartX++) {
                        for (int quartZ = 0; quartZ < 4; quartZ++) {
                            mutableBiomes.getAndSetUnchecked(quartX, quartY, quartZ, lowerOceanBiome);
                            assignedCells++;
                        }
                    }
                }
            }
        } finally {
            for (LevelChunkSection section : lockedSections) {
                section.release();
            }
        }

        return assignedCells;
    }
}