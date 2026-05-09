package com.omnyth.aerofirmacraftterrain.mixin;

import com.omnyth.aerofirmacraftterrain.AerofirmacraftTerrain;
import net.minecraft.world.level.StructureManager;
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
    private static final int AFC_FILL_FROM_NOISE_LOG_LIMIT = 32;
    private static final AtomicInteger AFC_FILL_FROM_NOISE_HEAD_COUNT = new AtomicInteger();
    private static final AtomicInteger AFC_FILL_FROM_NOISE_COMPLETE_COUNT = new AtomicInteger();

    @Inject(method = "fillFromNoise", at = @At("HEAD"))
    private void afc$fillFromNoiseHead(
            final Blender blender,
            final RandomState randomState,
            final StructureManager structureManager,
            final ChunkAccess chunk,
            final CallbackInfoReturnable<CompletableFuture<ChunkAccess>> cir
    ) {
        final int count = AFC_FILL_FROM_NOISE_HEAD_COUNT.incrementAndGet();

        if (count <= AFC_FILL_FROM_NOISE_LOG_LIMIT) {
            AerofirmacraftTerrain.LOGGER.info(
                    "AFC mixin canary: TFC fillFromNoise HEAD count={} chunkX={} chunkZ={} chunkClass={} status={} minY={} maxY={}",
                    count,
                    chunk.getPos().x,
                    chunk.getPos().z,
                    chunk.getClass().getName(),
                    chunk.getPersistedStatus(),
                    chunk.getHeightAccessorForGeneration().getMinBuildHeight(),
                    chunk.getHeightAccessorForGeneration().getMaxBuildHeight()
            );
        } else if (count == AFC_FILL_FROM_NOISE_LOG_LIMIT + 1) {
            AerofirmacraftTerrain.LOGGER.info(
                    "AFC mixin canary: TFC fillFromNoise HEAD log limit reached. Further HEAD logs suppressed."
            );
        }
    }

    @Inject(method = "fillFromNoise", at = @At("RETURN"))
    private void afc$fillFromNoiseReturn(
            final Blender blender,
            final RandomState randomState,
            final StructureManager structureManager,
            final ChunkAccess chunk,
            final CallbackInfoReturnable<CompletableFuture<ChunkAccess>> cir
    ) {
        final CompletableFuture<ChunkAccess> future = cir.getReturnValue();

        if (future == null) {
            AerofirmacraftTerrain.LOGGER.warn(
                    "AFC mixin canary: TFC fillFromNoise RETURN had null future for chunkX={} chunkZ={}",
                    chunk.getPos().x,
                    chunk.getPos().z
            );
            return;
        }

        future.whenComplete((result, throwable) -> {
            final int count = AFC_FILL_FROM_NOISE_COMPLETE_COUNT.incrementAndGet();

            if (count > AFC_FILL_FROM_NOISE_LOG_LIMIT) {
                if (count == AFC_FILL_FROM_NOISE_LOG_LIMIT + 1) {
                    AerofirmacraftTerrain.LOGGER.info(
                            "AFC mixin canary: TFC fillFromNoise COMPLETE log limit reached. Further COMPLETE logs suppressed."
                    );
                }
                return;
            }

            if (throwable != null) {
                AerofirmacraftTerrain.LOGGER.error(
                        "AFC mixin canary: TFC fillFromNoise COMPLETE failed. count={} chunkX={} chunkZ={}",
                        count,
                        chunk.getPos().x,
                        chunk.getPos().z,
                        throwable
                );
                return;
            }

            AerofirmacraftTerrain.LOGGER.info(
                    "AFC mixin canary: TFC fillFromNoise COMPLETE count={} chunkX={} chunkZ={} resultClass={} resultStatus={}",
                    count,
                    result.getPos().x,
                    result.getPos().z,
                    result.getClass().getName(),
                    result.getPersistedStatus()
            );
        });
    }
}