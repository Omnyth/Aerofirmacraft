param(
  [switch]$IncludeGregTech
)

$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
$ModsDir = Join-Path $Root "mods"
New-Item -ItemType Directory -Force -Path $ModsDir | Out-Null

$coreMods = @(
  [PSCustomObject]@{ Name = "TerraFirmaCraft"; File = "TerraFirmaCraft-NeoForge-1.21.1-4.1.1.jar"; Url = "https://cdn.modrinth.com/data/JaCEZUhg/versions/3FO3as07/TerraFirmaCraft-NeoForge-1.21.1-4.1.1.jar"; Side = "both" }
  [PSCustomObject]@{ Name = "Patchouli"; File = "Patchouli-1.21.1-93-NEOFORGE.jar"; Url = "https://cdn.modrinth.com/data/nU0bVIaL/versions/BIogJv2D/Patchouli-1.21.1-93-NEOFORGE.jar"; Side = "both" }
  [PSCustomObject]@{ Name = "Create"; File = "create-1.21.1-6.0.9.jar"; Url = "https://cdn.modrinth.com/data/LNytGWDc/versions/n7NADxiG/create-1.21.1-6.0.9.jar"; Side = "both" }
  [PSCustomObject]@{ Name = "Sable"; File = "sable-neoforge-1.21.1-1.2.1.jar"; Url = "https://cdn.modrinth.com/data/T9PomCSv/versions/ADGYo8vU/sable-neoforge-1.21.1-1.2.1.jar"; Side = "both" }
  [PSCustomObject]@{ Name = "Create Aeronautics"; File = "create-aeronautics-bundled-1.21.1-1.2.1.jar"; Url = "https://cdn.modrinth.com/data/oWaK0Q19/versions/YhZLrAFC/create-aeronautics-bundled-1.21.1-1.2.1.jar"; Side = "both" }
  [PSCustomObject]@{ Name = "Just Enough Items"; File = "jei-1.21.1-neoforge-19.27.0.340.jar"; Url = "https://cdn.modrinth.com/data/u6dRKJwZ/versions/YAcQ6elZ/jei-1.21.1-neoforge-19.27.0.340.jar"; Side = "both" }
  [PSCustomObject]@{ Name = "KubeJS"; File = "kubejs-neoforge-2101.7.2-build.295.jar"; Url = "https://cdn.modrinth.com/data/umyGl7zF/versions/YZqAKbnI/kubejs-neoforge-2101.7.2-build.295.jar"; Side = "both" }
  [PSCustomObject]@{ Name = "Rhino"; File = "rhino-2101.2.7-build.81.jar"; Url = "https://cdn.modrinth.com/data/sk9knFPE/versions/ZdLtebKH/rhino-2101.2.7-build.81.jar"; Side = "both" }
  [PSCustomObject]@{ Name = "Jade"; File = "Jade-1.21.1-NeoForge-15.10.5.jar"; Url = "https://cdn.modrinth.com/data/nvQzSEkH/versions/yd8FKCmx/Jade-1.21.1-NeoForge-15.10.5.jar"; Side = "both" }
)

$clientMods = @(
  [PSCustomObject]@{ Name = "World Preview TFC"; File = "world_preview_tfc-2.0.1.jar"; Url = "https://cdn.modrinth.com/data/LEqrn5wh/versions/owwYzHd9/world_preview_tfc-2.0.1.jar"; Side = "client" }
)

$gregMods = @(
  [PSCustomObject]@{ Name = "GregTech CEu Modern"; File = "gtceu-1.21.1-1.4.4.jar"; Url = "https://cdn.modrinth.com/data/7tG215v7/versions/OdpXbS3h/gtceu-1.21.1-1.4.4.jar"; Side = "both" }
  [PSCustomObject]@{ Name = "Configuration"; File = "configuration-neoforge-1.21.1-3.1.1.jar"; Url = "https://cdn.modrinth.com/data/3WjjSM5O/versions/6Mztfq1u/configuration-neoforge-1.21.1-3.1.1.jar"; Side = "both" }
  [PSCustomObject]@{ Name = "LDLib"; File = "ldlib-neoforge-1.21.1-1.0.29.b.jar"; Url = "https://cdn.modrinth.com/data/B1CBVXHX/versions/YNLkNCSo/ldlib-neoforge-1.21.1-1.0.29.b.jar"; Side = "both" }
)

$allMods = @()
$allMods += $coreMods
$allMods += $clientMods
if ($IncludeGregTech) {
  $allMods += $gregMods
}

foreach ($mod in $allMods) {
  $dest = Join-Path $ModsDir $mod.File
  if (Test-Path $dest) {
    Write-Host "Already exists: $($mod.File)"
  } else {
    Write-Host "Downloading: $($mod.Name)"
    Invoke-WebRequest -Uri $mod.Url -OutFile $dest
  }
}

Write-Host "Done. Mods folder: $ModsDir"
