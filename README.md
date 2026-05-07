# TFC Aeronautica Starter Pack

This is a starter scaffold for a new pack concept:

TFC survival -> Create mechanical industry -> optional GregTech industrialization -> Create Aeronautics travel.

## Target

- Minecraft: 1.21.1
- Loader: NeoForge
- Java: 21
- Recommended NeoForge: 21.1.218 or newer 21.1.x

## What this zip is

This is not a finished importable modpack with every mod jar embedded. It is a starter pack scaffold with:

- download scripts
- curated mod lists
- KubeJS starter scripts
- progression/worldgen design notes
- an optional GregTech add-on list

The scripts download the mod jars directly from Modrinth CDN into your `mods` folder.

## Fast start

1. Create a fresh Minecraft 1.21.1 NeoForge instance.
2. Copy this zip's contents into the instance folder.
3. Run one of the client download scripts:
   - Windows: `scripts/download_client_mods.ps1`
   - Linux/macOS: `scripts/download_client_mods.sh`
4. Launch once.
5. Do not enable the worldgen stub as a real datapack yet. It is documentation only right now.

## Server start

Use the server script to download only server-safe mods:

- Windows: `scripts/download_server_mods.ps1`
- Linux: `scripts/download_server_mods.sh`

World Preview TFC is client-only and should not go on a dedicated server.

## Optional GregTech

GregTech is not downloaded by default. Add it only after the base pack boots cleanly.

- Windows client: `scripts/download_client_mods.ps1 -IncludeGregTech`
- Windows server: `scripts/download_server_mods.ps1 -IncludeGregTech`
- Linux/macOS client: `scripts/download_client_mods.sh --include-gregtech`
- Linux server: `scripts/download_server_mods.sh --include-gregtech`

I left GregTech optional because the 1.21.1 NeoForge branch is a bigger compatibility risk than the TFC/Create/Aeronautics core.
