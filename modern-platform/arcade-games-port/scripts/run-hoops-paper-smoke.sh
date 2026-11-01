#!/usr/bin/env bash
set -euo pipefail

ARCADE_JAR="${1:?ArcadeMechanicsModern.jar path required}"
SMOKE_PLUGIN_JAR="${2:?HoopsPaperSmoke.jar path required}"
SERVER_ROOT="${3:?Disposable Paper server root required}"
BOT_NODE_MODULES="${4:?Minecraft 26.3 protocol client node_modules path required}"
USER_AGENT="Testplex-Hoops-Paper-Smoke/1.0 (https://github.com/joshbet9/testplex)"
SERVER_ROOT="$(mkdir -p "$SERVER_ROOT" && cd "$SERVER_ROOT" && pwd)"
mkdir -p "$SERVER_ROOT/plugins"
[[ -s "$ARCADE_JAR" ]] || { echo "Missing Arcade jar: $ARCADE_JAR" >&2; exit 1; }
[[ -s "$SMOKE_PLUGIN_JAR" ]] || { echo "Missing smoke-test plugin jar: $SMOKE_PLUGIN_JAR" >&2; exit 1; }
[[ -d "$BOT_NODE_MODULES/@wp2508/minecraft-protocol" ]] || {
  echo "Missing pinned Minecraft 26.3 protocol client under $BOT_NODE_MODULES" >&2
  exit 1
}
BOT_SCRIPT="$(dirname "$0")/hoops-paper-clients.js"
[[ -s "$BOT_SCRIPT" ]] || { echo "Missing protocol client script: $BOT_SCRIPT" >&2; exit 1; }

echo "[hoops-smoke] Resolving stable Paper 26.3."
builds_json="$(curl --fail --silent --show-error --retry 3 --user-agent "$USER_AGENT" \
  "https://fill.papermc.io/v3/projects/paper/versions/26.3/builds")"
paper_build="$(printf '%s' "$builds_json" | jq -er 'first(.[] | select(.channel == "STABLE") | .id)')"
paper_url="$(printf '%s' "$builds_json" | jq -er 'first(.[] | select(.channel == "STABLE") | .downloads."server:default".url)')"
paper_sha="$(printf '%s' "$builds_json" | jq -er 'first(.[] | select(.channel == "STABLE") | .downloads."server:default".checksums.sha256)')"
paper_jar="$SERVER_ROOT/paper-26.3-$paper_build.jar"
curl --fail --location --retry 3 --user-agent "$USER_AGENT" "$paper_url" -o "$paper_jar"
printf '%s  %s\n' "$paper_sha" "$paper_jar" | sha256sum -c -

FPP_VERSION="2.1.7"
FPP_TMP="$SERVER_ROOT/fake-player-plugin-$FPP_VERSION.jar"
FPP_URL="https://hangarcdn.papermc.io/plugins/Janky-Development/FakePlayerPlugin/versions/$FPP_VERSION/PAPER/fake-player-plugin-$FPP_VERSION.jar"
echo "[hoops-smoke] Fetching required FakePlayerPlugin $FPP_VERSION dependency."
curl --fail --location --retry 3 --user-agent "$USER_AGENT" "$FPP_URL" -o "$FPP_TMP"
unzip -tq "$FPP_TMP" >/dev/null
fpp_descriptor="$(unzip -p "$FPP_TMP" plugin.yml)"
grep -Fxq "name: FakePlayerPlugin" <<< "$fpp_descriptor"
grep -Fxq "version: $FPP_VERSION" <<< "$fpp_descriptor"
cp "$FPP_TMP" "$SERVER_ROOT/plugins/FakePlayerPlugin.jar"
cp "$ARCADE_JAR" "$SERVER_ROOT/plugins/ArcadeMechanicsModern.jar"
cp "$SMOKE_PLUGIN_JAR" "$SERVER_ROOT/plugins/HoopsPaperSmoke.jar"
mkdir -p "$SERVER_ROOT/plugins/ArcadeMechanicsModern"
cat > "$SERVER_ROOT/plugins/ArcadeMechanicsModern/config.yml" <<'CONFIG'
# The smoke plugin adds Hoops to this disposable server's in-memory
# enabled-games list after the production READY_FOR_TEST guard runs.
enabled-games:
  - death-tag
  - one-in-the-quiver
lobby-selected-game: death-tag
owner-usernames:
  - jn_playzmc
admin-usernames:
  - jn_playzmc
