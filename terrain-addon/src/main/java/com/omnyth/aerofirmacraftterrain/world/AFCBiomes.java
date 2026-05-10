package com.omnyth.aerofirmacraftterrain.world;

import com.omnyth.aerofirmacraftterrain.AerofirmacraftTerrain;
import net.dries007.tfc.world.biome.BiomeBlendType;
import net.dries007.tfc.world.biome.BiomeBuilder;
import net.dries007.tfc.world.biome.BiomeExtension;
import net.dries007.tfc.world.biome.BiomeNoise;
import net.dries007.tfc.world.biome.TFCBiomes;
import net.dries007.tfc.world.surface.builder.ShoreAndOceanSurfaceBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class AFCBiomes {
    public static final ResourceLocation LOWER_OCEAN_ID = ResourceLocation.fromNamespaceAndPath(
            AerofirmacraftTerrain.MODID,
            "lower_ocean"
    );

    public static final ResourceKey<Biome> LOWER_OCEAN_BIOME_KEY = ResourceKey.create(
            Registries.BIOME,
            LOWER_OCEAN_ID
    );

    private static final DeferredRegister<BiomeExtension> EXTENSIONS = DeferredRegister.create(
            TFCBiomes.KEY,
            AerofirmacraftTerrain.MODID
    );

    public static final DeferredHolder<BiomeExtension, BiomeExtension> LOWER_OCEAN_EXTENSION = EXTENSIONS.register(
            "lower_ocean",
            () -> new BiomeBuilder()
                    // Copied from TFC ocean shape, then shifted downward for AFC testing.
                    // TFC ocean: BiomeNoise.ocean(seed, -26, -12), aquiferHeightOffset(-24)
                    // AFC v20 lower ocean: intentionally much lower.
                    .heightmap(seed -> BiomeNoise.ocean(seed, -90, -76))
                    .surface(ShoreAndOceanSurfaceBuilder.OCEAN)
                    .aquiferHeightOffset(-88)
                    .salty()
                    .type(BiomeBlendType.OCEAN)
                    .noRivers()
                    .build(LOWER_OCEAN_BIOME_KEY)
    );

    private AFCBiomes() {}

    public static void register(IEventBus modEventBus) {
        EXTENSIONS.register(modEventBus);
    }
}