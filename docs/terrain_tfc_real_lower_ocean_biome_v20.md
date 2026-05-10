# Real Lower Ocean Biome V20

## Purpose

Register `aerofirmacraft_terrain:lower_ocean` as a real TFC-compatible biome, not only a vanilla biome label.

## Why

Previous branches assigned a biome ID and/or placed blocks manually. That did not test the thing we actually need: a proper TFC `BiomeExtension` with ocean-style terrain parameters.

## What V20 Adds

Normal biome JSON:

`data/aerofirmacraft_terrain/worldgen/biome/lower_ocean.json`

This is copied from TFC ocean JSON.

TFC BiomeExtension:

`aerofirmacraft_terrain:lower_ocean`

Extension parameters copied from TFC ocean and shifted lower:

- heightmap: `BiomeNoise.ocean(seed, -90, -76)`
- surface: `ShoreAndOceanSurfaceBuilder.OCEAN`
- aquifer offset: `-88`
- salty
- biome blend: ocean
- no rivers

## What V20 Does Not Do

V20 does not place water, stone, bedrock, or carve sky gaps.

It only:

- registers the real custom TFC lower ocean biome extension
- assigns the lower Y range to that biome
- probes what generation produces

## Interpretation

If this still does not generate lower-ocean blocks naturally, then the custom BiomeExtension is valid but not sufficient by itself because TFC's generator is not sampling a vertical biome-extension layer. The next target would be the actual TFC height/noise filler path.