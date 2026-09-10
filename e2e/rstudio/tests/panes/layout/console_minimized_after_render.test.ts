// A minimized Console pane goes back to minimized after a successful render
// (#11622). Render output lives in the Console pane's tabset, so starting a
// render raises the pane; once the render succeeds and control returns to the
// console, the pane is minimized again. A failed render leaves it open so the
// output can be read.

import type { Page } from 'playwright';
import { test, expect } from '@fixtures/rstudio.fixture';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { TIMEOUTS } from '@utils/constants';
import { clearPref, executeCommand, setPref } from '@utils/commands';
import { closeAndDeleteSandboxFiles, writeAndOpenFile } from '@utils/files';
import { heredoc } from '@utils/heredoc';
import { useSuiteSandbox } from '@utils/sandbox';
import * as fs from 'fs';
import * as path from 'path';

const CONSOLE_PANE = '#rstudio_Console_pane';
const CONSOLE_MIN_BTN = `${CONSOLE_PANE} .rstudio_panel_min_btn_console`;

// rmarkdown::render of a trivial document plus pandoc; generous on CI.
const RENDER_TIMEOUT = 90000;

// heredoc reads its template raw, so a chunk fence has to be interpolated
const FENCE = '```';

async function minimizeConsole(page: Page): Promise<void> {
  await page.locator(CONSOLE_MIN_BTN).click();
  await expect(page.locator(CONSOLE_PANE)).toBeHidden({ timeout: TIMEOUTS.fileOpen });
}

test.describe.serial('Console pane stays minimized across a render', () => {
  const sandbox = useSuiteSandbox();
  let consoleActions: ConsolePaneActions;
  let missingPackages: string[] = [];
  let fileName = '';

  test.beforeAll(async ({ rstudioPage: page }) => {
    test.setTimeout(300000);
    consoleActions = new ConsolePaneActions(page);
    await consoleActions.resetSourcePane();
    missingPackages = await consoleActions.ensurePackages(['rmarkdown'], 180_000);
    await consoleActions.clearConsole();

    // keep the rendered document out of a preview window
    await setPref(page, 'rmd_viewer_type', 'none');
  });

  test.beforeEach(() => {
    test.skip(
      missingPackages.length > 0,
      `required R package(s) not available: ${missingPackages.join(', ')}`,
    );
  });

  test.afterEach(async ({ rstudioPage: page }) => {
    // un-minimize the Console pane for whatever runs next
    await executeCommand(page, 'activateConsolePane');
    await expect(page.locator(CONSOLE_PANE)).toBeVisible({ timeout: TIMEOUTS.fileOpen });
    if (fileName) {
      const stem = fileName.replace(/\.Rmd$/, '');
      await closeAndDeleteSandboxFiles(page, sandbox.dir, [fileName, `${stem}.html`]);
      fileName = '';
    }
  });

  test.afterAll(async ({ rstudioPage: page }) => {
    await clearPref(page, 'rmd_viewer_type');
  });

  test('a successful render minimizes the pane again', async ({ rstudioPage: page }) => {
    fileName = `minimized_ok_${Date.now()}.Rmd`;
    const rmd = heredoc`
      ---
      title: "Minimized console"
      output: html_document
      ---

      Body.
    `;
    await writeAndOpenFile(page, sandbox.dir, fileName, rmd);
    await minimizeConsole(page);

    await executeCommand(page, 'knitDocument');

    // The render is done once its output exists; the pane then has to be
    // minimized again (it was raised for the Render tab in between). Watch the
    // file from here rather than via the console: typing into the console
    // would itself raise the pane and defeat the test.
    const outputPath = path.join(sandbox.dir, fileName.replace(/\.Rmd$/, '.html'));
    await expect.poll(() => fs.existsSync(outputPath), { timeout: RENDER_TIMEOUT }).toBe(true);
    await expect(page.locator(CONSOLE_PANE)).toBeHidden({ timeout: TIMEOUTS.fileOpen });
  });

  test('a failed render leaves the pane open', async ({ rstudioPage: page }) => {
    fileName = `minimized_fail_${Date.now()}.Rmd`;
    const rmd = heredoc`
      ---
      title: "Minimized console"
      output: html_document
      ---

      ${FENCE}{r}
      stop("render should fail")
      ${FENCE}
    `;
    await writeAndOpenFile(page, sandbox.dir, fileName, rmd);
    await minimizeConsole(page);

    await executeCommand(page, 'knitDocument');

    // the Render tab (inside the Console pane) reports the failure and the pane
    // is opened for it; "Execution halted" is the last line rmarkdown prints,
    // so the pane must still be showing once it has arrived
    const consolePane = page.locator(CONSOLE_PANE);
    await expect(consolePane).toContainText('render should fail', { timeout: RENDER_TIMEOUT });
    await expect(consolePane).toContainText('Execution halted', { timeout: RENDER_TIMEOUT });
    await expect(consolePane).toBeVisible();
  });
});
