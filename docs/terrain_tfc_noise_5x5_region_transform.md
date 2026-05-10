# TFC Noise-Stage 5x5 Region Transform

## Purpose

Scale the small-region floating transform from 3x3 to 5x5.

## Target

`net.dries007.tfc.world.TFCChunkGenerator#fillFromNoise`

## Changes from 3x3 V2

- Region radius increased from 1 to 2 chunks.
- Max transformed chunks increased from 9 to 25.
- Center-underside fallback is fixed for low-center chunks.
- Early chunks skipped before target selection are stored briefly and replayed if they fall inside the selected region.

## Behavior

This prototype:

- selects the first height-valid chunk as the region center
- transforms up to 25 chunks in a target 5x5 region
- preserves original TFC surface Y
- carves below land-like columns
- carves low/wet/shore-like columns above Y=0
- avoids block/fluid registry lookups
- does not use `serverLevel.setBlock`
- does not use `ChunkEvent.Load`