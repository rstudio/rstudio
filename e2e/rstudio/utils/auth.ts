import * as fs from 'fs';
import * as path from 'path';

// AUTH_STORAGE_KEY and POSITAI_STORE_RELATIVE mirror the on-disk layout
// owned by the Posit AI client (NodeAuthService in the assistant repo). If
// either changes there without a matching change here, the test gate will
// silently disagree with the IDE about whether the sandbox is signed in.
// NOTE: assistant main is already migrating the store to
// ~/.posit/ai/auth/data.json (its migrateCredentialStore.ts copies the old
// file to the new path at startup, leaving this path in place as a fallback
// for now); when the shipped assistant picks that migration up, these
// constants must follow.
export const AUTH_STORAGE_KEY = 'auth:positai:oauth';

// The whole Posit AI state directory, and the token store within it. The store
// path is derived from the directory so the shared .posit/assistant prefix
// lives in exactly one place.
const POSITAI_DIR_RELATIVE = path.join('.posit', 'assistant');
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

// Narrowing membership test for the as-const vocabularies below: unlike a
// plain includes() call, this propagates the literal-union type onto the
// checked value, so readAuthStatus needs no casts.
function isOneOf<T extends string>(value: unknown, allowed: readonly T[]): value is T {
  return typeof value === 'string' && (allowed as readonly string[]).includes(value);
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
    // Optional fields: absent is fine, but present-with-wrong-type must not
    // pass a guard whose return type declares them as strings.
    && (tokenData.refreshToken === undefined || typeof tokenData.refreshToken === 'string')
    && (tokenData.scope === undefined || typeof tokenData.scope === 'string')
    && (oauthAuth.scope === undefined || typeof oauthAuth.scope === 'string')
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

// The global host-copy kill-switch: when set, copy mode and Copilot's
// host-copy in sandbox-setup.ts are both suppressed. The sign-in flow itself
// is unaffected -- it copies nothing from the host. Shared by sandbox-setup.ts
// (Copilot) and auth.setup.ts (Posit AI) so the "what counts as on" rule lives
// in one place.
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

// Valid PW_SANDBOX_POSITAI_AUTH values. The array is the single source of
// truth -- the type is derived from it -- so the runtime check, its error
// message, and the type can't drift apart. Unset behaves as "copy". Lives
// here rather than in auth.setup.ts because mode is serialized into the
// status file below, making it part of the cross-process contract.
export const AUTH_MODES = ['off', 'copy', 'login'] as const;
export type PositAiAuthMode = (typeof AUTH_MODES)[number];

// Same single-source-of-truth pattern as AUTH_MODES: readAuthStatus validates
// against the array, so a status file written by a different code version
// can't launder an unknown outcome string into the union.
export const AUTH_OUTCOMES = [
  'success',            // token store written and verified
  'off',                // mode=off: deliberately not provisioned
  'copy-suppressed',    // mode=copy suppressed by PW_SANDBOX_NO_SEED_CREDENTIALS
  'host-not-signed-in', // mode=copy: no valid token store on the host
  'credentials-unset',  // mode=login: POSIT_EMAIL/POSIT_PASSWORD not set
  'login-failed',       // mode=login: sign-in attempted but failed (usually transient)
] as const;
export type PositAiAuthOutcome = (typeof AUTH_OUTCOMES)[number];

export interface PositAiAuthStatus {
  mode: PositAiAuthMode;
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
// falls back to a generic skip reason in that case. Absent (ENOENT) is a
// normal state and stays silent; everything else warns, mirroring
// readAuthStore, so a corrupt or version-skewed status file leaves a trace
// of why the accurate skip reason was unavailable.
export function readAuthStatus(sandbox: string): PositAiAuthStatus | null {
  const file = path.join(sandbox, AUTH_STATUS_FILE);
  let raw: string;
  try {
    raw = fs.readFileSync(file, 'utf-8');
  } catch (err) {
    const code = (err as NodeJS.ErrnoException).code;
    if (code !== 'ENOENT') {
      console.warn(`[auth] WARNING: could not read auth status file: ${(err as Error).message}`);
    }
    return null;
  }
  let parsed: unknown;
  try {
    parsed = JSON.parse(raw);
  } catch (err) {
    console.warn(`[auth] WARNING: malformed JSON in auth status file ${file}: ${(err as Error).message}`);
    return null;
  }
  if (
    !isPlainObject(parsed)
    || !isOneOf(parsed.mode, AUTH_MODES)
    || !isOneOf(parsed.outcome, AUTH_OUTCOMES)
    || typeof parsed.reason !== 'string'
  ) {
    console.warn(`[auth] WARNING: unrecognized auth status shape in ${file}; ignoring it`);
    return null;
  }
  return { mode: parsed.mode, outcome: parsed.outcome, reason: parsed.reason };
}
