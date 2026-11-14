#!/usr/bin/env node
import { createServer } from 'node:http';
import { chmod, mkdir, open, readFile, rename, writeFile } from 'node:fs/promises';
import { dirname, resolve } from 'node:path';
import { randomUUID, timingSafeEqual } from 'node:crypto';
import { platform } from 'node:os';
import { verifyIdToken, REQUIRED_SCOPE, RESOURCE } from './siwc-auth.mjs';

const UPSTREAM = 'https://api.openai.com';
const HOST = process.env.SIWC_PROXY_HOST ?? '127.0.0.1';
const PORT = Number(process.env.SIWC_PROXY_PORT ?? '8768');
const CREDENTIALS = process.env.SIWC_CREDENTIALS_FILE;
const HOST_ID_FILE = process.env.SIWC_HOST_ID_FILE;
const PROXY_KEY_FILE = process.env.SIWC_PROXY_SHARED_KEY_FILE;
const MAX_BODY = 20 * 1024 * 1024;
const REFRESH_EARLY_MS = 90_000;
let current;
let refreshing;
let proxyKey;

function log(message) {
  process.stdout.write(`[siwc-proxy] ${message}\n`);
}

async function readCredentials() {
  if (!CREDENTIALS) throw new Error('SIWC_CREDENTIALS_FILE is required.');
  if (!HOST_ID_FILE) throw new Error('SIWC_HOST_ID_FILE is required.');
  const hostId = (await readFile(resolve(HOST_ID_FILE), 'utf8')).trim();
  if (!/^urn:uuid:[0-9a-f-]{36}$/i.test(hostId)) throw new Error('Stable Oracle host ID is invalid.');
  const path = resolve(CREDENTIALS);
  if (platform() !== 'win32') {
    const file = await open(path, 'r');
    const stat = await file.stat();
    await file.close();
    if ((stat.mode & 0o077) !== 0) throw new Error('Credential file permissions must be owner-only (0600).');
  }
  const data = JSON.parse(await readFile(path, 'utf8'));
  if (data.version !== 1 || data.appId !== 'testplex-feedback-agent' || !data.clientId ||
      data.hostId !== hostId || !data.accessToken || !data.refreshToken || !data.idToken) {
    throw new Error('Credential file is incomplete or belongs to another application.');
  }
  if (!Array.isArray(data.scopes) || !data.scopes.includes(REQUIRED_SCOPE)) throw new Error('Credential file does not authorize ChatGPT plan usage.');
  if (typeof data.subject !== 'string' || !data.subject) throw new Error('Credential file does not contain a validated account identity.');
  return data;
}

async function readProxyKey() {
  if (!PROXY_KEY_FILE) throw new Error('SIWC_PROXY_SHARED_KEY_FILE is required.');
  const key = (await readFile(resolve(PROXY_KEY_FILE), 'utf8')).trim();
  if (!/^[0-9a-f]{64}$/i.test(key)) throw new Error('Internal Codex proxy key is invalid.');
  return Buffer.from(key, 'hex');
}

async function saveCredentials(data) {
  const path = resolve(CREDENTIALS);
  await mkdir(dirname(path), { recursive: true, mode: 0o700 });
  const temporary = `${path}.${process.pid}.${randomUUID()}.tmp`;
  await writeFile(temporary, `${JSON.stringify(data, null, 2)}\n`, { encoding: 'utf8', mode: 0o600, flag: 'wx' });
  await rename(temporary, path);
  if (platform() !== 'win32') await chmod(path, 0o600);
}

async function discoverTokenEndpoint() {
  const response = await fetch('https://auth.openai.com/.well-known/openid-configuration', { signal: AbortSignal.timeout(10_000) });
  if (!response.ok) throw new Error(`OpenID discovery failed (HTTP ${response.status}).`);
  const metadata = await response.json();
  if (metadata.issuer !== 'https://auth.openai.com' || typeof metadata.token_endpoint !== 'string') throw new Error('Unexpected OpenID token endpoint.');
  return metadata.token_endpoint;
}

async function refresh() {
  if (refreshing) return refreshing;
  refreshing = (async () => {
    if (current.expiresAt > Date.now() + REFRESH_EARLY_MS) return current;
    const endpoint = await discoverTokenEndpoint();
    const response = await fetch(endpoint, {
      method: 'POST',
      headers: { 'content-type': 'application/x-www-form-urlencoded' },
      body: new URLSearchParams({
        grant_type: 'refresh_token',
        client_id: current.clientId,
        refresh_token: current.refreshToken,
        resource: RESOURCE,
      }),
      signal: AbortSignal.timeout(15_000),
    });
    if (!response.ok) throw new Error(`ChatGPT token refresh failed (HTTP ${response.status}); reauthorization is needed.`);
    const tokens = await response.json();
    if (!tokens.access_token) throw new Error('ChatGPT refresh did not return an access token.');
    if (tokens.id_token) {
      const identity = await verifyIdToken(tokens.id_token, { audience: current.clientId });
      if (identity.sub !== current.subject) throw new Error('Refreshed ChatGPT credentials belong to a different account.');
    }
    const scopes = String(tokens.scope ?? current.scopes.join(' ')).split(/\s+/).filter(Boolean);
    if (!scopes.includes(REQUIRED_SCOPE)) throw new Error('ChatGPT plan usage is no longer authorized.');
    const now = Date.now();
    current = {
      ...current,
      accessToken: tokens.access_token,
      refreshToken: tokens.refresh_token ?? current.refreshToken,
      idToken: tokens.id_token ?? current.idToken,
      scopes,
      expiresAt: now + Number(tokens.expires_in ?? 0) * 1000,
      refreshedAt: new Date(now).toISOString(),
    };
    if (!Number.isFinite(current.expiresAt) || current.expiresAt <= now) throw new Error('ChatGPT refresh returned an invalid access-token lifetime.');
    await saveCredentials(current);
    log('ChatGPT credentials refreshed.');
    return current;
  })().finally(() => { refreshing = undefined; });
  return refreshing;
}

