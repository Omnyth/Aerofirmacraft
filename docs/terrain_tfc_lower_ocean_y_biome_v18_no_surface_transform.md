# Lower Ocean Y-Biome V18 No Surface Transform

## Purpose

Separate lower-ocean biome/generation logic from sky-gap and island terrain transforms.

## Why

The previous branches mixed biome identity with surface carving. V16 especially showed that carving after features leaves unsupported feature leftovers in the air.

V18 removes the surface transform entirely so the lower-ocean layer can be validated independently.

## What V18 Does

- Keeps the overworld minY extension from V14/V15.
- Assigns existing `tfc:ocean` to the lower Y range.
- Fills only the new lower band below old TFC minY.
- Does not carve old ocean into sky gaps.
- Does not carve island undersides.
- Does not assign `sky_gap` biome.

## Vertical Model

- Y=-128: bedrock
- Y=-127..-120: crust
- Y=-119..-65: lower ocean water
- Y=-64..-61 biome cell: `tfc:ocean`
- Y=-60+ normal TFC terrain/biomes, untouched by AFC surface transforms

## Important Note

Biome JSON does not define the vertical placement range in this TFC setup. V18 implements the lower-ocean Y range at the biome assignment stage.