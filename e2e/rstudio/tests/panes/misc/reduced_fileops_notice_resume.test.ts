import { test, expect } from '@fixtures/rstudio.fixture';
import { executeInConsole, CONSOLE_OUTPUT } from '@pages/console_pane.page';
import { dismissBlockingModals } from '@pages/modals.page';
import {
  closeProjectIfOpen,
  createAndOpenProject,
  restartSessionWithSentinel,
  waitForSessionRestart,
} from '@utils/project';
import { useSuiteSandbox } from '@utils/sandbox';
import { executeCommand, openProject } from '@utils/commands';
import { sleep, TIMEOUTS } from '@utils/constants';
import type { Page } from 'playwright';

const NOTICE = 'RStudio has reduced background file operations';

// The per-project override stands in for a remote filesystem. It is read once
// at project startup, so it only takes effect in the next session of the
// project (a reopen or a restart).
async function enableReducedFileOps(page: Page): Promise<void> {
  await executeInConsole(
    page,
    '.rs.writeProjectPref("reduce_remote_filesystem_operations", TRUE)',
    { wait: true },
  );
}

async function suspendAndResume(page: Page): Promise<void> {
  await executeCommand(page, 'suspendSession');
  await waitForSessionRestart(page);
  await dismissBlockingModals(page);
}

async function restartR(page: Page): Promise<void> {
  await restartSessionWithSentinel(page);
  await dismissBlockingModals(page);
}

async function countNotices(page: Page): Promise<number> {
  const output = await page.locator(CONSOLE_OUTPUT).innerText();
  return output.split(NOTICE).length - 1;
}

// Reload so the console renders purely from the console actions persisted in
// the session state plus whatever the (re)started session emitted. The bridge's
// ready flag follows the deferred-init-completed event, which the session
// enqueues after the hook that prints the notice, so the count is read after
// that hook has run. It is then held over a window, since a broken build adds a
// copy some time after the first one rather than removing it.
async function expectNoticeCount(page: Page, expected: number, label: string): Promise<void> {
  await page.reload();
  await page.waitForFunction(() => window.rstudio?.ready === true, null, {
    timeout: TIMEOUTS.sessionRestart,
    polling: 50,
  });

  await expect.poll(() => countNotices(page), { message: label }).toBe(expected);
  const deadline = Date.now() + 2000;
  while (Date.now() < deadline) {
    await sleep(100);
    expect(await countNotices(page), label).toBe(expected);
  }
}

// Suspend/resume is a Server-only flow.
test.describe('Reduced background file operations notice', { tag: ['@server_only'] }, () => {
  const sandbox = useSuiteSandbox();

  test.afterAll(async ({ rstudioPage: page }) => {
    await closeProjectIfOpen(page);
  });

  test('is not re-emitted when a suspended session is resumed (#19240)', async ({ rstudioPage: page }) => {
    const projectDir = await createAndOpenProject(page, sandbox.dir, 'ReducedFileOps');
    await enableReducedFileOps(page);
    await closeProjectIfOpen(page);
    await openProject(page, `${projectDir}/ReducedFileOps.Rproj`);
    await expectNoticeCount(page, 1, 'notice after project open');

    // Before the fix the resumed session printed the notice again, on top of
    // the copy replayed from the persisted console output.
    await suspendAndResume(page);
    await expectNoticeCount(page, 1, 'notice after suspend + resume');
  });

  // Restart R resumes from the restart context, the same path as a resumed
  // suspended session, but here no earlier session printed the notice.
  test('is emitted when a restart first turns the reduction on', async ({ rstudioPage: page }) => {
    await createAndOpenProject(page, sandbox.dir, 'ReducedFileOpsRestart');
    await expectNoticeCount(page, 0, 'notice before the override');

    await enableReducedFileOps(page);
    await restartR(page);
    await expectNoticeCount(page, 1, 'notice after restart');

    await suspendAndResume(page);
    await expectNoticeCount(page, 1, 'notice after restart + suspend + resume');
  });
});
