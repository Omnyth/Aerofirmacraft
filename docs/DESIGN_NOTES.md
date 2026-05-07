# Design Notes

## Pack Identity

This should feel close to TerraFirmaGreg in philosophy, but with an airship/sky-region identity.

Core progression:

1. TFC survival foundation
2. Create mechanical industry
3. GregTech electrical/chemical industry, optional once stable
4. Create Aeronautics for expansion/logistics between sky continents

## Ocean-to-Sky Worldgen Goal

The target is not vanilla skyblock terrain.

The target is:

- TFC landmasses preserved
- TFC climate preserved
- TFC rock layers and ore logic preserved
- oceans/low-water regions removed into sky/void
- island undersides shaped so the world does not look sliced flat

This likely needs a small NeoForge addon, not only a datapack.

## Important Resource Problems

Removing oceans creates issues:

- seawater
- kelp/seaweed
- ocean plants/materials
- travel between climate/rock regions

Planned solutions:

- saltwater springs
- brine pools
- salt flats
- kelp lagoon islands
- sky-ocean remnant islands
- early nearby island clusters
- far regions gated by Aeronautics

## TFC Gravity/Support Problem

The world should not collapse from existing in the sky.

Proposed rule:

Generated island shell = stable/naturally supported.
Player-created mines = normal TFC cave-in/support mechanics.

Implementation idea:

- keep soil thin
- island body mostly rock
- avoid gravity blocks on undersides
- mark generated island shell as naturally supported if TFC exposes support state/data
- do not globally disable cave-ins
