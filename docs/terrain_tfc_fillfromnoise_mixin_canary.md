# TFC fillFromNoise Mixin Canary

## Purpose

Confirm that Aerofirmacraft can hook into TerraFirmaCraft terrain generation before full chunk load.

## Target

`net.dries007.tfc.world.TFCChunkGenerator#fillFromNoise`

## Why this hook

The TFC source shows `fillFromNoise` performs the terrain fill workflow:

- create chunk noise settings
- generate chunk data
- create `ChunkNoiseFiller`
- call `filler.fillFromNoise()`
- modify base groundwater
- build the surface with `surfaceManager.buildSurface(...)`

This is much earlier than `ChunkEvent.Load`.

## Current behavior

Logs only:

- HEAD of `fillFromNoise`
- completion of the returned `CompletableFuture`

No terrain mutation.

## Success criteria

In a fresh test world, latest.log should contain:

- `AFC mixin canary: TFC fillFromNoise HEAD`
- `AFC mixin canary: TFC fillFromNoise COMPLETE`

If those appear, the next step is a tiny controlled mutation inside the returned future path, not a `ChunkEvent.Load` rewrite.