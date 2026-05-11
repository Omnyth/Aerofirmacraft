package com.omnyth.aerofirmacraftterrain.mixin;

import com.omnyth.aerofirmacraftterrain.AerofirmacraftTerrain;
import com.omnyth.aerofirmacraftterrain.world.AFCBiomes;
import net.dries007.tfc.world.NoopClimateSampler;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

@Mixin(targets = "net.dries007.tfc.world.TFCChunkGenerator", remap = false)
public abstract class TFCChunkGeneratorMixin {
    @Unique
    private static final int AFC_LOWER_OCEAN_BIOME_TOP_Y = -189;

    @Unique
    private static final AtomicBoolean AFC_V33_BIOME_LOGGED = new AtomicBoolean(false);

    @Inject(method = "createBiomes", at = @At("RETURN"), cancellable = true)
    private void afc$createLayeredBiomesV33(
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

            result.fillBiomesFromNoise((quartX, quartY, quartZ, sampler) -> {
                final int blockY = quartY << 2;

                if (blockY <= AFC_LOWER_OCEAN_BIOME_TOP_Y) {
                    return lowerOceanBiome;
                }

                return result.getNoiseBiome(quartX, quartY, quartZ);
            }, NoopClimateSampler.INSTANCE);

            if (AFC_V33_BIOME_LOGGED.compareAndSet(false, true)) {
                AerofirmacraftTerrain.LOGGER.info(
                        "AFC v33 biome layer active: lower_ocean assigned where blockY <= {} using ChunkAccess.fillBiomesFromNoise",
                        AFC_LOWER_OCEAN_BIOME_TOP_Y
                );
            }

            result.setUnsaved(true);
            return result;
        }));
    }
}