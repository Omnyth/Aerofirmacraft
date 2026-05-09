# Terrain Surface Sampling Diagnostics

## Purpose

Add read-only samples from newly loaded/generated overworld chunks.

## What this records

For the center column of early overworld chunks:

- chunk position
- whether NeoForge reports the chunk as new
- WORLD_SURFACE height
- OCEAN_FLOOR height
- surface block ID
- whether the sampled surface block is air
- whether the sampled surface position contains water-tagged fluid

## Why

Before terrain can be reshaped, the addon needs a reliable way to classify generated TFC terrain columns as land, ocean, lowland, or other special cases.

## Safety rule

This branch still does not modify terrain.