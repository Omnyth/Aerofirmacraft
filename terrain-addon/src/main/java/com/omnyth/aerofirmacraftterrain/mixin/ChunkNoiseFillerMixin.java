package com.omnyth.aerofirmacraftterrain.mixin;

import com.omnyth.aerofirmacraftterrain.AerofirmacraftTerrain;
import com.omnyth.aerofirmacraftterrain.world.AFCBiomes;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import net.dries007.tfc.world.BiomeNoiseSampler;
import net.dries007.tfc.world.ChunkNoiseFiller;
import net.dries007.tfc.world.biome.BiomeExtension;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Mixin(value = ChunkNoiseFiller.class, remap = false)
public abstract class ChunkNoiseFillerMixin {
    private static final int AFC_LOWER_OCEAN_MIN_Y = -256;
    private static final int AFC_LOWER_OCEAN_TOP_Y = -193;

    private static final int AFC_DETAILED_LOG_LIMIT = 16;
    private static final int AFC_SUMMARY_LOG_INTERVAL = 4096;

    @Unique
    private static final AtomicInteger afc$lowerOceanNoiseSamples = new AtomicInteger();

    @Shadow
    protected int blockX;

    @Shadow
    protected int blockZ;

    @Shadow
    protected Object2DoubleMap<BiomeNoiseSampler> columnBiomeNoiseSamplers;

    @Shadow
    protected Map<BiomeExtension, BiomeNoiseSampler> biomeNoiseSamplers;

    @Inject(method = "calculateNoiseAtHeight", at = @At("HEAD"), cancellable = true)
    private void afc$useLowerOceanNoiseInLowerBand(
            final int y,
            final double originalColumnSurfaceHeight,
            final CallbackInfoReturnable<Double> cir
    ) {
        if (y < AFC_LOWER_OCEAN_MIN_Y || y > AFC_LOWER_OCEAN_TOP_Y) {
            return;
        }

        final BiomeExtension lowerOceanExtension = AFCBiomes.LOWER_OCEAN_EXTENSION.get();
        final BiomeNoiseSampler lowerOceanSampler = biomeNoiseSamplers.get(lowerOceanExtension);

        if (lowerOceanSampler == null) {
            return;
        }

        lowerOceanSampler.setColumn(blockX, blockZ);

        final double lowerOceanSurfaceHeight = lowerOceanSampler.height();

        // This mirrors the core shape of TFC's ChunkNoiseFiller#calculateNoiseAtHeight,
        // but uses only the lower_ocean sampler rather than the upper X/Z column biome weights.
        double density = 0.4D - lowerOceanSampler.noise(y);

        if (y > lowerOceanSurfaceHeight) {
            density -= (y - lowerOceanSurfaceHeight) * 0.20000000298023224D;
        }

        density = Mth.clamp(density, -1.0D, 1.0D);

        final int sampleIndex = afc$lowerOceanNoiseSamples.incrementAndGet();

        if (sampleIndex <= AFC_DETAILED_LOG_LIMIT || sampleIndex % AFC_SUMMARY_LOG_INTERVAL == 0) {
            AerofirmacraftTerrain.LOGGER.info(
                    "AFC v23 lower-noise: sample={} x={} y={} z={} lowerOceanSurfaceHeight={} density={} originalColumnSurfaceHeight={}",
                    sampleIndex,
                    blockX,
                    y,
                    blockZ,
                    lowerOceanSurfaceHeight,
                    density,
                    originalColumnSurfaceHeight
            );
        }

        cir.setReturnValue(density);
    }
}