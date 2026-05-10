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

## Current branch: terrain-tfc-noise-9x9-ocean-future-chain-locked

Goal:
Add LevelChunkSection locking to the 9x9 future-chain ocean transform.

Changes:
- keep cancellable RETURN injection
- keep originalFuture.thenApply(...)
- acquire/release all LevelChunkSections during block mutation
- fix low-only chunk underside fallback from -9999 to oceanTop + 4
- keep 9x9 bounded test

## Current branch: terrain-tfc-noise-9x9-ocean-future-chain-locked-v2

Goal:
Diagnose and fix locked V1 stopping after target selection.

Changes:
- acquire LevelChunkSection locks before any getBlockState/setBlockState calls
- move center surface scanning inside locked section
- add lock attempt/acquired/released logs
- wrap transform in try/catch for visible latest.log failures
- keep bounded 9x9 future-chain transform

## Current branch: terrain-tfc-noise-9x9-ocean-future-chain-locked-v3

Goal:
Fix locked V2 hanging after section locks were acquired.

Changes:
- keep future-chain RETURN injection
- keep LevelChunkSection acquire/release
- replace ChunkAccess get/set block state calls with direct LevelChunkSection get/set calls
- keep bounded 9x9 ocean transform
- keep low-only underside fallback

## Current branch: terrain-tfc-noise-continuous-ocean-locked-v3

Goal:
Move from successful bounded 9x9 locked v3 to continuous transform.

Changes:
- remove target-region cap
- transform every generated TFC overworld ProtoChunk
- keep future-chain RETURN injection
- keep LevelChunkSection acquire/release
- keep direct LevelChunkSection get/set
- reduce logs to first 12 detailed chunks and every 128 chunk summary

## Current branch: terrain-tfc-noise-continuous-ocean-locked-v4-river-preserve

Goal:
Preserve rivers/lakes while keeping broad old-ocean regions as sky gaps.

Changes:
- keep continuous locked v3 foundation
- classify low columns using nearby same-chunk land shape
- preserve low columns that appear surrounded by land/opposite banks
- carve only low columns classified as broad sky-gap/ocean
- log landLikeColumns, preservedLowColumns, and skyGapColumns separately

## Current branch: terrain-tfc-noise-continuous-ocean-locked-v6-ocean-biome-skygap

Goal:
Classify sky gaps using reviewed TFC biome IDs, not height or name substring matching.

Changes:
- preserve all non-reviewed-ocean biomes
- carve definite old-ocean biomes as sky gaps
- carve reviewed coastal/ocean-edge biomes as separate sky-gap category
- preserve tfc:river, all lake variants, dune_sea, oceanic_mountains, and oceanic mountain/lake variants
- log definiteOceanSkyGapColumns and coastalSkyGapColumns separately

## Current branch: terrain-tfc-noise-sky-gap-biome-v7

Goal:
Add the first AFC sky-gap biome resource.

Changes:
- add data/aerofirmacraft_terrain/worldgen/biome/sky_gap.json
- keep V6 biome-driven terrain transform unchanged
- do not mutate biome palettes yet
- next branch should assign aerofirmacraft_terrain:sky_gap to carved old-ocean/coastal columns

## Current branch: terrain-tfc-noise-sky-gap-biome-v8-assign

Goal:
Assign the new AFC sky_gap biome to carved old-ocean/coastal biome cells above Y=0.

Changes:
- keep V6 biome-driven terrain transform
- keep V7 sky_gap biome resource
- add LevelChunkSectionAccessor for biome container replacement
- recreate each section biome palette when needed
- assign sky_gap biome to majority sky-gap quart cells above the lower ocean
- log skyGapBiomeCells
- lower-ocean biome assignment is deferred

## Current branch: terrain-tfc-noise-lower-ocean-biome-v9

Goal:
Add and assign a lower ocean biome copied from TFC ocean.json.

Changes:
- extract data/tfc/worldgen/biome/ocean.json from installed TFC jar
- write it as data/aerofirmacraft_terrain/worldgen/biome/lower_ocean.json
- assign lower_ocean biome to biome cells from minY through the Y=0 cell
- keep sky_gap assignment above Y=0 for carved old-ocean/coastal gaps
- keep original TFC biomes above Y=0 for preserved islands/rivers/lakes

## Current branch: terrain-tfc-noise-lower-ocean-use-tfc-ocean-v10

Goal:
Fix V9 crash by using existing tfc:ocean for the lower ocean biome layer.

Changes:
- remove custom lower_ocean.json
- replace custom aerofirmacraft_terrain:lower_ocean biome holder with existing tfc:ocean holder
- assign tfc:ocean to biome cells from minY through the Y=0 cell
- keep sky_gap assignment above Y=0 for carved old-ocean/coastal gaps
- keep original TFC biomes above Y=0 for preserved islands/rivers/lakes

## Current branch: terrain-tfc-extend-min-y-lower-ocean-v14

