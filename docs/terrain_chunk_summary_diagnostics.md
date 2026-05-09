# Terrain Chunk Summary Diagnostics

## Purpose

Summarize each newly generated overworld chunk using all 256 columns.

## What it records

- min/max WORLD_SURFACE height
- min/max OCEAN_FLOOR height
- number of solid columns
- number of water columns
- number of air columns
- number of plant/leaves surface columns
- dominant surface block
- rough terrain class

## Why

This is the first step toward land/ocean/shore classification for the floating TFC regions terrain transform.

## Safety

Read-only. No terrain modification.