import * as fs from 'fs';
import * as path from 'path';

// AUTH_STORAGE_KEY and POSITAI_STORE_RELATIVE mirror the on-disk layout
// owned by the Posit AI client (NodeAuthService in the assistant repo). If
// either changes there without a matching change here, the test gate will
// silently disagree with the IDE about whether the sandbox is signed in.
export const AUTH_STORAGE_KEY = 'auth:positai:oauth';

// The whole Posit AI state directory, and the token store within it. The store
// path is derived from the directory so the shared .posit/assistant prefix
// lives in exactly one place.
export const POSITAI_DIR_RELATIVE = path.join('.posit', 'assistant');
export const POSITAI_STORE_RELATIVE = path.join(POSITAI_DIR_RELATIVE, 'store', 'data.json');

export interface PositAiOAuthEntry {
  authenticated: boolean;
  oauthAuth: {
    tokenData: {
      accessToken: string;
      refreshToken?: string;
      expiresAt: string;
      tokenType: string;
      scope?: string;
    };
    expiresAt: string;
    scope?: string;
  };
}

function isPlainObject(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value);
}

function isAuthEntry(value: unknown): value is PositAiOAuthEntry {
  if (!isPlainObject(value)) return false;
  if (value.authenticated !== true) return false;
  const oauthAuth = value.oauthAuth;
  if (!isPlainObject(oauthAuth)) return false;
  const tokenData = oauthAuth.tokenData;
  if (!isPlainObject(tokenData)) return false;
  return (
    typeof tokenData.accessToken === 'string'
    && tokenData.accessToken.length > 0
    && typeof tokenData.tokenType === 'string'
    && tokenData.tokenType.length > 0
    && typeof tokenData.expiresAt === 'string'
    && tokenData.expiresAt.length > 0
  );
}

function readAuthStore(file: string): Record<string, unknown> | null {
  let raw: string;
  try {
    raw = fs.readFileSync(file, 'utf-8');
  } catch (err) {
    const code = (err as NodeJS.ErrnoException).code;
    if (code !== 'ENOENT') {
      console.warn(`[auth] WARNING: could not read token store: ${(err as Error).message}`);
    }
    return null;
  }
  let parsed: unknown;
  try {
    parsed = JSON.parse(raw);
  } catch (err) {
    console.warn(`[auth] WARNING: malformed JSON in token store ${file}: ${(err as Error).message}`);
    return null;
  }
  return isPlainObject(parsed) ? parsed : null;
}

export function isStoreFileAuthenticated(file: string): boolean {
  const data = readAuthStore(file);
  const entry = data?.[AUTH_STORAGE_KEY];
  if (!isAuthEntry(entry)) return false;
  const expiresAt = new Date(entry.oauthAuth.tokenData.expiresAt);
  return !Number.isNaN(expiresAt.getTime()) && expiresAt.getTime() > Date.now();
}

// Absolute path to the Posit AI token store under a given home directory.
export function storeFile(homeDir: string): string {
  return path.join(homeDir, POSITAI_STORE_RELATIVE);
}

export function isPositAiAuthenticated(): boolean {
  const sandbox = process.env.PW_SANDBOX;
  if (!sandbox) {
    throw new Error('isPositAiAuthenticated called outside the sandbox; PW_SANDBOX is unset');
  }
  return isStoreFileAuthenticated(storeFile(path.join(sandbox, 'user-home')));
}

// The global host-copy kill-switch: when set, seed mode (and Copilot's
// host-copy in sandbox-setup.ts) is suppressed. The sign-in flow is unaffected
// -- it copies nothing from the host. Shared by sandbox-setup.ts (Copilot) and
// auth.setup.ts (Posit AI seed) so the "what counts as on" rule lives in one
// place.
export function noSeedCredentials(): boolean {
  return ['1', 'true'].includes(
    (process.env.PW_SANDBOX_NO_SEED_CREDENTIALS ?? '').toLowerCase(),
  );
}
