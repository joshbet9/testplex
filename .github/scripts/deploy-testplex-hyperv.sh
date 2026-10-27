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
MAP_DEST="$(realpath -m "$ROOT/../../update/maps")"
[[ "$MAP_DEST" == "/home/monstermaze/update/maps" ]] || {
  echo "Refusing map destination outside /home/monstermaze/update/maps: $MAP_DEST" >&2
  exit 2
}
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
UPLOAD="${INCOMING}.upload"
if [[ -s "$UPLOAD" ]]; then
  echo "Using the plugin/map artifact uploaded over the authenticated SSH route."
  mv "$UPLOAD" "$INCOMING"
else
  IFS= read -r DOWNLOAD_URL || {
    echo "Missing uploaded artifact and one-time GitHub download URL on standard input." >&2
    exit 2
  }
  case "$DOWNLOAD_URL" in
    https://*.blob.core.windows.net/*|https://*.actions.githubusercontent.com/*|https://*.githubusercontent.com/*|https://github.com/*) ;;
    *)
      echo "Refusing artifact URL outside GitHub's download hosts." >&2
      exit 2
      ;;
  esac
fi
timeout 8 systemctl cat "$SERVICE" >/dev/null
SERVICE_STATE="$(timeout 8 systemctl is-active "$SERVICE" 2>/dev/null || true)"
case "$SERVICE_STATE" in
  active|inactive|failed)
    echo "Verified isolated service unit $SERVICE (state: $SERVICE_STATE)."
    ;;
  *)
    echo "Refusing deployment: could not safely resolve $SERVICE state ('$SERVICE_STATE')." >&2
    exit 2
    ;;
esac
WORKING_DIR="$(timeout 8 systemctl show "$SERVICE" --property=WorkingDirectory --value)"

# Return 0 when active, 1 when confirmed inactive/failed, 2 when the service
# state cannot be determined. Callers must never treat a timeout as "stopped".
service_active_safe() {
  local state rc
  if state="$(timeout 5 systemctl is-active "$SERVICE" 2>/dev/null)"; then
    :
  else
    rc=$?
    case "$rc" in
      124|125|137) return 2 ;;
    esac
  fi
  case "$state" in
    active) return 0 ;;
    inactive|failed) return 1 ;;
    *) return 2 ;;
  esac
}
[[ "$WORKING_DIR" == "$ROOT" ]] || {
  echo "Refusing deployment: $SERVICE WorkingDirectory is '$WORKING_DIR', expected '$ROOT'." >&2
  exit 2
}
echo "Pre-deployment memory and disk:"
free -h
df -h "$ROOT"

if [[ ! -s "$INCOMING" ]]; then
  echo "Downloading selected GitHub artifact directly to the test VM."
  curl --fail --location --silent --show-error \
    --retry 3 --retry-delay 2 --connect-timeout 15 --max-time 1800 \
    --output "$INCOMING" "$DOWNLOAD_URL"
fi
[[ -s "$INCOMING" ]] || { echo "GitHub artifact download is empty." >&2; exit 2; }
unzip -tq "$INCOMING" >/dev/null
BUNDLE_LIST="$(unzip -Z1 "$INCOMING")"
[[ "$(grep -Fxc "Arcade.jar" <<< "$BUNDLE_LIST" || true)" == "1" ]] || {
  echo "Build bundle must contain exactly one Arcade.jar." >&2
  exit 2
}
[[ "$(grep -Fxc "testplex-map-assets.zip" <<< "$BUNDLE_LIST" || true)" == "1" ]] || {
  echo "Build bundle must contain exactly one testplex-map-assets.zip from the same source revision." >&2
  exit 2
}
BUNDLE_NAMES=()
declare -A BUNDLE_SEEN=()
JAR_ENTRY_RE='^[A-Za-z0-9._+() -]+[.]jar$'
while IFS= read -r entry; do
  if [[ "$entry" == "testplex-map-assets.zip" ]]; then
    continue
  fi
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
unzip -p "$INCOMING" "testplex-map-assets.zip" > "$STAGE/testplex-map-assets.zip"
[[ -s "$STAGE/testplex-map-assets.zip" ]] || { echo "Extracted Mineplex map asset bundle is empty." >&2; exit 2; }
unzip -tq "$STAGE/testplex-map-assets.zip" >/dev/null
while IFS= read -r map_entry; do
  [[ "$map_entry" =~ ^maps/[^/]+(/[^/]+)*[.]zip$ ]] || {
    echo "Unexpected entry in Mineplex map bundle: $map_entry" >&2
    exit 2
  }
  map_rel="${map_entry#maps/}"
  case "/$map_rel/" in
    *"/../"*|*"/./"*|*"//"*)
      echo "Refusing unsafe path in Mineplex map bundle: $map_entry" >&2
      exit 2
      ;;
  esac
