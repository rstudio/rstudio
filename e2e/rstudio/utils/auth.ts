import * as fs from 'fs';
import * as path from 'path';

// AUTH_STORAGE_KEY and POSITAI_STORE_CANDIDATES mirror the on-disk layout
// owned by the Posit AI client (NodeAuthService in the assistant repo). If
// either changes there without a matching change here, the test gate will
// silently disagree with the IDE about whether the sandbox is signed in.
export const AUTH_STORAGE_KEY = 'auth:positai:oauth';

// The token store locations, newest first. The assistant migrated the store
// from ~/.posit/assistant/store/data.json to ~/.posit/ai/auth/data.json (its
// migrateCredentialStore.ts copies the old file to the new path at startup);
// shipped builds use the new path as of the 2026.08 dailies, verified
// 2026-07-15. The harness reads whichever location validates (new preferred)
// and writes both, so it works against builds on either side of the
// migration.
export const POSITAI_STORE_CANDIDATES = [
  path.join('.posit', 'ai', 'auth', 'data.json'),
  path.join('.posit', 'assistant', 'store', 'data.json'),
] as const;

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

// Absolute paths of all candidate token stores under a given home directory,
// newest first.
export function storeFileCandidates(homeDir: string): string[] {
  return POSITAI_STORE_CANDIDATES.map((rel) => path.join(homeDir, rel));
}

// First candidate store under homeDir holding a valid (unexpired) token, or
// null when none does. Order matters: the new location wins when both exist,
// matching the assistant's own preference after migration.
export function findAuthenticatedStore(homeDir: string): string | null {
  return storeFileCandidates(homeDir).find(isStoreFileAuthenticated) ?? null;
}

export function isPositAiAuthenticated(): boolean {
  const sandbox = process.env.PW_SANDBOX;
  if (!sandbox) {
    throw new Error('isPositAiAuthenticated called outside the sandbox; PW_SANDBOX is unset');
  }
  return findAuthenticatedStore(path.join(sandbox, 'user-home')) !== null;
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

// GitHub Copilot's credential directory inside a given home dir. Platform-
// dependent, and the single definition of where sandbox-setup.ts seeds Copilot
// creds to and where scrubCredentials removes them from, so the two can't
// drift. (The host-side source path is computed separately in sandbox-setup.ts:
// on Windows it honors %LOCALAPPDATA%, which need not equal AppData/Local.)
export function copilotConfigDir(homeDir: string): string {
  return process.platform === 'win32'
    ? path.join(homeDir, 'AppData', 'Local', 'github-copilot')
    : path.join(homeDir, '.config', 'github-copilot');
}

// The canonical Posit AI token, stashed by the auth.setup project in the
// sandbox root (outside any user-home) after a successful provision. The
// per-launch auth-state fixture re-seeds authenticated homes from it, so a
// signed-out test that deletes the token from its own home can't strand a
// later authenticated launch with no source to restore from. It holds a real
// token, so scrubCredentials removes it too.
const CANONICAL_TOKEN_FILE = 'positai-token.json';

export function canonicalTokenPath(sandbox: string): string {
  return path.join(sandbox, CANONICAL_TOKEN_FILE);
}

// Remove every piece of credential material from the sandbox: the Posit AI
// token stores and Copilot creds in each user-home* (the shared template plus
// any per-worker or per-auth-state copies), and the canonical reference token.
// Teardown calls this before a sandbox is left on disk -- preserved for
// inspection, or stranded by a failed whole-tree delete -- so a surviving
// sandbox never carries real tokens. Returns the paths it could not remove,
// each with its error (empty on full success), so the caller can warn loudly
// that a preserved sandbox still holds credentials. Missing paths are not
// failures.
export function scrubCredentials(sandbox: string): string[] {
  const failures: string[] = [];
  const remove = (target: string): void => {
    try {
      fs.rmSync(target, { recursive: true, force: true });
    } catch (err) {
      failures.push(`${target}: ${(err as Error).message}`);
    }
  };

  let entries: string[];
  try {
    entries = fs.readdirSync(sandbox);
  } catch (err) {
    return [`${sandbox}: could not list sandbox to find user homes: ${(err as Error).message}`];
  }
  // Any dir whose name begins "user-home" is a home variant (the template, a
  // per-worker copy, a per-auth-state copy) or an aborted copy's .partial temp
  // -- all can hold seeded credentials.
  const homes = entries
    .filter((name) => name.startsWith('user-home'))
    .map((name) => path.join(sandbox, name));

  for (const home of homes) {
    for (const store of storeFileCandidates(home)) remove(store);
    remove(copilotConfigDir(home));
  }
  remove(canonicalTokenPath(sandbox));

  return failures;
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
