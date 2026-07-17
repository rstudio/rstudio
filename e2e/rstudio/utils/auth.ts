import * as fs from 'fs';
import * as path from 'path';

// AUTH_STORAGE_KEY and POSITAI_STORE_CANDIDATES mirror the on-disk layout
// owned by the Posit AI client (NodeAuthService in the assistant repo). If
// either changes there without a matching change here, the test gate will
// silently disagree with the IDE about whether the sandbox is signed in.
export const AUTH_STORAGE_KEY = 'auth:positai:oauth';

// The token store location. Shipped builds use this path as of the 2026.08
// dailies, verified 2026-07-15.
export const POSITAI_STORE_CANDIDATES = [
  path.join('.posit', 'ai', 'auth', 'data.json'),
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
  if (!isPlainObject(parsed)) {
    console.warn(`[auth] WARNING: token store ${file} is valid JSON but not an object; ignoring`);
    return null;
  }
  return parsed;
}

export function isStoreFileAuthenticated(file: string): boolean {
  const data = readAuthStore(file);
  const entry = data?.[AUTH_STORAGE_KEY];
  if (!isAuthEntry(entry)) return false;
  const expiresAt = new Date(entry.oauthAuth.tokenData.expiresAt);
  return !Number.isNaN(expiresAt.getTime()) && expiresAt.getTime() > Date.now();
}

// Absolute path of the token store under a given home directory.
export function storeFileCandidates(homeDir: string): string[] {
  return POSITAI_STORE_CANDIDATES.map((rel) => path.join(homeDir, rel));
}

// The token store under homeDir if it holds a valid (unexpired) token, or
// null otherwise.
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

// The global seed kill-switch: when set, copy mode and Copilot's
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

// The AI providers whose credentials the harness manages. The array is the
// single source of truth -- the AIProvider type, the per-test aiAuth option
// shape, and the credential scrub all derive from it, so adding a provider
// here forces the compiler to surface every place that must handle it.
export const AI_PROVIDERS = ['positai', 'copilot'] as const;
export type AIProvider = (typeof AI_PROVIDERS)[number];

// Every path under homeDir where `provider` keeps credentials. The one map
// from provider to on-disk credential locations: the teardown scrub and the
// signed-out home variants both use it, so "what counts as this provider's
// credentials" can't drift between them.
export function credentialPathsFor(provider: AIProvider, homeDir: string): string[] {
  switch (provider) {
    case 'positai':
      return storeFileCandidates(homeDir);
    case 'copilot':
      return [copilotConfigDir(homeDir)];
  }
}

// Remove every piece of credential material from the sandbox: each provider's
// credential paths in each user-home* (the shared template plus any per-worker
// or per-auth-state copies). Teardown calls this before a sandbox is left on
// disk -- preserved for inspection, or stranded by a failed whole-tree delete
// -- so a surviving sandbox never carries real tokens. Returns the paths it
// could not remove, each with its error (empty on full success), so the caller
// can warn loudly that a preserved sandbox still holds credentials. Missing
// paths are not failures.
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
    for (const provider of AI_PROVIDERS) {
      for (const credentialPath of credentialPathsFor(provider, home)) {
        remove(credentialPath);
      }
    }
  }

  return failures;
}

// ---------------------------------------------------------------------------
// Per-test AI auth state: the aiAuth fixture option (rstudio.fixture.ts) lets
// a test file declare which providers its RStudio should be signed OUT of,
// e.g. test.use({ aiAuth: { positai: 'none' } }). The option is worker-scoped,
// so Playwright runs tests with a different aiAuth value in their own worker
// -- which is what makes the state real: RStudio reads credentials at launch,
// and a new worker is a new launch. The fixture serializes the option into an
// internal env var (the launch helpers are plain functions, not fixtures);
// userHomeForAuthState reads it back and swaps the launch HOME for a
// credential-stripped copy. Launches outside the rstudioSession fixture (a
// test calling launchRStudio itself, the warmup launch) never see the env var
// set and get the default, fully-authenticated home.

export const AI_AUTH_STATES = ['authenticated', 'none'] as const;
export type AiAuthState = (typeof AI_AUTH_STATES)[number];

// The aiAuth option value: providers omitted default to 'authenticated'.
export type AiAuthOption = Partial<Record<AIProvider, AiAuthState>>;

const AI_AUTH_NONE_ENV = 'PW_AI_AUTH_NONE';

// Serialize the aiAuth option into the internal env var for the launch
// helpers. Called by the rstudioSession fixture before launching. Validates
// the option shape at runtime because test.use values reach here untyped from
// the config/test files.
export function setAuthStateEnv(option: AiAuthOption): void {
  for (const [provider, state] of Object.entries(option)) {
    if (!(AI_PROVIDERS as readonly string[]).includes(provider)) {
      throw new Error(
        `aiAuth names unknown provider "${provider}"; expected: ${AI_PROVIDERS.join(', ')}`,
      );
    }
    if (state !== undefined && !(AI_AUTH_STATES as readonly string[]).includes(state)) {
      throw new Error(
        `aiAuth.${provider}="${state}" is not a valid state; expected: ${AI_AUTH_STATES.join(', ')}`,
      );
    }
  }
  const stripped = AI_PROVIDERS.filter((provider) => option[provider] === 'none');
  if (stripped.length > 0) {
    process.env[AI_AUTH_NONE_ENV] = stripped.join(',');
  } else {
    delete process.env[AI_AUTH_NONE_ENV];
  }
}

