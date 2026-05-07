# Suggested Build Order

## Phase 0: Base boot test

Install only:

- TFC
- Patchouli
- Create
- Sable
- Create Aeronautics
- JEI
- KubeJS
- Rhino
- Jade
- World Preview TFC client-only

Goal: confirm the pack boots and creates a normal TFC world.

## Phase 1: Create/TFC recipe bridge

Use KubeJS to gate basic Create behind TFC materials.

Examples to design:

- shafts from TFC lumber
- cogwheels from TFC lumber
- press from bronze/wrought iron plates
- mixer from TFC metal progression
- brass/precision mechanisms behind meaningful TFC metalwork

## Phase 2: Optional GregTech

Only add GregTech after Phase 0 and Phase 1 are stable.

Goal: verify GTCEu Modern + dependencies boot and recipes load.

## Phase 3: Worldgen addon prototype

Create a small NeoForge addon that:

1. lets TFC generate terrain data
2. identifies ocean/low areas
3. replaces those regions with air
4. preserves land columns
5. shapes island undersides
6. handles ocean resources as sky-ocean remnants

## Phase 4: Balance

- island spacing
- early bridges/travel
- seawater/kelp routes
- ore and rock region access
- Aeronautics gating
- optional questing
