# Version Policy

For this pack, use the latest appropriate stable/release version unless there is a specific reason not to.

## Selection rules

1. Match Minecraft version first.
   - Current target: Minecraft 1.21.1

2. Match loader second.
   - Current target: NeoForge

3. Prefer stable/release files.
   - Avoid alpha/beta unless the mod has no release option or we intentionally choose to test it.

4. Verify dependencies.
   - Required dependencies must also match Minecraft 1.21.1 and NeoForge.

5. Record the exact jar filename and download URL.
   - Update mods_core.tsv
   - Update mods_client_only.tsv
   - Update mods_optional_gregtech.tsv
   - Update pack_manifest_notes.json
   - Update download scripts

6. Do not assume old versions are correct.
   - Always check the current release page before recommending a mod version.

## Current baseline

Core pack boots with:

- Minecraft 1.21.1
- NeoForge
- Java 21
- Create 1.21.1-6.0.10
- TerraFirmaCraft
- Create Aeronautics
- Sable
