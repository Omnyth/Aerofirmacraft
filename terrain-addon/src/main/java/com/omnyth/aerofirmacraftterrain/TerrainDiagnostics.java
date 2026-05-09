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

import java.util.concurrent.atomic.AtomicInteger;

public final class TerrainDiagnostics {
    private static final int OVERWORLD_CHUNK_LOAD_LOG_LIMIT = 32;
    private static final int SURFACE_SAMPLE_LOG_LIMIT = 24;

    private static final AtomicInteger OVERWORLD_CHUNK_LOAD_LOG_COUNT = new AtomicInteger();
    private static final AtomicInteger SURFACE_SAMPLE_LOG_COUNT = new AtomicInteger();

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

        final ChunkAccess chunk = event.getChunk();

        logChunkLoad(serverLevel, chunk, event.isNewChunk());
        logSurfaceSample(serverLevel, chunk, event.isNewChunk());
    }

    private static void logChunkLoad(final ServerLevel serverLevel, final ChunkAccess chunk, final boolean isNewChunk) {
        final int count = OVERWORLD_CHUNK_LOAD_LOG_COUNT.incrementAndGet();

        if (count > OVERWORLD_CHUNK_LOAD_LOG_LIMIT) {
            if (count == OVERWORLD_CHUNK_LOAD_LOG_LIMIT + 1) {
                AerofirmacraftTerrain.LOGGER.info(
                        "AFC terrain diagnostics: overworld chunk load log limit reached. Further chunk-load logs suppressed."
                );
            }
            return;
        }

        AerofirmacraftTerrain.LOGGER.info(
                "AFC terrain diagnostics: overworld chunk load. count={} chunkX={} chunkZ={} isNewChunk={} persistedStatus={} highestGeneratedStatus={} minY={} maxY={} chunkClass={}",
                count,
                chunk.getPos().x,
                chunk.getPos().z,
                isNewChunk,
                chunk.getPersistedStatus(),
                chunk.getHighestGeneratedStatus(),
                serverLevel.getMinBuildHeight(),
                serverLevel.getMaxBuildHeight(),
                chunk.getClass().getName()
        );
    }

    private static void logSurfaceSample(final ServerLevel serverLevel, final ChunkAccess chunk, final boolean isNewChunk) {
        final int count = SURFACE_SAMPLE_LOG_COUNT.incrementAndGet();

        if (count > SURFACE_SAMPLE_LOG_LIMIT) {
            if (count == SURFACE_SAMPLE_LOG_LIMIT + 1) {
                AerofirmacraftTerrain.LOGGER.info(
                        "AFC terrain sample: surface sample log limit reached. Further samples suppressed."
                );
            }
            return;
        }

        // Center-column sample for now. This is read-only and does not mutate terrain.
        final int localX = 8;
        final int localZ = 8;

        final int worldX = chunk.getPos().getMinBlockX() + localX;
        final int worldZ = chunk.getPos().getMinBlockZ() + localZ;

        final int surfaceY = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, localX, localZ) - 1;
        final int oceanFloorY = chunk.getHeight(Heightmap.Types.OCEAN_FLOOR, localX, localZ) - 1;

        final int clampedSurfaceY = Math.max(serverLevel.getMinBuildHeight(), Math.min(surfaceY, serverLevel.getMaxBuildHeight() - 1));
        final BlockPos surfacePos = new BlockPos(worldX, clampedSurfaceY, worldZ);

        final BlockState surfaceState = chunk.getBlockState(surfacePos);
        final ResourceLocation surfaceBlockId = BuiltInRegistries.BLOCK.getKey(surfaceState.getBlock());

        final boolean surfaceIsAir = surfaceState.isAir();
        final boolean surfaceHasWaterFluid = chunk.getFluidState(surfacePos).is(FluidTags.WATER);

        AerofirmacraftTerrain.LOGGER.info(
                "AFC terrain sample: count={} chunkX={} chunkZ={} isNewChunk={} sampleX={} sampleZ={} surfaceY={} oceanFloorY={} surfaceBlock={} surfaceIsAir={} surfaceHasWaterFluid={}",
                count,
                chunk.getPos().x,
                chunk.getPos().z,
                isNewChunk,
                worldX,
                worldZ,
                surfaceY,
                oceanFloorY,
                surfaceBlockId,
                surfaceIsAir,
                surfaceHasWaterFluid
        );
    }
}