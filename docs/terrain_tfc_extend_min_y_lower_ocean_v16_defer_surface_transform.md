# Extend Min-Y Lower Ocean V16 Deferred Surface Transform

## Purpose

Reduce or eliminate feature-stage far-chunk setBlock errors by delaying surface/island carving until after biome decoration.

## V15 Problem

V15 carved sky gaps and island undersides during the noise stage. Later TFC/vanilla features then ran on already-hollowed terrain. Logs showed `Detected setBlock in a far chunk ... status: minecraft:features`.

## V16 Change

`fillFromNoise` now only handles the new lower band:

- bedrock
- crust
- lower ocean water
- lower ocean biome cells

The island/sky-gap surface transform is now deferred to:

`applyBiomeDecoration(... RETURN)`

So features run against normal TFC terrain first, then Aerofirmacraft carves the floating terrain afterward.

## Expected Result

The lower ocean should remain stable, and feature-stage far-chunk warnings should be reduced or disappear if they were caused by the early surface transform.