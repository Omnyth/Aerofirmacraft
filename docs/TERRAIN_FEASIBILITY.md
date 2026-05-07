# Terrain Feasibility

## Core Question

Can Aerofirmacraft use TerraFirmaCraft terrain generation, but replace oceans/water gaps with sky/void?

The desired result is not generic skyblock terrain.

Target:

- TFC landmasses preserved
- TFC climate preserved
- TFC rock layers preserved
- TFC ores/resources preserved
- oceans/low-water regions converted into sky
- landmasses become floating continents/islands
- island undersides shaped instead of flat-cut
- TFC support/cave-in mechanics still matter for mining, but generated islands do not collapse by default

## Why this matters

If this terrain concept is not feasible, then the pack should not move forward as planned.

The pack identity depends on:

TFC survival -> Create industry -> Aeronautics travel between sky regions

## Main Risks

1. Datapack-only worldgen may not be enough.
2. Replacing only water with air leaves ocean floors behind.
3. Removing ocean floors may break TFC features, ores, climate, or spawn logic.
4. TFC support/gravity mechanics may interact badly with floating terrain.
5. Ocean materials need replacement routes:
   - seawater
   - kelp/seaweed
   - salt/ocean resources

## First Prototype Goal

Use TFC worldgen as much as possible, then test whether we can create:

- island-heavy TFC landmasses
- reduced or removed oceans
- no normal ocean floors
- playable spawn
- reachable early resources

## Prototype Stages

### Stage 1: TFC config/datapack exploration

Try to push TFC toward island-heavy generation using world preset/generator settings.

Questions:

- Can continentalness create usable island-heavy worlds?
- Can sea level/default fluid changes help?
- Can oceans be made shallower or less dominant?
- Can World Preview TFC help find useful seeds/settings?

### Stage 2: Datapack limitation test

Try a datapack override for the overworld dimension/preset.

Questions:

- Can a datapack alone remove or reduce oceans?
- Does it preserve TFC rocks/climate/features?
- Does it create sky islands, or just dry basins?

### Stage 3: NeoForge addon proof of concept

If datapack-only fails, test a small addon.

Addon goal:

- let TFC generate terrain
- detect ocean/low-water columns
- remove those columns to air
- preserve land columns
- shape undersides later

### Stage 4: Resource replacement design

If sky terrain works, add worldgen/resource solutions:

- saltwater springs
- brine pools
- kelp lagoon islands
- salt flats
- sky-ocean remnant islands
- early island clusters
- far-region Aeronautics travel

## Decision Rule

Do not continue major recipe/progression work until terrain feasibility is proven.

Acceptable result:

- TFC-like floating landmasses are possible
- TFC resources mostly survive
- ocean resources have replacement routes
- spawn can be made playable

Failed result:

- TFC terrain cannot be converted cleanly
- resource generation breaks too badly
- island generation requires too much custom terrain rewriting
