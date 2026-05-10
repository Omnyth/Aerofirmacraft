# TFC Noise-Stage Height-Gated Land Patch

## Purpose

Avoid TFC fluid/block registry lookups while testing a land-bearing 8x8 floating patch.

## Target

`net.dries007.tfc.world.TFCChunkGenerator#fillFromNoise`

## Behavior

This prototype:

- skips chunks whose center surface height is below 66
- transforms the first chunk with center surface height 66 or higher
- modifies one 8x8 patch only
- treats high columns as land-like
- treats low columns as water/lowland-like
- uses no block registry ID lookup

## Reason

The previous land-patch and land-center prototypes crashed during mod loading with:

`ResourceKey[minecraft:fluid / tfc:salt_water]`

This branch avoids block/fluid registry inspection entirely.