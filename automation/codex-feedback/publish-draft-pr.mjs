#!/usr/bin/env node
import { chmod, mkdtemp, readFile, rm, writeFile } from 'node:fs/promises';
import { spawn } from 'node:child_process';
import { tmpdir } from 'node:os';
import { join, resolve } from 'node:path';
import { createHash, randomUUID } from 'node:crypto';

const API = 'https://api.github.com';
const GH = process.env.GH_BIN ?? '/usr/bin/gh';
const GIT = process.env.GIT_BIN ?? '/usr/bin/git';
const REPO = 'joshbet9/testplex';
const BASE = process.env.GITHUB_DEFAULT_BRANCH ?? 'stable';
const STATUS_LABELS = new Set([
  'submitted', 'in-progress', 'waiting-tester', 'resolved', 'deployed',
  'needs-triage', 'needs-tester-check', 'accepted',
]);

function run(command, args, options = {}) {
  return new Promise((resolveRun, rejectRun) => {
    const child = spawn(command, args, { stdio: ['ignore', 'pipe', 'pipe'], ...options });
    const stdout = [];
    const stderr = [];
    child.stdout.on('data', (chunk) => stdout.push(chunk));
    child.stderr.on('data', (chunk) => stderr.push(chunk));
    child.once('error', rejectRun);
    child.once('exit', (code) => {
      if (code === 0) resolveRun(Buffer.concat(stdout).toString('utf8'));
      else rejectRun(new Error(`${command} failed (${code ?? 'unknown'}): ${Buffer.concat(stderr).toString('utf8').slice(-4000)}`));
    });
  });
}

async function github(path, method = 'GET', body) {
  const response = await fetch(`${API}${path}`, {
    method,
    headers: {
      accept: 'application/vnd.github+json',
      authorization: `Bearer ${process.env.GH_TOKEN}`,
      'content-type': 'application/json',
      'x-github-api-version': '2022-11-28',
      'user-agent': 'TestplexFeedbackPublisher/0.1.0',
    },
    body: body === undefined ? undefined : JSON.stringify(body),
    signal: AbortSignal.timeout(20_000),
  });
  if (response.status === 204) return null;
  const text = await response.text();
  if (!response.ok) throw new Error(`GitHub API returned HTTP ${response.status}: ${text.slice(0, 400)}`);
  return text ? JSON.parse(text) : null;
}

async function ensureLabel(name) {
  try {
    await github(`/repos/${REPO}/labels`, 'POST', {
      name,
      color: name === 'in-progress' ? '1d76db' : name === 'waiting-tester' ? 'c5def5' : 'd9eaf7',
      description: 'Testplex feedback lifecycle status',
    });
  } catch (error) {
    if (!String(error.message).includes('HTTP 422')) throw error;
  }
}

async function setLifecycle(issueNumber, status) {
  const labels = await github(`/repos/${REPO}/issues/${issueNumber}/labels?per_page=100`);
  for (const name of labels.map((label) => label.name)) {
    if (STATUS_LABELS.has(name) && name !== status) {
      await github(`/repos/${REPO}/issues/${issueNumber}/labels/${encodeURIComponent(name)}`, 'DELETE');
    }
  }
  if (!labels.some((label) => label.name === status)) {
    await ensureLabel(status);
    await github(`/repos/${REPO}/issues/${issueNumber}/labels`, 'POST', { labels: [status] });
  }
}

async function addCommentOnce(issueNumber, marker, body) {
  for (let page = 1; page <= 10; page++) {
    const comments = await github(`/repos/${REPO}/issues/${issueNumber}/comments?per_page=100&page=${page}`);
    if (comments.some((comment) => comment.body?.includes(marker))) return;
    if (comments.length < 100) break;
  }
  await github(`/repos/${REPO}/issues/${issueNumber}/comments`, 'POST', { body: `${body}\n\n${marker}` });
}

async function assertReviewIsCurrent(job, issue) {
  const comments = [];
  for (let page = 1; page <= 10; page++) {
    const batch = await github(`/repos/${REPO}/issues/${job.issueNumber}/comments?per_page=100&page=${page}`);
    comments.push(...batch.filter((comment) => comment.user?.type !== 'Bot'));
    if (batch.length < 100) break;
  }
  const fingerprint = createHash('sha256').update(JSON.stringify({
    title: issue.title,
    body: issue.body ?? '',
    humanComments: comments.map((comment) => ({ id: comment.id, body: comment.body ?? '', updated_at: comment.updated_at })),
  })).digest('hex');
  if (fingerprint !== job.reviewFingerprint) {
    throw new Error('A human changed the report or added context after review; waiting for a fresh event before publishing.');
  }
}

function safePaths(paths) {
  for (const path of paths) {
    const lower = path.toLowerCase();
    if (path.startsWith('/') || path.split(/[\\/]/).includes('..') ||
        lower === '.github' || lower.startsWith('.github/') ||
        lower === '.codex' || lower.startsWith('.codex/') ||
        lower === 'automation/codex-feedback' || lower.startsWith('automation/codex-feedback/') ||
        /(^|\/)agents\.md$/.test(lower) || /(^|\/)security\.md$/.test(lower) ||
        /(^|\/)codeowners$/.test(lower) || lower === '.gitmodules') {
      throw new Error(`Refusing to publish a protected or unsafe path: ${path}`);
    }
  }
}

