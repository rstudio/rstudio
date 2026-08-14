import { test, expect } from '@fixtures/rstudio.fixture';
import { executeInConsole, waitForConsoleBusy } from '@pages/console_pane.page';
import { createSandbox, SANDBOX_DIR_PREFIX } from '@utils/sandbox';
import * as path from 'path';

/**
 * Harness self-test for the busy-console recovery in createSandbox()
 * (ensureConsoleIdle in pages/console_pane.page.ts).
 *
 * A test that leaks a long-running console command -- the real-world case is
 * a shiny app its teardown failed to stop -- used to fail the sandbox
 * beforeAll of every remaining suite in the worker with an opaque
 * waitForConsoleIdle timeout (in Server mode, poisoning the rest of the
 * shard). createSandbox must instead interrupt the leaked command and carry
 * on.
 *
 * Expect ~30s runtime: the recovery deliberately starts only after the
 * normal waitForConsoleIdle grace period, so an in-progress command from a
 * healthy suite is never interrupted.
 */
test.describe('busy console recovery', () => {
  test('createSandbox interrupts a leaked busy console instead of failing', async ({ rstudioPage: page }) => {
    // Leak a busy console the way a crashed test would: submit and walk away.
    // 120s comfortably outlasts the idle grace period plus the interrupt
    // escalation budget, so the sleep expiring on its own can't mask a
    // recovery that didn't actually work.
    await executeInConsole(page, 'Sys.sleep(120)', { wait: false });
    await waitForConsoleBusy(page).catch(() => {});

    const dir = await createSandbox(page);
    expect(path.basename(dir).startsWith(SANDBOX_DIR_PREFIX)).toBe(true);

    // The console must be usable again for whatever runs next.
    await executeInConsole(page, 'invisible(NULL)', { wait: true });
  });
});
