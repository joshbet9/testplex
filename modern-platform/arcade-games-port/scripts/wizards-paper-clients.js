#!/usr/bin/env node
'use strict';

/**
 * Three disposable offline-mode Minecraft 26.3 clients for the Paper acceptance
 * harness. They join as real server-side Player entities; the test plugin
 * performs the queue/kit/start/leave commands and checks the resulting state.
 */
const minecraft = require('@wp2508/minecraft-protocol');

const host = process.env.WIZARDS_SMOKE_HOST || '127.0.0.1';
const port = Number(process.env.WIZARDS_SMOKE_PORT || 25578);
const usernames = ['jn_playzmc', 'WizardSmoke2', 'WizardSmoke3'];
const clients = [];
const finished = new Set();
let shuttingDown = false;

function log(message) {
  process.stdout.write(new Date().toISOString() + ' ' + message + '\n');
}

function connect(username) {
  return new Promise((resolve, reject) => {
    const client = minecraft.createClient({
      host,
      port,
      username,
      auth: 'offline',
      version: '26.3',
      hideErrors: false,
    });
    clients.push(client);
    const rawWrite = client.write.bind(client);
    client.write = (name, data) => {
      const result = rawWrite(name, data);
      // Pair each position-bearing packet at the socket-write boundary. A free-
      // running timer can race a buffered movement packet and leave it unpaired.
      if (name === 'position' || name === 'position_look' || name === 'look' || name === 'flying') {
        try {
          rawWrite('tick_end', {});
          log('BOT_MOVEMENT_TICK|PASS|' + username + '|after=' + name);
        } catch (error) {
          log('BOT_MOVEMENT_TICK|FAIL|' + username + '|after=' + name + '|' + String(error));
        }
      }
      return result;
    };
    let loggedIn = false;
    let settled = false;
    let lastPlayerLoadedAt = 0;

    const timeout = setTimeout(() => {
      if (!settled) {
        settled = true;
        reject(new Error(username + ' did not complete the Minecraft 26.3 login handshake'));
        client.end('Wizards smoke login timeout');
      }
    }, 90_000);

    function sendPlayerLoaded(reason) {
      // The library emits both a packet event and a convenience respawn event;
      // acknowledge each actual load once rather than duplicating the packet.
      const now = Date.now();
      if (now - lastPlayerLoadedAt < 100) return;
      lastPlayerLoadedAt = now;
      try {
        // Vanilla sends the empty player_loaded packet once its terrain-loading
        // screen closes. Paper keeps ServerPlayer invulnerable until this
        // acknowledgement (or its 60-tick timeout), so protocol clients must
        // send it explicitly after login and every respawn/world reload.
        client.write('player_loaded', {});
        log('BOT_PLAYER_LOADED|PASS|' + username + '|reason=' + reason);
      } catch (error) {
        log('BOT_PLAYER_LOADED|FAIL|' + username + '|reason=' + reason + '|' + String(error));
      }
    }

    const acknowledgedTeleports = new Set();
    let pendingServerPosition = null;
    let lastKnownPosition = null;
    let pendingPositionTimer = null;
    function acknowledgeServerPosition(packet, reason) {
      if (!packet || !Number.isFinite(Number(packet.teleportId))) return;
      const teleportId = Number(packet.teleportId);
      if (acknowledgedTeleports.has(teleportId)) return;

      // Paper/Arcade may immediately issue a second teleport from PlayerJoinEvent
      // to the shared lobby. Coalesce the login spawn and that hand-off so a stale
      // position packet cannot move a freshly teleported player backwards.
      pendingServerPosition = { packet: { ...packet }, reason };
      if (pendingPositionTimer) clearTimeout(pendingPositionTimer);
      pendingPositionTimer = setTimeout(() => {
        pendingPositionTimer = null;
        const pending = pendingServerPosition;
        pendingServerPosition = null;
        if (!pending) return;
        const latest = pending.packet;
        const latestTeleportId = Number(latest.teleportId);
        if (acknowledgedTeleports.has(latestTeleportId)) return;
        try {
          // Protocol 777 changed the serverbound teleport acknowledgement:
          // teleport_confirm now echoes the resolved position and rotation.
          // Its position packet flags are relative-to-current, so resolve them
          // before acknowledging and before sending the mandatory movement echo.
          const relative = latest.flags || {};
          const prior = lastKnownPosition || { x: 0, y: 0, z: 0, yaw: 0, pitch: 0 };
          const resolved = {
            x: Boolean(relative.x) ? prior.x + Number(latest.x) : Number(latest.x),
            y: Boolean(relative.y) ? prior.y + Number(latest.y) : Number(latest.y),
            z: Boolean(relative.z) ? prior.z + Number(latest.z) : Number(latest.z),
            yaw: Boolean(relative.yaw) ? prior.yaw + Number(latest.yaw || 0) : Number(latest.yaw || 0),
            pitch: Boolean(relative.pitch) ? prior.pitch + Number(latest.pitch || 0) : Number(latest.pitch || 0),
          };
          client.write('teleport_confirm', {
            teleportId: latestTeleportId,
            x: resolved.x, y: resolved.y, z: resolved.z,
            yaw: resolved.yaw, pitch: resolved.pitch,
          });
          client.write('position_look', {
            x: resolved.x, y: resolved.y, z: resolved.z,
            yaw: resolved.yaw, pitch: resolved.pitch,
            // Serverbound MovementFlags is a separate 8-bit field, not the
            // clientbound PositionUpdateRelatives bitset (x/y/z/yaw/pitch).
            flags: { onGround: false, hasHorizontalCollision: false },
          });
          lastKnownPosition = resolved;
          acknowledgedTeleports.add(latestTeleportId);
          log('BOT_TELEPORT_ACK|PASS|' + username + '|id=' + latestTeleportId
            + '|reason=' + pending.reason + '|pos=' + resolved.x + ',' + resolved.y + ',' + resolved.z
            + '|rotation=' + resolved.yaw + ',' + resolved.pitch
            + '|relativeFlags=' + JSON.stringify(relative));
          log('BOT_TICK_END|PASS|' + username + '|after-position');
          sendPlayerLoaded('teleport-' + latestTeleportId);
        } catch (error) {
          log('BOT_TELEPORT_ACK|FAIL|' + username + '|id=' + latestTeleportId + '|' + String(error));
        }
      }, 100);
    }

    client.once('login', () => {
      loggedIn = true;
      settled = true;
      clearTimeout(timeout);
      log('BOT_LOGIN|PASS|' + username + '|protocol=' + client.protocolVersion);

      resolve(client);
    });

    // Cross-world arena teleports and death respawns restart Paper's client
    // load state. Mirror the vanilla client acknowledgement after each one.
    client.on('position', (packet) => {
      acknowledgeServerPosition(packet, 'position-event');
    });

    client.on('packet', (data, meta) => {
      if (meta && meta.state === 'play' && meta.name === 'position') {
        acknowledgeServerPosition(data, 'packet-event');
      }
      if (meta && meta.state === 'play' && meta.name === 'respawn') {
        setImmediate(() => sendPlayerLoaded('respawn-packet'));
      }
    });

    // The packet event above is the single source of respawn load acknowledgements.
    // Do not also listen to the library's convenience respawn event: some versions
    // emit both for the same Play packet.

    let respawnPending = false;
    client.on('update_health', (packet) => {
      const health = Number(packet && packet.health);
      if (Number.isFinite(health) && health <= 0 && !respawnPending) {
        respawnPending = true;
        log('BOT_RESPAWN|REQUEST|' + username);
        setTimeout(() => {
          try {
            // Serverbound play/client_command actionId 0 is the vanilla respawn action.
            client.write('client_command', { actionId: 0 });
            log('BOT_RESPAWN|SENT|' + username);
          } catch (error) {
            log('BOT_RESPAWN|ERROR|' + username + '|' + String(error));
          }
        }, 1000);
      } else if (Number.isFinite(health) && health > 0) {
        respawnPending = false;
      }
    });

    client.on('error', (error) => {
      log('BOT_ERROR|' + username + '|' + (error && error.stack ? error.stack : String(error)));
      if (!settled) {
        settled = true;
        clearTimeout(timeout);
        reject(error);
      }
    });

    client.once('end', (reason) => {
      clearTimeout(timeout);
      if (loggedIn) {
        finished.add(username);
        log('BOT_END|' + username + '|' + String(reason || 'server closed connection'));
        if (shuttingDown && finished.size === usernames.length) {
          log('BOT_SHUTDOWN|PASS|all three clients disconnected after Paper shutdown');
          process.exitCode = 0;
        }
      } else if (!settled) {
        settled = true;
        reject(new Error(username + ' disconnected before login: ' + String(reason || 'unknown reason')));
      }
    });

    client.on('kick', (reason) => {
      log('BOT_KICK|' + username + '|' + JSON.stringify(reason));
    });
  });
}

async function main() {
  log('BOT_CONNECT|START|host=' + host + '|port=' + port + '|version=26.3');
  await Promise.all(usernames.map(connect));
  log('BOT_READY|PASS|count=3|users=' + usernames.join(','));
  shuttingDown = true;

  const watchdog = setTimeout(() => {
    log('BOT_SHUTDOWN|FAIL|timed out waiting for Paper to close both clients');
    for (const client of clients) {
      try { client.end('Wizards smoke watchdog'); } catch (_) { /* best-effort cleanup */ }
    }
    process.exitCode = 1;
  }, 20 * 60 * 1000);
  watchdog.unref();

  // Keep the process alive until Bukkit.shutdown() closes both player sockets.
  await new Promise((resolve) => {
    const check = setInterval(() => {
      if (finished.size === usernames.length) {
        clearInterval(check);
        clearTimeout(watchdog);
        resolve();
      }
    }, 250);
  });
}

main().catch((error) => {
  log('BOT_FATAL|FAIL|' + (error && error.stack ? error.stack : String(error)));
  for (const client of clients) {
    try { client.end('Wizards smoke failed'); } catch (_) { /* best-effort cleanup */ }
  }
  process.exitCode = 1;
});