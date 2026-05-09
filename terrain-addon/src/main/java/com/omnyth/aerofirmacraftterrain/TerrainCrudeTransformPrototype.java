package com.omnyth.aerofirmacraftterrain;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public final class TerrainCrudeTransformPrototype {
    private static final int MAX_TRANSFORMED_CHUNKS = 24;

    private static final int GLOBAL_OCEAN_TOP_Y = 0;
    private static final int BASE_UNDERSIDE_Y = 38;
    private static final int UNDERSIDE_VARIATION = 8;

    private static final AtomicInteger TRANSFORMED_CHUNK_COUNT = new AtomicInteger();
    private static final Set<Long> TRANSFORMED_CHUNKS = new HashSet<>();

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

        final ChunkAccess chunk = event.getChunk();
        final long chunkKey = chunk.getPos().toLong();

        synchronized (TRANSFORMED_CHUNKS) {
            if (TRANSFORMED_CHUNKS.contains(chunkKey)) {
                return;
            }

            if (TRANSFORMED_CHUNK_COUNT.get() >= MAX_TRANSFORMED_CHUNKS) {
                if (TRANSFORMED_CHUNK_COUNT.get() == MAX_TRANSFORMED_CHUNKS) {
                    AerofirmacraftTerrain.LOGGER.info(
                            "AFC crude transform: transform chunk limit reached. Further chunks will not be transformed."
                    );
                    TRANSFORMED_CHUNK_COUNT.incrementAndGet();
                }
                return;
            }

            TRANSFORMED_CHUNKS.add(chunkKey);
        }

        final int count = TRANSFORMED_CHUNK_COUNT.incrementAndGet();

        transformChunk(serverLevel, chunk, count);
    }

    private static void transformChunk(final ServerLevel serverLevel, final ChunkAccess chunk, final int count) {
        int landColumns = 0;
        int fluidColumns = 0;
        int transformedBlocks = 0;

        final BlockState air = Blocks.AIR.defaultBlockState();
        final BlockState oceanWater = chooseOceanWaterBlock();

        final int minY = serverLevel.getMinBuildHeight();
        final int maxY = serverLevel.getMaxBuildHeight() - 1;

        for (int localX = 0; localX < 16; localX++) {
            for (int localZ = 0; localZ < 16; localZ++) {
                final int worldX = chunk.getPos().getMinBlockX() + localX;
                final int worldZ = chunk.getPos().getMinBlockZ() + localZ;

                final int surfaceY = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, localX, localZ) - 1;
                final int clampedSurfaceY = clamp(surfaceY, minY, maxY);

                final BlockPos surfacePos = new BlockPos(worldX, clampedSurfaceY, worldZ);
                final BlockState surfaceState = chunk.getBlockState(surfacePos);
                final String surfaceBlockId = BuiltInRegistries.BLOCK.getKey(surfaceState.getBlock()).toString();

                final boolean fluidColumn = isFluidColumn(chunk, surfacePos, surfaceBlockId);

                if (fluidColumn) {
                    fluidColumns++;
                    transformedBlocks += carveOceanColumn(serverLevel, worldX, worldZ, minY, maxY, oceanWater, air);
                } else {
                    landColumns++;
                    transformedBlocks += carveFloatingLandColumn(serverLevel, chunk, worldX, worldZ, minY, maxY, surfaceY, oceanWater, air);
                }
            }
        }

        AerofirmacraftTerrain.LOGGER.info(
                "AFC crude transform: count={} chunkX={} chunkZ={} landColumns={} fluidColumns={} transformedBlocks={} minY={} maxY={} oceanTopY={} baseUndersideY={}",
                count,
                chunk.getPos().x,
                chunk.getPos().z,
                landColumns,
                fluidColumns,
                transformedBlocks,
                minY,
                maxY,
                GLOBAL_OCEAN_TOP_Y,
                BASE_UNDERSIDE_Y
        );
    }

    private static int carveOceanColumn(
            final ServerLevel serverLevel,
            final int worldX,
            final int worldZ,
            final int minY,
            final int maxY,
            final BlockState oceanWater,
            final BlockState air
    ) {
        int changed = 0;

        final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int y = minY; y <= GLOBAL_OCEAN_TOP_Y; y++) {
            pos.set(worldX, y, worldZ);
            if (!serverLevel.getBlockState(pos).is(oceanWater.getBlock())) {
                serverLevel.setBlock(pos, oceanWater, Block.UPDATE_CLIENTS);
                changed++;
            }
        }

        for (int y = GLOBAL_OCEAN_TOP_Y + 1; y <= maxY; y++) {
            pos.set(worldX, y, worldZ);
            if (!serverLevel.getBlockState(pos).isAir()) {
                serverLevel.setBlock(pos, air, Block.UPDATE_CLIENTS);
                changed++;
            }
        }

        return changed;
    }

    private static int carveFloatingLandColumn(
            final ServerLevel serverLevel,
            final ChunkAccess chunk,
            final int worldX,
            final int worldZ,
            final int minY,
            final int maxY,
            final int surfaceY,
            final BlockState oceanWater,
            final BlockState air
    ) {
        int changed = 0;

        final int undersideY = computeUndersideY(worldX, worldZ, surfaceY);

        final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int y = minY; y <= GLOBAL_OCEAN_TOP_Y; y++) {
            pos.set(worldX, y, worldZ);
            if (!serverLevel.getBlockState(pos).is(oceanWater.getBlock())) {
                serverLevel.setBlock(pos, oceanWater, Block.UPDATE_CLIENTS);
                changed++;
            }
        }

        final int carveTopY = Math.min(undersideY, maxY);

        for (int y = GLOBAL_OCEAN_TOP_Y + 1; y <= carveTopY; y++) {
            pos.set(worldX, y, worldZ);
            if (!serverLevel.getBlockState(pos).isAir()) {
                serverLevel.setBlock(pos, air, Block.UPDATE_CLIENTS);
                changed++;
            }
        }

        return changed;
    }

    private static int computeUndersideY(final int worldX, final int worldZ, final int surfaceY) {
        final int noise = Math.floorMod(hash(worldX, worldZ), UNDERSIDE_VARIATION * 2 + 1) - UNDERSIDE_VARIATION;
        final int roughUnderside = BASE_UNDERSIDE_Y + noise;

        // Keep at least 10 blocks of mass under the natural surface where possible.
        return Math.min(roughUnderside, surfaceY - 10);
    }

    private static boolean isFluidColumn(
            final ChunkAccess chunk,
            final BlockPos surfacePos,
            final String surfaceBlockId
    ) {
        return chunk.getFluidState(surfacePos).is(FluidTags.WATER)
                || surfaceBlockId.startsWith("tfc:fluid/")
                || surfaceBlockId.contains("water");
    }

    private static BlockState chooseOceanWaterBlock() {
        final ResourceLocation tfcSaltWaterId = ResourceLocation.fromNamespaceAndPath("tfc", "fluid/salt_water");

        final Block tfcSaltWater = BuiltInRegistries.BLOCK.get(tfcSaltWaterId);

        if (tfcSaltWater != Blocks.AIR) {
            return tfcSaltWater.defaultBlockState();
        }

        return Blocks.WATER.defaultBlockState();
    }

    private static int hash(final int x, final int z) {
        int h = x * 73428767 ^ z * 912367;
        h ^= (h >>> 13);
        h *= 1274126177;
        h ^= (h >>> 16);
        return h;
    }

    private static int clamp(final int value, final int min, final int max) {
        return Math.max(min, Math.min(max, value));
    }
}