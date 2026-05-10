# TFC Noise-Stage 9x9 Ocean Future-Chain Prototype

## Purpose

Test the safer future-chaining hook for the 9x9 ocean-floor transform.

## Previous Problem

The older 9x9 and continuous branches mutated chunks from a `whenComplete` side-effect callback. Some logs showed transformed chunks at `minecraft:carvers`, meaning the chunk status could advance before or during AFC mutation.

## Fix

This branch replaces:

`future.whenComplete(...)`

with:

`cir.setReturnValue(originalFuture.thenApply(...))`

This makes the AFC transform part of the returned `fillFromNoise` future.

## Behavior

- selects the first height-valid chunk as target center
- transforms up to 81 chunks in a 9x9 region
- preserves original TFC surface Y
- adds lower ocean crust/water
- avoids block/fluid registry lookups
- does not use `serverLevel.setBlock`
- does not use `ChunkEvent.Load`
- does not replay pending chunks, because replaying would mutate already-completed futures