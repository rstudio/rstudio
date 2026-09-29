// Commands for the active document survive a refresh in an inactive source
// column (#18955). A column's own refresh (a dirty-state change, a tab close)
// used to write the shared commands on its own, hiding ones only the active
// column shows, such as Publish (File > Publish...) for an R Markdown document.

import type { Page } from 'playwright';
import { test, expect } from '@fixtures/rstudio.fixture';
import { executeInConsole } from '@pages/console_pane.page';
import { documentOpen, executeCommand, resetSourcePaneState } from '@utils/commands';
import { TIMEOUTS } from '@utils/constants';
import { seedSandboxFile } from '@utils/files';
import { rStringLiteral } from '@utils/r';
import { useSuiteSandbox } from '@utils/sandbox';

const MAIN_COLUMN = '#rstudio_Source_pane';
const EXTRA_COLUMNS = '[id^="rstudio_Source"][id$="_pane"]:not(#rstudio_Source_pane)';
const SELECTED_DOC_TAB = "[class*='rstudio_source_panel'] [class*='PanelTab-selected']";

async function isPublishVisible(page: Page): Promise<boolean> {
  return page.evaluate(() => window.rstudio!.commands.rsconnectDeploy.isVisible());
}

// Open an extra column holding an Untitled document and, selected in front of
// it, an R script with unsaved changes; then open an R Markdown document in the
// main column, leaving the main column active. Returns the Untitled's id.
async function openScriptBesideRmd(page: Page, dir: string, name: string): Promise<string> {
  const script = await seedSandboxFile(page, dir, `${name}.R`, '# script\n');
  const rmd = await seedSandboxFile(page, dir, `${name}.Rmd`, '---\ntitle: t\n---\n');

  await executeCommand(page, 'newSourceColumn');
  await expect(page.locator(`${EXTRA_COLUMNS} .ace_editor`)).toHaveCount(1);
  const untitledId = await page.evaluate(() => window.rstudio!.documents.active()!.id);
  await documentOpen(page, script);
  await page.evaluate(() => window.rstudio!.documents.activeEditor()!.insert('# edited\n'));
  await expect.poll(() => page.evaluate(() => window.rstudio!.documents.active()?.dirty))
    .toBe(true);

  await page.locator(`${MAIN_COLUMN} ${SELECTED_DOC_TAB}`).click();
  await documentOpen(page, rmd);
  await expect(page.locator(`${MAIN_COLUMN} ${SELECTED_DOC_TAB}`)).toContainText(`${name}.Rmd`);
  // The R Markdown extended type that enables Publish arrives after the open.
  await expect.poll(() => isPublishVisible(page), { timeout: TIMEOUTS.fileOpen }).toBe(true);
  return untitledId;
}

test.describe('Inactive source column commands (#18955)', () => {
  const sandbox = useSuiteSandbox();

  test.afterEach(async ({ rstudioPage: page }) => {
    await resetSourcePaneState(page);
  });

  test('saving a document in an inactive column keeps Publish', async ({ rstudioPage: page }) => {
    await openScriptBesideRmd(page, sandbox.dir, 'cols_save_all');

    await executeCommand(page, 'saveAllSourceDocs');
    // The extra column's Save button is disabled by the same refresh that
    // used to hide Publish, so Publish is read after that refresh.
    await expect(page.locator(`${EXTRA_COLUMNS} [title^='Save current doc']:visible`))
      .toBeDisabled({ timeout: TIMEOUTS.fileOpen });
    expect(await isPublishVisible(page)).toBe(true);
  });

  test('closing a tab in an inactive column keeps Publish', async ({ rstudioPage: page }) => {
    // Close the extra column's unselected tab: closing its selected tab would
    // select another one there, which makes that column active.
    const untitledId = await openScriptBesideRmd(page, sandbox.dir, 'cols_close');
    const extraColumn = page.locator(EXTRA_COLUMNS);
    await expect(extraColumn).toContainText('Untitled');

    await executeInConsole(page, `.rs.api.documentClose(${rStringLiteral(untitledId)}, save = FALSE)`);
    // The tab is removed in the same task that runs the column's refresh.
    await expect(extraColumn).not.toContainText('Untitled', { timeout: TIMEOUTS.fileOpen });
    expect(await page.evaluate(() => window.rstudio!.documents.active()?.path))
      .toMatch(/cols_close\.Rmd$/);
    expect(await isPublishVisible(page)).toBe(true);
  });
});
