import * as fs from 'fs';
import * as path from 'path';

/**
 * Remove the per-invocation sandbox subtree created by sandbox-setup.
 *
 * Skips the rm in two cases:
 *  - Any test failed. Playwright's globalTeardown signature is (config) only
 *    -- it doesn't receive the FullResult, and process.exitCode isn't set
 *    until after globalTeardown runs. So we rely on SandboxReporter (see
 *    fixtures/sandbox-reporter.ts) to write a `.failed` marker file inside
 *    the sandbox before this teardown runs.
 *  - PW_SANDBOX_SKIP_CLEANUP=1/true (explicit user opt-out).
 *
 * Both skip cases log the sandbox path so the user can inspect or delete
 * manually. Only the auto-created subtree is ever removed -- the parent
 * (PW_SANDBOX_ROOT, if user-set) is untouched.
 */
export default async function globalTeardown() {
  const sandbox = process.env.PW_SANDBOX;
  if (!sandbox) return;

  const keep = ['1', 'true'].includes(
    (process.env.PW_SANDBOX_SKIP_CLEANUP ?? '').toLowerCase(),
  );
  const failed = fs.existsSync(path.join(sandbox, '.failed'));

  if (keep || failed) {
    const reason = keep ? 'PW_SANDBOX_SKIP_CLEANUP set' : 'test failures';
    console.log(`[sandbox] preserving ${sandbox} (${reason})`);
    return;
  }

  await fs.promises.rm(sandbox, { recursive: true, force: true });
}
