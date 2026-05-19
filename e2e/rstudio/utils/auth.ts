import * as fs from 'fs';
import * as os from 'os';
import * as path from 'path';

const AUTH_STORAGE_KEY = 'auth:positai:oauth';
const POSITAI_STORE_RELATIVE = path.join('.positai', 'store', 'data.json');

export function isPositAiAuthenticated(): boolean {
  const sandbox = process.env.PW_SANDBOX;
  const home = sandbox ? path.join(sandbox, 'user-home') : os.homedir();
  const storeFile = path.join(home, POSITAI_STORE_RELATIVE);
  try {
    const data = JSON.parse(fs.readFileSync(storeFile, 'utf-8')) as Record<string, unknown>;
    const entry = data?.[AUTH_STORAGE_KEY] as { authenticated?: boolean } | undefined;
    return entry?.authenticated === true;
  } catch {
    return false;
  }
}
