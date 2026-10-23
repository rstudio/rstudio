import { test, expect } from '@fixtures/rstudio.fixture';
import { executeInConsole, CONSOLE_OUTPUT } from '@pages/console_pane.page';
import { dismissBlockingModals } from '@pages/modals.page';
import { closeProjectIfOpen, createAndOpenProject, waitForSessionRestart } from '@utils/project';
import { useSuiteSandbox } from '@utils/sandbox';
import { executeCommand, openProject } from '@utils/commands';
import { TIMEOUTS } from '@utils/constants';
import type { Page } from 'playwright';

const NOTICE = 'RStudio has reduced background file operations';

async function suspendAndResume(page: Page): Promise<void> {
  await executeCommand(page, 'suspendSession');
  await waitForSessionRestart(page);
  await dismissBlockingModals(page);
}

// Reload so the console renders purely from the console actions persisted in
// the session state plus whatever the (re)started session emitted.
async function reloadAndCountNotices(page: Page): Promise<number> {
  await page.reload();
  await page.waitForFunction(() => window.rstudio?.ready === true, null, {
    timeout: TIMEOUTS.sessionRestart,
    polling: 50,
  });
  await expect(page.locator(CONSOLE_OUTPUT)).toContainText(NOTICE);
  const output = await page.locator(CONSOLE_OUTPUT).innerText();
  return output.split(NOTICE).length - 1;
}

// Suspend/resume is a Server-only flow.
test.describe('Reduced background file operations notice', { tag: ['@server_only'] }, () => {
  const sandbox = useSuiteSandbox();

  test('is not re-emitted when a suspended session is resumed (#19240)', async ({ rstudioPage: page }) => {
    const projectDir = await createAndOpenProject(page, sandbox.dir, 'ReducedFileOps');

    // The per-project override stands in for a remote filesystem. It is read
    // once at project startup, so reopen the project for it to take effect;
    // that fresh session is where the notice is expected to print.
    await executeInConsole(
      page,
      '.rs.writeProjectPref("reduce_remote_filesystem_operations", TRUE)',
      { wait: true },
    );
    await closeProjectIfOpen(page);
    await openProject(page, `${projectDir}/ReducedFileOps.Rproj`);
    expect(await reloadAndCountNotices(page), 'notice after project open').toBe(1);

    // Before the fix the resumed session printed the notice again, on top of
    // the copy replayed from the persisted console output.
    await suspendAndResume(page);
    expect(await reloadAndCountNotices(page), 'notice after suspend + resume').toBe(1);
  });
});
