package com.omnyth.aerofirmacraftterrain.mixin;

import com.omnyth.aerofirmacraftterrain.AerofirmacraftTerrain;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.block.Block;
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
    private static final AtomicBoolean AFC_DID_FLOATING_PATCH = new AtomicBoolean(false);

    private static final int GLOBAL_OCEAN_TOP_Y = 0;
    private static final int LAND_MASS_THICKNESS = 14;
    private static final int PATCH_MIN_LOCAL = 4;
    private static final int PATCH_MAX_LOCAL = 11;

    @Inject(method = "fillFromNoise", at = @At("RETURN"))
    private void afc$floatingPatchPrototype(
            final Blender blender,
            final RandomState randomState,
            final StructureManager structureManager,
            final ChunkAccess chunk,
            final CallbackInfoReturnable<CompletableFuture<ChunkAccess>> cir
    ) {
        final CompletableFuture<ChunkAccess> future = cir.getReturnValue();

        if (future == null) {
            AerofirmacraftTerrain.LOGGER.warn(
                    "AFC floating patch: fillFromNoise returned null future for chunkX={} chunkZ={}",
                    chunk.getPos().x,
                    chunk.getPos().z
            );
            return;
        }

        future.whenComplete((result, throwable) -> {
            if (throwable != null) {
                AerofirmacraftTerrain.LOGGER.error(
                        "AFC floating patch: fillFromNoise future failed for chunkX={} chunkZ={}",
                        chunk.getPos().x,
                        chunk.getPos().z,
                        throwable
                );
                return;
            }

            if (!AFC_DID_FLOATING_PATCH.compareAndSet(false, true)) {
                return;
            }

            applyFloatingPatch(result);
        });
    }

    private static void applyFloatingPatch(final ChunkAccess chunk) {
        final int minY = chunk.getHeightAccessorForGeneration().getMinBuildHeight();
        final int maxY = chunk.getHeightAccessorForGeneration().getMaxBuildHeight() - 1;

        final int centerWorldX = chunk.getPos().getBlockX(8);
        final int centerWorldZ = chunk.getPos().getBlockZ(8);

        final BlockState air = Blocks.AIR.defaultBlockState();
        final BlockState glowstone = Blocks.GLOWSTONE.defaultBlockState();

        final BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();

        int landColumns = 0;
        int fluidColumns = 0;
        int airBlocks = 0;
        int markerBlocks = 0;

        int minSurfaceY = Integer.MAX_VALUE;
        int maxSurfaceY = Integer.MIN_VALUE;
        int minUndersideY = Integer.MAX_VALUE;
        int maxUndersideY = Integer.MIN_VALUE;

        String centerSurfaceBlockBefore = "unknown";
        int centerSurfaceY = minY;
        int centerUndersideY = minY;

        for (int localX = PATCH_MIN_LOCAL; localX <= PATCH_MAX_LOCAL; localX++) {
            for (int localZ = PATCH_MIN_LOCAL; localZ <= PATCH_MAX_LOCAL; localZ++) {
                final int worldX = chunk.getPos().getBlockX(localX);
                final int worldZ = chunk.getPos().getBlockZ(localZ);

                final int surfaceY = findTopNonAirY(chunk, worldX, worldZ, minY, maxY);

                mutablePos.set(worldX, surfaceY, worldZ);
                final BlockState surfaceState = chunk.getBlockState(mutablePos);
                final String surfaceBlockId = BuiltInRegistries.BLOCK.getKey(surfaceState.getBlock()).toString();

                final boolean fluidColumn = isFluidSurface(surfaceBlockId);

                minSurfaceY = Math.min(minSurfaceY, surfaceY);
                maxSurfaceY = Math.max(maxSurfaceY, surfaceY);

                if (localX == 8 && localZ == 8) {
                    centerSurfaceBlockBefore = surfaceBlockId;
                    centerSurfaceY = surfaceY;
                }

                if (fluidColumn) {
                    fluidColumns++;

                    final int carveTopY = clamp(surfaceY + 6, GLOBAL_OCEAN_TOP_Y + 1, maxY);

                    for (int y = GLOBAL_OCEAN_TOP_Y + 1; y <= carveTopY; y++) {
                        mutablePos.set(worldX, y, worldZ);

                        if (!chunk.getBlockState(mutablePos).isAir()) {
                            chunk.setBlockState(mutablePos, air, false);
                            airBlocks++;
                        }
                    }
                } else {
                    landColumns++;

                    final int undersideY = clamp(surfaceY - LAND_MASS_THICKNESS, GLOBAL_OCEAN_TOP_Y + 8, maxY - 1);
                    final int carveTopY = undersideY - 1;

                    minUndersideY = Math.min(minUndersideY, undersideY);
                    maxUndersideY = Math.max(maxUndersideY, undersideY);

                    if (localX == 8 && localZ == 8) {
                        centerUndersideY = undersideY;
                    }

                    mutablePos.set(worldX, undersideY, worldZ);
                    chunk.setBlockState(mutablePos, glowstone, false);
                    markerBlocks++;

                    for (int y = GLOBAL_OCEAN_TOP_Y + 1; y <= carveTopY; y++) {
                        mutablePos.set(worldX, y, worldZ);

                        if (!chunk.getBlockState(mutablePos).isAir()) {
                            chunk.setBlockState(mutablePos, air, false);
                            airBlocks++;
                        }
                    }
                }
            }
        }

        if (minUndersideY == Integer.MAX_VALUE) {
            minUndersideY = -9999;
            maxUndersideY = -9999;
        }

        chunk.setUnsaved(true);

        AerofirmacraftTerrain.LOGGER.info(
                "AFC floating patch: applied chunkX={} chunkZ={} centerX={} centerZ={} landColumns={} fluidColumns={} airBlocks={} markerBlocks={} surfaceY={}..{} undersideY={}..{} centerSurfaceY={} centerUndersideY={} centerSurfaceBlockBefore={} chunkStatus={} chunkClass={} surfaceTp='/tp @s {} {} {}' undersideTp='/tp @s {} {} {}'",
                chunk.getPos().x,
                chunk.getPos().z,
                centerWorldX,
                centerWorldZ,
                landColumns,
                fluidColumns,
                airBlocks,
                markerBlocks,
                minSurfaceY,
                maxSurfaceY,
                minUndersideY,
                maxUndersideY,
                centerSurfaceY,
                centerUndersideY,
                centerSurfaceBlockBefore,
                chunk.getPersistedStatus(),
                chunk.getClass().getName(),
                centerWorldX,
                centerSurfaceY + 10,
                centerWorldZ,
                centerWorldX,
                Math.max(GLOBAL_OCEAN_TOP_Y + 4, centerUndersideY - 6),
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

    private static boolean isFluidSurface(final String surfaceBlockId) {
        return surfaceBlockId.startsWith("tfc:fluid/")
                || surfaceBlockId.contains("water");
    }

    private static int clamp(final int value, final int min, final int max) {
        return Math.max(min, Math.min(max, value));
    }
}