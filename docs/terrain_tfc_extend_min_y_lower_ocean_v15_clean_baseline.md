# Extend Min-Y Lower Ocean V15 Clean Baseline

## Purpose

Make V14 the clean baseline architecture.

## Baseline World Model

- Overworld dimension minimum Y is extended to `-128`.
- TFC continues to own its normal terrain space from old Y `-64` upward.
- Aerofirmacraft owns the new lower band from `-128..-65`.

## Vertical Layout

- `Y=-128`: bedrock
- `Y=-127..-120`: crust
- `Y=-119..-65`: lower ocean water
- `Y=-64+`: TFC terrain, carved into floating terrain or sky gaps

## Surface Classification

Old TFC ocean/coastal biomes become sky gaps.

Definite ocean sky gaps:

- `tfc:ocean`
- `tfc:ocean_reef`
- `tfc:deep_ocean`
- `tfc:deep_ocean_trench`

Coastal sky gaps:

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

Everything else is preserved as floating terrain, including rivers, lakes, dune sea, and oceanic mountain variants.

## Notes

V15 reduces log noise, adds a dimension sanity check, and keeps V14 generation behavior.