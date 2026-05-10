# TFC Noise-Stage 9x9 Ocean Floor Prototype

## Purpose

Scale the working 5x5 floating-island + ocean-floor transform to a larger 9x9 gameplay-scale test region.

## Target

`net.dries007.tfc.world.TFCChunkGenerator#fillFromNoise`

## Behavior

This prototype:

- selects the first height-valid chunk as the target center
- transforms up to 81 chunks in a 9x9 region
- preserves original TFC surface Y
- carves below land-like columns
- carves low/wet/shore-like columns above Y=0
- adds a simple ocean crust at the bottom of the world
- fills the lower world with water up to Y=0
- avoids block/fluid registry lookups
- does not use `serverLevel.setBlock`
- does not use `ChunkEvent.Load`

## Notes

This is a heavier test than 5x5. If world creation pauses longer, that is expected. The important thing is whether it completes without watchdog-style stalls, salt-water registry crashes, or far-chunk setBlock warnings.