import type { TestType } from '@playwright/test';
import {
  isCopilotAuthenticated,
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

/**
 * Gate the surrounding describe block on having real credentials available for
 * `provider`. Each test inside the describe is marked skipped (with reason)
 * when the credential is absent.
 *
 * Both providers are provisioned by the auth.setup project (a live sign-in
 * flow when the provider's credentials are set -- POSIT_EMAIL/POSIT_PASSWORD
 * or GH_COPILOT_USER/GH_COPILOT_PASSWORD -- else a copy of the local
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
 * Call at the top of any `test.describe(..., { tag: ['@ai'] }, () => { ... })`
 * that drives an AI provider. The skip-vs-fail distinction matters: a missing
 * credential is a setup gap, not a product bug, and the test output should
 * reflect that.
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
  test.beforeEach(() => {
    switch (provider) {
      case 'positai':
        if (!isPositAiAuthenticated()) {
          test.skip(true, skipReason(
            'positai',
            'Posit AI',
            'No Posit AI credentials in the sandbox. Sign in to Posit AI '
              + 'locally so the setup project can copy the token store, or set '
              + 'POSIT_EMAIL/POSIT_PASSWORD for the sign-in flow.',
          ));
        }
        return;
      case 'copilot':
        if (!isCopilotAuthenticated()) {
          test.skip(true, skipReason(
            'copilot',
            'GitHub Copilot',
            'No GitHub Copilot credentials in the sandbox. Sign in to Copilot '
              + 'locally so the setup project can copy the credential store, or '
              + 'set GH_COPILOT_USER/GH_COPILOT_PASSWORD for the sign-in flow.',
          ));
        }
        return;
      default:
        // Exhaustiveness: a new AIProvider member must be given its own gate
        // here, not silently fall through.
        provider satisfies never;
    }
  });
}
