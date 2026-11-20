#!/usr/bin/env bash
set -euo pipefail

if [[ $# != 2 ]]; then
    echo "Usage: provision-monster-maze-worlds.sh SOURCE_ROOT WORLD_ROOT" >&2
    exit 2
fi

SOURCE_ROOT="$(realpath -e -- "$1")"
WORLD_ROOT="$2"
RUNTIME_ROOT="${TESTPLEX_MM_RUNTIME_ROOT:-/tmp/testplex-monster-maze-runtime}"
EXPECTED_SOURCE="$(tr -d '\r\n[:space:]' < "$SOURCE_ROOT/.testplex-mm-source-maps")"
WORLD_NAMES=(mm_void mm_volcano mm_colombia mm_sandycoast mm_siberian mm_swampland mm_tesorohundido)

if [[ ! "$EXPECTED_SOURCE" =~ ^[0-9a-f]{40}$ ]]; then
    echo "Monster Maze source revision marker is missing or invalid." >&2
    exit 1
fi
if [[ -L "$WORLD_ROOT" ]]; then
    echo "Refusing to provision maps through a symbolic-link world container." >&2
    exit 1
fi
mkdir -p -- "$WORLD_ROOT"
if [[ -z "${MSYSTEM:-}" ]]; then
    chmod 0750 -- "$WORLD_ROOT"
fi
WORLD_ROOT="$(realpath -e -- "$WORLD_ROOT")"

for world in "${WORLD_NAMES[@]}"; do
    source="$SOURCE_ROOT/$world"
    if [[ ! -d "$source" || -L "$source" || ! -s "$source/level.dat" || ! -d "$source/region" ]]; then
        echo "Monster Maze source world is incomplete or unsafe: $world" >&2
        exit 1
    fi
done

has_complete_world_set() {
    local installed_source
    [[ -f "$WORLD_ROOT/.testplex-mm-source-maps" ]] || return 1
    installed_source="$(tr -d '\r\n[:space:]' < "$WORLD_ROOT/.testplex-mm-source-maps")"
    [[ "$installed_source" == "$EXPECTED_SOURCE" ]] || return 1
    local world
    for world in "${WORLD_NAMES[@]}"; do
        local installed_world="$WORLD_ROOT/$world"
        [[ -d "$installed_world" && ! -L "$installed_world" ]] || return 1
        [[ -s "$installed_world/level.dat" ]] || return 1
        local region="$installed_world/region"
        if [[ ! -d "$region" ]]; then
            region="$installed_world/dimensions/minecraft/overworld/region"
        fi
        [[ -d "$region" ]] || return 1
        find "$region" -maxdepth 1 -type f -name '*.mca' -print -quit | grep -q . || return 1
    done
}

if has_complete_world_set; then
    echo "Monster Maze source worlds already match $EXPECTED_SOURCE; preserving the runtime copies."
    exit 0
fi

stage="$(mktemp -d "$WORLD_ROOT/.testplex-mm-stage.XXXXXX")"
cleanup() { rm -rf -- "$stage"; }
trap cleanup EXIT

# Paper 26.3's legacy world migration walks the world directory using a
# copy-on-write filesystem and rejects a symlink at the world root. Install
# real map directories on the existing volume; gameplay still uses disposable
# runtime copies under /tmp. Keep the data-volume footprint bounded and retain
# headroom for Paper/plugin state while staging a complete replacement set.
source_size_kb="$(du -sk -- "${WORLD_NAMES[@]/#/$SOURCE_ROOT/}" | awk '{ total += $1 } END { print total + 0 }')"
existing_size_kb=0
for world in "${WORLD_NAMES[@]}"; do
    if [[ -d "$WORLD_ROOT/$world" && ! -L "$WORLD_ROOT/$world" ]]; then
        size="$(du -sk -- "$WORLD_ROOT/$world" | awk '{ print $1 + 0 }')"
        existing_size_kb=$((existing_size_kb + size))
    fi
done
available_kb="$(df -Pk -- "$WORLD_ROOT" | awk 'NR == 2 { print $4 + 0 }')"
reserve_kb="${TESTPLEX_MM_VOLUME_RESERVE_KB:-32768}"
if [[ ! "$reserve_kb" =~ ^[0-9]+$ ]]; then
    echo "Monster Maze map volume reserve must be a non-negative integer." >&2
    exit 1
fi
required_kb=$((source_size_kb + existing_size_kb + reserve_kb))
if (( available_kb < required_kb )); then
    echo "Insufficient Fly volume space for safe Monster Maze map staging: available=${available_kb}KiB required=${required_kb}KiB (maps=${source_size_kb}KiB existing=${existing_size_kb}KiB reserve=${reserve_kb}KiB)." >&2
    exit 1
fi

# Validate every existing destination before staging. Symlinks are accepted
# only when they are from the previous known image/runtime provisioning. Real
# directories are replaceable only when the root marker proves they were
# installed by this provisioner; unrelated world data is never removed.
installed_source=""
if [[ -f "$WORLD_ROOT/.testplex-mm-source-maps" ]]; then
    installed_source="$(tr -d '\r\n[:space:]' < "$WORLD_ROOT/.testplex-mm-source-maps")"
fi
if [[ -n "$installed_source" && ! "$installed_source" =~ ^[0-9a-f]{40}$ ]]; then
    echo "Existing Monster Maze source revision marker is invalid; refusing to replace map data." >&2
    exit 1
fi
for world in "${WORLD_NAMES[@]}"; do
    destination="$WORLD_ROOT/$world"
    if [[ -L "$destination" ]]; then
        installed_target="$(readlink -f -- "$destination" || true)"
        link_target="$(readlink -- "$destination" || true)"
        if [[ "$installed_target" != "$SOURCE_ROOT/$world" && "$link_target" != "$RUNTIME_ROOT/$world" ]]; then
            echo "Refusing to replace an unexpected Monster Maze world link at $destination" >&2
            exit 1
        fi
    elif [[ -e "$destination" ]]; then
        if [[ ! -d "$destination" || -z "$installed_source" ]]; then
            echo "Refusing to replace untracked world data at $destination" >&2
            exit 1
        fi
    fi
    if find "$SOURCE_ROOT/$world" -type l -print -quit | grep -q .; then
        echo "Monster Maze source world contains a symlink; refusing unsafe install: $world" >&2
        exit 1
    fi
    cp -a -- "$SOURCE_ROOT/$world" "$stage/$world"
    [[ -s "$stage/$world/level.dat" && -d "$stage/$world/region" ]] || {
        echo "Staged Monster Maze world failed validation: $world" >&2
        exit 1
    }
done

if [[ -n "${TESTPLEX_WORLD_OWNER:-}" ]]; then
    chown -- "$TESTPLEX_WORLD_OWNER" "$stage"
    chown -R -- "$TESTPLEX_WORLD_OWNER" "$stage"
fi
for world in "${WORLD_NAMES[@]}"; do
    destination="$WORLD_ROOT/$world"
    if [[ -L "$destination" ]]; then
        rm -f -- "$destination"
    elif [[ -d "$destination" ]]; then
        rm -rf -- "$destination"
    fi
    mv -- "$stage/$world" "$destination"
    if [[ -n "${TESTPLEX_WORLD_OWNER:-}" ]]; then
        chown -R -- "$TESTPLEX_WORLD_OWNER" "$destination"
    fi
done
printf '%s\n' "$EXPECTED_SOURCE" > "$stage/.testplex-mm-source-maps"
mv -f -- "$stage/.testplex-mm-source-maps" "$WORLD_ROOT/.testplex-mm-source-maps"
has_complete_world_set || { echo "Monster Maze source worlds failed post-install verification." >&2; exit 1; }
echo "Copied and verified seven real Monster Maze source worlds at revision $EXPECTED_SOURCE; player world, plugin data, and statistics were preserved."