async function publish(directory) {
  if (!process.env.GH_TOKEN) throw new Error('GH_TOKEN is required by the isolated publisher.');
  const inbox = resolve(directory);
  const inboxRoot = resolve(process.env.FEEDBACK_PUBLISHER_INBOX ?? '/var/lib/testplex-feedback-publisher/inbox');
  if (!inbox.startsWith(`${inboxRoot}/`)) throw new Error('Publisher input path is outside the configured inbox.');
  const job = JSON.parse(await readFile(join(inbox, 'job.json'), 'utf8'));
  if (job.repository !== REPO || !Number.isInteger(job.issueNumber) || !/^\d+-[0-9a-f]{32}$/i.test(inbox.split(/[\\/]/).at(-1))) {
    throw new Error('Publisher input identity is invalid.');
  }
  const issue = await github(`/repos/${REPO}/issues/${job.issueNumber}`);
  if (issue.state !== 'open' || issue.pull_request) throw new Error('Report is no longer open feedback.');
  await assertReviewIsCurrent(job, issue);
  const summary = (await readFile(join(inbox, 'summary.md'), 'utf8')).slice(0, 30_000).trim();
  const patch = await readFile(join(inbox, 'changes.patch'));
  if (patch.length > 15 * 1024 * 1024) throw new Error('Patch exceeds the publisher size limit.');
  const hasPatch = patch.length > 0;
  const status = hasPatch ? 'in-progress' : job.status;
  let prUrl = '';
  const markerId = createHash('sha256').update(`${job.issueNumber}:${job.issueUpdatedAt}:${job.createdAt}`).digest('hex').slice(0, 20);
  const marker = `<!-- testplex-feedback-event:${markerId} -->`;

  if (hasPatch) {
    const branch = `codex/feedback-${job.issueNumber}-${markerId}`;
    const existing = await github(`/repos/${REPO}/pulls?state=open&head=${encodeURIComponent(`${REPO.split('/')[0]}:${branch}`)}&per_page=100`);
    if (existing.length) {
      prUrl = existing[0].html_url;
    } else {
      const workspace = await mkdtemp(join(tmpdir(), 'testplex-feedback-publish-'));
      try {
      await run(GIT, ['clone', '--depth', '1', '--branch', BASE, `https://github.com/${REPO}.git`, workspace]);
      const patchPath = join(workspace, '.feedback-changes.patch');
      await writeFile(patchPath, patch, { flag: 'wx', mode: 0o600 });
      await run(GIT, ['apply', '--check', patchPath], { cwd: workspace });
      const pathsRaw = await run(GIT, ['apply', '--stat', patchPath], { cwd: workspace });
      void pathsRaw;
      await run(GIT, ['apply', '--index', patchPath], { cwd: workspace });
      const namesRaw = await run(GIT, ['diff', '--cached', '--name-only', '-z'], { cwd: workspace });
      const names = namesRaw.split('\0').filter(Boolean);
      if (!names.length) throw new Error('Patch contained no staged changes.');
      safePaths(names);
      const rawDiff = await run(GIT, ['diff', '--cached', '--raw'], { cwd: workspace });
      if (/\b(?:120000|160000)\s/.test(rawDiff)) throw new Error('Refusing to publish a symlink or submodule change.');
      await run(GIT, ['diff', '--cached', '--check'], { cwd: workspace });
      await run(GIT, ['config', 'user.name', 'github-actions[bot]'], { cwd: workspace });
      await run(GIT, ['config', 'user.email', '41898282+github-actions[bot]@users.noreply.github.com'], { cwd: workspace });
      await run(GIT, ['checkout', '-b', branch], { cwd: workspace });
      await run(GIT, ['commit', '-m', `Prepare feedback fix for #${job.issueNumber}`], { cwd: workspace });
      const askpassPath = join(workspace, '.git-askpass.sh');
      await writeFile(askpassPath, '#!/bin/sh\ncase "$1" in\n  *Username*) printf "%s\\n" "x-access-token" ;;\n  *Password*) printf "%s\\n" "$GH_TOKEN" ;;\n  *) exit 1 ;;\nesac\n', { flag: 'wx', mode: 0o700 });
      await chmod(askpassPath, 0o700);
      await run(GIT, ['push', '--set-upstream', 'origin', branch], {
        cwd: workspace,
        env: { ...process.env, GIT_ASKPASS: askpassPath, GIT_TERMINAL_PROMPT: '0' },
      });
      const bodyPath = join(workspace, '.draft-pr-body.md');
      await writeFile(bodyPath,
        `Automated draft prepared from tester feedback.\n\nRefs #${job.issueNumber}\n\nThis change requires maintainer review and successful CI. It has not been merged or deployed.\n\n## Codex findings\n\n${summary}`,
        { flag: 'wx', mode: 0o600 });
      const title = `[Codex] ${job.issueTitle.replace(/[\r\n]/g, ' ').slice(0, 125)}`;
      prUrl = (await run(GH, ['pr', 'create', '--draft', '--base', BASE, '--head', branch, '--title', title, '--body-file', bodyPath], { cwd: workspace, env: { ...process.env } })).trim();
      } finally {
        await rm(workspace, { recursive: true, force: true });
      }
    }
  }

  await setLifecycle(job.issueNumber, status);
  const next = prUrl
    ? `Draft PR: ${prUrl}. It is not merged or deployed. Please review CI and the diff.`
    : 'No code change was published. Review the findings and answer any specific question in this report.';
  await addCommentOnce(job.issueNumber, marker, `## Codex feedback review\n\n${summary}\n\n${next}`);
  return { issueNumber: job.issueNumber, prUrl, status };
}

if (import.meta.url === new URL(process.argv[1], 'file:').href) {
  try {
    process.stdout.write(`${JSON.stringify(await publish(process.argv[2]))}\n`);
  } catch (error) {
    console.error(error.message);
    process.exitCode = 1;
  }
}

export { publish, safePaths };
