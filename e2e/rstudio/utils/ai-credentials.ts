import type { TestType } from '@playwright/test';
import {
  isCopilotAuthenticated,
  isExternalServerRun,
  isPositAiAuthenticated,
  readAuthStatus,
  strictAiAuth,
  type AIProvider,
} from './auth';
import { isServiceReachable, reprobeService } from './network';

/**
 * AI provider identifier (defined in auth.ts, the credential single source of
 * truth; re-exported here for the test files that gate on it). Both providers
 * are provisioned by the auth.setup project and gate on their on-disk
 * credential store in the sandbox user-home.
 */
export type { AIProvider } from './auth';

// Display label, the hint shown when a provider's sandbox store is missing
// outright, and the external services the provider cannot work without.
// Kept in one table so the gate, the beforeAll guard, the external-server
// check, and the mid-run re-probe cannot disagree about how a provider is
// named or what it needs to reach.
//
// serviceUrls are probed from Node (utils/network.ts), not through the
// product, so an outage or blocked egress on the runner skips the suite while
// a regression in RStudio's own request path still fails it. The Posit AI
// list covers the completion/chat service and the install manifest: every
// session checks cdn.posit.co for the assistant bundle before the agent can
// run (kManifestUrl in SessionChat.cpp), and a timed-out manifest download
// leaves the agent uninstalled or NotSignedIn, which then reads as a
// completion timeout rather than as a network problem (run 34728179667).
const PROVIDERS: Record<
  AIProvider,
  { label: string; missingStoreHint: string; serviceUrls: string[] }
> = {
  positai: {
    label: 'Posit AI',
    missingStoreHint:
      'No Posit AI credentials in the sandbox. Sign in to Posit AI '
      + 'locally so the setup project can copy the token store, or set '
      + 'POSIT_EMAIL/POSIT_PASSWORD for the sign-in flow.',
    serviceUrls: [
      'https://api.posit.ai',
      'https://gateway.posit.ai',
      'https://cdn.posit.co/posit-ai/manifest.json',
    ],
  },
  copilot: {
    label: 'GitHub Copilot',
    missingStoreHint:
      'No GitHub Copilot credentials in the sandbox. Sign in to Copilot '
      + 'locally so the setup project can copy the credential store, or '
      + 'set COPILOT_USER/COPILOT_PASSWORD for the sign-in flow.',
    serviceUrls: [
      'https://api.github.com',
      'https://api.githubcopilot.com',
    ],
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

// Why `provider`'s service cannot be reached from this runner, or null when
// every host answers. `probe` is isServiceReachable (cached for the worker)
// for the gate, or reprobeService for a mid-run re-check.
async function serviceSkipReason(
  provider: AIProvider,
  probe: (url: string) => Promise<boolean>,
): Promise<string | null> {
  const { label, serviceUrls } = PROVIDERS[provider];
  for (const url of serviceUrls) {
    if (!(await probe(url))) {
      return `${label} is unreachable from this runner (${url} did not answer); `
        + 'an outage or blocked egress is not a product bug';
    }
  }
  return null;
}

/**
 * Why `provider` cannot be exercised in this run, or null when it can.
 *
 * The single source of truth behind both the `requireAiCredentials` gate below
 * and the `hasAiCredentials` guard: being runnable means the sandbox store is
 * valid AND, on external-server runs, that the store actually reached the
 * remote home AND the provider's services answer from this runner. Deriving
 * all of it from one function is what stops the guard and the gate from
 * disagreeing about whether a suite can run -- a disagreement that would let
 * expensive `beforeAll` setup proceed for tests `beforeEach` then skips.
 *
 * Under PW_AI_AUTH_STRICT an unreachable service throws instead of skipping:
 * the setup project has no step that could fail the run for it, so the gate
 * is where a run that expects the service to be up turns red rather than
 * green-with-skips.
 */
async function aiCredentialSkipReason(provider: AIProvider): Promise<string | null> {
  const { label, missingStoreHint } = PROVIDERS[provider];
  if (!(await sandboxStoreHasCredentials(provider))) {
    return skipReason(provider, label, missingStoreHint);
  }

  const externalReason = externalServerSkipReason(provider, label);
  if (externalReason !== null) {
    return externalReason;
  }

  const serviceReason = await serviceSkipReason(provider, isServiceReachable);
  if (serviceReason !== null && strictAiAuth()) {
    throw new Error(`${serviceReason} (failing rather than skipping: PW_AI_AUTH_STRICT is set)`);
  }
  return serviceReason;
}

/**
 * Re-probe `provider`'s services after a request through the product came
 * back empty or timed out. The gate's probe only proves the hosts answered at
 * test start; a runner's network can degrade mid-run (#18426 for the citation
 * services). Returns a skip reason when a host has since become unreachable,
 * null when they all still answer -- in which case the failure is real and
 * the caller must let it stand. The refreshed cache entry also lets later
 * tests' gates see the degraded state.
 */
export async function aiServiceOutageReason(provider: AIProvider): Promise<string | null> {
  const reason = await serviceSkipReason(provider, reprobeService);
  if (reason !== null && strictAiAuth()) {
    // Strict runs want the outage to fail; null makes the caller rethrow.
    return null;
  }
  return reason;
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
 * provider, so a missing credential or an unreachable service skips cleanly
 * instead of hitting the feature's own timeout. The gate keys off the on-disk
 * credential store plus a Node-side reachability probe. The skip-vs-fail
 * distinction matters: a missing credential is a setup gap and an outage is
 * an environment problem, not a product bug, and the test output should
 * reflect that. For a service that degrades after the gate passed, see
 * aiServiceOutageReason.
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
