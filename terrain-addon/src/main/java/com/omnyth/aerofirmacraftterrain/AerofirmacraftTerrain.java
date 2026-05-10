package com.omnyth.aerofirmacraftterrain;

import com.mojang.logging.LogUtils;
import com.omnyth.aerofirmacraftterrain.world.AFCBiomes;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import org.slf4j.Logger;

@Mod(AerofirmacraftTerrain.MODID)
public final class AerofirmacraftTerrain {
    public static final String MODID = "aerofirmacraft_terrain";
    public static final Logger LOGGER = LogUtils.getLogger();

    public AerofirmacraftTerrain(IEventBus modEventBus) {
        AFCBiomes.register(modEventBus);

        modEventBus.addListener(this::commonSetup);

        LOGGER.info("Aerofirmacraft Terrain constructed. Real TFC-compatible lower_ocean biome v23a startup-safe lower-ocean diagnostic active.");
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("Aerofirmacraft Terrain common setup complete. V23a disables the direct ChunkNoiseFiller mixin and keeps the minY=-256 lower_ocean diagnostic baseline.");
    }
}