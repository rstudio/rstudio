import * as fs from 'fs';
import * as path from 'path';
import type { FullConfig, Reporter, Suite, TestCase, TestResult } from '@playwright/test/reporter';
import { formatRunVersions, loadRunVersions, runVersionsKey } from '../utils/versions';

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
 *
 * Also lifts the "what did this run against?" line the workers recorded into the
 * report's metadata, which is what puts it at the top of the HTML report. This
 * has to happen here rather than in the fixture: workers run in their own
 * processes with their own copy of the config, so only the reporter can reach the
 * object the report is built from.
 */
export default class SandboxReporter implements Reporter {
  private markerWritten = false;
  private config?: FullConfig;

  onBegin(config: FullConfig, _suite: Suite): void {
    this.config = config;
  }

  onTestEnd(_test: TestCase, result: TestResult): void {
    this.recordRunVersions();
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

  /**
   * Copy the workers' recorded versions into the report metadata, keyed per
   * engine so a merged multi-engine report keeps every engine's line instead of
   * the last one overwriting the rest. Re-read on every test end rather than
   * once, so a worker that starts late still gets its line in.
   */
  private recordRunVersions(): void {
    if (!this.config) return;
    const versions = loadRunVersions();
    if (!versions) return;
    this.config.metadata[runVersionsKey(versions)] = formatRunVersions(versions);
  }
}
