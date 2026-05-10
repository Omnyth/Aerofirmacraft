# TFC Noise-Stage Continuous Ocean Locked V3

## Purpose

Move from the successful bounded 9x9 locked v3 prototype to continuous terrain transformation.

## Proven Pattern

The successful 9x9 locked v3 prototype used:

- `fillFromNoise` return future chaining
- `LevelChunkSection#acquire()` / `release()`
- direct `LevelChunkSection#getBlockState`
- direct `LevelChunkSection#setBlockState(..., false)`

## Behavior

This prototype:

- transforms every generated TFC overworld ProtoChunk
- preserves original TFC surface Y
- carves below land-like columns
- carves low/wet/shore-like columns above Y=0
- adds bottom crust using bedrock + vanilla stone
- fills lower ocean water up to Y=0
- avoids TFC block/fluid registry lookups
- does not use `serverLevel.setBlock`
- does not use `ChunkEvent.Load`

## Current Constants

- Ocean top: Y=0
- Ocean crust top: minY + 8
- Land trigger: surface Y >= 66
- Base island thickness: 44
- Thickness variation: +/-10