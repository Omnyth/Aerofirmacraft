# Floating TFC Regions Terrain Plan

## Goal

Use TFC's normal region/island/continent layout, but make the landmasses float above a global ocean floor.

## Interpretation

Existing TFC land:
- preserved horizontally
- becomes floating island/continent terrain

Existing TFC ocean:
- becomes open sky gap between floating landmasses

Bottom of world:
- filled with a separate global ocean/deep-water layer

## First implementation milestone

Create a separate NeoForge addon under:

terrain-addon/

Initial behavior:
- load successfully
- log during common setup
- log during server starting
- make no worldgen changes yet

## Later implementation milestones

1. Identify worldgen/chunk-generation hook point.
2. Detect overworld generation context.
3. Inspect TFC terrain height/biome/region data if accessible.
4. Prototype crude land/ocean column classification.
5. Preserve land columns.
6. Carve air under land columns.
7. Replace normal ocean columns above the bottom ocean with air.
8. Fill bottom layer with water.
9. Add underside shaping after crude transform works.