done < <(unzip -Z1 "$STAGE/testplex-map-assets.zip")
MAP_STAGE="$STAGE/map-assets"
mkdir -p "$MAP_STAGE"
unzip -q "$STAGE/testplex-map-assets.zip" -d "$MAP_STAGE"
mapfile -d '' -t MAP_FILES < <(find "$MAP_STAGE/maps" -type f -name '*.zip' -print0 | sort -z)
[[ "${#MAP_FILES[@]}" -gt 0 ]] || { echo "Map asset bundle contains no map ZIPs." >&2; exit 2; }
for map_file in "${MAP_FILES[@]}"; do
  unzip -tq "$map_file" >/dev/null || { echo "Invalid map archive: $map_file" >&2; exit 2; }
done
JAR_PATHS=("$STAGE/Arcade.jar")
JAR_NAMES=("Arcade.jar")
echo "Validated ${#BUNDLE_NAMES[@]} artifact JARs and ${#MAP_FILES[@]} Mineplex map archives; this test server will install Arcade.jar only."

if [[ -f "$PLUGINS/Arcade.jar" ]]; then
  OWNER_GROUP="$(stat -c '%U:%G' "$PLUGINS/Arcade.jar")"
else
  OWNER_GROUP="$(stat -c '%U:%G' "$PLUGINS")"
fi
IFS=: read -r PLUGIN_OWNER PLUGIN_GROUP <<< "$OWNER_GROUP"

STAMP="$(date -u +%Y%m%dT%H%M%SZ)"
BACKUP_DIR="$ROOT/.testplex-backups/$STAMP-$SHA"
install -d -m 0750 "$BACKUP_DIR"
WORLD_BACKUP="$BACKUP_DIR/world-before-test-lobby.tar.gz"
WORLD_BACKUP_CREATED=0
MAP_ADDED_LIST="$BACKUP_DIR/maps-added.list"
: > "$MAP_ADDED_LIST"
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
    if service_active_safe; then
      service_state_rc=0
    else
      service_state_rc=$?
    fi
    if [[ "$service_state_rc" -ne 1 ]]; then
      if [[ "$service_state_rc" -eq 0 ]]; then
        echo "ERROR: $SERVICE still appears active; refusing to overwrite plugin files while Java may be using them." >&2
      else
        echo "ERROR: $SERVICE state is unknown; refusing to overwrite plugin files because Java may still be using them." >&2
      fi
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

    if [[ -d "$BACKUP_DIR/maps" ]]; then
      while IFS= read -r -d '' backup_map; do
        rel="${backup_map#"$BACKUP_DIR/maps/"}"
        install -d "$(dirname "$MAP_DEST/$rel")"
        cp -p "$backup_map" "$MAP_DEST/$rel" || true
      done < <(find "$BACKUP_DIR/maps" -type f -print0)
    fi
    if [[ -s "$MAP_ADDED_LIST" ]]; then
      while IFS= read -r added_map; do
        [[ "$added_map" == "$MAP_DEST/"* ]] && rm -f "$added_map" || true
      done < "$MAP_ADDED_LIST"
    fi

    if [[ "$WORLD_BACKUP_CREATED" -eq 1 && -s "$WORLD_BACKUP" ]]; then
      echo "Restoring the pre-deployment world after failure." >&2
      rm -rf "$ROOT/world"
      tar -xzf "$WORLD_BACKUP" -C "$ROOT" || true
    fi

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
if service_active_safe; then
  echo "$SERVICE still reports active after stop." >&2
  exit 1
