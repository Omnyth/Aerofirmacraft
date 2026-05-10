# Real Lower Ocean Biome V22 Min-Y -256 Diagnostic

## Purpose

Move the lower world down to `min_y=-256` so there is a larger separation between the original TFC terrain band and the experimental lower ocean band.

## V21b Result

V21b proved that extending noise settings downward works: TFC generated blocks in the added lower band instead of leaving it as air.

However, the lower band generated mostly raw rock, not ocean water.

## V22 Changes

- Dimension type min_y: `-256`
- Overworld noise_settings min_y: `-256`
- Original TFC terrain floor remains conceptually at `-64`
- Experimental lower ocean probe band:
  - `Y=-256..-193`
- Gap / transition space:
  - `Y=-192..-65`
- Original TFC terrain starts around:
  - `Y=-64+`

## Lower Ocean Extension Parameters

The experimental `aerofirmacraft_terrain:lower_ocean` BiomeExtension is shifted deeper:

- heightmap: `BiomeNoise.ocean(seed, -218, -204)`
- aquifer offset: `-216`
- salty
- ocean blend
- no rivers

## No Manual Fill

V22 still does not manually place water, stone, or bedrock for the lower ocean.

The point is to observe what TFC generates from the deeper dimension/noise/biome-extension setup.