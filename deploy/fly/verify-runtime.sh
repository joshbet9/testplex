#!/bin/sh
set -eu

expected_sha="${1:?usage: verify-runtime.sh <expected-source-sha>}"
root="${TESTPLEX_VERIFY_ROOT:-}"
build_file="${root}/opt/testplex/source-build.txt"
server_root="${root}/server"

actual_sha="$(cat "$build_file")"
if [ "$actual_sha" != "$expected_sha" ]; then
    echo "Fly runtime source SHA mismatch: expected $expected_sha, found $actual_sha" >&2
    exit 1
fi

monster_maze_assets="$root/opt/testplex/MonsterMazeMaps"
monster_maze_revision_file="$monster_maze_assets/.testplex-mm-source-maps"
if [ ! -s "$monster_maze_revision_file" ] || ! grep -Eq '^[0-9a-f]{40}$' "$monster_maze_revision_file"; then
    echo "Fly Monster Maze packaged-world revision marker is missing or invalid." >&2
    exit 1
fi
installed_revision_file="$server_root/.testplex-mm-source-maps"
if [ ! -s "$installed_revision_file" ] || ! cmp -s "$monster_maze_revision_file" "$installed_revision_file"; then
    echo "Fly installed Monster Maze world revision does not match the packaged map set." >&2
    exit 1
fi
for world in mm_void mm_volcano mm_colombia mm_sandycoast mm_siberian mm_swampland mm_tesorohundido; do
    installed="$server_root/$world"
    if [ ! -d "$installed" ] || [ -L "$installed" ]; then
        echo "Fly Monster Maze world directory is missing or is still a symlink: $installed" >&2
        exit 1
    fi
    if [ ! -s "$installed/level.dat" ]; then
        echo "Fly Monster Maze world is missing level.dat: $installed" >&2
        exit 1
    fi
    region="$installed/region"
    if [ ! -d "$region" ]; then
        region="$installed/dimensions/minecraft/overworld/region"
    fi
    if [ ! -d "$region" ] || ! find "$region" -maxdepth 1 -type f -name '*.mca' -print -quit | grep -q .; then
        echo "Fly Monster Maze world has no readable terrain regions: $installed" >&2
        exit 1
    fi
done

for map_directory in "Wizards" "Nano Wizards"; do
    if [ ! -d "$server_root/update/maps/$map_directory" ]; then
        echo "Fly source map directory is missing: $server_root/update/maps/$map_directory" >&2
        exit 1
    fi
done

# Check the Paper server's own persistent startup log, rather than Fly's
# best-effort recent log stream. This is the actual final Arcade bootstrap marker.
startup_log="$server_root/logs/latest.log"
max_checks="${TESTPLEX_VERIFY_BOOTSTRAP_ATTEMPTS:-75}"
case "$max_checks" in
    ''|*[!0-9]*) echo "Invalid Arcade bootstrap readiness attempt limit." >&2; exit 1 ;;
esac
if [ "$max_checks" -lt 1 ]; then
    echo "Arcade bootstrap readiness attempt limit must be positive." >&2
    exit 1
fi
attempt=1
while :; do
    if [ -f "$startup_log" ] && grep -Fq 'TESTPLEX_ARCADE_BOOTSTRAP phase=full-runtime-readiness-published' "$startup_log"; then
        break
    fi
    if [ "$attempt" -ge "$max_checks" ]; then
        echo "Fly Arcade bootstrap completion marker did not appear in $startup_log after $attempt check(s)." >&2
        exit 1
    fi
    sleep 2
    attempt=$((attempt + 1))
done

printf 'TESTPLEX_FLY_RUNTIME_VERIFIED:%s\n' "$expected_sha"
