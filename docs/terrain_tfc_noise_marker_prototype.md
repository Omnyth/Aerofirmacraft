# TFC Noise-Stage Marker Prototype

## Purpose

Confirm that Aerofirmacraft can safely mutate a ProtoChunk during TFC terrain generation.

## Target

`net.dries007.tfc.world.TFCChunkGenerator#fillFromNoise`

## Behavior

After TFC fillFromNoise completes its returned CompletableFuture, this prototype places a tiny 3-block glowstone marker at the center of a limited number of generated chunks.

## Why this is safer than ChunkEvent.Load

This uses `ChunkAccess#setBlockState(...)` on a `ProtoChunk` at the noise generation stage, instead of using `serverLevel.setBlock(...)` on a fully loaded `LevelChunk`.

## Limits

- Marker placement only.
- First 12 chunks only.
- No full terrain carving.
- No server-level block updates.
- Fresh disposable worlds only.

## Success criteria

The latest.log should contain `AFC noise marker: placed` lines with coordinates.

If visible in-world and no loading stall occurs, the next prototype can test a very small terrain carve inside the same generation-stage path.