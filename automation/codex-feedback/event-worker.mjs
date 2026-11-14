#!/usr/bin/env node
import { setTimeout as delay } from 'node:timers/promises';
import { resolve } from 'node:path';
import { EventStore } from './event-store.mjs';
import { processEvent } from './process-event.mjs';

const DATABASE = resolve(process.env.FEEDBACK_DATABASE ?? './feedback-events.sqlite');
const POLL_MS = Math.max(250, Number(process.env.FEEDBACK_POLL_MS ?? '1500'));
const store = new EventStore(DATABASE);
let stopping = false;
for (const signal of ['SIGINT', 'SIGTERM']) process.on(signal, () => { stopping = true; });

function safeError(error) {
  return String(error?.message ?? error)
    .replace(/Bearer\s+\S+/gi, 'Bearer [redacted]')
    .replace(/(access|refresh)_token[=: ]+[^\s,]+/gi, '$1=[redacted]')
    .slice(0, 2000);
}

async function main() {
  let idle = false;
  while (!stopping) {
    const event = store.claim();
    if (!event) {
      if (!idle) process.stdout.write('[feedback-worker] waiting for queued events\n');
      idle = true;
      await delay(POLL_MS);
      continue;
    }
    idle = false;
    process.stdout.write(`[feedback-worker] processing issue=${event.issue_number} delivery=${event.delivery_id}\n`);
    try {
      const result = await processEvent(event);
      if (result.stagedForPublisher) store.stage(event.event_key, result.inboxPath);
      else store.complete(event.event_key);
      process.stdout.write(`[feedback-worker] ${result.stagedForPublisher ? 'staged' : 'complete'} issue=${event.issue_number} result=${JSON.stringify(result)}\n`);
    } catch (error) {
      const message = safeError(error);
      store.fail(event.event_key, message);
      process.stderr.write(`[feedback-worker] failed issue=${event.issue_number}: ${message}\n`);
    }
  }
  store.close();
}

main().catch((error) => {
  process.stderr.write(`[feedback-worker] fatal: ${safeError(error)}\n`);
  try { store.close(); } catch { /* ignore shutdown errors */ }
  process.exitCode = 1;
});
