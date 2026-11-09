#!/usr/bin/env bash
set -euo pipefail

source_dir="${1:?Existing Actions build artifact directory required}"
output_dir="${2:?Output directory required}"
source_sha="${SOURCE_SHA:?Verified source SHA required}"
source_run_id="${SOURCE_RUN_ID:?Verified source run ID required}"
[[ "$source_sha" =~ ^[a-f0-9]{40}$ ]] || { echo 'Invalid source SHA.' >&2; exit 1; }
[[ "$source_run_id" =~ ^[0-9]+$ ]] || { echo 'Invalid source run ID.' >&2; exit 1; }
mkdir -p "$output_dir"

declare -a checked=()
for name in TestplexModernArcade.jar ArcadeMechanicsModern.jar WizardsSourceMaps.tar.gz; do
  mapfile -t matches < <(find "$source_dir" -type f -name "$name" -print)
  [[ "${#matches[@]}" -eq 1 && -s "${matches[0]}" ]] || {
    echo "Expected exactly one nonempty $name in the source build artifact." >&2
    exit 1
  }
  checked+=("${matches[0]}")
done

unzip -tq "${checked[0]}" >/dev/null
unzip -tq "${checked[1]}" >/dev/null
unzip -Z -1 "${checked[0]}" | grep -Fx 'au/com/testplex/modern/TestplexModernArcade.class' >/dev/null
unzip -Z -1 "${checked[1]}" | grep -Fx 'me/monstermaze/game/GameManager.class' >/dev/null
unzip -p "${checked[1]}" plugin.yml | grep -Fx 'name: ArcadeMechanicsModern' >/dev/null
tar -tzf "${checked[2]}" >/dev/null
for folder in Wizards 'Nano Wizards'; do
  count="$(tar -tzf "${checked[2]}" | awk -v prefix="$folder/" 'index($0, prefix) == 1 && $0 ~ /\.zip$/ {count++} END {print count+0}')"
  [[ "$count" == 10 ]] || { echo "Expected 10 $folder source maps; found $count." >&2; exit 1; }
done

{
  printf 'source_run_id=%s\nsource_sha=%s\n' "$source_run_id" "$source_sha"
  printf 'artifact_name=testplex-modern-paper-26.3\n'
  printf 'fly_deployed=false\n'
  printf 'monster_maze_physical_worlds=NOT_INCLUDED_REQUIRES_SEPARATE_PINNED_ASSET_REVIEW\n'
  printf 'paper_runtime=NOT_INCLUDED_REQUIRES_PINNED_BUILD_AND_SHA256\n'
} > "$output_dir/release-candidate.properties"

for i in "${!checked[@]}"; do
  name="$(basename "${checked[$i]}")"
  sha256sum "${checked[$i]}" | awk -v name="$name" '{print $1 "  " name}'
done > "$output_dir/artifact-sha256.txt"

echo "Verified Testplex plugin artifact from run $source_run_id ($source_sha)."
echo 'No Fly deployment has been attempted.'
