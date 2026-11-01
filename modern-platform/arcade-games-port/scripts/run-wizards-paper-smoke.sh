#!/usr/bin/env bash
set -euo pipefail

ARCADE_JAR="${1:?ArcadeMechanicsModern.jar path required}"
SMOKE_PLUGIN_JAR="${2:?WizardsPaperSmoke.jar path required}"
SERVER_ROOT="${3:?Disposable Paper server root required}"
BOT_NODE_MODULES="${4:?Protocol-777 bot node_modules path required}"
USER_AGENT="Testplex-Wizards-Paper-Smoke/1.0 (https://github.com/joshbet9/testplex)"
SERVER_ROOT="$(mkdir -p "$SERVER_ROOT" && cd "$SERVER_ROOT" && pwd)"
mkdir -p "$SERVER_ROOT/plugins" "$SERVER_ROOT/update/maps"
[[ -s "$ARCADE_JAR" ]] || { echo "Missing Arcade jar: $ARCADE_JAR" >&2; exit 1; }
[[ -s "$SMOKE_PLUGIN_JAR" ]] || { echo "Missing smoke-test plugin jar: $SMOKE_PLUGIN_JAR" >&2; exit 1; }
[[ -d "$BOT_NODE_MODULES/@wp2508/minecraft-protocol" ]] || {
  echo "Missing pinned Minecraft 26.3 protocol client under $BOT_NODE_MODULES" >&2
  exit 1
}
BOT_SCRIPT="$(dirname "$0")/wizards-paper-clients.js"
[[ -s "$BOT_SCRIPT" ]] || { echo "Missing protocol client script: $BOT_SCRIPT" >&2; exit 1; }

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
  - wizards
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
  allow-solo-start: false
  allow-generated-test-arena: false
  include-nano-maps: true
  minimum-players: 3
  maximum-players: 3
  kits:
    default: WIZARD
  destructive-spells: false
  temporary-spell-fire: true
  destructive-endgame-blocks: false
CONFIG

cat > "$SERVER_ROOT/eula.txt" <<'EULA'
eula=true
EULA
cat > "$SERVER_ROOT/server.properties" <<'PROPERTIES'
motd=Testplex Wizards Paper 26.3 Import and Gameplay Smoke Test
server-ip=127.0.0.1
server-port=25578
online-mode=false
enforce-secure-profile=false
white-list=false
enforce-whitelist=false
spawn-protection=0
view-distance=3
simulation-distance=2
max-players=4
enable-rcon=false
enable-query=false
PROPERTIES

console_fifo="$SERVER_ROOT/server-console.fifo"
server_log="$SERVER_ROOT/server.log"
client_log="$SERVER_ROOT/client-bots.log"
result_file="$SERVER_ROOT/plugins/WizardsPaperSmoke/results.tsv"
rm -f "$console_fifo" "$result_file" "$client_log"
mkfifo "$console_fifo"
# Keep console input open; the smoke plugin calls Bukkit.shutdown() when done.
exec 3<> "$console_fifo"

server_alive() {
  [[ -n "${SERVER_PID:-}" ]] || return 1
  local state
  state="$(ps -o stat= -p "$SERVER_PID" 2>/dev/null | awk '{print $1}')"
  [[ -n "$state" && "$state" != Z* ]]
}

client_alive() {
  [[ -n "${CLIENT_PID:-}" ]] || return 1
  local state
  state="$(ps -o stat= -p "$CLIENT_PID" 2>/dev/null | awk '{print $1}')"
  [[ -n "$state" && "$state" != Z* ]]
}

