# Extend Min-Y Lower Ocean V14

## Purpose

Move the lower ocean into newly-added vertical world space below TFC's normal floor.

## Reason

V12 proved that assigning `tfc:ocean` below Y=0 does not make TFC naturally generate lower ocean water. The biome identity changed, but the block terrain remained mostly solid.

## V14 Model

- Override overworld dimension type:
  - old min_y: usually -64
  - new min_y: -128
  - top Y preserved
- Let TFC continue generating its normal terrain mostly at old Y=-64 and above.
- AFC fills the new lower band:
  - new bottom: bedrock
  - bottom crust: stone
  - lower ocean: water up to Y=-65
- Old TFC ocean/coastal biomes above the lower ocean are still carved as sky gaps.
- Preserved land/river/lake biomes are undercarved into floating terrain.

## New-World Warning

Changing dimension min_y should only be tested on fresh disposable worlds.