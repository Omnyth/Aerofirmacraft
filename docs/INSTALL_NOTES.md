# Install Notes

## Windows client

1. Make a new Minecraft 1.21.1 NeoForge instance.
2. Open the instance folder.
3. Copy this zip's contents into the instance folder.
4. In PowerShell from the instance folder:

```powershell
Set-ExecutionPolicy -Scope Process Bypass
.\scripts\download_client_mods.ps1
```

To include optional GregTech:

```powershell
.\scripts\download_client_mods.ps1 -IncludeGregTech
```

## Ubuntu/Linux server

From the server instance folder:

```bash
chmod +x scripts/download_server_mods.sh
./scripts/download_server_mods.sh
```

With optional GregTech:

```bash
./scripts/download_server_mods.sh --include-gregtech
```

## Java

Use Java 21.

## Loader

Use NeoForge 1.21.1. I recommend 21.1.218+ because World Preview TFC documents that range as a recommended baseline.
