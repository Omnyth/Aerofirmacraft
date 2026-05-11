package com.omnyth.tfcgenerationbands.mixin;

import com.omnyth.tfcgenerationbands.TFCGenerationBands;
import com.omnyth.tfcgenerationbands.config.TFCGenerationBandsConfig;
import net.dries007.tfc.world.noise.ChunkNoiseSamplingSettings;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Mixin(targets = "net.dries007.tfc.world.TFCChunkGenerator", remap = false)
public abstract class TFCChunkGeneratorMixin {
    @Unique
    private static final AtomicBoolean TFC_BANDS_LOGGED_FIRST_RANGE = new AtomicBoolean(false);

    @Unique
    private static final AtomicInteger TFC_BANDS_RANGED_CHUNKS = new AtomicInteger();

    @Inject(
            method = "createNoiseSamplingSettingsForChunk(Lnet/minecraft/world/level/ChunkPos;Lnet/minecraft/world/level/LevelHeightAccessor;)Lnet/dries007/tfc/world/noise/ChunkNoiseSamplingSettings;",
            at = @At("RETURN"),
            cancellable = true
    )
    private void tfcGenerationBands$clampTFCNoiseSamplingRange(
            final ChunkPos chunkPos,
            final LevelHeightAccessor heightAccessor,
            final CallbackInfoReturnable<ChunkNoiseSamplingSettings> cir
    ) {
        final ChunkNoiseSamplingSettings original = cir.getReturnValue();

        if (original == null || !TFCGenerationBandsConfig.enabled()) {
            return;
        }

        final int originalMinY = original.minY();
        final int originalMaxYExclusive = original.minY() + original.cellCountY() * original.cellHeight();

        final int configuredMinY = TFCGenerationBandsConfig.tfcGenerationMinY();
        final int configuredMaxYInclusive = TFCGenerationBandsConfig.tfcGenerationMaxY();
        final int configuredMaxYExclusive = configuredMaxYInclusive + 1;

        final int clampedMinY = Math.max(originalMinY, configuredMinY);
        final int clampedMaxYExclusive = Math.min(originalMaxYExclusive, configuredMaxYExclusive);

        if (clampedMinY >= clampedMaxYExclusive) {
            TFCGenerationBands.LOGGER.warn(
                    "TFC Generation Bands produced an empty TFC generation range for chunk ({}, {}). Keeping original range. original={}..{} configured={}..{}",
                    chunkPos.x,
                    chunkPos.z,
                    originalMinY,
                    originalMaxYExclusive - 1,
                    configuredMinY,
                    configuredMaxYInclusive
            );
            return;
        }

        final int clampedCellCountY = Math.max(
                1,
                Math.floorDiv(clampedMaxYExclusive - clampedMinY, original.cellHeight())
        );

        final int effectiveMaxYExclusive = clampedMinY + clampedCellCountY * original.cellHeight();
        final int clampedFirstCellY = Math.floorDiv(clampedMinY, original.cellHeight());

        final boolean changed = clampedMinY != originalMinY || effectiveMaxYExclusive != originalMaxYExclusive;

        if (!changed) {
            final int index = TFC_BANDS_RANGED_CHUNKS.incrementAndGet();

            if (TFC_BANDS_LOGGED_FIRST_RANGE.compareAndSet(false, true) || index <= 6) {
                TFCGenerationBands.LOGGER.info(
                        "TFC Generation Bands range unchanged: index={} chunkX={} chunkZ={} originalRange={}..{} configuredRange={}..{} cellHeight={} worldRange={}..{}",
                        index,
                        chunkPos.x,
                        chunkPos.z,
                        originalMinY,
                        originalMaxYExclusive - 1,
                        configuredMinY,
                        configuredMaxYInclusive,
                        original.cellHeight(),
                        heightAccessor.getMinBuildHeight(),
                        heightAccessor.getMaxBuildHeight() - 1
                );
            }

            return;
        }

        final ChunkNoiseSamplingSettings clamped = new ChunkNoiseSamplingSettings(
                clampedMinY,
                original.cellCountXZ(),
                clampedCellCountY,
                original.cellWidth(),
                original.cellHeight(),
                original.firstCellX(),
                clampedFirstCellY,
                original.firstCellZ()
        );

        cir.setReturnValue(clamped);

        final int index = TFC_BANDS_RANGED_CHUNKS.incrementAndGet();

        if (TFC_BANDS_LOGGED_FIRST_RANGE.compareAndSet(false, true) || index <= 12 || index % 2048 == 0) {
            TFCGenerationBands.LOGGER.info(
                    "TFC Generation Bands clamp: index={} chunkX={} chunkZ={} originalRange={}..{} configuredRange={}..{} resultRange={}..{} originalCellCountY={} resultCellCountY={} cellHeight={} worldRange={}..{}",
                    index,
                    chunkPos.x,
                    chunkPos.z,
                    originalMinY,
                    originalMaxYExclusive - 1,
                    configuredMinY,
                    configuredMaxYInclusive,
                    clamped.minY(),
                    clamped.minY() + clamped.cellCountY() * clamped.cellHeight() - 1,
                    original.cellCountY(),
                    clamped.cellCountY(),
                    clamped.cellHeight(),
                    heightAccessor.getMinBuildHeight(),
                    heightAccessor.getMaxBuildHeight() - 1
            );
        }
    }
}