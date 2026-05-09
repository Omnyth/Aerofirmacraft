# Aerofirmacraft AI Project Context

## Current target

Aerofirmacraft is a Minecraft 1.21.1 NeoForge modpack concept built around:

- TerraFirmaCraft survival
- Create mechanical industry
- optional GregTech industrialization
- Create Aeronautics travel

## Current terrain direction

Create an Aerofirmacraft terrain addon that preserves normal TFC regions, islands, continents, climate, rock, and biome layout horizontally, but transforms the vertical profile.

## Desired world structure

- Normal TFC land regions become floating landmasses.
- Normal TFC ocean regions become open sky gaps.
- A separate global ocean exists near the bottom of the world.
- Do not create tiny generic sky islands.
- Do not generate custom fantasy islands from scratch unless TFC-preserving reshaping proves impossible.

## Prototype goal

First goal is not terrain changes.

First goal:

1. Build a NeoForge addon that targets Minecraft 1.21.1 and NeoForge 21.1.228.
2. Confirm the addon compiles.
3. Confirm the addon logs lifecycle events.
4. Then investigate safe NeoForge/TFC worldgen hook points.
