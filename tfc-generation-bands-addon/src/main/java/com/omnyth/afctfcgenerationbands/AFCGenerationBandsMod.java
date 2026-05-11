package com.omnyth.afctfcgenerationbands;

import com.mojang.logging.LogUtils;
import com.omnyth.afctfcgenerationbands.config.AFCGenerationBands;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import org.slf4j.Logger;

@Mod(AFCGenerationBandsMod.MODID)
public final class AFCGenerationBandsMod {
    public static final String MODID = "afc_tfc_generation_bands";
    public static final Logger LOGGER = LogUtils.getLogger();

    public AFCGenerationBandsMod(final IEventBus modEventBus) {
        modEventBus.addListener(this::commonSetup);

        LOGGER.info(
                "AFC TFC Generation Bands constructed. TFC generation range={}..{}",
                AFCGenerationBands.TFC_GENERATION_MIN_Y,
                AFCGenerationBands.TFC_GENERATION_MAX_Y
        );
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info(
                "AFC TFC Generation Bands common setup complete. World target range={}..{}, AFC lower owned range={}..{}",
                AFCGenerationBands.WORLD_MIN_Y,
                AFCGenerationBands.WORLD_MAX_Y,
                AFCGenerationBands.AFC_LOWER_WORLD_MIN_Y,
                AFCGenerationBands.AFC_LOWER_WORLD_MAX_Y
        );
    }
}