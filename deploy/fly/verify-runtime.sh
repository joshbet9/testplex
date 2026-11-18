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

for world in mm_void mm_volcano mm_colombia mm_sandycoast mm_siberian mm_swampland mm_tesorohundido; do
    if [ ! -L "$server_root/$world" ]; then
        echo "Fly Monster Maze world link is missing: $server_root/$world" >&2
        exit 1
    fi
done

for map_directory in "Wizards" "Nano Wizards"; do
    if [ ! -d "$server_root/update/maps/$map_directory" ]; then
        echo "Fly source map directory is missing: $server_root/update/maps/$map_directory" >&2
        exit 1
    fi
done

# Read Paper's own active startup log from the persistent volume, rather than
# relying on flyctl logs --no-tail (which may not include startup history).
# This verifies the Arcade plugin completed startup in the deployed Machine.
paper_log="$server_root/logs/latest.log"
if [ ! -s "$paper_log" ] || ! grep -Fq 'TESTPLEX_ARCADE_BOOTSTRAP phase=complete' "$paper_log"; then
    echo "Fly Paper startup log does not confirm Arcade bootstrap completion: $paper_log" >&2
    exit 1
fi

printf 'TESTPLEX_FLY_PAPER_BOOTSTRAP_VERIFIED:%s\n' "$expected_sha"
printf 'TESTPLEX_FLY_RUNTIME_VERIFIED:%s\n' "$expected_sha"
