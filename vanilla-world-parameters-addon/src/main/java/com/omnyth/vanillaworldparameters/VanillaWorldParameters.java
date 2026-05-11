package com.omnyth.vanillaworldparameters;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import org.slf4j.Logger;

@Mod(VanillaWorldParameters.MODID)
public final class VanillaWorldParameters {
    public static final String MODID = "vanilla_world_parameters";
    public static final Logger LOGGER = LogUtils.getLogger();

    public VanillaWorldParameters(final IEventBus modEventBus) {
        modEventBus.addListener(this::commonSetup);

        LOGGER.info("Vanilla World Parameters constructed. V7 overworld dimension type target: min_y=-256 height=576 maxY=319.");
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("Vanilla World Parameters common setup complete. New worlds should load the mod-provided minecraft:overworld dimension type.");
    }
}