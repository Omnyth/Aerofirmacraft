# TFC Noise-Stage 3x3 Region Transform

## Purpose

Scale the full-chunk transform to a small adjacent region.

## Target

`net.dries007.tfc.world.TFCChunkGenerator#fillFromNoise`

## Behavior

This prototype:

- selects the first height-valid chunk as the region center
- transforms chunks within a 3x3 area around that center
- transforms up to 9 chunks
- preserves original TFC surface Y
- carves below terrain
- uses thicker island mass for later ore validation
- avoids block/fluid registry lookups
- does not use `serverLevel.setBlock`
- does not use `ChunkEvent.Load`

## Notes

Because chunk generation is asynchronous, not every neighbor is guaranteed to arrive after the target is selected. This branch is primarily for visual and border behavior testing.