Goal:
Extend overworld min_y to -128 and move AFC lower ocean into the new vertical band below old TFC minY.

Changes:
- add data/minecraft/dimension_type/overworld.json override copied from installed Minecraft jar
- set min_y to -128 and preserve original top Y
- fill only new lower band below old TFC floor with bedrock/crust/water
- assign tfc:ocean to lower band biome cells
- keep sky_gap above lower ocean for old ocean/coastal biomes
- preserve original TFC biomes above lower ocean for land/rivers/lakes
- fresh-world only test

## Current branch: terrain-tfc-extend-min-y-lower-ocean-v15-clean-baseline

Goal:
Make V14 the clean baseline.

Changes:
- keep overworld min_y=-128 architecture
- keep AFC-owned lower band below old TFC floor
- keep tfc:ocean biome cells in the lower ocean band
- keep sky_gap biome cells in old ocean/coastal sky-gap areas
- reduce detailed logging to first 4 chunks
- reduce summary logging to every 512 chunks
- add one-time dimension sanity log

## Current branch: terrain-tfc-lower-ocean-y-biome-v18-no-surface-transform

Goal:
Validate lower-ocean biome/generation without any sky-gap or island surface transforms.

Changes:
- base from V15 clean baseline
- keep min_y=-128 dimension override
- assign tfc:ocean to lower Y range during createBiomes
- reassign tfc:ocean after fillFromNoise for persistence
- fill only the AFC-owned lower band with bedrock/crust/water
- remove old-ocean sky-gap carving
- remove preserved island underside carving
- remove sky_gap biome assignment from generation path

## Current branch: terrain-tfc-real-lower-ocean-biome-v20

Goal:
Register an actual TFC-compatible lower_ocean biome extension instead of only assigning a biome ID.

Changes:
- add data/aerofirmacraft_terrain/worldgen/biome/lower_ocean.json copied from TFC ocean JSON
- add AFCBiomes DeferredRegister for TFCBiomes.KEY
- register aerofirmacraft_terrain:lower_ocean BiomeExtension
- copy/edit TFC ocean extension parameters:
  - BiomeNoise.ocean(seed, -90, -76)
  - ShoreAndOceanSurfaceBuilder.OCEAN
  - aquiferHeightOffset(-88)
  - salty
  - BiomeBlendType.OCEAN
  - noRivers
- assign lower Y biome cells to aerofirmacraft_terrain:lower_ocean
- no manual water/stone/bedrock fill
- no sky-gap/island carving

## Current branch: terrain-tfc-real-lower-ocean-biome-v21-noise-settings

Goal:
Test whether extending overworld noise_settings min_y to -128 lets TFC generate the lower band using the real lower_ocean BiomeExtension.

Changes:
- base from V20c
- keep aerofirmacraft_terrain:lower_ocean BiomeExtension
- keep lower Y biome assignment
- add data/minecraft/worldgen/noise_settings/overworld.json override
- set noise_settings min_y=-128 while preserving top Y
- no manual water/stone/bedrock fill
- no sky-gap/island carving

## Current branch: terrain-tfc-real-lower-ocean-biome-v21b-raw-noise-json

Goal:
Fix malformed V21 noise settings override by raw-patching vanilla overworld JSON.

Changes:
- base from V21
- keep lower_ocean BiomeExtension
- keep lower Y biome assignment
- overwrite data/minecraft/worldgen/noise_settings/overworld.json from vanilla source
- raw-patch first min_y to -128
- raw-patch first height to preserve original top Y
- avoid ConvertTo-Json rewrite
- no manual water/stone/bedrock fill

## Current branch: terrain-tfc-real-lower-ocean-biome-v22-min-y-256

Goal:
Move dimension/noise min_y to -256 and place the experimental lower_ocean target much deeper.

Changes:
- base from V21b
- raw-patch dimension_type/overworld.json to min_y=-256
- raw-patch worldgen/noise_settings/overworld.json to min_y=-256
- preserve original top Y
- expected minY now -256
- lower ocean probe band now Y=-256..-193
- lower_ocean biome cap now Y=-189
- experimental lower_ocean BiomeExtension shifted to BiomeNoise.ocean(seed, -218, -204)
- aquiferHeightOffset shifted to -216
- no manual water/stone/bedrock fill

## Current branch: terrain-tfc-real-lower-ocean-biome-v23-lower-noise-hook

Goal:
Make the lower band use lower_ocean terrain noise, not the upper X/Z column biome noise.

Changes:
- base from V22
- keep min_y=-256
- fix lower_ocean BiomeNoise.ocean offsets to -281..-267, targeting actual Y around -218..-204
- set aquiferHeightOffset back to -24, matching TFC ocean relative behavior
- add ChunkNoiseFillerMixin
- inject calculateNoiseAtHeight for Y=-256..-193
- return density from lower_ocean BiomeNoiseSampler
- no manual water/stone/bedrock fill
