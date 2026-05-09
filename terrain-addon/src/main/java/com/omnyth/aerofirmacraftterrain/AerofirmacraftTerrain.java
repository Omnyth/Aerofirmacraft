package com.omnyth.aerofirmacraftterrain;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

@Mod(AerofirmacraftTerrain.MODID)
public final class AerofirmacraftTerrain {
    public static final String MODID = "aerofirmacraft_terrain";
    public static final Logger LOGGER = LogUtils.getLogger();

    public AerofirmacraftTerrain(IEventBus modEventBus) {
        modEventBus.addListener(this::commonSetup);

        NeoForge.EVENT_BUS.register(new TerrainCrudeTransformPrototype());

        LOGGER.info("Aerofirmacraft Terrain constructed. CRUDE POST-GENERATION TRANSFORM PROTOTYPE IS ACTIVE.");
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("Aerofirmacraft Terrain common setup complete. Crude post-generation transform prototype active.");
    }
}