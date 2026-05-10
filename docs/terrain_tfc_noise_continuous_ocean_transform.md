# TFC Noise-Stage Continuous Ocean Transform

## Purpose

Move from bounded region prototypes to a continuous transform applied to all TFC overworld chunks as they generate.

## Target

`net.dries007.tfc.world.TFCChunkGenerator#fillFromNoise`

## Behavior

This prototype:

- transforms every generated TFC overworld ProtoChunk
- preserves original TFC surface Y
- carves below land-like columns
- carves low/wet/shore-like columns above Y=0
- adds a simple lower ocean crust
- fills the lower world with water up to Y=0
- avoids block/fluid registry lookups
- does not use `serverLevel.setBlock`
- does not use `ChunkEvent.Load`

## Current Constants

- Ocean top: Y=0
- Ocean crust top: minY + 8
- Land trigger: surface Y >= 66
- Base island thickness: 44
- Thickness variation: +/-10

## Notes

The ocean crust uses vanilla stone for now to avoid TFC registry timing problems. Later versions should replace this with TFC-compatible rock/crust logic after the continuous terrain transform is stable.