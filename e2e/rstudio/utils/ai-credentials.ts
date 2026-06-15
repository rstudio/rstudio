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
 * Posit AI account credentials sourced from env vars (populated in CI by the
 * "Load secrets from 1Password" workflow step; set manually for local dev).
 * Returns null when either var is missing -- callers should fall back to the
 * file-based seeded path (~/.posit/assistant) in that case.
 *
 * Marked optional in CI on fork PRs (no 1Password service-account token); the
 * matching @ai tests then skip via requireAiCredentials.
 */
export interface PositAiAccount {
  email: string;
  password: string;
}

export function getPositAiAccount(): PositAiAccount | null {
  const email = process.env.POSIT_AI_EMAIL;
  const password = process.env.POSIT_AI_PASSWORD;
  if (!email || !password) {
    return null;
  }
  return { email, password };
}

/**
 * Gate the surrounding describe block on having real credentials seeded for
 * `provider`. Each test inside the describe is marked skipped (with reason)
 * when the matching PW_AI_SEEDED_* env var is unset, which happens when the
 * host has no creds at the expected location or when the user opted out via
 * PW_SANDBOX_NO_SEED_CREDENTIALS.
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
    // Strict "1" check so a stray PW_AI_SEEDED_*=0 / "false" doesn't read as
    // seeded. sandbox-setup.ts clears these at start of run and sets "1"
    // only on a successful copy, so any other value is treated as unseeded.
    test.skip(
      process.env[PROVIDER_ENV_KEY[provider]] !== '1',
      `No ${PROVIDER_LABEL[provider]} credentials seeded; sign in on the host (${PROVIDER_HOST_PATH[provider]}) and re-run.`,
    );
  });
}
