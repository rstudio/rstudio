import type { TestType } from '@playwright/test';
import { isPositAiAuthenticated, readAuthStatus } from './auth';

/**
 * AI provider identifier. The two providers are provisioned differently, and
 * so gate differently: Posit AI by the auth.setup project (its tests gate on
 * the on-disk token store), Copilot by a host copy in sandbox-setup.ts (its
 * tests gate on the PW_AI_SEEDED_COPILOT env flag it sets).
 */
export type AIProvider = 'positai' | 'copilot';

// Build the Posit AI skip reason from the status file the auth.setup project
// wrote, so a skipped test reports what actually happened (login failed, copy
// suppressed, mode=off, ...) rather than guessing at missing env vars. Only
// called when the token store gate has already failed, so PW_SANDBOX is set
// (isPositAiAuthenticated would have thrown otherwise).
function positAiSkipReason(): string {
  const status = readAuthStatus(process.env.PW_SANDBOX!);
  if (status === null) {
    return 'No Posit AI credentials in the sandbox. Set POSIT_EMAIL/POSIT_PASSWORD '
      + 'for the sign-in flow (default), or run with PW_SANDBOX_POSITAI_AUTH=copy '
      + 'while signed in to Posit AI on the host.';
  }
  if (status.outcome === 'success') {
    return 'Posit AI auth setup reported success, but the sandbox token store is '
      + 'now missing or invalid -- the token may have expired mid-run.';
  }
  return `No Posit AI credentials in the sandbox: ${status.reason}`;
}

// Copilot is the only provider still gated on a seeded env flag; Posit AI
// switched to the on-disk store check (isPositAiAuthenticated) below.
const COPILOT_SEEDED_ENV = 'PW_AI_SEEDED_COPILOT';
const COPILOT_HOST_PATH = process.platform === 'win32'
  ? '%LOCALAPPDATA%\\github-copilot'
  : '~/.config/github-copilot';

/**
 * Gate the surrounding describe block on having real credentials available for
 * `provider`. Each test inside the describe is marked skipped (with reason)
 * when the credential is absent.
 *
 * The two providers use different signals:
 *   positai  The auth.setup project (sign-in flow by default, or a host copy
 *            under PW_SANDBOX_POSITAI_AUTH=copy) leaves a token store on disk.
 *            It runs in a separate process, so the signal is the store itself,
 *            not an env flag: isPositAiAuthenticated() reads it and also checks
 *            the token has not expired. When the store is absent or invalid,
 *            the skip reason is built from the status file the setup project
 *            wrote (see PositAiAuthStatus in auth.ts), so the report shows the
 *            actual cause -- mode=off, copy suppressed, sign-in flow failed --
 *            instead of a generic "set POSIT_EMAIL/POSIT_PASSWORD".
 *   copilot  sandbox-setup.ts host-copies the creds and sets
 *            PW_AI_SEEDED_COPILOT on success; the gate reads that flag.
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
    if (provider === 'positai') {
      if (!isPositAiAuthenticated()) {
        test.skip(true, positAiSkipReason());
      }
      return;
    }
    // Strict "1" check so a stray PW_AI_SEEDED_COPILOT=0 / "false" doesn't read
    // as seeded. sandbox-setup.ts clears this at start of run and sets "1" only
    // on a successful copy, so any other value is treated as unseeded.
    test.skip(
      process.env[COPILOT_SEEDED_ENV] !== '1',
      `No GitHub Copilot credentials seeded; sign in on the host (${COPILOT_HOST_PATH}) and re-run.`,
    );
  });
}
