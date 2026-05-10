# Real Lower Ocean Biome V23a Disable Noise Mixin

## Purpose

Recover from the V23 startup stop.

## V23 Problem

The V23 jar was installed and contained `ChunkNoiseFillerMixin`, but the game stopped before Aerofirmacraft's constructor or V23 terrain logs appeared.

This points to the direct `ChunkNoiseFiller` private-method mixin being too risky for startup.

## V23a Change

- Remove `ChunkNoiseFillerMixin` from active mixins.
- Keep the V22/V23 lower_ocean setup:
  - min_y = -256
  - extended overworld noise settings
  - real `aerofirmacraft_terrain:lower_ocean` BiomeExtension
  - lower Y biome assignment
- Do not manually fill water/stone/bedrock.

## Next Direction

After startup is stable again, use a less invasive generator hook instead of injecting directly into `ChunkNoiseFiller#calculateNoiseAtHeight`.