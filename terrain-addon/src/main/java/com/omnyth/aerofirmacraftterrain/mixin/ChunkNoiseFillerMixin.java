package com.omnyth.aerofirmacraftterrain.mixin;

import com.omnyth.aerofirmacraftterrain.AerofirmacraftTerrain;
import net.dries007.tfc.world.ChunkBaseBlockSource;
import net.dries007.tfc.world.MutableDensityFunctionContext;
import net.dries007.tfc.world.biome.BiomeExtension;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.atomic.AtomicLong;

@Mixin(targets = "net.dries007.tfc.world.ChunkNoiseFiller", remap = false)
public abstract class ChunkNoiseFillerMixin {
    @Unique
    private static final int AFC_OLD_TFC_MIN_Y = -64;

    @Unique
    private static final int AFC_LOWER_OCEAN_WATER_TOP_Y = -193;

    @Unique
    private static final int AFC_SKY_GAP_TOP_Y = AFC_OLD_TFC_MIN_Y - 1;

    @Unique
    private static final int AFC_OCEAN_FLOOR_BASE_Y = -222;

    @Unique
    private static final int AFC_OCEAN_FLOOR_MIN_Y = -238;

    @Unique
    private static final int AFC_OCEAN_FLOOR_MAX_Y = -205;

    @Unique
    private static final int AFC_ISLAND_BOTTOM_BASE_Y = -121;

    @Unique
    private static final int AFC_ISLAND_BOTTOM_MIN_Y = -142;

    @Unique
    private static final int AFC_ISLAND_BOTTOM_MAX_Y = -101;

    @Unique
    private static final int AFC_SEDIMENT_DEPTH = 2;

    @Unique
    private static final AtomicLong AFC_V33_SUBSTITUTIONS = new AtomicLong();

    @Unique
    private static volatile BlockState afc$saltWaterState;

    @Shadow
    @Final
    private MutableDensityFunctionContext mutableDensityFunctionContext;

    @Shadow
    @Final
    private ChunkBaseBlockSource baseBlockSource;

    @Shadow
    @Final
    private BiomeExtension[] localBiomesNoRivers;

    @Inject(method = "calculateBlockStateAtNoise", at = @At("RETURN"), cancellable = true)
    private void afc$substituteLowerWorldStateV33(
            final int y,
            final double noise,
            final CallbackInfoReturnable<BlockState> cir
    ) {
        if (y >= AFC_OLD_TFC_MIN_Y) {
            return;
        }

        final BlockPos pos = this.mutableDensityFunctionContext.cursor();
        final int x = pos.getX();
        final int z = pos.getZ();

        final BlockState originalState = cir.getReturnValue();
        final BlockState replacementState = afc$resolveLowerWorldState(x, y, z, originalState);

        if (replacementState != originalState && !replacementState.equals(originalState)) {
            cir.setReturnValue(replacementState);
        }

        final long count = AFC_V33_SUBSTITUTIONS.incrementAndGet();

        if (count <= 16 || count % 1000000L == 0L) {
            AerofirmacraftTerrain.LOGGER.info(
                    "AFC v33 no-transform substitution: count={} pos=({}, {}, {}) upperLayer={} replacement='{}'",
                    count,
                    x,
                    y,
                    z,
                    afc$upperLayerName(x, z),
                    replacementState
            );
        }
    }

    @Unique
    private BlockState afc$resolveLowerWorldState(
            final int x,
            final int y,
            final int z,
            final BlockState originalState
    ) {
        final int floorY = afc$computeOceanFloorY(x, z);
        final BlockState rawRock = afc$pickRockState(x, z);
        final BlockState sediment = afc$pickSedimentState(rawRock);

        if (y <= floorY) {
            return y >= floorY - AFC_SEDIMENT_DEPTH + 1 ? sediment : rawRock;
        }

        if (y <= AFC_LOWER_OCEAN_WATER_TOP_Y) {
            return afc$getSaltWaterState();
        }

        if (y <= AFC_SKY_GAP_TOP_Y) {
            if (afc$isOpenSkyColumn(x, z)) {
                return Blocks.AIR.defaultBlockState();
            }

            final int islandBottomY = afc$computeIslandBottomY(x, z);

            if (y >= islandBottomY) {
                return rawRock;
            }

            return Blocks.AIR.defaultBlockState();
        }

        return originalState;
    }

    @Unique
    private boolean afc$isOpenSkyColumn(final int x, final int z) {
        final BiomeExtension biome = afc$currentColumnBiome(x, z);

        if (biome == null || biome.key() == null) {
            return false;
        }

        final String id = biome.key().location().toString().toLowerCase();

        return id.contains("ocean")
                || id.contains("reef")
                || id.contains("beach")
                || id.contains("shore")
                || id.contains("lake");
    }

