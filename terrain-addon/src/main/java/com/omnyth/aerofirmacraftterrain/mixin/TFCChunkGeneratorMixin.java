package com.omnyth.aerofirmacraftterrain.mixin;

import com.omnyth.aerofirmacraftterrain.AerofirmacraftTerrain;
import com.omnyth.aerofirmacraftterrain.world.AFCSecondLayerPrototype;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.block.state.BlockState;
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
import java.util.concurrent.atomic.AtomicInteger;

@Mixin(targets = "net.dries007.tfc.world.TFCChunkGenerator", remap = false)
public abstract class TFCChunkGeneratorMixin {
    @Unique
    private static final AtomicBoolean AFC_V40B_LOGGED_FIRST_COPY = new AtomicBoolean(false);

    @Unique
    private static final AtomicInteger AFC_V40B_COPIED_CHUNKS = new AtomicInteger();

    @Inject(
            method = "fillFromNoise(Lnet/minecraft/world/level/levelgen/blending/Blender;Lnet/minecraft/world/level/levelgen/RandomState;Lnet/minecraft/world/level/StructureManager;Lnet/minecraft/world/level/chunk/ChunkAccess;)Ljava/util/concurrent/CompletableFuture;",
            at = @At("RETURN"),
            cancellable = true
    )
    private void afc$chainSecondTfcLayerPrototypeV40b(
            final Blender blender,
            final RandomState randomState,
            final StructureManager structureManager,
            final ChunkAccess chunk,
            final CallbackInfoReturnable<CompletableFuture<ChunkAccess>> cir
    ) {
        final CompletableFuture<ChunkAccess> originalFuture = cir.getReturnValue();

        if (originalFuture == null) {
            return;
        }

        cir.setReturnValue(originalFuture.thenApply(generatedChunk -> {
            afc$copyTfcGeneratedBandIntoLowerWorld(generatedChunk);
            return generatedChunk;
        }));
    }

    @Unique
    private static void afc$copyTfcGeneratedBandIntoLowerWorld(final ChunkAccess chunk) {
        final LevelHeightAccessor height = chunk.getHeightAccessorForGeneration();
        final int worldMinY = height.getMinBuildHeight();
        final int worldMaxYExclusive = height.getMaxBuildHeight();

        final int targetMinY = Math.max(worldMinY + AFCSecondLayerPrototype.BOTTOM_BEDROCK_RESERVE, worldMinY);
        final int targetMaxY = Math.min(AFCSecondLayerPrototype.TARGET_MAX_Y, worldMaxYExclusive - 1);

        if (targetMinY > targetMaxY) {
            return;
        }

        final int sourceMinY = AFCSecondLayerPrototype.SOURCE_MIN_Y;
        final int sourceMaxY = sourceMinY + (targetMaxY - targetMinY);

        if (sourceMinY < worldMinY || sourceMaxY >= worldMaxYExclusive) {
            AerofirmacraftTerrain.LOGGER.warn(
                    "AFC v40b second layer skipped chunk {} because source range {}..{} is outside world range {}..{}",
                    chunk.getPos(),
                    sourceMinY,
                    sourceMaxY,
                    worldMinY,
                    worldMaxYExclusive - 1
            );
            return;
        }

        final ChunkPos chunkPos = chunk.getPos();
        final int minBlockX = chunkPos.getMinBlockX();
        final int minBlockZ = chunkPos.getMinBlockZ();

        final BlockPos.MutableBlockPos sourcePos = new BlockPos.MutableBlockPos();
        final BlockPos.MutableBlockPos targetPos = new BlockPos.MutableBlockPos();

        int copiedBlocks = 0;
        int copiedSolidBlocks = 0;
        int copiedAirBlocks = 0;

        for (int localX = 0; localX < 16; localX++) {
            final int blockX = minBlockX + localX;

            for (int localZ = 0; localZ < 16; localZ++) {
                final int blockZ = minBlockZ + localZ;

                for (int targetY = targetMinY; targetY <= targetMaxY; targetY++) {
                    final int sourceY = sourceMinY + (targetY - targetMinY);

                    sourcePos.set(blockX, sourceY, blockZ);
                    targetPos.set(blockX, targetY, blockZ);

                    final BlockState state = chunk.getBlockState(sourcePos);

                    chunk.setBlockState(targetPos, state, false);

                    copiedBlocks++;

                    if (state.isAir()) {
                        copiedAirBlocks++;
                    } else {
                        copiedSolidBlocks++;
                    }
                }
            }
        }

        final int index = AFC_V40B_COPIED_CHUNKS.incrementAndGet();

        if (AFC_V40B_LOGGED_FIRST_COPY.compareAndSet(false, true) || index <= 12 || index % 2048 == 0) {
            AerofirmacraftTerrain.LOGGER.info(
                    "AFC v40b second TFC layer copy: index={} chunkX={} chunkZ={} sourceRange={}..{} targetRange={}..{} worldRange={}..{} copiedBlocks={} solidBlocks={} airBlocks={} sampleSource=({}, {}, {}) sampleTarget=({}, {}, {})",
                    index,
                    chunkPos.x,
                    chunkPos.z,
                    sourceMinY,
                    sourceMaxY,
                    targetMinY,
                    targetMaxY,
                    worldMinY,
                    worldMaxYExclusive - 1,
                    copiedBlocks,
                    copiedSolidBlocks,
                    copiedAirBlocks,
                    minBlockX + 8,
                    sourceMinY,
                    minBlockZ + 8,
                    minBlockX + 8,
                    targetMinY,
                    minBlockZ + 8
            );
        }
    }
}