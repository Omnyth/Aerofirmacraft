# TFC Noise-Stage 3x3 Region Transform V2

## Purpose

Move from partial 3x3 behavior toward a complete small-region floating terrain transform.

## Target

`net.dries007.tfc.world.TFCChunkGenerator#fillFromNoise`

## Changes from V1

- The first height-valid chunk still selects the target region center.
- Once the target is selected, all chunks inside the target 3x3 may transform, including low/shore chunks.
- Chunks outside the target 3x3 are skipped.
- Transform numbering uses an AtomicInteger instead of a concurrent set size.
- Skip log spam is reduced.

## Behavior

This prototype:

- transforms up to 9 chunks in a target 3x3 region
- preserves original TFC surface Y
- carves below land-like columns
- carves low/wet/shore-like columns above Y=0
- avoids block/fluid registry lookups
- does not use `serverLevel.setBlock`
- does not use `ChunkEvent.Load`