# Real Lower Ocean Biome V21b Raw Noise JSON

## Purpose

Fix the V21 registry crash caused by malformed generated `minecraft:overworld` noise settings JSON.

## V21 Problem

V21 built, but registry loading failed when parsing:

`data/minecraft/worldgen/noise_settings/overworld.json`

The error was:

`Not a JSON object: null`

## V21b Change

V21b replaces the generated noise settings file with a raw copy of vanilla overworld noise settings and only patches the first `min_y` and `height` fields.

This avoids rewriting the full registry JSON through PowerShell's JSON serializer.

## Goal

Test whether extending overworld noise settings to `min_y=-128` allows TFC to generate terrain/fluid in the new lower band using the registered `aerofirmacraft_terrain:lower_ocean` BiomeExtension.

## Still No Manual Fill

V21b does not manually place water, stone, or bedrock.