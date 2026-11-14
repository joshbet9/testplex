#!/usr/bin/env node
import { createServer } from 'node:http';
import { createHash, createPublicKey, randomBytes, randomUUID, verify } from 'node:crypto';
import { link, open, readFile, unlink, writeFile, mkdir } from 'node:fs/promises';
import { dirname, join, resolve } from 'node:path';
import { pathToFileURL } from 'node:url';
import { homedir, platform } from 'node:os';
import { execFileSync, spawn } from 'node:child_process';

const ISSUER = 'https://auth.openai.com';
const RESOURCE = 'https://api.openai.com/v1';
const CLIENT_NAME = 'testplex_feedback_agent';
const CLIENT_ID = 'testplex-feedback-agent';
const CALLBACK_HOST = '127.0.0.1';
const CALLBACK_PORT = 1455;
const CALLBACK_PATH = '/auth/callback';
const REQUIRED_SCOPE = 'chatgpt.tokens.use.direct';
const DEFAULT_CREDENTIAL_FILE = join(homedir(), '.config', CLIENT_ID, 'credentials.json');

function base64url(data) {
  return Buffer.from(data).toString('base64url');
}

function parseJwt(token) {
  const parts = token.split('.');
  if (parts.length !== 3) throw new Error('OpenAI returned a malformed ID token.');
  return {
    header: JSON.parse(Buffer.from(parts[0], 'base64url').toString('utf8')),
    claims: JSON.parse(Buffer.from(parts[1], 'base64url').toString('utf8')),
    signingInput: Buffer.from(`${parts[0]}.${parts[1]}`),
    signature: Buffer.from(parts[2], 'base64url'),
  };
}

async function openIdConfiguration() {
  const response = await fetch(`${ISSUER}/.well-known/openid-configuration`, { signal: AbortSignal.timeout(10_000) });
  if (!response.ok) throw new Error(`OpenID discovery failed (HTTP ${response.status}).`);
  const config = await response.json();
  if (config.issuer !== ISSUER || typeof config.jwks_uri !== 'string' || typeof config.token_endpoint !== 'string' ||
      new URL(config.jwks_uri).origin !== ISSUER || new URL(config.token_endpoint).origin !== ISSUER) {
    throw new Error('OpenID discovery returned unexpected issuer metadata.');
  }
  return config;
}

async function verifyIdToken(idToken, { audience, nonce }) {
  const { header, claims, signingInput, signature } = parseJwt(idToken);
  if (header.alg !== 'RS256' || typeof header.kid !== 'string') throw new Error('OpenAI ID token has an unsupported signature.');
  const config = await openIdConfiguration();
  const keyResponse = await fetch(config.jwks_uri, { signal: AbortSignal.timeout(10_000) });
  if (!keyResponse.ok) throw new Error(`OpenID signing-key lookup failed (HTTP ${keyResponse.status}).`);
  const jwks = await keyResponse.json();
  const jwk = jwks.keys?.find((candidate) => candidate.kid === header.kid && candidate.kty === 'RSA');
  if (!jwk) throw new Error('OpenAI ID token signing key was not found.');
  const validSignature = verify('RSA-SHA256', signingInput, createPublicKey({ key: jwk, format: 'jwk' }), signature);
  const now = Math.floor(Date.now() / 1000);
  const audienceMatches = Array.isArray(claims.aud) ? claims.aud.includes(audience) : claims.aud === audience;
  if (!validSignature || claims.iss !== ISSUER || !audienceMatches ||
      (nonce !== undefined && claims.nonce !== nonce) || !Number.isFinite(claims.exp) || claims.exp <= now ||
      (Array.isArray(claims.aud) && claims.aud.length > 1 && claims.azp !== audience)) {
    throw new Error('OpenAI ID token failed signature, issuer, audience, nonce, or expiry validation.');
  }
  return claims;
}

function openBrowser(url) {
  let command;
  let args;
  if (platform() === 'win32') [command, args] = ['rundll32.exe', ['url.dll,FileProtocolHandler', url]];
  else if (platform() === 'darwin') [command, args] = ['open', [url]];
  else [command, args] = ['xdg-open', [url]];
  const child = spawn(command, args, { detached: true, stdio: 'ignore', windowsHide: true });
  child.unref();
}

