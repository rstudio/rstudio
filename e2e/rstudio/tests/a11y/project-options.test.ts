import { test, expect } from '@fixtures/a11y.fixture';
import type { Page } from '@playwright/test';
import { scanOptionsDialog } from '@utils/a11y-options-dialog';
import { executeCommand, isCommandEnabled } from '@utils/commands';
import { TIMEOUTS } from '@utils/constants';
import { waitForRpcResponse } from '@utils/rpc';
import { closeProjectIfOpen, createAndOpenProject } from '@utils/project';
import { createSandbox } from '@utils/sandbox';

const PROJECT_OPTIONS_DIALOG = '[role="dialog"][aria-label="Project Options"]';
// The first Recent Projects entry, enabled while the list has one.
const FIRST_RECENT_PROJECT = 'projectMru0';
// Creating and opening the project, then closing it: two session switches.
const PROJECT_BUDGET = 120_000;

/**
 * Close the project, so the next session doesn't reopen it, and clear the
 * Recent Projects list it was added to; both outlive the session. A list that
 * held projects before the test is left alone, as clearing it would lose them.
 */
async function restoreNoProject(page: Page, recentWasEmpty: boolean): Promise<void> {
  await closeProjectIfOpen(page);
  if (!recentWasEmpty) {
    console.warn('Recent Projects held entries before the test; the test project is left in it');
    return;
  }
  if (!await isCommandEnabled(page, FIRST_RECENT_PROJECT))
    return;
  const cleared = waitForRpcResponse(page, 'list_clear');
  // Awaited below; this keeps a command failure from also surfacing as an
  // unhandled rejection.
  cleared.catch(() => {});
  await executeCommand(page, 'clearRecentProjects');
  await cleared;
  await expect.poll(() => isCommandEnabled(page, FIRST_RECENT_PROJECT),
    { timeout: TIMEOUTS.action }).toBe(false);
}

/**
 * A new, empty project with Tools > Project Options open, one state per pane
 * (e.g. project-options-build-tools). The dialog is cancelled and the project
 * closed before the assertions.
 */
test('project options dialog has no accessibility violations', { tag: ['@a11y', '@server_only'] },
  async ({ a11yPage: page }, testInfo) => {
    test.setTimeout(testInfo.timeout + PROJECT_BUDGET);
    const recentWasEmpty = !await isCommandEnabled(page, FIRST_RECENT_PROJECT);
    let violations: Map<string, string[]>;
    try {
      await createAndOpenProject(page, await createSandbox(page), 'a11y-project-options');
      violations = await scanOptionsDialog(page, testInfo, PROJECT_OPTIONS_DIALOG,
        () => executeCommand(page, 'projectOptions'));
    } catch (err) {
      // Logged only, so a restore problem can't hide the original error.
      await restoreNoProject(page, recentWasEmpty)
        .catch((restoreErr: Error) => console.warn(`Project not closed: ${restoreErr.message}`));
      throw err;
    }
    await restoreNoProject(page, recentWasEmpty);
    for (const [state, summary] of violations)
      expect.soft(summary, state).toEqual([]);
  });
