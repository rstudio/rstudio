import { test, expect } from '@fixtures/a11y.fixture';
import { scanA11y, violationSummary } from '@utils/a11y';
import { withFilesTabsetTab } from '@utils/a11y-tabs';
import { executeCommand } from '@utils/commands';
import { TIMEOUTS } from '@utils/constants';

const PACKAGES_PANEL = '#rstudio_workbench_panel_packages';
const PACKAGES_TAB = '#rstudio_workbench_tab_packages';

/** The initial state with the Packages pane shown in place of Files (Ctrl+7). */
test('packages pane has no accessibility violations', { tag: ['@a11y', '@server_only'] },
  async ({ a11yPage: page }, testInfo) => {
    const results = await withFilesTabsetTab(page, PACKAGES_TAB,
      () => executeCommand(page, 'activatePackages'),
      async () => {
        // stats is always attached, so its checked box means the list has loaded.
        await expect(page.locator(`${PACKAGES_PANEL} input[type='checkbox'][aria-label='stats']`).first())
          .toBeChecked({ timeout: TIMEOUTS.consoleReady });
        return scanA11y(page, testInfo);
      });
    expect(violationSummary(results)).toEqual([]);
  });
