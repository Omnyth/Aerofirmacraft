package com.omnyth.aerofirmacraftterrain;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public final class TerrainDiagnostics {
    private static final int CHUNK_SUMMARY_LOG_LIMIT = 40;
    private static final AtomicInteger CHUNK_SUMMARY_LOG_COUNT = new AtomicInteger();

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

        if (!event.isNewChunk()) {
            return;
        }

        final int count = CHUNK_SUMMARY_LOG_COUNT.incrementAndGet();

        if (count > CHUNK_SUMMARY_LOG_LIMIT) {
            if (count == CHUNK_SUMMARY_LOG_LIMIT + 1) {
                AerofirmacraftTerrain.LOGGER.info(
                        "AFC chunk summary: log limit reached. Further chunk summaries suppressed."
                );
            }
            return;
        }

        logChunkSummary(serverLevel, event.getChunk(), count);
    }

    private static void logChunkSummary(final ServerLevel serverLevel, final ChunkAccess chunk, final int count) {
        int minSurfaceY = Integer.MAX_VALUE;
        int maxSurfaceY = Integer.MIN_VALUE;
        int minOceanFloorY = Integer.MAX_VALUE;
        int maxOceanFloorY = Integer.MIN_VALUE;

        int airColumns = 0;
        int waterColumns = 0;
        int solidColumns = 0;
        int plantOrLeavesColumns = 0;

        final Map<String, Integer> surfaceBlockCounts = new HashMap<>();

        for (int localX = 0; localX < 16; localX++) {
            for (int localZ = 0; localZ < 16; localZ++) {
                final int worldX = chunk.getPos().getMinBlockX() + localX;
                final int worldZ = chunk.getPos().getMinBlockZ() + localZ;

                final int surfaceY = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, localX, localZ) - 1;
                final int oceanFloorY = chunk.getHeight(Heightmap.Types.OCEAN_FLOOR, localX, localZ) - 1;

                minSurfaceY = Math.min(minSurfaceY, surfaceY);
                maxSurfaceY = Math.max(maxSurfaceY, surfaceY);
                minOceanFloorY = Math.min(minOceanFloorY, oceanFloorY);
                maxOceanFloorY = Math.max(maxOceanFloorY, oceanFloorY);

                final int clampedSurfaceY = Math.max(
                        serverLevel.getMinBuildHeight(),
                        Math.min(surfaceY, serverLevel.getMaxBuildHeight() - 1)
                );

                final BlockPos surfacePos = new BlockPos(worldX, clampedSurfaceY, worldZ);
                final BlockState surfaceState = chunk.getBlockState(surfacePos);
                final ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(surfaceState.getBlock());

                final String blockKey = blockId.toString();
                surfaceBlockCounts.merge(blockKey, 1, Integer::sum);

                if (surfaceState.isAir()) {
                    airColumns++;
                } else if (chunk.getFluidState(surfacePos).is(FluidTags.WATER)) {
                    waterColumns++;
                } else if (blockKey.contains("plant") || blockKey.contains("leaves")) {
                    plantOrLeavesColumns++;
                } else {
                    solidColumns++;
                }
            }
        }

        final String dominantSurfaceBlock = surfaceBlockCounts.entrySet()
                .stream()
                .max(Map.Entry.comparingByValue())
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .orElse("none");

        final String terrainClass = classifyChunk(waterColumns, solidColumns, airColumns, minSurfaceY, maxSurfaceY);

        AerofirmacraftTerrain.LOGGER.info(
                "AFC chunk summary: count={} chunkX={} chunkZ={} class={} surfaceY={}..{} oceanFloorY={}..{} solidColumns={} waterColumns={} airColumns={} plantOrLeavesColumns={} dominantSurface={} persistedStatus={} highestGeneratedStatus={}",
                count,
                chunk.getPos().x,
                chunk.getPos().z,
                terrainClass,
                minSurfaceY,
                maxSurfaceY,
                minOceanFloorY,
                maxOceanFloorY,
                solidColumns,
                waterColumns,
                airColumns,
                plantOrLeavesColumns,
                dominantSurfaceBlock,
                chunk.getPersistedStatus(),
                chunk.getHighestGeneratedStatus()
        );
    }

    private static String classifyChunk(
            final int waterColumns,
            final int solidColumns,
            final int airColumns,
            final int minSurfaceY,
            final int maxSurfaceY
    ) {
        if (waterColumns >= 192) {
            return "mostly_water";
        }

        if (waterColumns > 0 && solidColumns > 0) {
            return "mixed_shore_or_river";
        }

        if (solidColumns >= 192) {
            if (maxSurfaceY - minSurfaceY >= 32) {
                return "solid_land_high_relief";
            }

            return "solid_land";
        }

        if (airColumns >= 192) {
            return "mostly_air";
        }

        return "mixed_unknown";
    }
}