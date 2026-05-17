import * as fs from 'fs';
import * as path from 'path';
import type { Reporter, TestCase, TestResult } from '@playwright/test/reporter';

/**
 * Writes a `.failed` marker file inside $PW_SANDBOX as soon as any test
 * finishes with a non-passing status.
 *
 * Playwright's globalTeardown signature is (config) only -- it doesn't
 * receive the FullResult. Reporters' onEnd(result) runs *after*
 * globalTeardown, so it can't help either. But onTestEnd fires after each
 * individual test, before globalTeardown runs. We track failures
 * incrementally and write the marker on the first non-pass, so it's in
 * place when globalTeardown decides whether to preserve the sandbox.
 *
 * Treats 'failed', 'timedOut', and 'interrupted' as preserve-worthy;
 * 'skipped' is normal (test.skip()) and 'passed' is the happy path.
 */
export default class SandboxReporter implements Reporter {
  private markerWritten = false;

  onTestEnd(_test: TestCase, result: TestResult): void {
    if (this.markerWritten) return;
    if (result.status === 'passed' || result.status === 'skipped') return;
    const sandbox = process.env.PW_SANDBOX;
    if (!sandbox) return;
    try {
      fs.writeFileSync(path.join(sandbox, '.failed'), '');
      this.markerWritten = true;
    } catch (err) {
      // Best effort; if the marker can't be written, teardown will fall back
      // to deleting the sandbox on failure -- same as before the reporter existed.
      console.warn(
        `[sandbox] failed to write .failed marker: ${(err as Error).message} -- sandbox will be removed on teardown`,
      );
    }
  }
}
