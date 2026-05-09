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
    private static final int CHUNK_SUMMARY_LOG_LIMIT = 48;
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

        int landColumns = 0;
        int fluidColumns = 0;
        int airColumns = 0;
        int plantOrLeavesColumns = 0;
        int rawRockColumns = 0;
        int dirtOrMudColumns = 0;

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

                final boolean isAir = surfaceState.isAir();
                final boolean hasMinecraftWaterFluid = chunk.getFluidState(surfacePos).is(FluidTags.WATER);

                // TFC salt water is represented by block IDs such as tfc:fluid/salt_water.
                // Treat TFC fluid blocks as fluid columns even if the vanilla FluidTags.WATER test does not catch them.
                final boolean isTfcFluidBlock = blockKey.startsWith("tfc:fluid/");
                final boolean looksLikeWaterBlock = blockKey.contains("water");

                final boolean isFluidColumn = hasMinecraftWaterFluid || isTfcFluidBlock || looksLikeWaterBlock;
                final boolean isPlantOrLeaves = blockKey.contains("plant") || blockKey.contains("leaves");
                final boolean isRawRock = blockKey.contains("rock/raw");
                final boolean isDirtOrMud = blockKey.contains("dirt/") || blockKey.contains("mud/");

                if (isAir) {
                    airColumns++;
                } else if (isFluidColumn) {
                    fluidColumns++;
                } else {
                    landColumns++;

                    if (isPlantOrLeaves) {
                        plantOrLeavesColumns++;
                    }

                    if (isRawRock) {
                        rawRockColumns++;
                    }

                    if (isDirtOrMud) {
                        dirtOrMudColumns++;
                    }
                }
            }
        }

        final String dominantSurfaceBlock = surfaceBlockCounts.entrySet()
                .stream()
                .max(Map.Entry.comparingByValue())
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .orElse("none");

        final String terrainClass = classifyChunk(
                fluidColumns,
                landColumns,
                airColumns,
                minSurfaceY,
                maxSurfaceY,
                minOceanFloorY,
                maxOceanFloorY
        );

        AerofirmacraftTerrain.LOGGER.info(
                "AFC chunk summary: count={} chunkX={} chunkZ={} class={} surfaceY={}..{} oceanFloorY={}..{} landColumns={} fluidColumns={} airColumns={} plantOrLeavesColumns={} rawRockColumns={} dirtOrMudColumns={} dominantSurface={} persistedStatus={} highestGeneratedStatus={}",
                count,
                chunk.getPos().x,
                chunk.getPos().z,
                terrainClass,
                minSurfaceY,
                maxSurfaceY,
                minOceanFloorY,
                maxOceanFloorY,
                landColumns,
                fluidColumns,
                airColumns,
                plantOrLeavesColumns,
                rawRockColumns,
                dirtOrMudColumns,
                dominantSurfaceBlock,
                chunk.getPersistedStatus(),
                chunk.getHighestGeneratedStatus()
        );
    }

    private static String classifyChunk(
            final int fluidColumns,
            final int landColumns,
            final int airColumns,
            final int minSurfaceY,
            final int maxSurfaceY,
            final int minOceanFloorY,
            final int maxOceanFloorY
    ) {
        final int relief = maxSurfaceY - minSurfaceY;
        final int waterDepthSignal = maxSurfaceY - minOceanFloorY;

        if (fluidColumns >= 224) {
            if (waterDepthSignal >= 8) {
                return "open_water_deep";
            }

            return "open_water_shallow";
        }

        if (fluidColumns >= 96 && landColumns >= 96) {
            return "shore_mixed";
        }

        if (fluidColumns > 0 && landColumns > 0) {
            return "shore_edge";
        }

        if (landColumns >= 224) {
            if (relief >= 32) {
                return "land_high_relief";
            }

            if (minSurfaceY <= 63 && maxOceanFloorY <= 61) {
                return "low_coastal_land";
            }

            return "land";
        }

        if (airColumns >= 224) {
            return "mostly_air";
        }

        return "mixed_unknown";
    }
}