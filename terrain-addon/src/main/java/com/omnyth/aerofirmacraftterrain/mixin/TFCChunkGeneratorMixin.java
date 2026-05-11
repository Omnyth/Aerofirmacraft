package com.omnyth.aerofirmacraftterrain.mixin;

import com.omnyth.aerofirmacraftterrain.AerofirmacraftTerrain;
import com.omnyth.aerofirmacraftterrain.config.AFCGenerationBands;
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
    private static final AtomicBoolean AFC_V34_LOGGED_FIRST_CLAMP = new AtomicBoolean(false);

    @Unique
    private static final AtomicInteger AFC_V34_CLAMPED_CHUNKS = new AtomicInteger();

    @Inject(
            method = "createNoiseSamplingSettingsForChunk(Lnet/minecraft/world/level/ChunkPos;Lnet/minecraft/world/level/LevelHeightAccessor;)Lnet/dries007/tfc/world/noise/ChunkNoiseSamplingSettings;",
            at = @At("RETURN"),
            cancellable = true
    )
    private void afc$clampTFCNoiseSamplingRangeV34(
            final ChunkPos chunkPos,
            final LevelHeightAccessor heightAccessor,
            final CallbackInfoReturnable<ChunkNoiseSamplingSettings> cir
    ) {
        final ChunkNoiseSamplingSettings original = cir.getReturnValue();

        if (original == null) {
            return;
        }

        final int originalMinY = original.minY();
        final int originalMaxY = original.minY() + original.cellCountY() * original.cellHeight();

        final int clampedMinY = Math.max(originalMinY, AFCGenerationBands.TFC_GENERATION_MIN_Y);

        if (clampedMinY == originalMinY) {
            return;
        }

        final int clampedCellCountY = Math.max(
                0,
                Math.floorDiv(originalMaxY - clampedMinY, original.cellHeight())
        );

        final int clampedFirstCellY = Math.floorDiv(clampedMinY, original.cellHeight());

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

        final int index = AFC_V34_CLAMPED_CHUNKS.incrementAndGet();

        if (AFC_V34_LOGGED_FIRST_CLAMP.compareAndSet(false, true) || index <= 12 || index % 2048 == 0) {
            AerofirmacraftTerrain.LOGGER.info(
                    "AFC v34 TFC generation band clamp: index={} chunkX={} chunkZ={} originalMinY={} originalMaxY={} clampedMinY={} clampedMaxY={} originalCellCountY={} clampedCellCountY={} cellHeight={} worldMinY={} worldMaxY={}",
                    index,
                    chunkPos.x,
                    chunkPos.z,
                    originalMinY,
                    originalMaxY,
                    clamped.minY(),
                    clamped.minY() + clamped.cellCountY() * clamped.cellHeight(),
                    original.cellCountY(),
                    clamped.cellCountY(),
                    clamped.cellHeight(),
                    heightAccessor.getMinBuildHeight(),
                    heightAccessor.getMaxBuildHeight() - 1
            );
        }
    }
}