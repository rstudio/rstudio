import * as fs from 'fs';

/**
 * Remove the per-invocation sandbox subtree created by sandbox-setup.
 *
 * Skips the rm in two cases:
 *  - Any test failed (Playwright sets process.exitCode to a non-zero value
 *    by the time globalTeardown runs).
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
  const failed = (process.exitCode ?? 0) !== 0;

  if (keep || failed) {
    const reason = keep ? 'PW_SANDBOX_SKIP_CLEANUP set' : 'test failures';
    console.log(`[sandbox] preserving ${sandbox} (${reason})`);
    return;
  }

  await fs.promises.rm(sandbox, { recursive: true, force: true });
}
