# TFC Noise-Stage Land Patch Prototype

## Purpose

Find a generated ProtoChunk with enough land columns and apply a small floating-land transform.

## Target

`net.dries007.tfc.world.TFCChunkGenerator#fillFromNoise`

## Behavior

This prototype:

- skips fluid-only patches
- waits for an 8x8 patch with at least 24 land columns
- transforms one chunk only
- carves air under land columns
- carves water/fluid columns above Y=0
- marks land undersides with glowstone

## Safety

This does not use:

- `serverLevel.setBlock`
- `ChunkEvent.Load`
- cross-chunk writes
- full world transforms