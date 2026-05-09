# Terrain Chunk Classifier Notes

## Finding

TFC salt water appears as surface block IDs such as:

- tfc:fluid/salt_water

The previous classifier counted these as solid because the vanilla water fluid tag did not catch them.

## Fix

Treat these as fluid columns:

- any block ID starting with tfc:fluid/
- any block ID containing water
- any surface fluid state tagged as vanilla water

## Current rough classes

- open_water_deep
- open_water_shallow
- shore_mixed
- shore_edge
- low_coastal_land
- land
- land_high_relief
- mostly_air
- mixed_unknown