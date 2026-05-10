# Lower Ocean TFC-Ocean V10

## Purpose

Fix the V9 crash caused by assigning a custom AFC lower-ocean biome.

## V9 Problem

V9 copied TFC ocean biome JSON into:

`aerofirmacraft_terrain:lower_ocean`

The JSON loaded, but later generation crashed with:

`NullPointerException: Biome: aerofirmacraft_terrain:lower_ocean`

Likely cause: TFC terrain/decoration systems expect TFC biome-extension data for biomes participating in TFC worldgen. Copying the JSON does not make the biome a native TFC biome.

## V10 Fix

Use the existing `tfc:ocean` biome holder directly for the lower ocean layer.

## Vertical Assignment

- Y <= 0 biome cells: `tfc:ocean`
- Y > 0 carved old-ocean/coastal gap cells: `aerofirmacraft_terrain:sky_gap`
- Y > 0 preserved land/river/lake cells: original TFC biome

## Notes

This keeps the user's intended behavior: original TFC ocean behavior, but moved down into the generated lower ocean layer.

The custom `aerofirmacraft_terrain:lower_ocean` biome is removed for now.