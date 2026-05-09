package com.omnyth.aerofirmacraftterrain.mixin;

import com.omnyth.aerofirmacraftterrain.AerofirmacraftTerrain;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

@Mixin(targets = "net.dries007.tfc.world.TFCChunkGenerator", remap = false)
public abstract class TFCChunkGeneratorMixin {
    private static final AtomicBoolean AFC_DID_SMALL_CARVE = new AtomicBoolean(false);

    @Inject(method = "fillFromNoise", at = @At("RETURN"))
    private void afc$smallNoiseStageCarve(
            final Blender blender,
            final RandomState randomState,
            final StructureManager structureManager,
            final ChunkAccess chunk,
            final CallbackInfoReturnable<CompletableFuture<ChunkAccess>> cir
    ) {
        final CompletableFuture<ChunkAccess> future = cir.getReturnValue();

        if (future == null) {
            AerofirmacraftTerrain.LOGGER.warn(
                    "AFC small carve: fillFromNoise returned null future for chunkX={} chunkZ={}",
                    chunk.getPos().x,
                    chunk.getPos().z
            );
            return;
        }

        future.whenComplete((result, throwable) -> {
            if (throwable != null) {
                AerofirmacraftTerrain.LOGGER.error(
                        "AFC small carve: fillFromNoise future failed for chunkX={} chunkZ={}",
                        chunk.getPos().x,
                        chunk.getPos().z,
                        throwable
                );
                return;
            }

            if (!AFC_DID_SMALL_CARVE.compareAndSet(false, true)) {
                return;
            }

            carveOneSmallShaft(result);
        });
    }

    private static void carveOneSmallShaft(final ChunkAccess chunk) {
        final int minY = chunk.getHeightAccessorForGeneration().getMinBuildHeight();
        final int maxY = chunk.getHeightAccessorForGeneration().getMaxBuildHeight() - 1;

        final int centerWorldX = chunk.getPos().getBlockX(8);
        final int centerWorldZ = chunk.getPos().getBlockZ(8);

        final int surfaceY = findTopNonAirY(chunk, centerWorldX, centerWorldZ, minY, maxY);
        final int carveTopY = clamp(surfaceY + 3, minY + 8, maxY - 2);
        final int floorY = clamp(surfaceY - 10, minY + 1, carveTopY - 1);

        final BlockState air = Blocks.AIR.defaultBlockState();
        final BlockState glowstone = Blocks.GLOWSTONE.defaultBlockState();

        final BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();

        int airBlocks = 0;
        int markerBlocks = 0;

        for (int localX = 6; localX <= 9; localX++) {
            for (int localZ = 6; localZ <= 9; localZ++) {
                final int worldX = chunk.getPos().getBlockX(localX);
                final int worldZ = chunk.getPos().getBlockZ(localZ);

                mutablePos.set(worldX, floorY, worldZ);
                chunk.setBlockState(mutablePos, glowstone, false);
                markerBlocks++;

                for (int y = floorY + 1; y <= carveTopY; y++) {
                    mutablePos.set(worldX, y, worldZ);
                    chunk.setBlockState(mutablePos, air, false);
                    airBlocks++;
                }
            }
        }

        chunk.setUnsaved(true);

        final BlockState originalCenterState = chunk.getBlockState(new BlockPos(centerWorldX, surfaceY, centerWorldZ));
        final String originalCenterBlock = BuiltInRegistries.BLOCK.getKey(originalCenterState.getBlock()).toString();

        AerofirmacraftTerrain.LOGGER.info(
                "AFC small carve: applied chunkX={} chunkZ={} centerX={} centerZ={} surfaceY={} floorY={} carveTopY={} airBlocks={} markerBlocks={} centerBlock={} chunkStatus={} chunkClass={} tpHint='/tp @s {} {} {}'",
                chunk.getPos().x,
                chunk.getPos().z,
                centerWorldX,
                centerWorldZ,
                surfaceY,
                floorY,
                carveTopY,
                airBlocks,
                markerBlocks,
                originalCenterBlock,
                chunk.getPersistedStatus(),
                chunk.getClass().getName(),
                centerWorldX,
                carveTopY + 3,
                centerWorldZ
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