async function readBody(request) {
  const parts = [];
  let size = 0;
  for await (const part of request) {
    size += part.length;
    if (size > MAX_BODY) throw new Error('Request body exceeds the configured size limit.');
    parts.push(part);
  }
  return Buffer.concat(parts);
}

function sendError(response, status, message) {
  response.writeHead(status, { 'content-type': 'application/json', 'cache-control': 'no-store' });
  response.end(JSON.stringify({ error: message }));
}

function upstreamRequestHeaders(request, accessToken) {
  const headers = {
    authorization: `Bearer ${accessToken}`,
    'content-type': 'application/json',
    accept: 'text/event-stream',
    'user-agent': request.headers['user-agent'] ?? 'testplex_feedback_agent/0.1.0',
  };
  for (const [name, value] of Object.entries(request.headers)) {
    if (typeof value !== 'string') continue;
    if (name === 'originator' || name === 'openai-beta' || /^x-openai-[a-z0-9-]+$/.test(name)) headers[name] = value;
  }
  return headers;
}

const server = createServer(async (request, response) => {
  if (request.method === 'GET' && request.url === '/healthz') {
    response.writeHead(200, { 'content-type': 'text/plain', 'cache-control': 'no-store' }).end('ok');
    return;
  }
  if (request.method !== 'POST' || request.url !== '/v1/responses') {
    sendError(response, 404, 'Not found');
    return;
  }
  const suppliedKey = /^Bearer ([0-9a-f]{64})$/i.exec(request.headers.authorization ?? '')?.[1];
  if (!suppliedKey || !timingSafeEqual(Buffer.from(suppliedKey, 'hex'), proxyKey)) {
    sendError(response, 401, 'Local proxy authorization failed.');
    return;
  }
  try {
    const body = await readBody(request);
    let payload;
    try { payload = JSON.parse(body.toString('utf8')); }
    catch { sendError(response, 400, 'Expected a JSON Responses API request.'); return; }
    if (payload.store !== false || payload.stream !== true) {
      sendError(response, 400, 'This plan-authenticated worker requires store=false and stream=true.');
      return;
    }
    const credentials = await refresh();
    const upstream = await fetch(`${UPSTREAM}/v1/responses`, {
      method: 'POST',
      headers: upstreamRequestHeaders(request, credentials.accessToken),
      body,
      signal: AbortSignal.timeout(5 * 60_000),
    });
    response.writeHead(upstream.status, {
      'content-type': upstream.headers.get('content-type') ?? 'application/json',
      'cache-control': 'no-store',
      ...(upstream.headers.get('retry-after') ? { 'retry-after': upstream.headers.get('retry-after') } : {}),
      ...(upstream.headers.get('x-request-id') ? { 'x-request-id': upstream.headers.get('x-request-id') } : {}),
      ...(upstream.headers.get('openai-processing-ms') ? { 'openai-processing-ms': upstream.headers.get('openai-processing-ms') } : {}),
      ...(upstream.headers.get('x-ratelimit-limit-requests') ? { 'x-ratelimit-limit-requests': upstream.headers.get('x-ratelimit-limit-requests') } : {}),
      ...(upstream.headers.get('x-ratelimit-remaining-requests') ? { 'x-ratelimit-remaining-requests': upstream.headers.get('x-ratelimit-remaining-requests') } : {}),
      ...(upstream.headers.get('x-ratelimit-reset-requests') ? { 'x-ratelimit-reset-requests': upstream.headers.get('x-ratelimit-reset-requests') } : {}),
    });
    if (!upstream.body) { response.end(); return; }
    for await (const chunk of upstream.body) {
      if (!response.write(chunk)) await new Promise((resolveDrain) => response.once('drain', resolveDrain));
    }
    response.end();
  } catch (error) {
    if (!response.headersSent) sendError(response, 502, error.message || 'Upstream request failed.');
    else response.destroy();
    log(`Request failed: ${error.message || 'unknown error'}`);
  }
});

async function start() {
  proxyKey = await readProxyKey();
  current = await readCredentials();
  await refresh();
  server.listen(PORT, HOST, () => log(`Listening on http://${HOST}:${PORT}; upstream credentials remain in this process only.`));
}

start().catch((error) => {
  log(`Startup failed: ${error.message}`);
  process.exitCode = 1;
});
