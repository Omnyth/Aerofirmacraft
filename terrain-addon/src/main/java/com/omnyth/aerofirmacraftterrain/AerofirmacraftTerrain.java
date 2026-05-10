package com.omnyth.aerofirmacraftterrain;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import org.slf4j.Logger;

@Mod(AerofirmacraftTerrain.MODID)
public final class AerofirmacraftTerrain {
    public static final String MODID = "aerofirmacraft_terrain";
    public static final Logger LOGGER = LogUtils.getLogger();

    public AerofirmacraftTerrain(IEventBus modEventBus) {
        modEventBus.addListener(this::commonSetup);

        LOGGER.info("Aerofirmacraft Terrain constructed. Sky-gap biome v7 resources active.");
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("Aerofirmacraft Terrain common setup complete. V7 adds aerofirmacraft_terrain:sky_gap biome data.");
    }
}