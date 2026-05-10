# TFC Noise-Stage Land-Center Prototype

## Purpose

Avoid the crashing land-patch prototype and test a minimal land-targeted floating transform.

## Target

`net.dries007.tfc.world.TFCChunkGenerator#fillFromNoise`

## Behavior

This prototype:

- skips chunks whose center surface is fluid-like
- transforms the first chunk whose center surface is land-like
- modifies one 8x8 patch only
- carves air under land columns
- carves fluid columns above Y=0
- marks land undersides with glowstone

## Safety

This avoids:

- `serverLevel.setBlock`
- `ChunkEvent.Load`
- full chunk transforms
- cross-chunk writes
- the previous heavier PatchStats land-patch implementation