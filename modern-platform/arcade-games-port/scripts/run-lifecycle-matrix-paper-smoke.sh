#!/usr/bin/env bash
set -euo pipefail

# Expensive by design: opt-in only. One Paper process and four real protocol
# clients exercise the entire enabled roster, with no FakePlayerPlugin bots.
ARCADE_JAR="${1:?ArcadeMechanicsModern.jar required}"
HARNESS_JAR="${2:?ArcadeLifecycleMatrixSmoke.jar required}"
SERVER_ROOT="${3:?Disposable server directory required}"
BOT_NODE_MODULES="${4:?Pinned Minecraft 26.3 client node_modules required}"
USER_AGENT="Testplex-Lifecycle-Matrix/1.0 (https://github.com/joshbet9/testplex)"
SERVER_ROOT="$(mkdir -p "$SERVER_ROOT" && cd "$SERVER_ROOT" && pwd)"
[[ -s "$ARCADE_JAR" && -s "$HARNESS_JAR" ]] || { echo "Missing smoke test JAR." >&2; exit 1; }
[[ -d "$BOT_NODE_MODULES/@wp2508/minecraft-protocol" ]] || {
  echo "Pinned Minecraft 26.3 client library is missing." >&2; exit 1;
}

mkdir -p "$SERVER_ROOT/plugins" "$SERVER_ROOT/update/maps"
echo "[lifecycle-matrix] Importing Wizards source archives once for the shared map preflight."
python3 "$(dirname "$0")/download-wizards-source-maps.py" --map-root "$SERVER_ROOT/update/maps"

builds_json="$(curl --fail --silent --show-error --retry 3 --user-agent "$USER_AGENT" \
  'https://fill.papermc.io/v3/projects/paper/versions/26.3/builds')"
paper_build="$(printf '%s' "$builds_json" | jq -er 'first(.[] | select(.channel == "STABLE") | .id)')"
paper_url="$(printf '%s' "$builds_json" | jq -er 'first(.[] | select(.channel == "STABLE") | .downloads."server:default".url)')"
paper_sha="$(printf '%s' "$builds_json" | jq -er 'first(.[] | select(.channel == "STABLE") | .downloads."server:default".checksums.sha256)')"
paper_jar="$SERVER_ROOT/paper-26.3-$paper_build.jar"
curl --fail --location --retry 3 --user-agent "$USER_AGENT" "$paper_url" -o "$paper_jar"
printf '%s  %s\n' "$paper_sha" "$paper_jar" | sha256sum -c -

cp "$ARCADE_JAR" "$SERVER_ROOT/plugins/ArcadeMechanicsModern.jar"
cp "$HARNESS_JAR" "$SERVER_ROOT/plugins/ArcadeLifecycleMatrixSmoke.jar"
# Sneaky Assassins needs the runnable, shaded PacketEvents server plugin.
# The Maven compile-time jar contains plugin.yml but omits the core API classes,
# so it cannot be installed directly on Paper. Use the immutable published
# 2.14.0 Spigot version from PacketEvents' official Modrinth distribution.
packet_events_jar="$SERVER_ROOT/plugins/PacketEvents.jar"
curl --fail --silent --show-error --location --retry 3 --user-agent "$USER_AGENT" \
  'https://cdn.modrinth.com/data/HYKaKraK/versions/m78nFxYg/packetevents-spigot-2.14.0.jar' \
  -o "$packet_events_jar"
if [[ ! -s "$packet_events_jar" ]] \
    || ! unzip -Z -1 "$packet_events_jar" | grep -E '^(plugin.yml|paper-plugin.yml)$' >/dev/null \
    || ! unzip -Z -1 "$packet_events_jar" | grep -E '^com/github/retrooper/packetevents/PacketEventsAPI.class$' >/dev/null; then
  echo '[lifecycle-matrix] PacketEvents 2.14.0 server plugin is missing required runtime classes.' >&2
  exit 1
