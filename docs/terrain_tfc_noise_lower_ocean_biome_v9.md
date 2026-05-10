# Lower Ocean Biome V9

## Purpose

Add `aerofirmacraft_terrain:lower_ocean` and assign it to the generated lower ocean layer.

## Biome Source

The biome JSON is copied directly from the installed TFC jar:

`data/tfc/worldgen/biome/ocean.json`

This is intentional. The goal is for the new lower ocean to behave like the original TFC ocean, only at the lower ocean layer.

## Vertical Assignment

- Y <= 0 biome cells: `aerofirmacraft_terrain:lower_ocean`
- Y > 0 carved old-ocean/coastal gap cells: `aerofirmacraft_terrain:sky_gap`
- Y > 0 preserved land/river/lake cells: original TFC biome

Biome cells are 4 blocks tall, so the Y=0 biome cell covers Y=0..3.

## Notes

This branch still generates lower ocean water/crust blocks physically. Biome assignment only gives that layer its biome identity.