import * as fs from 'fs';
import * as path from 'path';

// AUTH_STORAGE_KEY and POSITAI_STORE_RELATIVE mirror the on-disk layout
// owned by the Posit AI client (NodeAuthService in the assistant repo). If
// either changes there without a matching change here, the test gate will
// silently disagree with the IDE about whether the sandbox is signed in.
// NOTE: assistant main is already migrating the store to
// ~/.posit/ai/auth/data.json (its migrateCredentialStore.ts moves the old
// file at startup, keeping this path as a fallback for now); when the
// shipped assistant picks that migration up, these constants must follow.
export const AUTH_STORAGE_KEY = 'auth:positai:oauth';

// The whole Posit AI state directory, and the token store within it. The store
// path is derived from the directory so the shared .posit/assistant prefix
// lives in exactly one place.
export const POSITAI_DIR_RELATIVE = path.join('.posit', 'assistant');
export const POSITAI_STORE_RELATIVE = path.join(POSITAI_DIR_RELATIVE, 'store', 'data.json');

export interface PositAiOAuthEntry {
  // Literal true: the guard rejects anything else, and the setup project
  // only ever writes true.
  authenticated: true;
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
    && typeof oauthAuth.expiresAt === 'string'
    && oauthAuth.expiresAt.length > 0
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

// The global host-copy kill-switch: when set, copy mode (and Copilot's
// host-copy in sandbox-setup.ts) is suppressed. The sign-in flow is unaffected
// -- it copies nothing from the host. Shared by sandbox-setup.ts (Copilot) and
// auth.setup.ts (Posit AI copy) so the "what counts as on" rule lives in one
// place.
export function noSeedCredentials(): boolean {
  return ['1', 'true'].includes(
    (process.env.PW_SANDBOX_NO_SEED_CREDENTIALS ?? '').toLowerCase(),
  );
}

// ---------------------------------------------------------------------------
// Auth-setup status: a small file the auth.setup project writes into the
// sandbox recording what it did and why. requireAiCredentials() reads it to
// build an accurate skip reason. The setup project runs in a separate
// process, so a file in the sandbox (like the token store itself) is the
// only channel that reaches the test workers.

export type PositAiAuthOutcome =
  | 'success'            // token store written and verified
  | 'off'                // mode=off: deliberately not provisioned
  | 'copy-suppressed'    // mode=copy suppressed by PW_SANDBOX_NO_SEED_CREDENTIALS
  | 'host-not-signed-in' // mode=copy: no valid token store on the host
  | 'credentials-unset'  // mode=login/login-copy: POSIT_EMAIL/POSIT_PASSWORD not set
  | 'login-failed';      // mode=login/login-copy: sign-in attempted but failed (transient)

export interface PositAiAuthStatus {
  mode: string;
  outcome: PositAiAuthOutcome;
  reason: string;
}

const AUTH_STATUS_FILE = 'positai-auth-status.json';

export function writeAuthStatus(sandbox: string, status: PositAiAuthStatus): void {
  fs.writeFileSync(
    path.join(sandbox, AUTH_STATUS_FILE),
    JSON.stringify(status, null, 2),
  );
}

// Returns null when the file is absent, unreadable, or malformed; the gate
// falls back to a generic skip reason in that case.
export function readAuthStatus(sandbox: string): PositAiAuthStatus | null {
  let raw: string;
  try {
    raw = fs.readFileSync(path.join(sandbox, AUTH_STATUS_FILE), 'utf-8');
  } catch {
    return null;
  }
  let parsed: unknown;
  try {
    parsed = JSON.parse(raw);
  } catch {
    return null;
  }
  if (
    !isPlainObject(parsed)
    || typeof parsed.mode !== 'string'
    || typeof parsed.outcome !== 'string'
    || typeof parsed.reason !== 'string'
  ) {
    return null;
  }
  return parsed as unknown as PositAiAuthStatus;
}
