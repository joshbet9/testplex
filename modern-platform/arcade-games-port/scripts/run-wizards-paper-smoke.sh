#!/usr/bin/env bash
set -euo pipefail

ARCADE_JAR="${1:?ArcadeMechanicsModern.jar path required}"
SMOKE_PLUGIN_JAR="${2:?WizardsPaperSmoke.jar path required}"
SERVER_ROOT="${3:?Disposable Paper server root required}"
USER_AGENT="Testplex-Wizards-Paper-Smoke/1.0 (https://github.com/joshbet9/testplex)"
SERVER_ROOT="$(mkdir -p "$SERVER_ROOT" && cd "$SERVER_ROOT" && pwd)"
mkdir -p "$SERVER_ROOT/plugins" "$SERVER_ROOT/update/maps"
[[ -s "$ARCADE_JAR" ]] || { echo "Missing Arcade jar: $ARCADE_JAR" >&2; exit 1; }
[[ -s "$SMOKE_PLUGIN_JAR" ]] || { echo "Missing smoke-test plugin jar: $SMOKE_PLUGIN_JAR" >&2; exit 1; }

echo "[wizards-smoke] Downloading the 20 original map snapshots into temporary runner storage."
python3 "$(dirname "$0")/download-wizards-source-maps.py" --map-root "$SERVER_ROOT/update/maps"

echo "[wizards-smoke] Resolving the latest stable Paper 26.3 build."
builds_json="$(curl --fail --silent --show-error --retry 3 --user-agent "$USER_AGENT" \
  "https://fill.papermc.io/v3/projects/paper/versions/26.3/builds")"
paper_build="$(printf '%s' "$builds_json" | jq -er 'first(.[] | select(.channel == "STABLE") | .id)')"
paper_url="$(printf '%s' "$builds_json" | jq -er 'first(.[] | select(.channel == "STABLE") | .downloads."server:default".url)')"
paper_sha="$(printf '%s' "$builds_json" | jq -er 'first(.[] | select(.channel == "STABLE") | .downloads."server:default".checksums.sha256)')"
paper_jar="$SERVER_ROOT/paper-26.3-$paper_build.jar"
curl --fail --location --retry 3 --user-agent "$USER_AGENT" "$paper_url" -o "$paper_jar"
printf '%s  %s\n' "$paper_sha" "$paper_jar" | sha256sum -c -

cp "$ARCADE_JAR" "$SERVER_ROOT/plugins/ArcadeMechanicsModern.jar"
cp "$SMOKE_PLUGIN_JAR" "$SERVER_ROOT/plugins/WizardsPaperSmoke.jar"

mkdir -p "$SERVER_ROOT/plugins/ArcadeMechanicsModern"
cat > "$SERVER_ROOT/plugins/ArcadeMechanicsModern/config.yml" <<'CONFIG'
enabled-games:
  - death-tag
  - one-in-the-quiver
lobby-selected-game: death-tag
source-maps:
  directory: update/maps
owner-usernames: []
admin-usernames: []
wizards:
  arena-world: wizards_arena_smoke_fallback
  source-map-world: arcadeport_source_wizards
  arena-radius: 48
  chest-count: 32
  allow-solo-start: true
  minimum-players: 1
  kits:
    default: MAGE
  destructive-spells: false
  temporary-spell-fire: true
  destructive-endgame-blocks: false
CONFIG

cat > "$SERVER_ROOT/eula.txt" <<'EULA'
eula=true
EULA
cat > "$SERVER_ROOT/server.properties" <<'PROPERTIES'
motd=Testplex Wizards Paper 26.3 Import Smoke Test
server-ip=127.0.0.1
server-port=25578
online-mode=false
spawn-protection=0
view-distance=3
simulation-distance=2
max-players=4
enable-rcon=false
enable-query=false
PROPERTIES

console_fifo="$SERVER_ROOT/server-console.fifo"
server_log="$SERVER_ROOT/server.log"
result_file="$SERVER_ROOT/plugins/WizardsPaperSmoke/results.tsv"
rm -f "$console_fifo" "$result_file"
mkfifo "$console_fifo"
# Keep the console input pipe open; the smoke plugin calls Bukkit.shutdown() when done.
exec 3<> "$console_fifo"

server_alive() {
  [[ -n "${SERVER_PID:-}" ]] || return 1
  local state
  state="$(ps -o stat= -p "$SERVER_PID" 2>/dev/null | awk '{print $1}')"
  [[ -n "$state" && "$state" != Z* ]]
}

cleanup() {
  if server_alive; then
    kill "$SERVER_PID" 2>/dev/null || true
    wait "$SERVER_PID" 2>/dev/null || true
  fi
}
trap cleanup EXIT

echo "[wizards-smoke] Starting disposable Paper 26.3 build $paper_build."
(
  cd "$SERVER_ROOT"
  exec java -Xms1G -Xmx4G -jar "$paper_jar" --nogui < "$console_fifo"
) > "$server_log" 2>&1 &
SERVER_PID=$!

deadline=$((SECONDS + 1200))
while (( SECONDS < deadline )); do
  if [[ -s "$result_file" ]] && [[ "$(wc -l < "$result_file")" -ge 20 ]]; then
    break
  fi
  if ! server_alive; then
    echo "[wizards-smoke] Paper exited before the smoke plugin produced results." >&2
    tail -n 240 "$server_log" >&2 || true
    exit 1
  fi
  sleep 2
done

if [[ ! -s "$result_file" ]] || [[ "$(wc -l < "$result_file")" -lt 20 ]]; then
  echo "[wizards-smoke] Timed out waiting for all map imports." >&2
  tail -n 240 "$server_log" >&2 || true
  exit 1
fi

# Bukkit.shutdown() follows the report write. Allow Paper time to save/unload and exit.
shutdown_deadline=$((SECONDS + 120))
while server_alive && (( SECONDS < shutdown_deadline )); do
  sleep 2
done
if server_alive; then
  echo "[wizards-smoke] Paper did not stop after the smoke test completed." >&2
  tail -n 160 "$server_log" >&2 || true
  exit 1
fi
set +e
wait "$SERVER_PID"
server_exit=$?
set -e
SERVER_PID=""

echo
echo "=== Wizards Paper 26.3 source-map results ==="
cat "$result_file"
echo
pass_count="$(grep -c $'^PASS\t' "$result_file" || true)"
fail_count="$(grep -c $'^FAIL\t' "$result_file" || true)"
harness_fail_count="$(grep -c $'^HARNESS_FAIL\t' "$result_file" || true)"
echo "Paper exit=$server_exit; PASS=$pass_count; FAIL=$fail_count; HARNESS_FAIL=$harness_fail_count"
if [[ "$pass_count" -ne 20 || "$fail_count" -ne 0 || "$harness_fail_count" -ne 0 || "$server_exit" -ne 0 ]]; then
  echo "[wizards-smoke] One or more snapshot imports failed runtime acceptance." >&2
  tail -n 240 "$server_log" >&2 || true
  exit 1
fi

echo "[wizards-smoke] All 20 Wizards and Nano Wizards snapshots passed Paper 26.3 world-load, start and chest checks."
