# TFC Noise-Stage 9x9 Ocean Future-Chain Locked V2

## Purpose

Diagnose and fix the first locked branch hanging after target selection.

## Changes from locked V1

- Acquires section locks before all chunk block reads and writes.
- Moves center surface scanning inside the locked section.
- Adds section-lock attempt/acquired/released logs.
- Wraps the future-chain transform in try/catch so failures appear in latest.log.
- Keeps the 9x9 bounded transform.
- Keeps the low-only underside fallback.

## Expected Log Sequence

- `AFC locked v2: attempting section locks`
- `AFC locked v2: acquired section locks`
- `AFC locked v2: selected target center`
- `AFC locked v2: applied`
- `AFC locked v2: released section locks`