    @Unique
    private String afc$upperLayerName(final int x, final int z) {
        final BiomeExtension biome = afc$currentColumnBiome(x, z);

        if (biome == null || biome.key() == null) {
            return "unknown";
        }

        return biome.key().location().toString();
    }

    @Unique
    private BiomeExtension afc$currentColumnBiome(final int x, final int z) {
        if (this.localBiomesNoRivers == null || this.localBiomesNoRivers.length == 0) {
            return null;
        }

        final int index = (x & 15) + ((z & 15) * 16);

        if (index < 0 || index >= this.localBiomesNoRivers.length) {
            return null;
        }

        return this.localBiomesNoRivers[index];
    }

    @Unique
    private BlockState afc$pickRockState(final int x, final int z) {
        final BlockState sampled = this.baseBlockSource.getBaseBlock(x, AFC_OLD_TFC_MIN_Y - 1, z);

        if (afc$isUsableRockState(sampled)) {
            return sampled;
        }

        return BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("tfc", "rock/raw/granite"))
                .defaultBlockState();
    }

    @Unique
    private static boolean afc$isUsableRockState(final BlockState state) {
        if (state == null || state.isAir() || !state.getFluidState().isEmpty()) {
            return false;
        }

        final ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock());

        return blockId != null
                && "tfc".equals(blockId.getNamespace())
                && blockId.getPath().startsWith("rock/raw/");
    }

    @Unique
    private static BlockState afc$pickSedimentState(final BlockState rawRock) {
        final ResourceLocation rawId = BuiltInRegistries.BLOCK.getKey(rawRock.getBlock());

        if (rawId != null && "tfc".equals(rawId.getNamespace()) && rawId.getPath().startsWith("rock/raw/")) {
            final String rockName = rawId.getPath().substring("rock/raw/".length());

            final BlockState gravel = afc$getOptionalBlockState(ResourceLocation.fromNamespaceAndPath("tfc", "rock/gravel/" + rockName));

            if (gravel != null) {
                return gravel;
            }

            final BlockState sand = afc$getOptionalBlockState(ResourceLocation.fromNamespaceAndPath("tfc", "rock/sand/" + rockName));

            if (sand != null) {
                return sand;
            }
        }

        return rawRock;
    }

    @Unique
    private static BlockState afc$getOptionalBlockState(final ResourceLocation id) {
        if (BuiltInRegistries.BLOCK.containsKey(id)) {
            return BuiltInRegistries.BLOCK.get(id).defaultBlockState();
        }

        return null;
    }

    @Unique
    private static BlockState afc$getSaltWaterState() {
        BlockState cached = afc$saltWaterState;

        if (cached == null) {
            cached = BuiltInRegistries.FLUID.get(ResourceLocation.fromNamespaceAndPath("tfc", "salt_water"))
                    .defaultFluidState()
                    .createLegacyBlock();

            afc$saltWaterState = cached;

            AerofirmacraftTerrain.LOGGER.info(
                    "AFC v33 salt water state cached: '{}'",
                    cached
            );
        }

        return cached;
    }

    @Unique
    private static int afc$computeOceanFloorY(final int x, final int z) {
        final int broad = afc$centeredNoise(Math.floorDiv(x, 32), Math.floorDiv(z, 32), 11);
        final int medium = afc$centeredNoise(Math.floorDiv(x, 11), Math.floorDiv(z, 11), 6);
        final int fine = afc$centeredNoise(x, z, 3);

        return afc$clamp(AFC_OCEAN_FLOOR_BASE_Y + broad + medium + fine, AFC_OCEAN_FLOOR_MIN_Y, AFC_OCEAN_FLOOR_MAX_Y);
    }

    @Unique
    private static int afc$computeIslandBottomY(final int x, final int z) {
        final int broad = afc$centeredNoise(Math.floorDiv(x, 40), Math.floorDiv(z, 40), 12);
        final int medium = afc$centeredNoise(Math.floorDiv(x, 14), Math.floorDiv(z, 14), 6);
        final int fine = afc$centeredNoise(x, z, 2);

        return afc$clamp(AFC_ISLAND_BOTTOM_BASE_Y + broad + medium + fine, AFC_ISLAND_BOTTOM_MIN_Y, AFC_ISLAND_BOTTOM_MAX_Y);
    }

    @Unique
    private static int afc$centeredNoise(final int x, final int z, final int radius) {
        if (radius <= 0) {
            return 0;
        }

        return Math.floorMod(afc$hash(x, z), radius * 2 + 1) - radius;
    }

    @Unique
    private static int afc$hash(final int x, final int z) {
        int h = x * 73428767 ^ z * 912367;
        h ^= (h >>> 13);
        h *= 1274126177;
        h ^= (h >>> 16);
        return h;
    }

    @Unique
    private static int afc$clamp(final int value, final int min, final int max) {
        return Math.max(min, Math.min(max, value));
    }
}