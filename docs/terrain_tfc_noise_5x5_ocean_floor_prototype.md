# TFC Noise-Stage 5x5 Ocean Floor Prototype

## Purpose

Add the first lower-ocean layer beneath the working 5x5 floating-island transform.

## Target

`net.dries007.tfc.world.TFCChunkGenerator#fillFromNoise`

## Behavior

This prototype:

- selects the first height-valid chunk as the target center
- transforms up to 25 chunks in a 5x5 region
- preserves original TFC surface Y
- carves below land-like columns
- carves low/wet/shore-like columns above Y=0
- adds a simple ocean crust at the bottom of the world
- fills the lower world with water up to Y=0
- removes glowstone debug markers
- avoids block/fluid registry lookups
- does not use `serverLevel.setBlock`
- does not use `ChunkEvent.Load`

## Notes

The ocean crust uses vanilla stone for now to avoid TFC registry timing problems. Later versions should replace this with TFC-compatible rock/crust logic.