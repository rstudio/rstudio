// Packages pane behaviors ported from
// src/cpp/tests/automation/testthat/test-automation-packages-pane.R.
//
// Regression coverage for https://github.com/rstudio/rstudio/issues/16842
// (packages pane checkbox state diverges from search() after attach/detach).

import { test, expect } from '@fixtures/rstudio.fixture';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { TIMEOUTS } from '@utils/constants';
import type { Page } from 'playwright';

const PACKAGES_TAB = '#rstudio_workbench_tab_packages';
const PACKAGES_PANEL = '#rstudio_workbench_panel_packages';
const FILES_TAB = '#rstudio_workbench_tab_files';

function checkboxLocator(page: Page, packageName: string) {
  // A package can appear in more than one library section (e.g. recommended
  // packages live in the system lib AND a copy may be present in the user
  // lib). `.first()` keeps the locator deterministic regardless -- the global
  // attached-state is what we're verifying, and any one checkbox reflects it.
  return page
    .locator(`${PACKAGES_PANEL} input[type='checkbox'][aria-label='${packageName}']`)
    .first();
}

async function selectPackagesTab(page: Page): Promise<void> {
  await page.locator(PACKAGES_TAB).click();
  await expect(page.locator(PACKAGES_PANEL)).toBeVisible({ timeout: TIMEOUTS.consoleReady });
}

async function clickPackageCheckbox(page: Page, packageName: string): Promise<void> {
  const checkbox = checkboxLocator(page, packageName);
  await checkbox.scrollIntoViewIfNeeded();
  await checkbox.click();
}

test.describe('Packages pane', () => {
  let consoleActions: ConsolePaneActions;

  test.beforeAll(async ({ rstudioPage: page }) => {
    consoleActions = new ConsolePaneActions(page);
    await consoleActions.resetSourcePane();
  });

  // Restore the Files tab as the active tabset member when we're done so a
  // later spec doesn't inherit the Packages tab focus.
  test.afterAll(async ({ rstudioPage: page }) => {
    await page.locator(FILES_TAB).click().catch(() => {});
  });

  test('checkbox reflects loaded state when attaching and detaching DBI', async ({ rstudioPage: page }) => {
    // Use DBI rather than a recommended package: recommended packages can be
    // present in both the system lib AND the user lib (when pre-installed by
    // r-libs-setup.ts), producing duplicate checkboxes that the locator would
    // need to disambiguate. A non-recommended package guarantees a single row.
    const installed = await consoleActions.ensurePackage('DBI');
    test.skip(!installed, 'DBI is not installed');

    // Ensure DBI is not currently attached -- defensive against earlier specs.
    await consoleActions.executeInConsole(
      'if ("package:DBI" %in% search()) detach("package:DBI", character.only = TRUE)',
      { wait: true },
    );

    await selectPackagesTab(page);

    const checkbox = checkboxLocator(page, 'DBI');
    await expect(checkbox).toBeVisible({ timeout: TIMEOUTS.consoleReady });
    await expect(checkbox).not.toBeChecked();

    // Attach via checkbox.
    await clickPackageCheckbox(page, 'DBI');
    await expect(checkbox).toBeChecked({ timeout: TIMEOUTS.consoleReady });

    // Verify package is actually attached via search().
    const attachedMarker = `__ATTACH_${Date.now()}__`;
    await consoleActions.executeInConsole(
      `cat("${attachedMarker}", "package:DBI" %in% search(), "${attachedMarker}")`,
    );
    await expect(consoleActions.consolePane.consoleOutput).toContainText(
      `${attachedMarker} TRUE ${attachedMarker}`,
      { timeout: TIMEOUTS.consoleReady },
    );

    // Detach via checkbox.
    await clickPackageCheckbox(page, 'DBI');
    await expect(checkbox).not.toBeChecked({ timeout: TIMEOUTS.consoleReady });

    const detachedMarker = `__DETACH_${Date.now()}__`;
    await consoleActions.executeInConsole(
      `cat("${detachedMarker}", "package:DBI" %in% search(), "${detachedMarker}")`,
    );
    await expect(consoleActions.consolePane.consoleOutput).toContainText(
      `${detachedMarker} FALSE ${detachedMarker}`,
      { timeout: TIMEOUTS.consoleReady },
    );
  });

  test('shows correct initial checkbox state for an always-attached base package', async ({ rstudioPage: page }) => {
    // `stats` is always on the search path -- if its checkbox isn't checked,
    // the initial-state plumbing is broken.
    await selectPackagesTab(page);

    const checkbox = checkboxLocator(page, 'stats');
    await expect(checkbox).toBeVisible({ timeout: TIMEOUTS.consoleReady });
    await expect(checkbox).toBeChecked();
  });
});
