# TFC Noise-Stage Continuous Ocean Locked V6 Ocean Biome Skygap

## Purpose

Classify sky gaps by reviewed TFC biome IDs instead of height or name substring matching.

## Problem

Height-based classification removed rivers, ponds, and small inland water features.

Name-based classification would be dangerous because some TFC biome names contain ocean-like words but are not ocean-water regions, such as:

- `tfc:dune_sea`
- `tfc:oceanic_mountains`
- `tfc:oceanic_mountain_lake`
- `tfc:volcanic_oceanic_mountains`
- `tfc:glaciated_oceanic_mountains`

## V6 Rule

Definite ocean sky-gap biomes:

- `tfc:ocean`
- `tfc:ocean_reef`
- `tfc:deep_ocean`
- `tfc:deep_ocean_trench`

Coastal/edge sky-gap biomes:

- `tfc:embayments`
- `tfc:shore`
- `tfc:tidal_flats`
- `tfc:sea_stacks`
- `tfc:terrace_upper`
- `tfc:terrace_lower`
- `tfc:setback_cliffs`
- `tfc:coastal_dunes`
- `tfc:rocky_shores`
- `tfc:shield_volcano_shore`
- `tfc:old_shield_volcano_shore`
- `tfc:ice_sheet_oceanic`
- `tfc:ice_sheet_shore`

Everything else is preserved as floating terrain, including rivers, lakes, ponds, dune seas, oceanic mountains, and oceanic mountain lakes.

## Foundation

This branch keeps:

- future-chain RETURN injection
- LevelChunkSection acquire/release
- direct LevelChunkSection get/set
- ProtoChunk-only transform
- lower ocean/crust layer