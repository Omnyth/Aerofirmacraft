package com.omnyth.tfcgenerationbands;

import com.mojang.logging.LogUtils;
import com.omnyth.tfcgenerationbands.config.TFCGenerationBandsConfig;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import org.slf4j.Logger;

@Mod(TFCGenerationBands.MODID)
public final class TFCGenerationBands {
    public static final String MODID = "tfc_generation_bands";
    public static final Logger LOGGER = LogUtils.getLogger();

    public TFCGenerationBands(final IEventBus modEventBus) {
        TFCGenerationBandsConfig.loadOrCreate();
        modEventBus.addListener(this::commonSetup);

        LOGGER.info(
                "TFC Generation Bands constructed. enabled={} TFC generation range={}..{}",
                TFCGenerationBandsConfig.enabled(),
                TFCGenerationBandsConfig.tfcGenerationMinY(),
                TFCGenerationBandsConfig.tfcGenerationMaxY()
        );
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info(
                "TFC Generation Bands common setup complete. Config file: {}",
                TFCGenerationBandsConfig.configPath()
        );
    }
}