async function writePrivateJson(path, data) {
  const target = resolve(path);
  const directory = dirname(target);
  await mkdir(directory, { recursive: true, mode: 0o700 });
  if (platform() === 'win32') {
    const account = execFileSync('whoami.exe', [], { encoding: 'utf8', windowsHide: true }).trim();
    execFileSync('icacls.exe', [directory, '/inheritance:r', '/grant:r', `${account}:(OI)(CI)F`], { stdio: 'ignore', windowsHide: true });
  }
  const temporary = `${target}.${process.pid}.${randomUUID()}.tmp`;
  await writeFile(temporary, `${JSON.stringify(data, null, 2)}\n`, { encoding: 'utf8', mode: 0o600, flag: 'wx' });
  try {
    await link(temporary, target);
  } catch (error) {
    if (error.code === 'EEXIST') throw new Error(`Credential file already exists at ${target}; choose a new --output path.`);
    throw error;
  } finally {
    await unlink(temporary).catch(() => {});
  }
  if (platform() !== 'win32') {
    const file = await open(target, 'r');
    await file.chmod(0o600);
    await file.close();
  } else {
    const account = execFileSync('whoami.exe', [], { encoding: 'utf8', windowsHide: true }).trim();
    execFileSync('icacls.exe', [target, '/inheritance:r', '/grant:r', `${account}:F`], { stdio: 'ignore', windowsHide: true });
  }
}

function getArg(name) {
  const index = process.argv.indexOf(name);
  return index >= 0 ? process.argv[index + 1] : undefined;
}

