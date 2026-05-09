# Terrain Worldgen Diagnostics

## Branch

terrain-worldgen-diagnostics

## Purpose

Add safe logging only. No terrain changes.

## Current diagnostic targets

- Server starting event
- Server level load event
- Newly generated overworld chunk load event

## Safety rule

Chunk load diagnostics must not inspect or modify block data yet. NeoForge chunk load events can happen before the chunk is fully promoted, so diagnostics should only record metadata until a safer hook point is identified.

## Current questions

1. Which dimensions are loaded in the pack?
2. When does the overworld become visible to the addon?
3. Are newly generated chunks detectable with ChunkEvent.Load and isNewChunk?
4. What chunk status is visible at that point?
5. Is this event too late, too early, or unsafe for terrain reshaping?