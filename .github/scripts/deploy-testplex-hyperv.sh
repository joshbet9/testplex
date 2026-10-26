#!/usr/bin/env bash
# Run on the Hyper-V host as root through the existing Tailscale/SSH deployment path.
set -Eeuo pipefail
umask 077

if [[ $# -ne 3 ]]; then
  echo "Usage: deploy-testplex-hyperv.sh <40-char-build-sha> <server-root> <systemd-service>" >&2
  exit 2
fi

SHA="$1"
ROOT_INPUT="$2"
SERVICE="$3"

[[ "$SHA" =~ ^[0-9a-f]{40}$ ]] || { echo "Refusing invalid build SHA." >&2; exit 2; }
# Keep this deployment fenced into a dedicated test-only directory namespace.
[[ "$ROOT_INPUT" =~ ^/home/monstermaze/servers/testplex(-[A-Za-z0-9._-]+)?$ ]] || {
  echo "Refusing server root outside /home/monstermaze/servers/testplex[-name]: $ROOT_INPUT" >&2
  exit 2
}
[[ "$SERVICE" =~ ^testplex-[A-Za-z0-9_.@-]+\.service$ ]] || {
  echo "Refusing service name outside the testplex-*.service namespace: $SERVICE" >&2
  exit 2
}
case "$SERVICE" in
  monstermaze-18.service|monstermaze-21.service)
    echo "Refusing to touch an existing Monster Maze standalone service." >&2
    exit 2
    ;;
esac

ROOT="$(realpath -e "$ROOT_INPUT")"
[[ "$ROOT" == "$ROOT_INPUT" ]] || {
  echo "Refusing symlinked or redirected server root: $ROOT_INPUT -> $ROOT" >&2
  exit 2
}
[[ -d "$ROOT" ]] || { echo "Server root does not exist: $ROOT" >&2; exit 2; }
MARKER="$ROOT/.testplex-sandbox"
EXPECTED_MARKER="TESTPLEX ISOLATED TEST SERVER - NOT PRODUCTION"
[[ -f "$MARKER" ]] || { echo "Refusing deployment: missing $MARKER" >&2; exit 2; }
[[ "$(cat "$MARKER")" == "$EXPECTED_MARKER" ]] || {
  echo "Refusing deployment: $MARKER does not contain the exact test-only marker." >&2
  exit 2
}

PLUGINS="$ROOT/plugins"
LOG="$ROOT/logs/latest.log"
INCOMING="/tmp/testplex-plugin-set-$SHA.zip"
STAGE=""
cleanup_preflight() {
  rm -f "$INCOMING"
  if [[ -n "$STAGE" ]]; then rm -rf "$STAGE"; fi
}
trap cleanup_preflight EXIT

[[ -d "$PLUGINS" ]] || { echo "Missing plugins directory: $PLUGINS" >&2; exit 2; }
IFS= read -r DOWNLOAD_URL || {
  echo "Missing one-time GitHub artifact download URL on standard input." >&2
  exit 2
}
case "$DOWNLOAD_URL" in
  https://*.blob.core.windows.net/*|https://*.actions.githubusercontent.com/*|https://*.githubusercontent.com/*|https://github.com/*) ;;
  *)
    echo "Refusing artifact URL outside GitHub's download hosts." >&2
    exit 2
    ;;
esac
systemctl cat "$SERVICE" >/dev/null
systemctl is-active --quiet "$SERVICE" || {
  echo "Refusing to deploy because $SERVICE is not active before deployment." >&2
  exit 2
}
WORKING_DIR="$(systemctl show "$SERVICE" --property=WorkingDirectory --value)"
[[ "$WORKING_DIR" == "$ROOT" ]] || {
  echo "Refusing deployment: $SERVICE WorkingDirectory is '$WORKING_DIR', expected '$ROOT'." >&2
  exit 2
}
echo "Pre-deployment memory and disk:"
free -h
df -h "$ROOT"

echo "Downloading selected GitHub artifact directly to the test VM."
curl --fail --location --silent --show-error \
  --retry 3 --retry-delay 2 --connect-timeout 15 --max-time 600 \
  --output "$INCOMING" "$DOWNLOAD_URL"
