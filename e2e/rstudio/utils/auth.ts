import * as fs from 'fs';
import * as os from 'os';
import * as path from 'path';

export const AUTH_STORAGE_KEY = 'auth:positai:oauth';
export const POSITAI_STORE_RELATIVE = path.join('.positai', 'store', 'data.json');

export function readAuthStore(file: string): Record<string, unknown> | null {
  try {
    return JSON.parse(fs.readFileSync(file, 'utf-8')) as Record<string, unknown>;
  } catch {
    return null;
  }
}

export function isStoreFileAuthenticated(file: string): boolean {
  const data = readAuthStore(file);
  const entry = data?.[AUTH_STORAGE_KEY] as { authenticated?: boolean } | undefined;
  return entry?.authenticated === true;
}

export function isPositAiAuthenticated(): boolean {
  const sandbox = process.env.PW_SANDBOX;
  const home = sandbox ? path.join(sandbox, 'user-home') : os.homedir();
  return isStoreFileAuthenticated(path.join(home, POSITAI_STORE_RELATIVE));
}
