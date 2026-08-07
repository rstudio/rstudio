import type { TestType } from '@playwright/test';
import {
  isCopilotAuthenticated,
  isExternalServerRun,
  isPositAiAuthenticated,
  readAuthStatus,
  type AIProvider,
} from './auth';

/**
 * AI provider identifier (defined in auth.ts, the credential single source of
 * truth; re-exported here for the test files that gate on it). Both providers
 * are provisioned by the auth.setup project and gate on their on-disk
 * credential store in the sandbox user-home.
 */
export type { AIProvider } from './auth';

// Display label, and the hint shown when a provider's sandbox store is missing
// outright. Kept in one table so the gate, the beforeAll guard, and the
// external-server check cannot disagree about how a provider is named.
const PROVIDERS: Record<AIProvider, { label: string; missingStoreHint: string }> = {
  positai: {
    label: 'Posit AI',
    missingStoreHint:
      'No Posit AI credentials in the sandbox. Sign in to Posit AI '
      + 'locally so the setup project can copy the token store, or set '
      + 'POSIT_EMAIL/POSIT_PASSWORD for the sign-in flow.',
  },
  copilot: {
    label: 'GitHub Copilot',
    missingStoreHint:
      'No GitHub Copilot credentials in the sandbox. Sign in to Copilot '
      + 'locally so the setup project can copy the credential store, or '
      + 'set COPILOT_USER/COPILOT_PASSWORD for the sign-in flow.',
  },
};

// Whether the provider's credential store exists and is valid in the sandbox
// user-home. Necessary for a run to be able to exercise the provider, but not
// sufficient on external-server runs (see externalServerSkipReason), so this
// is deliberately private -- callers want aiCredentialSkipReason below.
//
// Async because the Copilot store check is: it reads the agent's auth.db from
// a child process. The Posit AI check is synchronous underneath.
async function sandboxStoreHasCredentials(provider: AIProvider): Promise<boolean> {
  switch (provider) {
    case 'positai':
      return isPositAiAuthenticated();
    case 'copilot':
      return await isCopilotAuthenticated();
    default:
      // Exhaustiveness: a new AIProvider member must be given its own check
      // here, not silently report "no credentials".
      provider satisfies never;
      return false;
  }
}

// Build the skip reason from the status file the auth.setup project wrote for
// the provider, so a skipped test reports what actually happened (login
// failed, copy suppressed, not signed in locally, ...) rather than guessing at
// missing credentials. Only called when the store gate has already failed, so
// PW_SANDBOX is set (the is*Authenticated call would have thrown otherwise).
function skipReason(provider: AIProvider, label: string, fallback: string): string {
  const status = readAuthStatus(process.env.PW_SANDBOX!, provider);
  if (status === null) {
    return fallback;
  }
  if (status.outcome === 'success') {
    return `${label} auth setup reported success, but the sandbox credential store is `
      + 'now missing or invalid -- the credential may have expired or been removed mid-run.';
  }
  return `No ${label} credentials in the sandbox: ${status.reason}`;
}

// External-server runs only: the sandbox store never reaches the remote
// rsession (#18348), so a valid sandbox store is necessary but not
// sufficient -- the provisioning step (auth.setup.ts, via
// utils/remote-provision.ts) must also have confirmed the credentials are in
// place in the remote home, recorded as remoteProvisioned in the status
// file. Returns a skip reason when it hasn't, null when the gate passes.
function externalServerSkipReason(provider: AIProvider, label: string): string | null {
  if (!isExternalServerRun()) return null;
  const status = readAuthStatus(process.env.PW_SANDBOX!, provider);
  if (status?.remoteProvisioned === true) return null;
  return `${label} credentials are in the sandbox, but were not provisioned to the external `
    + `server at PW_RSTUDIO_SERVER_URL (whose rsession reads the logged-in account's own home `
    + `directory, not the sandbox)${status ? `: ${status.reason}` : ''}`;
}

/**
 * Why `provider` cannot be exercised in this run, or null when it can.
 *
 * The single source of truth behind both the `requireAiCredentials` gate below
 * and the `hasAiCredentials` guard: being runnable means the sandbox store is
 * valid AND, on external-server runs, that the store actually reached the
 * remote home. Deriving both from one function is what stops the guard and the
 * gate from disagreeing about whether a suite can run -- a disagreement that
 * would let expensive `beforeAll` setup proceed for tests `beforeEach` then
 * skips.
 */
async function aiCredentialSkipReason(provider: AIProvider): Promise<string | null> {
  const { label, missingStoreHint } = PROVIDERS[provider];
  if (!(await sandboxStoreHasCredentials(provider))) {
    return skipReason(provider, label, missingStoreHint);
  }
  return externalServerSkipReason(provider, label);
}

/**
 * Whether `provider` can actually be exercised in this run.
 *
 * For any `beforeAll` that has to bail out before the gate can run. Playwright
 * runs `beforeEach` (where `requireAiCredentials` lives) *after* `beforeAll`,
 * so a describe block that does expensive setup in `beforeAll` -- launching
 * RStudio, installing Posit Assistant -- would otherwise run it with no usable
 * credentials and turn what should be a clean skip into a failed test. Such a
 * block guards its `beforeAll` on this function; sharing
 * `aiCredentialSkipReason` with the gate means the guard and the gate cannot
 * drift apart.
 */
export async function hasAiCredentials(provider: AIProvider): Promise<boolean> {
  return (await aiCredentialSkipReason(provider)) === null;
}

/**
 * Gate the surrounding describe block on having real credentials available for
 * `provider`. Each test inside the describe is marked skipped (with reason)
 * when the credential is absent.
 *
 * Both providers are provisioned by the auth.setup project (a live sign-in
 * flow when the provider's credentials are set -- POSIT_EMAIL/POSIT_PASSWORD
 * or COPILOT_USER/COPILOT_PASSWORD -- else a copy of the local
 * credential store), which leaves the store on disk in the sandbox user-home.
 * It runs in a separate process, so the signal is the store itself, not an
 * env flag: isPositAiAuthenticated() reads the token store (and checks
 * expiry), isCopilotAuthenticated() reads the agent's auth.db (and checks for
 * a token row). When the store is absent or invalid, the skip reason is built
 * from the status file the setup project wrote (see AiAuthStatus in auth.ts),
 * so the report shows the actual cause instead of a generic hint.
 *
 * `test` must be the same TestType the surrounding describe uses, since
 * Playwright hooks are scoped per-TestType (an extended fixture's tests
 * don't see hooks registered on the base, and vice versa). Pass the
 * imported `test` from the file's fixture import.
 *
 * Call this inside any describe (or before its tests) that drives an AI
 * provider, so a missing credential skips cleanly instead of hitting the
 * feature's own timeout. The gate keys off the on-disk credential store. The
 * skip-vs-fail distinction matters: a missing credential is a setup gap, not a
 * product bug, and the test output should reflect that.
 */
// Playwright's TestType is parameterized by per-test and per-worker fixture
// argument types. The helper only ever calls beforeEach / skip, which don't
// depend on the fixture shape, so {} (the constraint TestType imposes) is the
// minimum type that accepts an extended-fixture `test`. Using `any, any` here
// would pollute callers with the wider type; {} keeps the signature honest.
export function requireAiCredentials(
  test: TestType<{}, {}>,
  provider: AIProvider,
): void {
  test.beforeEach(async () => {
    const reason = await aiCredentialSkipReason(provider);
    if (reason !== null) {
      test.skip(true, reason);
    }
  });
}
