# Lower Ocean Biome-Only V19 No Block Fill

## Purpose

Remove all lower-band block placement and test biome-driven generation alone.

## Why

V18 still explicitly placed lower-band blocks:

- bedrock
- stone crust
- water

So visually the lower ocean was still an AFC transform, even though the biome identity was `tfc:ocean`.

## V19 Behavior

V19 does not place lower-band blocks.

It only:

- keeps the minY=-128 dimension extension
- assigns existing `tfc:ocean` to the lower Y range
- reassigns it after fillFromNoise for persistence
- probes what blocks TFC actually generated in the lower band

## Interpretation

If the lower band is mostly air, then TFC does not generate there by default and biome assignment alone is insufficient.

If the lower band contains native TFC ocean/fluid without AFC placement, then the biome-only approach is viable.

## Expected Result

Most likely, the lower band will be mostly air because TFC's noise settings/generation still appear to own only the old -64+ terrain space.