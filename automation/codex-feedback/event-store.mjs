import { DatabaseSync } from 'node:sqlite';

export class EventStore {
  constructor(path) {
    this.db = new DatabaseSync(path, { timeout: 5000 });
    this.db.exec(`
      PRAGMA journal_mode = WAL;
      PRAGMA synchronous = FULL;
      PRAGMA busy_timeout = 5000;
      CREATE TABLE IF NOT EXISTS feedback_events (
        event_key TEXT PRIMARY KEY,
        delivery_id TEXT NOT NULL UNIQUE,
        issue_number INTEGER NOT NULL,
        event_name TEXT NOT NULL,
        action TEXT NOT NULL,
        payload_json TEXT NOT NULL,
        received_at TEXT NOT NULL,
        available_at TEXT NOT NULL DEFAULT '',
        state TEXT NOT NULL DEFAULT 'pending',
        attempts INTEGER NOT NULL DEFAULT 0,
        lease_started_at TEXT,
        artifact_path TEXT,
        finished_at TEXT,
        last_error TEXT
      );
      CREATE INDEX IF NOT EXISTS feedback_events_pending
        ON feedback_events(state, received_at);
      CREATE INDEX IF NOT EXISTS feedback_events_issue
        ON feedback_events(issue_number, received_at);
    `);
    const columns = this.db.prepare(`PRAGMA table_info(feedback_events)`).all();
    if (!columns.some((column) => column.name === 'available_at')) {
      this.db.exec(`ALTER TABLE feedback_events ADD COLUMN available_at TEXT NOT NULL DEFAULT ''`);
    }
    if (!columns.some((column) => column.name === 'artifact_path')) {
      this.db.exec(`ALTER TABLE feedback_events ADD COLUMN artifact_path TEXT`);
    }
    this.db.exec(`UPDATE feedback_events SET available_at=received_at WHERE available_at=''`);
    this.insertEvent = this.db.prepare(`
      INSERT OR IGNORE INTO feedback_events
        (event_key, delivery_id, issue_number, event_name, action, payload_json, received_at, available_at)
      VALUES (?, ?, ?, ?, ?, ?, ?, ?)
    `);
    this.claimNext = this.db.prepare(`
      UPDATE feedback_events
      SET state='processing', lease_started_at=?, attempts=attempts+1
      WHERE event_key = (
        SELECT event_key FROM feedback_events
        WHERE state='pending' AND available_at <= ?
          AND NOT EXISTS (
            SELECT 1 FROM feedback_events active
            WHERE active.issue_number=feedback_events.issue_number
              AND active.state IN ('processing','ready_to_publish','publishing')
          )
        ORDER BY received_at, event_key
        LIMIT 1
      ) AND state='pending'
      RETURNING *
    `);
  }

  enqueue({ eventKey, deliveryId, issueNumber, eventName, action, payload }) {
    const result = this.insertEvent.run(
      eventKey,
      deliveryId,
      issueNumber,
      eventName,
      action,
      JSON.stringify(payload),
      new Date().toISOString(),
      new Date().toISOString(),
    );
    return result.changes === 1;
  }

  claim() {
    const now = new Date().toISOString();
    return this.claimNext.get(now, now);
  }

  complete(eventKey) {
    this.db.prepare(`UPDATE feedback_events SET state='complete', finished_at=?, last_error=NULL WHERE event_key=?`)
      .run(new Date().toISOString(), eventKey);
  }

  stage(eventKey, artifactPath) {
    this.db.prepare(`UPDATE feedback_events SET state='ready_to_publish', artifact_path=?, lease_started_at=NULL WHERE event_key=?`)
      .run(artifactPath, eventKey);
  }

  claimReady() {
    const now = new Date().toISOString();
    return this.db.prepare(`UPDATE feedback_events SET state='publishing', lease_started_at=?, attempts=attempts+1
      WHERE event_key=(SELECT event_key FROM feedback_events
        WHERE state='ready_to_publish' AND available_at <= ? ORDER BY received_at, event_key LIMIT 1)
        AND state='ready_to_publish' RETURNING *`).get(now, now);
  }

  fail(eventKey, error, maxAttempts = 5, retryState = 'pending') {
    const row = this.db.prepare(`SELECT attempts FROM feedback_events WHERE event_key=?`).get(eventKey);
    const state = row?.attempts >= maxAttempts ? 'failed' : retryState;
    const delaySeconds = Math.min(30 * 2 ** Math.max(0, (row?.attempts ?? 1) - 1), 1800);
    const availableAt = new Date(Date.now() + delaySeconds * 1000).toISOString();
    this.db.prepare(`UPDATE feedback_events SET state=?, last_error=?, lease_started_at=NULL, available_at=? WHERE event_key=?`)
      .run(state, String(error).slice(0, 2000), availableAt, eventKey);
  }

  recoverStale(leaseMs = 45 * 60_000) {
    const threshold = new Date(Date.now() - leaseMs).toISOString();
    const now = new Date().toISOString();
    return this.db.prepare(`UPDATE feedback_events SET state=CASE state WHEN 'processing' THEN 'pending' ELSE 'ready_to_publish' END,
      lease_started_at=NULL, available_at=? WHERE state IN ('processing','publishing') AND lease_started_at < ?`)
      .run(now, threshold).changes;
  }

  close() { this.db.close(); }
}
