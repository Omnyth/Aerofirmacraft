# TFC Noise-Stage Continuous Ocean Locked V5 TFC River Biome

## Purpose

Preserve TFC river columns by reading the generated biome palette instead of relying only on local height-shape heuristics.

## Problem

V4 used a same-chunk neighborhood heuristic. It preserved some low columns, but real TFC rivers are biome-coded as `tfc:river`, so height-only and shape-only logic can still remove rivers.

## V5 Change

Low columns are now preserved when:

- the sampled generated biome is `tfc:river`, or
- the fallback local-shape heuristic says the low column is river/lake-like

Preserved low columns keep their river surface, but are still given floating-island underside carving instead of becoming full pillars.

## Foundation

This branch keeps:

- future-chain RETURN injection
- LevelChunkSection acquire/release
- direct LevelChunkSection get/set
- ProtoChunk-only transform
- lower ocean/crust layer