CONFIG
cat > "$SERVER_ROOT/eula.txt" <<'EULA'
eula=true
EULA
cat > "$SERVER_ROOT/server.properties" <<'PROPERTIES'
motd=Testplex Hoops Paper 26.3 Gameplay Acceptance
server-ip=127.0.0.1
server-port=25579
online-mode=false
enforce-secure-profile=false
white-list=false
enforce-whitelist=false
spawn-protection=0
view-distance=3
simulation-distance=2
max-players=8
enable-rcon=false
enable-query=false
PROPERTIES

console_fifo="$SERVER_ROOT/server-console.fifo"
server_log="$SERVER_ROOT/server.log"
client_log="$SERVER_ROOT/client-bots.log"
result_file="$SERVER_ROOT/plugins/HoopsPaperSmoke/results.tsv"
rm -f "$console_fifo" "$result_file" "$client_log"
mkfifo "$console_fifo"
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
  if client_alive; then kill "$CLIENT_PID" 2>/dev/null || true; wait "$CLIENT_PID" 2>/dev/null || true; fi
  if server_alive; then kill "$SERVER_PID" 2>/dev/null || true; wait "$SERVER_PID" 2>/dev/null || true; fi
}
trap cleanup EXIT

echo "[hoops-smoke] Starting disposable Paper 26.3 build $paper_build."
(
  cd "$SERVER_ROOT"
  exec java -Xms1G -Xmx4G -jar "$paper_jar" --nogui < "$console_fifo"
) > "$server_log" 2>&1 &
SERVER_PID=$!

deadline=$((SECONDS + 1200))
waiting_for_clients=false
while (( SECONDS < deadline )); do
  if grep -Fq "HOOPS_SMOKE|WAITING|" "$server_log"; then
    waiting_for_clients=true
    break
  fi
  if [[ -s "$result_file" ]]; then break; fi
  if ! server_alive; then
    echo "[hoops-smoke] Paper exited before the acceptance harness was ready." >&2
    tail -n 240 "$server_log" >&2 || true
    exit 1
  fi
  sleep 2
done

if [[ "$waiting_for_clients" == true ]]; then
  echo "[hoops-smoke] Connecting four real Minecraft 26.3 protocol clients."
  NODE_PATH="$BOT_NODE_MODULES" HOOPS_SMOKE_HOST=127.0.0.1 HOOPS_SMOKE_PORT=25579 \
    node "$BOT_SCRIPT" > "$client_log" 2>&1 &
  CLIENT_PID=$!
fi

result_deadline=$((SECONDS + 1200))
while (( SECONDS < result_deadline )); do
  if [[ -s "$result_file" ]]; then break; fi
  if ! server_alive; then break; fi
  sleep 2
done

if [[ ! -s "$result_file" ]]; then
  echo "[hoops-smoke] Timed out before the acceptance result was written." >&2
  tail -n 240 "$server_log" >&2 || true
  tail -n 160 "$client_log" >&2 2>/dev/null || true
  exit 1
fi

shutdown_deadline=$((SECONDS + 120))
while server_alive && (( SECONDS < shutdown_deadline )); do sleep 2; done
if server_alive; then
  echo "[hoops-smoke] Paper did not stop after the acceptance result." >&2
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
    echo "[hoops-smoke] Protocol clients did not exit after Paper shutdown." >&2
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
echo "=== Hoops Paper 26.3 gameplay acceptance ==="
cat "$result_file"
echo
pass_count="$(grep -c '^PASS[[:space:]]' "$result_file" || true)"
fail_count="$(grep -Ec '^(FAIL|HARNESS_FAIL)[[:space:]]' "$result_file" || true)"
bots_ready_count="$(grep -c 'BOT_READY|PASS|count=4' "$client_log" || true)"
echo "Paper exit=$server_exit; PASS=$pass_count; FAIL=$fail_count; four clients ready=$bots_ready_count; client exit=$client_exit"
if [[ "$pass_count" -ne 1 || "$fail_count" -ne 0 || "$bots_ready_count" -ne 1 \
      || "$server_exit" -ne 0 || "$client_exit" -ne 0 ]]; then
  echo "[hoops-smoke] Paper gameplay acceptance failed." >&2
  tail -n 240 "$server_log" >&2 || true
  tail -n 160 "$client_log" >&2 || true
  exit 1
fi

echo "[hoops-smoke] Four-client gameplay, match cleanup and snapshot restoration passed."