fi
mkdir -p "$SERVER_ROOT/plugins/ArcadeMechanicsModern"
# Retain the production 32-choice allow-list; no special debug enablement.
unzip -p "$ARCADE_JAR" config.yml > "$SERVER_ROOT/plugins/ArcadeMechanicsModern/config.yml"
# Monster Maze's production map service requires a physical world. The default
# Eye of Ender source ZIP only has WorldConfig.dat; use the repository's authentic
# Volcano terrain archive for all three mode lifecycle checks in this disposable
# server. Monster Maze terrain is intentionally not packaged in the Arcade JAR;
# this does not change the production map selection or enlarge the shipped JAR.
source_root="$(cd "$(dirname "$0")/../../.." && pwd)"
monster_maze_archive="$source_root/update/maps/Monster Maze/Volcano.zip"
[[ -s "$monster_maze_archive" ]] || { echo '[lifecycle-matrix] Missing original Volcano archive.' >&2; exit 1; }
mkdir -p "$SERVER_ROOT/mm_volcano" "$SERVER_ROOT/plugins/ArcadeMechanicsModern/monster-maze"
unzip -qq "$monster_maze_archive" -d "$SERVER_ROOT/mm_volcano"
if [[ ! -s "$SERVER_ROOT/mm_volcano/level.dat" || ! -d "$SERVER_ROOT/mm_volcano/region" ]]; then
  echo '[lifecycle-matrix] Bundled Volcano archive did not provide a usable Monster Maze world.' >&2
  exit 1
fi
printf 'map: volcano\n' > "$SERVER_ROOT/plugins/ArcadeMechanicsModern/monster-maze/config.yml"
printf 'eula=true\n' > "$SERVER_ROOT/eula.txt"
printf '%s\n' \
  'motd=Testplex all-game lifecycle matrix' \
  'server-ip=127.0.0.1' \
  'server-port=25579' \
  'online-mode=false' \
  'enforce-secure-profile=false' \
  'white-list=false' \
  'spawn-protection=0' \
  'view-distance=3' \
  'simulation-distance=2' \
  'max-players=8' \
  'enable-rcon=false' > "$SERVER_ROOT/server.properties"

console_fifo="$SERVER_ROOT/server-console.fifo"
server_log="$SERVER_ROOT/server.log"
client_log="$SERVER_ROOT/client-bots.log"
result_file="$SERVER_ROOT/plugins/ArcadeLifecycleMatrixSmoke/results.tsv"
mkfifo "$console_fifo"
exec 3<> "$console_fifo"

is_alive() {
  local pid="$1" state
  [[ -n "$pid" ]] || return 1
  state="$(ps -o stat= -p "$pid" 2>/dev/null | awk '{print $1}')"
  [[ -n "$state" && "$state" != Z* ]]
}
cleanup() {
  if is_alive "${CLIENT_PID:-}"; then kill "$CLIENT_PID" 2>/dev/null || true; wait "$CLIENT_PID" 2>/dev/null || true; fi
  if is_alive "${SERVER_PID:-}"; then kill "$SERVER_PID" 2>/dev/null || true; wait "$SERVER_PID" 2>/dev/null || true; fi
}
trap cleanup EXIT

echo "[lifecycle-matrix] Starting one disposable Paper 26.3 server ($paper_build)."
(
  cd "$SERVER_ROOT"
  exec java -Xms1G -Xmx5G -jar "$paper_jar" --nogui < "$console_fifo"
) > "$server_log" 2>&1 &
SERVER_PID=$!

ready=false
deadline=$((SECONDS + 180))
while (( SECONDS < deadline )); do
  if grep -Fq 'LIFECYCLE_MATRIX|WAITING|' "$server_log"; then ready=true; break; fi
  if ! is_alive "$SERVER_PID"; then break; fi
  sleep 2
done
if [[ "$ready" != true ]]; then
  echo "[lifecycle-matrix] Paper never reached the test harness readiness marker." >&2
  tail -n 150 "$server_log" >&2 || true
  exit 1
fi

echo '[lifecycle-matrix] Connecting four real Minecraft protocol clients once.'
NODE_PATH="$BOT_NODE_MODULES" HOOPS_SMOKE_HOST=127.0.0.1 HOOPS_SMOKE_PORT=25579 \
  node "$(dirname "$0")/hoops-paper-clients.js" > "$client_log" 2>&1 &
