#!/usr/bin/env node
import { setTimeout as delay } from 'node:timers/promises';
import { rm } from 'node:fs/promises';
import { EventStore } from './event-store.mjs';
import { publish } from './publish-draft-pr.mjs';

const DATABASE = process.env.FEEDBACK_DATABASE ?? './feedback-events.sqlite';
const POLL_MS = Math.max(250, Number(process.env.FEEDBACK_PUBLISH_POLL_MS ?? '1500'));
const store = new EventStore(DATABASE);
let stopping = false;
for (const signal of ['SIGINT', 'SIGTERM']) process.on(signal, () => { stopping = true; });

function safeError(error) {
  return String(error?.message ?? error)
    .replace(/Bearer\s+\S+/gi, 'Bearer [redacted]')
    .replace(/(?:access|refresh)_token[=: ]+[^\s,]+/gi, '[token redacted]')
    .slice(0, 2000);
}

async function main() {
  let idle = false;
  while (!stopping) {
    const event = store.claimReady();
    if (!event) {
      if (!idle) process.stdout.write('[feedback-publisher] waiting for staged patches\n');
      idle = true;
      await delay(POLL_MS);
      continue;
    }
    idle = false;
    try {
      const result = await publish(event.artifact_path);
      store.complete(event.event_key);
      await rm(event.artifact_path, { recursive: true, force: true }).catch((error) => {
        process.stderr.write(`[feedback-publisher] could not remove completed artifact issue=${event.issue_number}: ${error.message}\n`);
      });
      process.stdout.write(`[feedback-publisher] complete issue=${event.issue_number} result=${JSON.stringify(result)}\n`);
    } catch (error) {
      const message = safeError(error);
      store.fail(event.event_key, message, 5, 'ready_to_publish');
      process.stderr.write(`[feedback-publisher] failed issue=${event.issue_number}: ${message}\n`);
    }
  }
  store.close();
}

main().catch((error) => {
  process.stderr.write(`[feedback-publisher] fatal: ${safeError(error)}\n`);
  try { store.close(); } catch { /* ignore shutdown errors */ }
  process.exitCode = 1;
});
