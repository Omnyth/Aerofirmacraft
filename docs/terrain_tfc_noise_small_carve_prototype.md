# TFC Noise-Stage Small Carve Prototype

## Purpose

Confirm that Aerofirmacraft can safely remove a small amount of terrain during TFC noise-stage generation.

## Target

`net.dries007.tfc.world.TFCChunkGenerator#fillFromNoise`

## Behavior

After TFC fillFromNoise completes its returned CompletableFuture, this prototype modifies exactly one generated ProtoChunk:

- finds the center surface
- carves one 4x4 shaft
- places a 4x4 glowstone floor
- logs a teleport hint

## Why this is safer than the rejected post-generation transform

This uses `ChunkAccess#setBlockState(...)` on a `ProtoChunk` at `minecraft:noise`.

It does not use:

- `serverLevel.setBlock(...)`
- `ChunkEvent.Load`
- full chunk rewriting
- mass vertical carving

## Limits

- One chunk only.
- 4x4 columns only.
- Fresh disposable worlds only.