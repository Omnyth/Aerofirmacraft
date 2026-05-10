# Biome Pre-Noise Only V12 Diagnostic

## Purpose

Test whether changing the biome palette before `fillFromNoise` is enough to make TFC generate lower-ocean terrain naturally.

## Behavior

This branch intentionally disables the AFC block transform.

It only:

- assigns existing `tfc:ocean` to biome cells from world bottom through the Y=0 biome cell during `createBiomes`
- lets TFC `fillFromNoise` run normally
- probes the resulting chunk after noise generation
- logs lower-layer water/non-air counts and biome identity

## Interpretation

If lowerWaterBlocks are high without AFC placing water, then TFC is reacting to the assigned lower biome.

If lowerWaterBlocks remain low/zero or look like normal terrain, then TFC terrain generation is not driven by section biome palette alone. In that case, the correct deeper solution is to hook TFC's generator/height/noise logic rather than relying on biome JSON or palette edits.