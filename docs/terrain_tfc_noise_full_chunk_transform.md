# TFC Noise-Stage Full Chunk Transform

## Purpose

Scale from one 8x8 patch to one full 16x16 ProtoChunk transform.

## Target

`net.dries007.tfc.world.TFCChunkGenerator#fillFromNoise`

## Design Rules

- Preserve TFC's original surface Y.
- Do not vertically move islands.
- Carve under terrain to create floating mass.
- Use thicker land mass for later ore testing.
- Avoid block/fluid registry lookups.
- Do not use `serverLevel.setBlock`.
- Do not use `ChunkEvent.Load`.

## Behavior

This prototype:

- skips chunks until center surface height is at least 66
- transforms one full 16x16 chunk only
- treats high columns as land-like
- treats low columns as ocean/shore/lowland-like
- carves air below land-like columns
- adds sparse glowstone underside markers