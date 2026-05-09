# TFC Noise-Stage Floating Patch Prototype

## Purpose

Test a small version of the intended Aerofirmacraft terrain transform inside TFC noise-stage generation.

## Target

`net.dries007.tfc.world.TFCChunkGenerator#fillFromNoise`

## Behavior

After TFC fillFromNoise completes its returned CompletableFuture, this prototype modifies one generated ProtoChunk only:

- center 8x8 columns
- land columns preserve the top mass and carve air below the underside
- fluid columns are carved into air above Y=0
- land undersides are marked with glowstone

## Safety

This does not use:

- `serverLevel.setBlock`
- `ChunkEvent.Load`
- full chunk transforms
- cross-chunk writes

## Limits

- One chunk only.
- Center 8x8 columns only.
- Fresh disposable worlds only.