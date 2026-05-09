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
## Verified scaffold load

Date: 2026-05-09

The terrain addon scaffold was tested in the CurseForge Aerofirmacraft instance.

Confirmed log output:
- Aerofirmacraft Terrain 0.1.0 appears in the loaded mod list.
- Aerofirmacraft Terrain constructed successfully.
- Common setup completed.
- Server starting event detected minecraft:overworld.

Current addon behavior:
- No terrain changes.
- Lifecycle logging only.

## Current branch: terrain-worldgen-diagnostics

Goal:
Add diagnostics only.

This branch should confirm:
- the addon sees server level load events
- the addon sees newly generated overworld chunks
- the addon records chunk status and height range
- no terrain changes occur

Important:
Do not inspect or mutate chunk block contents in ChunkEvent.Load yet.

## Diagnostics v2

The first diagnostics pass confirmed:
- server level load events are visible
- overworld/nether/end dimensions are visible
- overworld build height is -64 to 320
- server starting event is visible

The first pass did not show new chunk logs, so v2 broadens ChunkEvent.Load logging:
- logs first 64 overworld chunk load events
- includes event.isNewChunk()
- still does not inspect or mutate block data

## Surface sampling diagnostics

Chunk diagnostics confirmed that ChunkEvent.Load sees new overworld chunks, but only at full generated status.

Next diagnostic step:
- read-only center-column samples
- record WORLD_SURFACE and OCEAN_FLOOR heights
- record surface block ID
- record whether water fluid is present

This is still diagnostics only. No terrain modification.

## Chunk summary diagnostics

Surface sampling confirmed land/highland samples around spawn.

Next diagnostic step:
- summarize all 256 columns per new overworld chunk
- classify chunks as solid_land, solid_land_high_relief, mostly_water, mixed_shore_or_river, mostly_air, or mixed_unknown
- continue read-only diagnostics only

## Chunk classifier correction

The first 16x16 chunk summary pass showed TFC salt water surfaces as dominantSurface=tfc:fluid/salt_water, but those columns were being counted as solid.

Classifier updated:
- tfc:fluid/* is now treated as fluid
- block IDs containing water are treated as fluid
- plants/leaves are treated as land surface rather than separate non-land columns

Goal:
Get reliable rough classes for open water, shore, coastal land, and normal land.

## Chunk classifier verified

Date: 2026-05-09

The corrected chunk classifier was tested in a fresh CurseForge Aerofirmacraft world.

Confirmed classes:
- open_water_deep
- shore_mixed
- shore_edge
- low_coastal_land
- land

Confirmed TFC fluid handling:
- tfc:fluid/salt_water is now counted as fluid.
- Open water chunks can show fluidColumns=256.
- Shore chunks show mixed landColumns/fluidColumns.
- Land chunks show landColumns near 256.

Conclusion:
The diagnostic classifier is useful enough to inform the first crude terrain transform prototype.
ChunkEvent.Load sees chunks at minecraft:full status, so it is likely too late for clean worldgen-stage shaping, but it may be usable for a crude post-generation proof of concept.

## Current branch: terrain-tfc-fillfromnoise-mixin-canary

Goal:
Prove we can hook TFCChunkGenerator.fillFromNoise before full chunk load.

Reason:
The post-generation transform from ChunkEvent.Load caused load stalls because chunks were already full LevelChunks.

New target:
Mixin into net.dries007.tfc.world.TFCChunkGenerator#fillFromNoise.

Current behavior:
- log at HEAD
- log when returned CompletableFuture completes
- no terrain mutation
