package com.omnyth.aerofirmacraftterrain;

import com.mojang.logging.LogUtils;
import com.omnyth.aerofirmacraftterrain.world.AFCSecondLayerPrototype;
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
                "Aerofirmacraft Terrain constructed. V40b second TFC layer prototype active. sourceMinY={} targetMaxY={}",
                AFCSecondLayerPrototype.SOURCE_MIN_Y,
                AFCSecondLayerPrototype.TARGET_MAX_Y
        );
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info(
                "Aerofirmacraft Terrain common setup complete. V40b copy generated TFC band into lower empty band."
        );
    }
}