cleanup() {
  if client_alive; then
    kill "$CLIENT_PID" 2>/dev/null || true
    wait "$CLIENT_PID" 2>/dev/null || true
  fi
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

# The map-import harness writes 20 PASS rows and then waits for three real clients.
# If the harness fails earlier, it writes a 21st failure row and shuts the server down.
deadline=$((SECONDS + 1200))
waiting_for_clients=false
while (( SECONDS < deadline )); do
  if grep -Fq "WIZARDS_GAMEPLAY_SMOKE|WAITING|" "$server_log"; then
    waiting_for_clients=true
    break
  fi
  if [[ -s "$result_file" ]] && [[ "$(wc -l < "$result_file")" -ge 21 ]]; then
    break
  fi
  if ! server_alive; then
    echo "[wizards-smoke] Paper exited before the source-map/gameplay smoke gate completed." >&2
    tail -n 240 "$server_log" >&2 || true
    exit 1
  fi
  sleep 2
done

if [[ "$waiting_for_clients" == true ]]; then
  echo "[wizards-smoke] Connecting three offline-mode Minecraft 26.3 protocol clients."
  NODE_PATH="$BOT_NODE_MODULES" WIZARDS_SMOKE_HOST=127.0.0.1 WIZARDS_SMOKE_PORT=25578 \
    node "$BOT_SCRIPT" > "$client_log" 2>&1 &
  CLIENT_PID=$!
fi

# Twenty map rows plus one gameplay PASS/FAIL row are required.
result_deadline=$((SECONDS + 1200))
while (( SECONDS < result_deadline )); do
  if [[ -s "$result_file" ]] && [[ "$(wc -l < "$result_file")" -ge 21 ]]; then
    break
  fi
  if ! server_alive; then
    break
  fi
  sleep 2
done

if [[ ! -s "$result_file" ]] || [[ "$(wc -l < "$result_file")" -lt 21 ]]; then
  echo "[wizards-smoke] Timed out before all 20 map checks and the three-client combat gameplay check were recorded." >&2
  tail -n 240 "$server_log" >&2 || true
  tail -n 160 "$client_log" >&2 2>/dev/null || true
  exit 1
fi

# Bukkit.shutdown() follows the complete results write. Allow Paper to save and exit.
shutdown_deadline=$((SECONDS + 120))
while server_alive && (( SECONDS < shutdown_deadline )); do
  sleep 2
done
if server_alive; then
  echo "[wizards-smoke] Paper did not stop after the complete smoke test." >&2
  tail -n 160 "$server_log" >&2 || true
  exit 1
fi
set +e
wait "$SERVER_PID"
server_exit=$?
set -e
SERVER_PID=""

client_exit=0
if [[ -n "${CLIENT_PID:-}" ]]; then
  client_deadline=$((SECONDS + 60))
  while client_alive && (( SECONDS < client_deadline )); do sleep 1; done
  if client_alive; then
    echo "[wizards-smoke] Protocol clients did not exit after Paper shutdown." >&2
    kill "$CLIENT_PID" 2>/dev/null || true
    client_exit=1
  fi
  set +e
  wait "$CLIENT_PID"
  bot_status=$?
  set -e
  CLIENT_PID=""
  if [[ "$bot_status" -ne 0 ]]; then client_exit="$bot_status"; fi
fi

echo
echo "=== Wizards Paper 26.3 source-map and gameplay results ==="
cat "$result_file"
echo
pass_count="$(grep -c '^PASS[[:space:]]' "$result_file" || true)"
fail_count="$(grep -Ec '^(FAIL|GAMEPLAY_FAIL|HARNESS_FAIL)[[:space:]]' "$result_file" || true)"
gameplay_pass_count="$(grep -c '^GAMEPLAY_PASS[[:space:]]' "$result_file" || true)"
bots_ready_count="$(grep -c 'BOT_READY|PASS|count=3' "$client_log" || true)"
echo "Paper exit=$server_exit; map PASS=$pass_count; FAIL=$fail_count; gameplay PASS=$gameplay_pass_count; bot clients ready=$bots_ready_count; client exit=$client_exit"
if [[ "$pass_count" -ne 20 || "$fail_count" -ne 0 || "$gameplay_pass_count" -ne 1 \
      || "$bots_ready_count" -ne 1 || "$server_exit" -ne 0 || "$client_exit" -ne 0 ]]; then
  echo "[wizards-smoke] Map import or three-client combat/lifecycle acceptance failed." >&2
  tail -n 240 "$server_log" >&2 || true
  tail -n 160 "$client_log" >&2 || true
  exit 1
fi

echo "[wizards-smoke] All 20 maps plus a real three-client combat Wizards round passed Paper 26.3 acceptance."