[[ -s "$INCOMING" ]] || { echo "GitHub artifact download is empty." >&2; exit 2; }
unzip -tq "$INCOMING" >/dev/null
BUNDLE_LIST="$(unzip -Z1 "$INCOMING")"
[[ "$(grep -Fxc "Arcade.jar" <<< "$BUNDLE_LIST" || true)" == "1" ]] || {
  echo "Build bundle must contain exactly one Arcade.jar." >&2
  exit 2
}
BUNDLE_NAMES=()
declare -A BUNDLE_SEEN=()
JAR_ENTRY_RE='^[A-Za-z0-9._+() -]+[.]jar$'
while IFS= read -r entry; do
  [[ "$entry" =~ $JAR_ENTRY_RE ]] || {
    echo "Unexpected entry in plugin bundle: $entry" >&2
    exit 2
  }
  if [[ -n "${BUNDLE_SEEN[$entry]:-}" ]]; then
    echo "Duplicate entry in plugin bundle: $entry" >&2
    exit 2
  fi
  BUNDLE_SEEN["$entry"]=1
  BUNDLE_NAMES+=("$entry")
done <<< "$BUNDLE_LIST"

# This is a single Arcade test instance, not a combined Hub/Clans/NanoGames server.
# Also clean up sibling modules potentially left by earlier full-set deployment attempts.
LEGACY_SERVER_MODULE_JARS=(
  "BungeeRotator.jar"
  "Clans.jar"
  "ClansCompensation.jar"
  "ClansGenerator.jar"
  "ClansHub.jar"
  "ClansQueue.jar"
  "DDoSProtectionSwitcher.jar"
  "EnjinTranslator.jar"
  "GemHunters.jar"
  "Google Sheets.jar"
  "Hub.jar"
  "MapParser.jar"
  "Mineplexer.jar"
  "NanoGames.jar"
  "ServerMonitor.jar"
  "StaffServer.jar"
  "UHC WorldGen.jar"
  "mavericks-review-hub.jar"
)
MANAGED_NAMES=()
declare -A MANAGED_SEEN=()
for name in "${BUNDLE_NAMES[@]}" "${LEGACY_SERVER_MODULE_JARS[@]}"; do
  if [[ -z "${MANAGED_SEEN[$name]:-}" ]]; then
    MANAGED_SEEN["$name"]=1
    MANAGED_NAMES+=("$name")
  fi
done

STAGE="$(mktemp -d "/tmp/testplex-stage-$SHA.XXXXXX")"
unzip -p "$INCOMING" "Arcade.jar" > "$STAGE/Arcade.jar"
[[ -s "$STAGE/Arcade.jar" ]] || { echo "Extracted Arcade.jar is empty." >&2; exit 2; }
JAR_PATHS=("$STAGE/Arcade.jar")
JAR_NAMES=("Arcade.jar")
echo "Validated ${#BUNDLE_NAMES[@]} artifact JARs; this test server will install Arcade.jar only."

if [[ -f "$PLUGINS/Arcade.jar" ]]; then
  OWNER_GROUP="$(stat -c '%U:%G' "$PLUGINS/Arcade.jar")"
else
  OWNER_GROUP="$(stat -c '%U:%G' "$PLUGINS")"
fi
IFS=: read -r PLUGIN_OWNER PLUGIN_GROUP <<< "$OWNER_GROUP"

STAMP="$(date -u +%Y%m%dT%H%M%SZ)"
BACKUP_DIR="$ROOT/.testplex-backups/$STAMP-$SHA"
install -d -m 0750 "$BACKUP_DIR"
declare -A EXISTED=()
for name in "${MANAGED_NAMES[@]}"; do
  if [[ -f "$PLUGINS/$name" ]]; then
    cp -p "$PLUGINS/$name" "$BACKUP_DIR/$name"
    EXISTED["$name"]=1
  else
    EXISTED["$name"]=0
  fi
done

