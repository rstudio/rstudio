/**
 * Package-provided extension indexing (#18060)
 *
 * The addin registry is populated by the package-provided-extension indexer,
 * which enumerates the package library on a background thread and invokes
 * feature workers on the main thread. Verify the pipeline end to end: a
 * package providing an addins.dcf, added to the library paths at runtime, is
 * discovered by the indexer and shows up in the Addins toolbar menu.
 */

import { expect } from '@playwright/test';
import { test } from '@fixtures/rstudio.fixture';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { heredoc } from '@utils/heredoc';

const PKG_NAME = 'ppe.e2e.addins';
const ADDIN_NAME = 'PPE E2E Test Addin';

test.describe('Addins toolbar menu', { tag: ['@parallel_safe'] }, () => {
  let consoleActions: ConsolePaneActions;

  test.beforeAll(async ({ rstudioPage: page }) => {
    consoleActions = new ConsolePaneActions(page);
  });

  test('discovers addins from a library added at runtime - #18060', async ({ rstudioPage: page }) => {
    // create a fake package providing an addin, inside a fresh library
    // folder, then put that library on the library paths; the library path
    // change triggers a package-provided-extension reindex
    await consoleActions.executeInConsole(heredoc`
      lib <- file.path(tempdir(), "ppe-e2e-library")
      addins <- file.path(lib, "${PKG_NAME}", "rstudio")
      dir.create(addins, recursive = TRUE)
      writeLines(
        c(
          "Name: ${ADDIN_NAME}",
          "Title: ${ADDIN_NAME}",
          "Description: An addin used by automated tests.",
          "Interactive: false",
          "Binding: identity"
        ),
        con = file.path(addins, "addins.dcf")
      )
      .libPaths(c(lib, .libPaths()))
    `);

    // the reindex is deferred and the library scan runs on a background
    // thread, so the registry updates a moment later; retry the menu until
    // the addin lands (scoped to menu items, since the console echoes the
    // addin name too)
    const addinsButton = page.locator('#rstudio_addins_toolbar_button');
    const menuItem = page.locator('td.gwt-MenuItem').filter({ hasText: ADDIN_NAME });

    await expect(async () => {
      await page.keyboard.press('Escape');
      await addinsButton.click();
      await expect(menuItem).toBeVisible({ timeout: 2000 });
    }).toPass({ timeout: 60000 });

    await page.keyboard.press('Escape');
    await expect(menuItem).toBeHidden();

    // drop the fake library again so later tests see a pristine session
    await consoleActions.executeInConsole(heredoc`
      .libPaths(setdiff(.libPaths(), normalizePath(lib, winslash = "/")))
      unlink(lib, recursive = TRUE)
    `);
  });
});
