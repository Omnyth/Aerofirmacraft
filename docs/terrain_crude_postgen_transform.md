# Terrain Crude Post-Generation Transform Prototype

## Branch

terrain-crude-postgen-transform

## Purpose

First terrain-changing prototype.

## Behavior

For newly generated overworld chunks only:

- TFC fluid/salt-water columns are carved into sky above a bottom ocean layer.
- Land columns are preserved above a rough underside height.
- Blocks below the underside are carved to air.
- The bottom world layer is filled with water.

## Current constants

- Global ocean top Y: 0
- Base underside Y: 38
- Underside variation: +/- 8
- Transform limit: 24 chunks

## Important caveat

This uses ChunkEvent.Load, which sees chunks at full generated status. This is probably too late for final worldgen architecture, but it is acceptable for a crude proof-of-concept.

## Safety

Use fresh disposable test worlds only.