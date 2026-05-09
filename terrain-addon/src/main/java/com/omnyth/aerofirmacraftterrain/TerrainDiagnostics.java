package com.omnyth.aerofirmacraftterrain;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;

import java.util.concurrent.atomic.AtomicInteger;

public final class TerrainDiagnostics {
    private static final int OVERWORLD_CHUNK_LOAD_LOG_LIMIT = 64;
    private static final AtomicInteger OVERWORLD_CHUNK_LOAD_LOG_COUNT = new AtomicInteger();

    @SubscribeEvent
    public void onServerStarting(final ServerStartingEvent event) {
        AerofirmacraftTerrain.LOGGER.info(
                "AFC terrain diagnostics: server starting. Overworld dimension = {}",
                event.getServer().overworld().dimension().location()
        );
    }

    @SubscribeEvent
    public void onLevelLoad(final LevelEvent.Load event) {
        final LevelAccessor level = event.getLevel();

        if (level instanceof ServerLevel serverLevel) {
            AerofirmacraftTerrain.LOGGER.info(
                    "AFC terrain diagnostics: server level loaded. dimension={} minY={} maxY={} chunkSource={}",
                    serverLevel.dimension().location(),
                    serverLevel.getMinBuildHeight(),
                    serverLevel.getMaxBuildHeight(),
                    serverLevel.getChunkSource().getClass().getName()
            );
        } else {
            AerofirmacraftTerrain.LOGGER.info(
                    "AFC terrain diagnostics: non-server level loaded. levelClass={}",
                    level.getClass().getName()
            );
        }
    }

    @SubscribeEvent
    public void onChunkLoad(final ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }

        if (!serverLevel.dimension().equals(Level.OVERWORLD)) {
            return;
        }

        final int count = OVERWORLD_CHUNK_LOAD_LOG_COUNT.incrementAndGet();

        if (count > OVERWORLD_CHUNK_LOAD_LOG_LIMIT) {
            if (count == OVERWORLD_CHUNK_LOAD_LOG_LIMIT + 1) {
                AerofirmacraftTerrain.LOGGER.info(
                        "AFC terrain diagnostics: overworld chunk load log limit reached. Further chunk-load logs suppressed."
                );
            }
            return;
        }

        final ChunkAccess chunk = event.getChunk();

        AerofirmacraftTerrain.LOGGER.info(
                "AFC terrain diagnostics: overworld chunk load. count={} chunkX={} chunkZ={} isNewChunk={} persistedStatus={} highestGeneratedStatus={} minY={} maxY={} chunkClass={}",
                count,
                chunk.getPos().x,
                chunk.getPos().z,
                event.isNewChunk(),
                chunk.getPersistedStatus(),
                chunk.getHighestGeneratedStatus(),
                serverLevel.getMinBuildHeight(),
                serverLevel.getMaxBuildHeight(),
                chunk.getClass().getName()
        );
    }
}