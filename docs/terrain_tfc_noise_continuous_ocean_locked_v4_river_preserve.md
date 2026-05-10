# TFC Noise-Stage Continuous Ocean Locked V4 River Preserve

## Purpose

Preserve river/lake-like low columns while continuing to carve broad old-ocean/lowland regions into sky gaps.

## Problem

V3 used a height-only classifier:

- surface Y >= 66: land-like
- surface Y < 66: low/old-ocean-like

That removed rivers, because TFC rivers often sit near sea level.

## V4 Change

Low columns are now split into:

- preserved low columns: likely rivers/lakes inside landmasses
- sky-gap columns: broad low/ocean regions that should become air over the lower ocean

## Heuristic

A low column is preserved when:

- its surface Y is in the river-like range, currently Y=58..65
- nearby same-chunk columns indicate land on opposite banks or on at least three sides

This is not final TFC river detection. It is a cheap local shape heuristic designed to avoid registry access and cross-chunk writes.

## Proven Foundation

This branch keeps the successful locked v3 pattern:

- future-chain RETURN injection
- LevelChunkSection acquire/release
- direct LevelChunkSection get/set
- ProtoChunk-only transform