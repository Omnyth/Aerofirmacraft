# Real Lower Ocean Biome V21 Noise Settings Diagnostic

## Purpose

Test whether TFC's lower-band generation is blocked by the vanilla overworld noise settings range.

## V20c Result

V20c successfully registered `aerofirmacraft_terrain:lower_ocean` as a real TFC BiomeExtension and successfully assigned lower Y biome cells to it.

However, the lower band remained entirely air:

- lowerAirBlocks = 16384
- lowerNonAirBlocks = 0
- lowerFluidBlocks = 0

## V21 Change

V21 keeps the V20c biome extension and lower Y biome assignment, but adds:

`data/minecraft/worldgen/noise_settings/overworld.json`

The override extends vanilla overworld noise settings from old min_y to:

`min_y = -128`

The top Y remains unchanged.

## What V21 Does Not Do

V21 does not manually place:

- water
- stone
- bedrock

V21 does not carve:

- sky gaps
- island undersides

## Interpretation

If lowerNonAirBlocks remains 0, then extending the noise settings did not make TFC generate the lower band.

If lowerNonAirBlocks becomes high but lowerFluidBlocks remains 0, then TFC noise is generating lower terrain but not using lower_ocean aquifer/ocean parameters vertically.

If lowerFluidBlocks becomes high, then the noise settings range was the missing piece.