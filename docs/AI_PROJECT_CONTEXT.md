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

## Current branch: terrain-tfc-noise-marker-prototype

Goal:
Perform the smallest possible terrain mutation inside TFCChunkGenerator.fillFromNoise completion.

Behavior:
- place a 3-block glowstone marker in the center of the first 12 generated chunks
- mutate the ProtoChunk directly with ChunkAccess#setBlockState
- do not use serverLevel.setBlock
- do not mass-carve terrain

Success:
Markers appear or at least log as placed, and world loading does not stall.

## Current branch: terrain-tfc-noise-small-carve-prototype

Goal:
Test the smallest useful terrain removal during TFCChunkGenerator.fillFromNoise completion.

Behavior:
- mutate one ProtoChunk only
- carve a 4x4 shaft around the chunk center
- place a glowstone floor
- log teleport coordinates
- do not use serverLevel.setBlock
- do not use ChunkEvent.Load
- do not mass-carve terrain

Success:
World loads without stalling and latest.log contains AFC small carve: applied.

## Current branch: terrain-tfc-noise-floating-patch-prototype

Goal:
Test a small version of the intended floating terrain transform inside TFCChunkGenerator.fillFromNoise completion.

Behavior:
- mutate one ProtoChunk only
- center 8x8 columns only
- land columns preserve top terrain and carve air underneath
- fluid columns carve air above Y=0
- glowstone marks land underside positions
- do not use serverLevel.setBlock
- do not use ChunkEvent.Load

Success:
World loads without stalling and latest.log contains AFC floating patch: applied.

## Current branch: terrain-tfc-noise-height-gated-land-patch

Goal:
Avoid the TFC salt_water registry crash while testing a land-bearing patch.

Reason:
The previous land-patch and land-center prototypes crashed during mod loading with tfc:salt_water unbound value.

Behavior:
- start from known-good floating patch branch
- no BuiltInRegistries/block ID inspection
- skip chunks until center surface height >= 66
- mutate one ProtoChunk only
- use ChunkAccess#setBlockState only

## Current branch: terrain-tfc-noise-full-chunk-transform

Goal:
Scale the height-gated transform from an 8x8 patch to one full 16x16 ProtoChunk.

Design:
- preserve original TFC surface Y to avoid disrupting climate assumptions
- carve underneath instead of moving islands
- use thicker island mass for later ore validation
- avoid block/fluid registry lookups because previous registry-ID classifiers triggered tfc:salt_water crashes
- mutate only one ProtoChunk at minecraft:noise

## Current branch: terrain-tfc-noise-3x3-region-transform

Goal:
Scale from one full transformed chunk to a small 3x3 transformed region.

Design:
- first height-valid chunk becomes target center
- transform up to 9 chunks in target 3x3
- preserve original TFC surface Y
- carve underneath
- no block/fluid registry lookups
- no serverLevel.setBlock
- no ChunkEvent.Load

## Current branch: terrain-tfc-noise-3x3-region-transform-v2

Goal:
Apply the V1 fixes while moving forward.

Changes:
- target center is still selected by first height-valid chunk
- once target is selected, all chunks inside the target 3x3 can transform
- low/shore chunks inside the selected target region are no longer skipped just because their center surface is low
- transform numbering is fixed with AtomicInteger
- skip log spam is reduced

## Current branch: terrain-tfc-noise-5x5-region-transform

Goal:
Scale from 3x3 v2 to 5x5 while carrying forward fixes.

Changes:
- 5x5 target region
- max 25 transformed chunks
- pending skipped chunks can be replayed after target selection
- centerUndersideY fallback fixed for low-center chunks
- still avoids registry/block-ID lookups
- still only mutates ProtoChunks at minecraft:noise

## Current branch: terrain-tfc-noise-5x5-ocean-floor-prototype

Goal:
Add a simple lower ocean layer under the working 5x5 floating-island transform.

Changes:
- remove glowstone debug markers
- add bottom crust using bedrock + vanilla stone
- fill lower water column up to Y=0
- keep original TFC surface height
- still mutate only ProtoChunks at minecraft:noise
- still avoid block/fluid registry lookups

## Current branch: terrain-tfc-noise-9x9-ocean-floor-prototype

Goal:
Scale from 5x5 ocean-floor prototype to a larger 9x9 gameplay-scale region.

Changes:
- radius 4 chunks
- max 81 transformed chunks
- keep ocean floor/crust layer
- keep original TFC surface height
- still mutate only ProtoChunks at minecraft:noise
- still avoid block/fluid registry lookups
- check outside-region status before transform-limit status for better skip logs

## Current branch: terrain-tfc-noise-9x9-ocean-future-chain

Goal:
Fix the suspected async race by chaining AFC terrain mutation into the returned TFC fillFromNoise future.

Changes:
- @Inject at RETURN is now cancellable
- replace whenComplete side-effect with originalFuture.thenApply(...)
- no pending chunk replay, because replay would mutate chunks after their own futures completed
- keep 9x9 ocean-floor transform
- keep ProtoChunk check
- keep no registry/block-ID lookup
