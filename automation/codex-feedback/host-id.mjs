#!/usr/bin/env node
import { randomUUID } from 'node:crypto';
import { chmod, mkdir, readFile, rename, writeFile } from 'node:fs/promises';
import { dirname, resolve } from 'node:path';

const path = resolve(process.env.SIWC_HOST_ID_FILE ?? './host-id');
await mkdir(dirname(path), { recursive: true, mode: 0o700 });
let value;
try {
  value = (await readFile(path, 'utf8')).trim();
  if (!/^urn:uuid:[0-9a-f-]{36}$/i.test(value)) throw new Error('Existing host ID file has an invalid value; refusing to replace it.');
} catch (error) {
  if (error.code !== 'ENOENT') throw error;
  value = `urn:uuid:${randomUUID()}`;
  const temporary = `${path}.${process.pid}.tmp`;
  await writeFile(temporary, `${value}\n`, { encoding: 'utf8', mode: 0o600, flag: 'wx' });
  await rename(temporary, path);
  await chmod(path, 0o600);
}
process.stdout.write(`${value}\n`);
