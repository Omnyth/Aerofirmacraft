#!/usr/bin/env bash
set -euo pipefail

INCLUDE_GREGTECH="false"
for arg in "$@"; do
  case "$arg" in
    --include-gregtech) INCLUDE_GREGTECH="true" ;;
  esac
done

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
MODS_DIR="$ROOT/mods"
mkdir -p "$MODS_DIR"

CORE_MODS=(
  'TerraFirmaCraft|TerraFirmaCraft-NeoForge-1.21.1-4.1.1.jar|https://cdn.modrinth.com/data/JaCEZUhg/versions/3FO3as07/TerraFirmaCraft-NeoForge-1.21.1-4.1.1.jar'
  'Patchouli|Patchouli-1.21.1-93-NEOFORGE.jar|https://cdn.modrinth.com/data/nU0bVIaL/versions/BIogJv2D/Patchouli-1.21.1-93-NEOFORGE.jar'
  'Create|create-1.21.1-6.0.9.jar|https://cdn.modrinth.com/data/LNytGWDc/versions/n7NADxiG/create-1.21.1-6.0.9.jar'
  'Sable|sable-neoforge-1.21.1-1.2.1.jar|https://cdn.modrinth.com/data/T9PomCSv/versions/ADGYo8vU/sable-neoforge-1.21.1-1.2.1.jar'
  'Create Aeronautics|create-aeronautics-bundled-1.21.1-1.2.1.jar|https://cdn.modrinth.com/data/oWaK0Q19/versions/YhZLrAFC/create-aeronautics-bundled-1.21.1-1.2.1.jar'
  'Just Enough Items|jei-1.21.1-neoforge-19.27.0.340.jar|https://cdn.modrinth.com/data/u6dRKJwZ/versions/YAcQ6elZ/jei-1.21.1-neoforge-19.27.0.340.jar'
  'KubeJS|kubejs-neoforge-2101.7.2-build.295.jar|https://cdn.modrinth.com/data/umyGl7zF/versions/YZqAKbnI/kubejs-neoforge-2101.7.2-build.295.jar'
  'Rhino|rhino-2101.2.7-build.81.jar|https://cdn.modrinth.com/data/sk9knFPE/versions/ZdLtebKH/rhino-2101.2.7-build.81.jar'
  'Jade|Jade-1.21.1-NeoForge-15.10.5.jar|https://cdn.modrinth.com/data/nvQzSEkH/versions/yd8FKCmx/Jade-1.21.1-NeoForge-15.10.5.jar'
)

CLIENT_MODS=(
  'World Preview TFC|world_preview_tfc-2.0.1.jar|https://cdn.modrinth.com/data/LEqrn5wh/versions/owwYzHd9/world_preview_tfc-2.0.1.jar'
)

GREGTECH_MODS=(
  'GregTech CEu Modern|gtceu-1.21.1-1.4.4.jar|https://cdn.modrinth.com/data/7tG215v7/versions/OdpXbS3h/gtceu-1.21.1-1.4.4.jar'
  'Configuration|configuration-neoforge-1.21.1-3.1.1.jar|https://cdn.modrinth.com/data/3WjjSM5O/versions/6Mztfq1u/configuration-neoforge-1.21.1-3.1.1.jar'
  'LDLib|ldlib-neoforge-1.21.1-1.0.29.b.jar|https://cdn.modrinth.com/data/B1CBVXHX/versions/YNLkNCSo/ldlib-neoforge-1.21.1-1.0.29.b.jar'
)

download_mod() {
  IFS='|' read -r name file url <<< "$1"
  dest="$MODS_DIR/$file"
  if [[ -f "$dest" ]]; then
    echo "Already exists: $file"
  else
    echo "Downloading: $name"
    curl -L "$url" -o "$dest"
  fi
}

for mod in "${CORE_MODS[@]}"; do download_mod "$mod"; done
for mod in "${CLIENT_MODS[@]}"; do download_mod "$mod"; done
if [[ "$INCLUDE_GREGTECH" == "true" ]]; then
  for mod in "${GREGTECH_MODS[@]}"; do download_mod "$mod"; done
fi

echo "Done. Mods folder: $MODS_DIR"
