#!/usr/bin/env node
'use strict';

/** Four disposable Minecraft 26.3 protocol clients for Hoops Paper acceptance. */
const minecraft = require('@wp2508/minecraft-protocol');

const host = process.env.HOOPS_SMOKE_HOST || '127.0.0.1';
const port = Number(process.env.HOOPS_SMOKE_PORT || 25579);
const usernames = ['jn_playzmc', 'HoopsSmoke2', 'HoopsSmoke3', 'HoopsSmoke4'];
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
    let loggedIn = false;
    let settled = false;

    const timeout = setTimeout(() => {
      if (!settled) {
        settled = true;
        reject(new Error(username + ' did not complete the Minecraft 26.3 login handshake'));
        client.end('Hoops smoke login timeout');
      }
    }, 90_000);

    client.once('login', () => {
      loggedIn = true;
      settled = true;
      clearTimeout(timeout);
      log('BOT_LOGIN|PASS|' + username + '|protocol=' + client.protocolVersion);
      resolve(client);
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
          log('BOT_SHUTDOWN|PASS|all four clients disconnected after Paper shutdown');
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
  log('BOT_READY|PASS|count=4|users=' + usernames.join(','));
  shuttingDown = true;

  const watchdog = setTimeout(() => {
    log('BOT_SHUTDOWN|FAIL|timed out waiting for Paper to close all four clients');
    for (const client of clients) {
      try { client.end('Hoops smoke watchdog'); } catch (_) { /* best-effort cleanup */ }
    }
    process.exitCode = 1;
  }, 20 * 60 * 1000);
  watchdog.unref();

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
    try { client.end('Hoops smoke failed'); } catch (_) { /* best-effort cleanup */ }
  }
  process.exitCode = 1;
});
