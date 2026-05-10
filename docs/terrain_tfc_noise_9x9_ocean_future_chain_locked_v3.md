# TFC Noise-Stage 9x9 Ocean Future-Chain Locked V3

## Purpose

Fix locked V2 hanging after section locks were acquired.

## Diagnosis

Locked V2 acquired section locks successfully, selected a target, then never logged `applied` or `released`.

The likely problem was using `ChunkAccess#getBlockState` / `ChunkAccess#setBlockState` while holding section locks.

## Changes from V2

- Keeps future-chain hook.
- Keeps section acquire/release.
- Replaces `chunk.getBlockState(...)` with direct `LevelChunkSection#getBlockState(...)`.
- Replaces `chunk.setBlockState(...)` with direct `LevelChunkSection#setBlockState(..., false)`.
- Keeps bounded 9x9 transform.
- Keeps low-only underside fallback.