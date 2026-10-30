import { test, expect } from '@fixtures/a11y.fixture';
import { scanA11y, violationSummary } from '@utils/a11y';
import { withEnvironmentTabsetTab } from '@utils/a11y-tabs';
import { executeCommand } from '@utils/commands';

const TUTORIAL_TAB = '#rstudio_workbench_tab_tutorial';
const TUTORIAL_FRAME = '#rstudio_tutorial_frame';
// The home page lists the installed tutorials, or asks for learnr when it is
// missing. With learnr installed it first says indexing is still running, and
// reloads itself once it finishes.
const HOME_SETTLED = '.rstudio-tutorials-entry, .rstudio-tutorials-install-learnr-link';
// Indexing the installed tutorials runs after the session starts.
const INDEX_TIMEOUT = 60_000;

/** The initial state with the Tutorial pane, on its home page, shown in place of Environment. */
test('tutorial pane has no accessibility violations', { tag: ['@a11y', '@server_only'] },
  async ({ a11yPage: page }, testInfo) => {
    const results = await withEnvironmentTabsetTab(page, TUTORIAL_TAB,
      () => executeCommand(page, 'activateTutorial'),
      async () => {
        await expect(page.frameLocator(TUTORIAL_FRAME).locator(HOME_SETTLED).first())
          .toBeVisible({ timeout: INDEX_TIMEOUT });
        return scanA11y(page, testInfo);
      });
    expect(violationSummary(results)).toEqual([]);
  });