export function strippedProvidersFromEnv(): AIProvider[] {
  const raw = process.env[AI_AUTH_NONE_ENV];
  if (!raw) return [];
  return raw.split(',').map((name) => {
    if (!(AI_PROVIDERS as readonly string[]).includes(name)) {
      throw new Error(
        `${AI_AUTH_NONE_ENV}="${raw}" names unknown provider "${name}" -- this env var is internal (set by the aiAuth fixture option); unset it and use test.use({ aiAuth: ... }) instead`,
      );
    }
    return name as AIProvider;
  });
}

// Resolve the HOME a launch should use, honoring the per-test auth state.
// With no providers stripped (the default), returns baseHome unchanged --
// byte-for-byte the historical behavior. Otherwise returns a lazily created
// credential-stripped copy of baseHome (e.g. user-home-no-positai), so the
// shared home is never mutated: the credential gate and any sibling
// authenticated worker keep seeing the original. The stripped providers'
// credential paths are re-removed on every launch, not just at creation --
// a signed-out test may itself sign in mid-test, and the declared state must
// win again at the next launch. The variant name keeps the user-home prefix
// so scrubCredentials covers it.
export function userHomeForAuthState(baseHome: string): string {
  const stripped = strippedProvidersFromEnv();
  if (stripped.length === 0) return baseHome;

  const sorted = [...stripped].sort();
  const variant = `${baseHome}-no-${sorted.join('-')}`;
  if (!fs.existsSync(variant)) {
    // Copy into a temp sibling and atomically rename, matching
    // workerUserHome() in desktop.fixture.ts, so a crash mid-copy can't leave
    // a partial home that later reads as complete. The temp name carries the
    // pid because in server mode baseHome is shared across workers (see
    // sharedUserHome in server.fixture.ts), so two parallel workers can build
    // the same variant at once; a pid-tagged temp keeps their copies separate,
    // and a rename that loses the race (the variant now exists) is another
    // worker's win, not an error.
    const tmp = `${variant}.partial-${process.pid}`;
    fs.rmSync(tmp, { recursive: true, force: true });
    fs.cpSync(baseHome, tmp, { recursive: true });
    try {
      fs.renameSync(tmp, variant);
      console.log(`[auth-state] created ${path.basename(variant)} (signed out of: ${sorted.join(', ')})`);
    } catch (err) {
      if (!fs.existsSync(variant)) throw err;
      fs.rmSync(tmp, { recursive: true, force: true });
    }
  }
  for (const provider of sorted) {
    for (const credentialPath of credentialPathsFor(provider, variant)) {
      fs.rmSync(credentialPath, { recursive: true, force: true });
    }
  }
  return variant;
}

// ---------------------------------------------------------------------------
// Auth-setup status: a small file the auth.setup project writes into the
// sandbox recording what it did and why. requireAiCredentials() reads it to
// build an accurate skip reason. The setup project runs in a separate
// process, so a file in the sandbox (like the token store itself) is the
// only channel that reaches the test workers.

// Which credential source the auth.setup project used, chosen by
// auto-detection (not user-selected). The array is the single source of truth
// -- the type is derived from it -- so the values written and the values
// validated on read can't drift apart. Lives here rather than in auth.setup.ts
// because source is serialized into the status file below, making it part of
// the cross-process contract.
export const AUTH_SOURCES = [
  'login', // signed in with POSIT_EMAIL/POSIT_PASSWORD
  'copy',  // copied the local token store
  'none',  // no source available; nothing provisioned
] as const;
export type PositAiAuthSource = (typeof AUTH_SOURCES)[number];

// Same single-source-of-truth pattern as AUTH_SOURCES: readAuthStatus
// validates against the array, so a status file written by a different code
// version can't launder an unknown outcome string into the union.
export const AUTH_OUTCOMES = [
  'success',      // token store written and verified
  'unavailable',  // no credential source (not signed in locally, no creds set, or the seed kill-switch blocked the copy); see reason
  'login-failed', // sign-in attempted but failed (usually transient)
] as const;
export type PositAiAuthOutcome = (typeof AUTH_OUTCOMES)[number];

export interface PositAiAuthStatus {
  source: PositAiAuthSource;
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
    || !isOneOf(parsed.source, AUTH_SOURCES)
    || !isOneOf(parsed.outcome, AUTH_OUTCOMES)
    || typeof parsed.reason !== 'string'
  ) {
    console.warn(`[auth] WARNING: unrecognized auth status shape in ${file}; ignoring it`);
    return null;
  }
  return { source: parsed.source, outcome: parsed.outcome, reason: parsed.reason };
}