CLIENT_PID=$!

deadline=$((SECONDS + 1200))
while (( SECONDS < deadline )); do
  if [[ -s "$result_file" ]] || ! is_alive "$SERVER_PID"; then break; fi
  sleep 2
done
if [[ ! -s "$result_file" ]]; then
  echo '[lifecycle-matrix] Timed out or server exited without a result file.' >&2
  tail -n 150 "$server_log" >&2 || true
  exit 1
fi

deadline=$((SECONDS + 90))
while is_alive "$SERVER_PID" && (( SECONDS < deadline )); do sleep 2; done
if is_alive "$SERVER_PID"; then
  echo '[lifecycle-matrix] Paper did not shut down cleanly.' >&2
  exit 1
fi
set +e
wait "$SERVER_PID"
server_exit=$?
set -e
SERVER_PID=''
client_deadline=$((SECONDS + 45))
while is_alive "$CLIENT_PID" && (( SECONDS < client_deadline )); do sleep 1; done
client_exit=0
if is_alive "$CLIENT_PID"; then
  echo '[lifecycle-matrix] Client process failed to exit after Paper shutdown.' >&2
  kill "$CLIENT_PID" 2>/dev/null || true
  client_exit=1
fi
set +e
wait "$CLIENT_PID"
wait_status=$?
set -e
if [[ "$wait_status" -ne 0 ]]; then client_exit="$wait_status"; fi
CLIENT_PID=''

echo '=== Paper 26.3 enabled-game lifecycle matrix ==='
cat "$result_file"
pass_count="$(grep -c '^PASS[[:space:]]' "$result_file" || true)"
fail_count="$(grep -c '^FAIL[[:space:]]' "$result_file" || true)"
summary="$(grep '^SUMMARY[[:space:]]' "$result_file" | tail -n 1)"
requested_games="${TESTPLEX_LIFECYCLE_GAMES:-}"
expected_count=32
if [[ -n "$requested_games" ]]; then
  expected_count="$(printf '%s' "$requested_games" | awk -F, '{print NF}')"
fi
echo "[lifecycle-matrix] PASS=$pass_count FAIL=$fail_count expected=$expected_count server_exit=$server_exit client_exit=$client_exit"
if [[ "$fail_count" -ne 0 || "$pass_count" -ne "$expected_count" || "$summary" != *
  echo '[lifecycle-matrix] Acceptance failed; focused plugin diagnostics follow.' >&2
  grep -E 'LIFECYCLE_MATRIX|ArcadeMechanicsModern|SneakyAssassins|\[ERROR\]|\[WARN\]|Exception|Caused by|Could not|Unable|Failed|failed' "$server_log" | tail -n 320 >&2 || true
  echo '[lifecycle-matrix] Monster Maze phase diagnostics:' >&2
  grep -F 'LIFECYCLE_MATRIX|MONSTER_MAZE_TRACE|' "$server_log" >&2 || true
  echo '[lifecycle-matrix] Full server and client logs remain in the uploaded artifact.' >&2
  exit 1
fi
echo '[lifecycle-matrix] All enabled games entered and exited Live with four real clients.'
\tPASS' \
      || "$server_exit" -ne 0 || "$client_exit" -ne 0 ]] \
    || ! grep -Fq 'BOT_READY|PASS|count=4' "$client_log"; then
  echo '[lifecycle-matrix] Acceptance failed; focused plugin diagnostics follow.' >&2
  grep -E 'LIFECYCLE_MATRIX|ArcadeMechanicsModern|SneakyAssassins|\[ERROR\]|\[WARN\]|Exception|Caused by|Could not|Unable|Failed|failed' "$server_log" | tail -n 320 >&2 || true
  echo '[lifecycle-matrix] Full server and client logs remain in the uploaded artifact.' >&2
  exit 1
fi
echo '[lifecycle-matrix] All enabled games entered and exited Live with four real clients.'
