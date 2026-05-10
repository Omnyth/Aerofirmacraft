# Real Lower Ocean Biome V23 Lower Noise Hook

## Purpose

Make the lower band actually sample the `aerofirmacraft_terrain:lower_ocean` BiomeExtension noise, rather than only labeling the vertical biome palette as lower_ocean.

## V22 Result

V22 proved:

- min_y=-256 works
- noise settings extension works
- lower_ocean BiomeExtension registers
- lower vertical biome cells identify as lower_ocean
- TFC generates in the lower band

But the generated lower band was mostly raw TFC rock, meaning TFC's filler still used the upper X/Z column biome extension for density.

## V23 Changes

- Keeps dimension/noise min_y=-256.
- Keeps lower_ocean biome cells at Y=-256..-189.
- Corrects lower_ocean height parameters:
  - `BiomeNoise.ocean(seed, -281, -267)`
  - This targets actual terrain heights around Y=-218..-204 because TFC's ocean helper internally uses sea-level-relative offsets.
- Restores TFC-style ocean aquifer offset:
  - `aquiferHeightOffset(-24)`
- Adds `ChunkNoiseFillerMixin`.
- For Y=-256..-193 only, `calculateNoiseAtHeight` now samples the lower_ocean noise sampler.

## Still No Manual Fill

V23 still does not place static water, stone, or bedrock.