else
  service_state_rc=$?
  if [[ "$service_state_rc" -ne 1 ]]; then
    echo "Cannot confirm that $SERVICE stopped cleanly; refusing to overwrite plugin files." >&2
    exit 1
  fi
fi

if [[ ! -f "$ROOT/world/NEW.dat" && ! -f "$ROOT/world/TEST_LOBBY_PLANE.dat" && -d "$ROOT/world" ]]; then
  echo "Backing up the current world before preparing the flat lobby."
  tar -czf "$WORLD_BACKUP" -C "$ROOT" world
  WORLD_BACKUP_CREATED=1
fi

install -d -o monstermaze -g monstermaze -m 0755 "$MAP_DEST"
for source_map in "${MAP_FILES[@]}"; do
  rel="${source_map#"$MAP_STAGE/maps/"}"
  [[ "$rel" != "$source_map" && "$rel" != *"/../"* && "$rel" != ../* ]] || {
    echo "Refusing unsafe extracted map path: $source_map" >&2
    exit 2
  }
  dest="$MAP_DEST/$rel"
  if [[ -f "$dest" ]] && cmp -s "$source_map" "$dest"; then
    continue
  fi
  if [[ -f "$dest" ]]; then
    backup_map="$BACKUP_DIR/maps/$rel"
    install -d -m 0750 "$(dirname "$backup_map")"
    cp -p "$dest" "$backup_map"
  else
    printf '%s\n' "$dest" >> "$MAP_ADDED_LIST"
  fi
  install -D -o monstermaze -g monstermaze -m 0644 "$source_map" "$dest"
done

source_count="${#MAP_FILES[@]}"
installed_count=0
while IFS= read -r -d '' installed_map; do
  rel="${installed_map#"$MAP_STAGE/maps/"}"
  cmp -s "$installed_map" "$MAP_DEST/$rel" || {
    echo "Installed map did not match the source bundle: $rel" >&2
    exit 2
  }
  installed_count=$((installed_count + 1))
done < <(find "$MAP_STAGE/maps" -type f -name '*.zip' -print0)
[[ "$installed_count" -eq "$source_count" ]] || {
  echo "Map verification count mismatch: source=$source_count installed=$installed_count" >&2
  exit 2
}
echo "Installed/verified $installed_count Mineplex map ZIPs under $MAP_DEST."

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

arcade_log_ready=0
lobby_marker_ready=0
ready=0
for attempt in $(seq 1 180); do
  if service_active_safe; then
    :
  else
    service_state_rc=$?
    if [[ "$service_state_rc" -eq 1 ]]; then
      echo "$SERVICE became inactive during startup at ${attempt}s." >&2
    else
      echo "Could not confirm $SERVICE is active during startup at ${attempt}s." >&2
    fi
    break
  fi
  if [[ "$arcade_log_ready" -eq 0 && -s "$LOG" && "$LOG" -nt "$START_MARKER" ]] \
    && tail -n 2500 "$LOG" | grep -Fq "Enabling Arcade"; then
    arcade_log_ready=1
    echo "Observed the fresh Arcade enable log; waiting for lobby provisioning to finish."
  fi
  if [[ -f "$ROOT/world/NEW.dat" || -f "$ROOT/world/TEST_LOBBY_PLANE.dat" ]]; then
    lobby_marker_ready=1
  fi
  if [[ "$arcade_log_ready" -eq 1 && "$lobby_marker_ready" -eq 1 ]]; then
    ready=1
    break
  fi
  sleep 1
done
if [[ "$ready" -ne 1 ]]; then
  if [[ "$arcade_log_ready" -ne 1 ]]; then
    echo "Arcade did not produce a fresh startup log marker within 180 seconds; diagnostics follow before rollback." >&2
  else
    echo "Arcade startup was logged, but neither world/NEW.dat nor world/TEST_LOBBY_PLANE.dat appeared within 180 seconds; diagnostics follow before rollback." >&2
  fi
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
