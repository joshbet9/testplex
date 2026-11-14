#!/usr/bin/env node
import { spawn } from 'node:child_process';
import { randomBytes } from 'node:crypto';
import { createInterface } from 'node:readline';

const PROVIDER = 'testplex_chatgpt_plan_proxy';
const MODEL = process.env.CODEX_MODEL ?? 'gpt-6.1-sol';
const PROXY = process.env.SIWC_PROXY_URL ?? 'http://127.0.0.1:8768/v1';
const CODEX = process.env.CODEX_BIN ?? 'codex';
const TIMEOUT_MS = Number(process.env.CODEX_TURN_TIMEOUT_MS ?? 30 * 60_000);

function safeEnvironment() {
  const env = {
    PATH: process.env.PATH ?? '/usr/local/bin:/usr/bin:/bin',
    HOME: process.env.HOME ?? '/var/lib/testplex-codex-runner',
    LANG: 'C.UTF-8',
    LC_ALL: 'C.UTF-8',
    ACCESS_TOKEN: randomBytes(32).toString('hex'),
  };
  if (process.env.TMPDIR) env.TMPDIR = process.env.TMPDIR;
  if (process.env.CODEX_HOME) env.CODEX_HOME = process.env.CODEX_HOME;
  return env;
}

function rpcClient(proc) {
  const readline = createInterface({ input: proc.stdout });
  let nextId = 1;
  const pending = new Map();
  const notifications = new Set();
  let stderr = '';
  proc.stderr.on('data', (chunk) => {
    stderr = (stderr + chunk.toString('utf8')).slice(-8000);
  });
  readline.on('line', (line) => {
    let message;
    try { message = JSON.parse(line); }
    catch {
      for (const entry of pending.values()) entry.reject(new Error('Codex app-server emitted invalid protocol output.'));
      pending.clear();
      return;
    }
    if (message.id !== undefined && pending.has(message.id)) {
      const entry = pending.get(message.id);
      pending.delete(message.id);
      if (message.error) entry.reject(new Error(message.error.message ?? 'Codex app-server request failed.'));
      else entry.resolve(message.result);
    } else {
      for (const listener of notifications) listener(message);
    }
  });
  proc.once('error', (error) => {
    for (const entry of pending.values()) entry.reject(error);
    pending.clear();
  });
  proc.once('exit', (code) => {
    const error = new Error(`Codex app-server exited (${code ?? 'unknown'}). ${stderr}`.trim());
    for (const entry of pending.values()) entry.reject(error);
    pending.clear();
  });
  return {
    notify(method, params = {}) { proc.stdin.write(`${JSON.stringify({ method, params })}\n`); },
    request(method, params = {}) {
      const id = nextId++;
      return new Promise((resolveRequest, rejectRequest) => {
        pending.set(id, { resolve: resolveRequest, reject: rejectRequest });
        proc.stdin.write(`${JSON.stringify({ method, id, params })}\n`);
      });
    },
    subscribe(listener) { notifications.add(listener); return () => notifications.delete(listener); },
    close() { readline.close(); proc.stdin.end(); },
  };
}

async function runTurn({ cwd, prompt }) {
  if (!cwd || !prompt) throw new Error('Both cwd and prompt are required.');
  const appServerArgs = [
    'app-server', '--listen', 'stdio://',
    '-c', `model_provider="${PROVIDER}"`,
    '-c', `model_providers.${PROVIDER}.name="ChatGPT plan via local credential proxy"`,
    '-c', `model_providers.${PROVIDER}.base_url="${PROXY}"`,
    '-c', `model_providers.${PROVIDER}.env_key="ACCESS_TOKEN"`,
    '-c', `model_providers.${PROVIDER}.wire_api="responses"`,
    '-c', `model_providers.${PROVIDER}.requires_openai_auth=false`,
    '-c', `model_providers.${PROVIDER}.supports_websockets=false`,
  ];
  const proc = spawn(CODEX, appServerArgs, {
    cwd,
    env: safeEnvironment(),
    stdio: ['pipe', 'pipe', 'pipe'],
    windowsHide: true,
  });
  const rpc = rpcClient(proc);
  const timeout = setTimeout(() => proc.kill('SIGTERM'), TIMEOUT_MS);
  try {
    await rpc.request('initialize', {
      clientInfo: { name: 'testplex_feedback_agent', title: 'Testplex Feedback Agent', version: '0.1.0' },
    });
    rpc.notify('initialized');
    const started = await rpc.request('thread/start', {
      model: MODEL,
      cwd,
      approvalPolicy: 'never',
      sandbox: 'workspace-write',
    });
    const threadId = started?.thread?.id;
    if (!threadId) throw new Error('Codex app-server did not return a thread ID.');
    let message = '';
    const completed = new Promise((resolveCompleted, rejectCompleted) => {
      rpc.subscribe((event) => {
        if (event.method === 'item/agentMessage/delta') message += event.params?.delta ?? '';
        if (event.method === 'turn/completed') {
          if (event.params?.turn?.status === 'completed') resolveCompleted();
          else rejectCompleted(new Error(event.params?.turn?.error?.message ?? `Codex turn ended with ${event.params?.turn?.status ?? 'unknown status'}.`));
        }
      });
    });
    await rpc.request('turn/start', {
      threadId,
      input: [{ type: 'text', text: prompt }],
    });
    await completed;
    return message.trim();
  } finally {
    clearTimeout(timeout);
    rpc.close();
    if (proc.exitCode === null) proc.kill('SIGTERM');
  }
}

if (import.meta.url === new URL(process.argv[1], 'file:').href) {
  const [cwd, promptPath] = process.argv.slice(2);
  if (!cwd || !promptPath) {
    console.error('Usage: node run-codex-turn.mjs <workspace> <prompt-file>');
    process.exitCode = 2;
  } else {
    const { readFile } = await import('node:fs/promises');
    try {
      const answer = await runTurn({ cwd, prompt: await readFile(promptPath, 'utf8') });
      process.stdout.write(`${answer}\n`);
    } catch (error) {
      console.error(error.message);
      process.exitCode = 1;
    }
  }
}

export { runTurn, safeEnvironment };
