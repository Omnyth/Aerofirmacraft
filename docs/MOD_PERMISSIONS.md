# Mod Permissions Tracker

Only include mods marked `approved` or `approved_with_credit` in public/shared builds.

Status meanings:

- `approved`: license or page clearly allows modpack use.
- `approved_with_credit`: modpacks allowed, attribution required.
- `needs_permission`: author requires direct permission.
- `not_allowed`: do not include.
- `unknown`: do not include in public release yet.
- `dev_only`: okay for private testing/dev use only.

| Mod | Source | Loader/Version | License/Permission | Attribution Needed | Status | Notes |
|---|---|---|---|---|---|---|
| TerraFirmaCraft | Modrinth/CurseForge | 1.21.1 NeoForge | Verify page/license | Yes | unknown | Core TFC survival/worldgen |
| Patchouli | Modrinth/CurseForge | 1.21.1 NeoForge | Verify page/license | Yes | unknown | Required by TFC |
| Create | Modrinth/CurseForge | 1.21.1 NeoForge | Verify page/license | Yes | unknown | Required core mod |
| Sable | Modrinth | 1.21.1 NeoForge | Verify page/license | Yes | unknown | Required by Create Aeronautics |
| Create Aeronautics | Modrinth | 1.21.1 NeoForge | Verify page/license | Yes | unknown | Core pack feature |
| JEI | Modrinth/CurseForge | 1.21.1 NeoForge | Verify page/license | Yes | unknown | Recipe viewer |
| KubeJS | Modrinth/CurseForge | 1.21.1 NeoForge | Verify page/license | Yes | unknown | Scripts |
| Rhino | Modrinth/CurseForge | 1.21.1 NeoForge | Verify page/license | Yes | unknown | KubeJS dependency |
| Jade | Modrinth/CurseForge | 1.21.1 NeoForge | Verify page/license | Yes | unknown | Block/entity info overlay |
| World Preview TFC | Modrinth | 1.21.1 NeoForge | Verify page/license | Yes | dev_only | Client-only development tool |
| GregTech CEu Modern | Modrinth/CurseForge | 1.21.1 NeoForge | Verify page/license | Yes | unknown | Optional |
| Configuration | Modrinth | 1.21.1 NeoForge | Verify page/license | Yes | unknown | GregTech dependency |
| LDLib | Modrinth | 1.21.1 NeoForge | Verify page/license | Yes | unknown | GregTech dependency |

## Release rule

Before publishing the pack, every included mod must have one of these statuses:

- `approved`
- `approved_with_credit`

Do not ship mods still marked `unknown`, `needs_permission`, `not_allowed`, or `dev_only`.