DEPLOYMENT_TOUCHED=0
rollback() {
  local status=$?
  trap - EXIT
  if [[ "$status" -ne 0 && "$DEPLOYMENT_TOUCHED" -eq 1 ]]; then
    echo "Deployment failed; attempting to restore the previous plugin files." >&2
    if ! timeout 20 systemctl stop "$SERVICE"; then
      echo "Graceful stop timed out; force-killing only the test service cgroup." >&2
      timeout 10 systemctl kill --kill-who=all --signal=KILL "$SERVICE" || true
    fi
    if systemctl is-active --quiet "$SERVICE"; then
      echo "ERROR: $SERVICE still appears active; refusing to overwrite plugin files while Java may be using them." >&2
      timeout 10 systemctl status "$SERVICE" --no-pager --full || true
      rm -f "$INCOMING"
      rm -rf "$STAGE"
      exit "$status"
    fi
    for name in "${MANAGED_NAMES[@]}"; do
      if [[ -f "$BACKUP_DIR/$name" ]]; then
        cp -p "$BACKUP_DIR/$name" "$PLUGINS/$name" || true
      elif [[ "${EXISTED[$name]:-0}" -eq 0 ]]; then
        rm -f "$PLUGINS/$name" || true
      fi
    done
    timeout 90 systemctl start "$SERVICE" || true
    sleep 3
    timeout 10 journalctl -u "$SERVICE" -n 60 --no-pager || true
  fi
  rm -f "$INCOMING"
  rm -rf "$STAGE"
  exit "$status"
}
trap rollback EXIT

DEPLOYMENT_TOUCHED=1
echo "Stopping isolated test service $SERVICE."
timeout 60 systemctl stop "$SERVICE"
if systemctl is-active --quiet "$SERVICE"; then
  echo "$SERVICE still reports active after stop." >&2
  exit 1
fi

for name in "${MANAGED_NAMES[@]}"; do
  if [[ "$name" != "Arcade.jar" ]]; then
    rm -f "$PLUGINS/$name"
  fi
done
for path in "${JAR_PATHS[@]}"; do
  name="$(basename "$path")"
  install -o "$PLUGIN_OWNER" -g "$PLUGIN_GROUP" -m 0644 "$path" "$PLUGINS/$name"
done

START_MARKER="$STAGE/.startup-check-started"
touch "$START_MARKER"
echo "Starting isolated Reborn test server $SERVICE."
timeout 90 systemctl start "$SERVICE"

dump_diagnostics() {
  echo "===== startup failure diagnostics: service status =====" >&2
  timeout 8 systemctl status "$SERVICE" --no-pager --full >&2 || true
  echo "===== startup failure diagnostics: recent systemd journal =====" >&2
  timeout 8 journalctl -u "$SERVICE" -n 80 --no-pager >&2 || true
  echo "===== startup failure diagnostics: latest.log =====" >&2
  if [[ -s "$LOG" ]]; then
    tail -n 120 "$LOG" >&2 || true
  else
    echo "Missing or empty log file: $LOG" >&2
  fi
  echo "===== startup failure diagnostics: memory and disk =====" >&2
  free -h >&2 || true
  df -h "$ROOT" >&2 || true
  echo "===== startup failure diagnostics: kernel OOM messages =====" >&2
  timeout 5 dmesg -T 2>/dev/null | grep -Ei 'out of memory|oom-kill|killed process' | tail -n 20 >&2 || true
}

ready=0
for attempt in $(seq 1 180); do
  if ! systemctl is-active --quiet "$SERVICE"; then
    echo "$SERVICE became inactive during startup at ${attempt}s." >&2
    break
  fi
  if [[ -s "$LOG" && "$LOG" -nt "$START_MARKER" ]] \
    && tail -n 2500 "$LOG" | grep -Fq "Enabling Arcade"; then
    ready=1
    break
  fi
  sleep 1
done
if [[ "$ready" -ne 1 ]]; then
  echo "Arcade did not produce a fresh startup marker within 180 seconds; diagnostics follow before rollback." >&2
  dump_diagnostics
  exit 1
fi

echo "Hyper-V Reborn Arcade test deployment succeeded for build $SHA."
echo "Installed JAR count: ${#JAR_PATHS[@]} (Arcade only)"
echo "Arcade.jar SHA-256:"
sha256sum "$PLUGINS/Arcade.jar"
echo "Rollback backup: $BACKUP_DIR"

rm -f "$INCOMING"
rm -rf "$STAGE"
trap - EXIT
