#!/usr/bin/env node
import { createHmac, timingSafeEqual } from 'node:crypto';
import { createServer } from 'node:http';
import { resolve } from 'node:path';
import { EventStore } from './event-store.mjs';

const HOST = process.env.WEBHOOK_HOST ?? '127.0.0.1';
const PORT = Number(process.env.WEBHOOK_PORT ?? '8787');
const SECRET = process.env.GITHUB_WEBHOOK_SECRET;
const REPOSITORY = (process.env.GITHUB_REPOSITORY ?? 'joshbet9/testplex').toLowerCase();
const DATABASE = resolve(process.env.FEEDBACK_DATABASE ?? './feedback-events.sqlite');
const MAX_BODY = 2 * 1024 * 1024;

if (!SECRET || Buffer.byteLength(SECRET) < 32) throw new Error('Set a random GITHUB_WEBHOOK_SECRET with at least 32 bytes.');

const store = new EventStore(DATABASE);
store.recoverStale();

async function readBody(request) {
  const chunks = [];
  let size = 0;
  for await (const chunk of request) {
    size += chunk.length;
    if (size > MAX_BODY) throw new Error('Webhook payload is too large.');
    chunks.push(chunk);
  }
  return Buffer.concat(chunks);
}

function validSignature(rawBody, signature) {
  if (!/^sha256=[0-9a-f]{64}$/i.test(signature ?? '')) return false;
  const expected = Buffer.from(`sha256=${createHmac('sha256', SECRET).update(rawBody).digest('hex')}`);
  const actual = Buffer.from(signature);
  return expected.length === actual.length && timingSafeEqual(expected, actual);
}

function feedbackIssue(issue) {
  return issue && !issue.pull_request && issue.state === 'open' &&
    (issue.title?.startsWith('[Bug]') || issue.title?.startsWith('[Idea]'));
}

function eventKey(eventName, action, payload) {
  if (eventName === 'issue_comment') return `comment:${payload.comment.id}`;
  return `issue:${payload.issue.id}:${action}:${payload.issue.updated_at}`;
}

function json(response, status, value) {
  response.writeHead(status, { 'content-type': 'application/json', 'cache-control': 'no-store' });
  response.end(JSON.stringify(value));
}

const server = createServer(async (request, response) => {
  if (request.method === 'GET' && request.url === '/healthz') {
    json(response, 200, { status: 'ok' });
    return;
  }
  if (request.method !== 'POST' || request.url !== '/github/webhook') {
    json(response, 404, { error: 'not_found' });
    return;
  }
  let raw;
  try { raw = await readBody(request); }
  catch (error) { json(response, 413, { error: error.message }); return; }
  if (!validSignature(raw, request.headers['x-hub-signature-256'])) {
    json(response, 401, { error: 'invalid_signature' });
    return;
  }
  const deliveryId = request.headers['x-github-delivery'];
  const eventName = request.headers['x-github-event'];
  if (!/^[0-9a-f-]{36}$/i.test(deliveryId ?? '')) {
    json(response, 400, { error: 'invalid_delivery_id' });
    return;
  }
  if (!['issues', 'issue_comment'].includes(eventName)) {
    json(response, 202, { accepted: false, reason: 'event_not_used' });
    return;
  }
  let payload;
  try { payload = JSON.parse(raw.toString('utf8')); }
  catch { json(response, 400, { error: 'invalid_json' }); return; }
  if (payload.repository?.full_name?.toLowerCase() !== REPOSITORY) {
    json(response, 403, { error: 'repository_not_allowed' });
    return;
  }
  const action = payload.action;
  const allowedAction = eventName === 'issues' ? ['opened', 'reopened'].includes(action) : action === 'created';
  if (!allowedAction || !feedbackIssue(payload.issue)) {
    json(response, 202, { accepted: false, reason: 'event_not_actionable' });
    return;
  }
  if (eventName === 'issue_comment' && (!payload.comment?.id || payload.comment.user?.type === 'Bot')) {
    json(response, 202, { accepted: false, reason: 'bot_or_invalid_comment' });
    return;
  }
  if (!Number.isInteger(payload.issue.number)) {
    json(response, 400, { error: 'invalid_issue_number' });
    return;
  }
  const inserted = store.enqueue({
    eventKey: eventKey(eventName, action, payload),
    deliveryId,
    issueNumber: payload.issue.number,
    eventName,
    action,
    payload: {
      repository: { full_name: payload.repository.full_name },
      issue: { number: payload.issue.number },
      sender: { login: payload.sender?.login },
      comment: eventName === 'issue_comment' ? { id: payload.comment.id } : undefined,
    },
  });
  process.stdout.write(`[feedback-webhook] ${inserted ? 'queued' : 'duplicate'} ${eventName}/${action} issue=${payload.issue.number} delivery=${deliveryId}\n`);
  json(response, 202, { accepted: true, duplicate: !inserted });
});

server.listen(PORT, HOST, () => process.stdout.write(`[feedback-webhook] listening on ${HOST}:${PORT}\n`));
for (const signal of ['SIGINT', 'SIGTERM']) process.on(signal, () => {
  server.close(() => { store.close(); process.exit(0); });
});
