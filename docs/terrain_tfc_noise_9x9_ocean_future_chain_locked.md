# TFC Noise-Stage 9x9 Ocean Future-Chain Locked Prototype

## Purpose

Test the safer AFC transform path:

- chain AFC mutation into the returned `fillFromNoise` future
- lock all chunk sections while mutating blocks
- keep the bounded 9x9 ocean-floor test

## Reason

The prior `whenComplete` branches could mutate chunks after their status advanced.

The first future-chain branch improved timing and showed transforms occurring while chunks still reported `minecraft:biomes`.

The next risk is `PalettedContainer` access from multiple threads, so this branch wraps mutations in `LevelChunkSection#acquire()` and `LevelChunkSection#release()`.

## Behavior

- selects the first height-valid chunk as target center
- transforms up to 81 chunks in a 9x9 region
- preserves original TFC surface Y
- adds lower ocean crust/water
- avoids block/fluid registry lookups
- does not use `serverLevel.setBlock`
- does not use `ChunkEvent.Load`
- fixes low-only chunk underside logging fallback