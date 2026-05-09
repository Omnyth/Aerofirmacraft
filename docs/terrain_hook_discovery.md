# Terrain Hook Discovery

## Purpose

Find the correct worldgen-stage hook for Aerofirmacraft terrain shaping.

## Why this branch exists

The crude post-generation transform attempted to reshape terrain from `ChunkEvent.Load`.

That approach is rejected for full terrain shaping because diagnostics showed chunks were already:

- `persistedStatus=minecraft:full`
- `highestGeneratedStatus=minecraft:full`
- `chunkClass=LevelChunk`

Mass editing finished chunks with `serverLevel.setBlock(...)` during spawn loading caused severe loading stalls.

## Rule going forward

Do not perform full terrain transforms in `ChunkEvent.Load`.

Acceptable uses of `ChunkEvent.Load`:

- read-only diagnostics
- small debug markers only
- confirming generated terrain classification

Not acceptable:

- mass vertical column carving
- full chunk rewriting
- ocean/land conversion after full chunk load

## Next technical target

Find an earlier hook where terrain can be shaped before the chunk becomes a fully loaded `LevelChunk`.

Candidate directions:

1. NeoForge worldgen events
2. Chunk generation status hooks
3. custom or wrapped chunk generator
4. TFC-specific worldgen classes/hooks
5. Mixin only if no clean NeoForge/TFC hook exists