package com.omnyth.aerofirmacraftterrain;

import com.mojang.logging.LogUtils;
import com.omnyth.aerofirmacraftterrain.config.AFCGenerationBands;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import org.slf4j.Logger;

@Mod(AerofirmacraftTerrain.MODID)
public final class AerofirmacraftTerrain {
    public static final String MODID = "aerofirmacraft_terrain";
    public static final Logger LOGGER = LogUtils.getLogger();

    public AerofirmacraftTerrain(final IEventBus modEventBus) {
        modEventBus.addListener(this::commonSetup);

        LOGGER.info(
                "Aerofirmacraft Terrain constructed. V34 TFC generation-band control active. TFC generation minY={}",
                AFCGenerationBands.TFC_GENERATION_MIN_Y
        );
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info(
                "Aerofirmacraft Terrain common setup complete. World minY target={}, TFC owned range={}..{}",
                AFCGenerationBands.WORLD_MIN_Y,
                AFCGenerationBands.TFC_GENERATION_MIN_Y,
                AFCGenerationBands.TFC_GENERATION_MAX_Y
        );
    }
}