async function authorize() {
  const hostId = getArg('--host-id');
  const credentialPath = getArg('--output') ?? DEFAULT_CREDENTIAL_FILE;
  if (!hostId || !/^urn:uuid:[0-9a-f-]{36}$/i.test(hostId)) {
    throw new Error('Pass the Oracle VM host ID with --host-id (format: urn:uuid:<UUID>).');
  }

  const state = base64url(randomBytes(32));
  const nonce = base64url(randomBytes(32));
  const verifier = base64url(randomBytes(48));
  const challenge = base64url(createHash('sha256').update(verifier).digest());
  const redirectUri = `http://${CALLBACK_HOST}:${CALLBACK_PORT}${CALLBACK_PATH}`;
  const server = createServer();
  const callback = new Promise((resolveCallback, rejectCallback) => {
    const timeout = setTimeout(() => rejectCallback(new Error('Sign-in timed out after 10 minutes.')), 10 * 60_000);
    server.on('request', (request, response) => {
      const url = new URL(request.url, redirectUri);
      if (url.pathname !== CALLBACK_PATH) {
        response.writeHead(404).end('Not found');
        return;
      }
      if (url.searchParams.get('state') !== state) {
        response.writeHead(400).end('Sign-in state did not match. Close this tab and retry.');
        clearTimeout(timeout);
        rejectCallback(new Error('OAuth state validation failed.'));
        server.close();
        return;
      }
      const error = url.searchParams.get('error');
      if (error) {
        response.writeHead(400).end('ChatGPT authorization was not completed. You may close this tab.');
        clearTimeout(timeout);
        rejectCallback(new Error(`ChatGPT authorization returned ${error}.`));
        server.close();
        return;
      }
      const code = url.searchParams.get('code');
      const issuedClientId = url.searchParams.get('client_id');
      if (!code || !issuedClientId || issuedClientId === 'dynamic_agent_client') {
        response.writeHead(400).end('The registration did not return an issued client ID. Retry sign-in.');
        clearTimeout(timeout);
        rejectCallback(new Error('Incomplete OAuth callback.'));
        server.close();
        return;
      }
      response.writeHead(200, { 'content-type': 'text/html; charset=utf-8' })
        .end('<p>Testplex Feedback Agent is authorized. You can close this tab.</p>');
      clearTimeout(timeout);
      resolveCallback({ code, issuedClientId, grantedScope: url.searchParams.get('scope') ?? '' });
      server.close();
    });
    server.on('error', (error) => {
      clearTimeout(timeout);
      rejectCallback(error);
    });
  });

  await new Promise((resolveListen, rejectListen) => {
    server.listen(CALLBACK_PORT, CALLBACK_HOST, resolveListen);
    server.once('error', rejectListen);
  });
  const authUrl = new URL(`${ISSUER}/api/accounts/authorize`);
  const params = {
    client_id: 'dynamic_agent_client',
    agent_name_hint: CLIENT_NAME,
    ext_agent_host_id: hostId,
    response_type: 'code',
    redirect_uri: redirectUri,
    scope: 'openid profile email offline_access resource.invoke chatgpt.tokens.use.direct',
    resource: RESOURCE,
    state,
    nonce,
    code_challenge_method: 'S256',
    code_challenge: challenge,
  };
  for (const [key, value] of Object.entries(params)) authUrl.searchParams.set(key, value);
  console.log('Opening ChatGPT sign-in. Approve plan usage only if you want this worker to use your Plus allowance.');
  openBrowser(authUrl.toString());

  let result;
  try {
    result = await callback;
  } finally {
    server.close();
  }
  const tokenEndpoint = (await openIdConfiguration()).token_endpoint;
  const tokenResponse = await fetch(tokenEndpoint, {
    method: 'POST',
    headers: { 'content-type': 'application/x-www-form-urlencoded' },
    redirect: 'error',
    body: new URLSearchParams({
      grant_type: 'authorization_code',
      client_id: result.issuedClientId,
      code: result.code,
      code_verifier: verifier,
      redirect_uri: redirectUri,
      resource: RESOURCE,
    }),
    signal: AbortSignal.timeout(15_000),
  });
  if (!tokenResponse.ok) throw new Error(`ChatGPT token exchange failed (HTTP ${tokenResponse.status}). Start a fresh sign-in.`);
  const tokens = await tokenResponse.json();
  const scopes = String(tokens.scope ?? result.grantedScope).split(/\s+/).filter(Boolean);
  if (!scopes.includes(REQUIRED_SCOPE)) throw new Error('ChatGPT plan-use permission was not granted; no credentials were saved.');
  if (!tokens.access_token || !tokens.refresh_token || !tokens.id_token) throw new Error('ChatGPT did not return all renewable credentials.');
  const lifetimeSeconds = Number(tokens.expires_in);
  if (!Number.isFinite(lifetimeSeconds) || lifetimeSeconds <= 0) throw new Error('ChatGPT returned an invalid access-token lifetime.');
  const identity = await verifyIdToken(tokens.id_token, { audience: result.issuedClientId, nonce });
  const now = Date.now();
  await writePrivateJson(credentialPath, {
    version: 1,
    appId: CLIENT_ID,
    clientId: result.issuedClientId,
    hostId,
    subject: identity.sub,
    email: identity.email,
    idToken: tokens.id_token,
    accessToken: tokens.access_token,
    refreshToken: tokens.refresh_token,
    scopes,
    expiresAt: now + lifetimeSeconds * 1000,
    savedAt: new Date(now).toISOString(),
  });
  console.log(`Authorized ChatGPT plan usage. Protected credentials saved to ${resolve(credentialPath)}.`);
  console.log('Transfer this file to the Oracle worker over SSH/SCP. Never paste it into chat, GitHub, or logs.');
}

async function main() {
  if (process.argv[2] !== 'authorize') {
    console.log(`Usage: node siwc-auth.mjs authorize --host-id urn:uuid:<VM-UUID> [--output <credential-file>]`);
    process.exitCode = 2;
    return;
  }
  await authorize();
}

if (process.argv[1] && import.meta.url === pathToFileURL(resolve(process.argv[1])).href) {
  main().catch((error) => {
    console.error(error.message);
    process.exitCode = 1;
  });
}

export { verifyIdToken, writePrivateJson, REQUIRED_SCOPE, RESOURCE };
