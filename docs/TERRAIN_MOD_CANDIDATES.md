# Terrain Mod Candidates

## Goal

Find or build a way to generate:

- TFC landmasses
- TFC climate
- TFC rock layers
- TFC ores/resources
- oceans converted into sky/void
- floating continents/islands

## Existing Mod Candidates

### Sky Archipelago

Status: candidate for gameplay testing only.

Pros:
- Minecraft 1.21.1
- NeoForge
- floating island worldgen
- configurable void/ocean behavior
- built toward Create Aeronautics-style gameplay

Cons:
- not TFC-aware
- not designed to preserve TFC rocks/ores/climate
- survival support may still be incomplete
- license appears to be All Rights Reserved, so do not include without permission

Decision:
Useful to test airship sky-island gameplay feel.
Not suitable as the final terrain solution unless permission and compatibility are solved.

### TFC: Real World

Status: candidate code/reference lead.

Pros:
- TFC-integrated worldgen addon
- reshapes TFC worlds using map data
- uses mixins to redirect worldgen rules
- MIT license
- source available

Cons:
- creates Earth-like terrain, not sky islands
- does not directly solve ocean-to-sky
- may be useful as reference rather than dependency

Decision:
Best lead for understanding how to alter TFC worldgen cleanly.

## Likely Final Solution

Create a small custom NeoForge addon:

Aerofirmacraft Terrain

Target behavior:
1. Let TFC generate normal terrain data.
2. Identify ocean or low-water columns.
3. Remove those columns to air/void.
4. Preserve land columns.
5. Later: shape undersides.
6. Later: add sky-ocean resource islands such as brine pools, kelp lagoons, salt flats.

## Decision

Do not continue major recipe/progression work until we prove this terrain approach is possible.
