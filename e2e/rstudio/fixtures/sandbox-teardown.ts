import * as fs from 'fs';
import * as path from 'path';
import { scrubCredentials } from '../utils/auth';

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
 *
 * Whenever the sandbox is left on disk -- preserved for inspection, or
 * stranded by a failed whole-tree delete -- its credential material (seeded
 * Posit AI tokens and Copilot creds, plus the canonical reference token) is
 * scrubbed first, so a surviving sandbox never carries real tokens. The normal
 * delete path needs no scrub: the rm takes the credentials with the tree.
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
    const scrubFailures = scrubCredentials(sandbox);
    if (scrubFailures.length > 0) {
      console.warn(
        `[sandbox] WARNING: preserving ${sandbox} (${reason}) WITH credentials -- could not remove:\n  ${scrubFailures.join('\n  ')}`,
      );
    } else {
      console.log(`[sandbox] preserving ${sandbox} (${reason}); credentials scrubbed`);
    }
    return;
  }

  try {
    await fs.promises.rm(sandbox, { recursive: true });
  } catch (err) {
    // The whole-tree delete failed, so the sandbox stays on disk. Scrub its
    // credentials out of the leftover so a failed rm can't strand real tokens.
    const scrubFailures = scrubCredentials(sandbox);
    const tail = scrubFailures.length > 0
      ? `; WARNING: also left credentials -- could not remove:\n  ${scrubFailures.join('\n  ')}`
      : '; credentials scrubbed from the leftover';
    console.warn(
      `[sandbox] failed to remove ${sandbox}: ${(err as Error).message} -- left in place${tail}`,
    );
  }
}
