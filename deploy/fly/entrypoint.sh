#!/usr/bin/env bash
set -euo pipefail

DATA_ROOT="${TESTPLEX_DATA_ROOT:-/server}"
PORT="${TESTPLEX_PORT:-25566}"
PLUGIN_DIR="$DATA_ROOT/plugins"
FPP_DATA_DIR="$PLUGIN_DIR/FakePlayerPlugin"
FPP_CONFIG="$FPP_DATA_DIR/config.yml"

# Refuse to start without the persistent volume. This prevents a deployment
# from silently recreating an empty /server and appearing to reset player data.
if ! grep -Fq " $DATA_ROOT " /proc/mounts; then
    echo "FATAL: persistent Testplex data volume is not mounted at $DATA_ROOT; refusing to start." >&2
    exit 1
fi

install -d -o minecraft -g minecraft "$DATA_ROOT" "$PLUGIN_DIR" "$FPP_DATA_DIR"

# The last volume snapshot predates the current Paper plugin-directory layout.
# Migrate the saved plugin data trees into the actual plugins/ data folders
# exactly once, before any plugin can load defaults or write an empty config.
STATE_MIGRATION_MARKER="$DATA_ROOT/.testplex-plugin-state-migrated-v1"
if [[ ! -f "$STATE_MIGRATION_MARKER" ]]; then
    RECOVERY_DIR="$DATA_ROOT/recovery/pre-state-migration-$(date -u +%Y%m%dT%H%M%SZ)"
    install -d -o minecraft -g minecraft "$RECOVERY_DIR"
    for plugin_data in ArcadeMechanicsModern TestplexModernArcade FakePlayerPlugin; do
        legacy_dir="$DATA_ROOT/$plugin_data"
        plugin_data_dir="$PLUGIN_DIR/$plugin_data"
        if [[ -d "$legacy_dir" ]]; then
            if [[ -d "$plugin_data_dir" ]]; then
                cp -a "$plugin_data_dir" "$RECOVERY_DIR/"
            fi
            install -d -o minecraft -g minecraft "$plugin_data_dir"
            cp -a "$legacy_dir/." "$plugin_data_dir/"
            chown -R minecraft:minecraft "$plugin_data_dir"
            echo "Migrated saved plugin data for $plugin_data into $plugin_data_dir."
        fi
    done
    touch "$STATE_MIGRATION_MARKER"
    chown minecraft:minecraft "$STATE_MIGRATION_MARKER"
fi

# Plugin binaries are immutable image inputs; player/game data remains on the
# attached persistent volume and must never be recreated from image defaults.
for plugin in TestplexModernArcade.jar ArcadeMechanicsModern.jar; do
    install -o minecraft -g minecraft -m 0644 "/opt/testplex/plugins/$plugin" "$PLUGIN_DIR/$plugin"
done
install -o minecraft -g minecraft -m 0644 /opt/testplex/fake-player-plugin.jar "$PLUGIN_DIR/FakePlayerPlugin.jar"
install -o minecraft -g minecraft -m 0644 /opt/testplex/packetevents.jar "$PLUGIN_DIR/PacketEvents.jar"

# FakePlayerPlugin restores saved fake players during its own startup. Seed its
# default config and disable persistence before Paper enables any plugins.
if [[ ! -f "$FPP_CONFIG" ]]; then
    unzip -p /opt/testplex/fake-player-plugin.jar config.yml > "$FPP_CONFIG"
fi
tmp_config="$FPP_CONFIG.testplex-tmp"
awk '
    /^persistence:[[:space:]]*$/ { in_persistence=1; print; next }
    in_persistence && /^[^[:space:]#]/ { in_persistence=0 }
    in_persistence && /^  enabled:[[:space:]]*/ { print "  enabled: false"; updated=1; next }
    { print }
    END { if (!updated) exit 41 }
' "$FPP_CONFIG" > "$tmp_config" || {
    rm -f "$tmp_config"
    echo "Could not safely disable FakePlayerPlugin restart persistence." >&2
    exit 1
}
install -o minecraft -g minecraft -m 0640 "$tmp_config" "$FPP_CONFIG"
rm -f "$tmp_config"
grep -A 2 '^persistence:' "$FPP_CONFIG" | grep -Fxq '  enabled: false' || {
    echo "FakePlayerPlugin persistence must be disabled before Paper starts." >&2
    exit 1
}

if [[ ! -f "$DATA_ROOT/eula.txt" ]]; then
    printf 'eula=true\n' > "$DATA_ROOT/eula.txt"
fi
if [[ ! -f "$DATA_ROOT/server.properties" ]]; then
    cat > "$DATA_ROOT/server.properties" <<PROPERTIES
motd=TESTPLEX MODERN 26.3 TEST SERVER
server-port=$PORT
server-ip=
online-mode=true
white-list=false
max-players=16
view-distance=5
simulation-distance=4
level-name=world
PROPERTIES
fi

set_property() {
    local key="$1" value="$2"
    if grep -q "^${key}=" "$DATA_ROOT/server.properties"; then
        sed -i "s|^${key}=.*|${key}=${value}|" "$DATA_ROOT/server.properties"
    else
        printf '%s=%s\n' "$key" "$value" >> "$DATA_ROOT/server.properties"
    fi
}
set_property server-port "$PORT"
set_property server-ip ""
set_property online-mode true
set_property white-list false
set_property max-players 16
set_property view-distance 5
set_property simulation-distance 4
set_property level-name world
chown minecraft:minecraft "$DATA_ROOT/eula.txt" "$DATA_ROOT/server.properties"

# Wizards is part of the shared rotation, and runtime readiness deliberately
# rejects startup unless all 20 original standard/Nano source maps are present.
install -d -o minecraft -g minecraft "$DATA_ROOT/update/maps"
tar -xzf /opt/testplex/WizardsSourceMaps.tar.gz -C "$DATA_ROOT/update/maps"
chown -R minecraft:minecraft "$DATA_ROOT/update/maps/Wizards" "$DATA_ROOT/update/maps/Nano Wizards"

cd "$DATA_ROOT"
exec gosu minecraft java -Xms384M -Xmx3G -jar /opt/testplex/paper.jar --nogui
