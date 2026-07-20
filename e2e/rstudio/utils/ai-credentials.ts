import type { TestType } from '@playwright/test';

/**
 * AI provider identifier. Matches the suffix on the PW_AI_SEEDED_* env vars
 * set by fixtures/sandbox-setup.ts when real host credentials were copied
 * into the sandbox.
 */
export type AIProvider = 'positai' | 'copilot';

const PROVIDER_LABEL: Record<AIProvider, string> = {
  positai: 'Posit AI',
  copilot: 'GitHub Copilot',
};

const PROVIDER_HOST_PATH: Record<AIProvider, string> = {
  positai: '~/.posit/assistant or legacy ~/.positai',
  copilot: process.platform === 'win32'
    ? '%LOCALAPPDATA%\\github-copilot'
    : '~/.config/github-copilot',
};

const PROVIDER_ENV_KEY: Record<AIProvider, string> = {
  positai: 'PW_AI_SEEDED_POSITAI',
  copilot: 'PW_AI_SEEDED_COPILOT',
};

/**
 * Gate the surrounding describe block on having real credentials seeded for
 * `provider`. When the matching PW_AI_SEEDED_* env var is unset -- the host has
 * no creds at the expected location, or the user opted out via
 * PW_SANDBOX_NO_SEED_CREDENTIALS -- every test in the describe is marked
 * skipped (with reason).
 *
 * The skip is registered at describe-group scope (evaluated at collection
 * time), NOT in a beforeEach. Playwright runs a group's `beforeAll` BEFORE any
 * `beforeEach`, so a beforeEach skip lets a beforeAll that configures the
 * provider (e.g. selecting it in the Options dialog) run -- and throw -- for a
 * provider that isn't available in this build/host, even though every test is
 * destined to skip. A group-level skip marks the whole group skipped before any
 * hook runs, so `beforeAll` never fires.
 *
 * `test` must be the same TestType the surrounding describe uses, so the skip
 * targets that describe. Pass the imported `test` from the file's fixture
 * import.
 *
 * Call at the top of any `test.describe(..., { tag: ['@ai'] }, () => { ... })`
 * that drives an AI provider. The skip-vs-fail distinction matters: a missing
 * credential is a setup gap, not a product bug, and the test output should
 * reflect that.
 */
// Playwright's TestType is parameterized by per-test and per-worker fixture
// argument types. The helper only ever calls test.skip, which doesn't depend
// on the fixture shape, so {} (the constraint TestType imposes) is the minimum
// type that accepts an extended-fixture `test`. Using `any, any` here would
// pollute callers with the wider type; {} keeps the signature honest.
export function requireAiCredentials(
  test: TestType<{}, {}>,
  provider: AIProvider,
): void {
  // Strict "1" check so a stray PW_AI_SEEDED_*=0 / "false" doesn't read as
  // seeded. sandbox-setup.ts clears these at start of run and sets "1" only on
  // a successful copy, so any other value is treated as unseeded.
  test.skip(
    process.env[PROVIDER_ENV_KEY[provider]] !== '1',
    `No ${PROVIDER_LABEL[provider]} credentials seeded; sign in on the host (${PROVIDER_HOST_PATH[provider]}) and re-run.`,
  );
}
