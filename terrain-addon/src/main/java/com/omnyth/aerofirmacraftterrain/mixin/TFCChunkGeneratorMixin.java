package com.omnyth.aerofirmacraftterrain.mixin;

import com.omnyth.aerofirmacraftterrain.AerofirmacraftTerrain;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

@Mixin(targets = "net.dries007.tfc.world.TFCChunkGenerator", remap = false)
public abstract class TFCChunkGeneratorMixin {
    private static final int AFC_MARKER_LIMIT = 12;
    private static final AtomicInteger AFC_MARKER_COUNT = new AtomicInteger();

    @Inject(method = "fillFromNoise", at = @At("RETURN"))
    private void afc$placeTinyNoiseStageMarker(
            final Blender blender,
            final RandomState randomState,
            final StructureManager structureManager,
            final ChunkAccess chunk,
            final CallbackInfoReturnable<CompletableFuture<ChunkAccess>> cir
    ) {
        final CompletableFuture<ChunkAccess> future = cir.getReturnValue();

        if (future == null) {
            AerofirmacraftTerrain.LOGGER.warn(
                    "AFC noise marker: fillFromNoise returned null future for chunkX={} chunkZ={}",
                    chunk.getPos().x,
                    chunk.getPos().z
            );
            return;
        }

        future.whenComplete((result, throwable) -> {
            if (throwable != null) {
                AerofirmacraftTerrain.LOGGER.error(
                        "AFC noise marker: fillFromNoise future failed for chunkX={} chunkZ={}",
                        chunk.getPos().x,
                        chunk.getPos().z,
                        throwable
                );
                return;
            }

            final int count = AFC_MARKER_COUNT.incrementAndGet();

            if (count > AFC_MARKER_LIMIT) {
                if (count == AFC_MARKER_LIMIT + 1) {
                    AerofirmacraftTerrain.LOGGER.info(
                            "AFC noise marker: marker limit reached. Further marker placement suppressed."
                    );
                }
                return;
            }

            placeMarker(result, count);
        });
    }

    private static void placeMarker(final ChunkAccess chunk, final int count) {
        final int worldX = chunk.getPos().getBlockX(8);
        final int worldZ = chunk.getPos().getBlockZ(8);

        final int minY = chunk.getHeightAccessorForGeneration().getMinBuildHeight();
        final int maxY = chunk.getHeightAccessorForGeneration().getMaxBuildHeight() - 1;

        final int surfaceY = findTopNonAirY(chunk, worldX, worldZ, minY, maxY);
        final int markerY = clamp(surfaceY + 4, minY + 4, maxY - 3);

        final BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();

        for (int dy = 0; dy < 3; dy++) {
            mutablePos.set(worldX, markerY + dy, worldZ);
            chunk.setBlockState(mutablePos, Blocks.GLOWSTONE.defaultBlockState(), false);
        }

        chunk.setUnsaved(true);

        AerofirmacraftTerrain.LOGGER.info(
                "AFC noise marker: placed count={} chunkX={} chunkZ={} worldX={} worldZ={} surfaceY={} markerY={} chunkStatus={} chunkClass={}",
                count,
                chunk.getPos().x,
                chunk.getPos().z,
                worldX,
                worldZ,
                surfaceY,
                markerY,
                chunk.getPersistedStatus(),
                chunk.getClass().getName()
        );
    }

    private static int findTopNonAirY(
            final ChunkAccess chunk,
            final int worldX,
            final int worldZ,
            final int minY,
            final int maxY
    ) {
        final BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();

        for (int y = maxY; y >= minY; y--) {
            mutablePos.set(worldX, y, worldZ);

            if (!chunk.getBlockState(mutablePos).isAir()) {
                return y;
            }
        }

        return minY;
    }

    private static int clamp(final int value, final int min, final int max) {
        return Math.max(min, Math.min(max, value));
    }
}