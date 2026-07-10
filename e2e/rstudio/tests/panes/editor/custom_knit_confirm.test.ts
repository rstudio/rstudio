// Confirmation prompt before running a document-specified custom render
// command. An R Markdown document can set a `knit:` field in its YAML front
// matter; RStudio evaluates that value as R code when rendering, so opening an
// untrusted document and clicking Knit could run arbitrary code with no
// warning. RStudio now confirms first. See rstudio/rstudio-pro#10989.

import { test, expect } from '@fixtures/rstudio.fixture';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { useSuiteSandbox } from '@utils/sandbox';
import { writeAndOpenFile, closeAndDeleteSandboxFiles, removeSandboxFile } from '@utils/files';
import { executeCommand } from '@utils/commands';
import { heredoc } from '@utils/heredoc';
import * as fs from 'fs';
import * as path from 'path';

// showYesNoMessage renders a GWT dialog with these stable button IDs; the
// confirmation prompt is the only yes/no dialog this test can raise.
const YES_BTN = '#rstudio_dlg_yes';
const NO_BTN = '#rstudio_dlg_no';

test.describe('Custom knit render confirmation', () => {
  const sandbox = useSuiteSandbox();
  let consoleActions: ConsolePaneActions;
  let missingPackages: string[] = [];
  let fileName = '';

  test.beforeAll(async ({ rstudioPage: page }) => {
    // A cold-cache package install can outlast the global per-test timeout;
    // keep the headroom this install hook's ensurePackages() budget assumes.
    test.setTimeout(300000);
    consoleActions = new ConsolePaneActions(page);
    await consoleActions.resetSourcePane();

    // The Knit command routes through withRMarkdownPackage before our gate,
    // so rmarkdown must be installed or the test would stall on an install
    // prompt. Skip cleanly if it (or its transitive deps) can't be installed.
    missingPackages = await consoleActions.ensurePackages(['rmarkdown'], 180_000);
    await consoleActions.clearConsole();
  });

  test.beforeEach(() => {
    test.skip(
      missingPackages.length > 0,
      `required R package(s) not available: ${missingPackages.join(', ')}`,
    );
  });

  test.afterEach(async ({ rstudioPage: page }) => {
    if (fileName)
      await closeAndDeleteSandboxFiles(page, sandbox.dir, [fileName, 'knit_ran.txt']);
  });

  test('prompts before running a custom knit: command, and remembers approval', async ({ rstudioPage: page }) => {
    fileName = `custom_knit_${Date.now()}.Rmd`;
    const markerPath = path.join(sandbox.dir, 'knit_ran.txt');

    // The knit: field is a custom function that writes a marker file next to
    // the document. RStudio evaluates it when rendering, so it must be
    // confirmed -- and the marker proves whether it actually ran.
    const rmd = heredoc`
      ---
      title: "Custom knit"
      knit: "(function(input, ...) writeLines('ran', file.path(dirname(input), 'knit_ran.txt')))"
      ---

      Body.
    `;
    await writeAndOpenFile(page, sandbox.dir, fileName, rmd);

    // The confirmation dialog is the gwt-DialogBox carrying the yes button.
    const dialog = page.locator('.gwt-DialogBox').filter({ has: page.locator(YES_BTN) });

    // 1) Knit -> confirmation appears, shows the command, and declining it
    //    runs nothing.
    await executeCommand(page, 'knitDocument');
    await expect(dialog).toBeVisible({ timeout: 20000 });
    await expect(dialog).toContainText('Run Custom Render Command?');
    await expect(dialog).toContainText('writeLines');

    await page.locator(NO_BTN).click();
    await expect(dialog).not.toBeVisible({ timeout: 5000 });

    // Give a declined render a chance to (erroneously) run; the marker must
    // not appear. This is a negative assertion, so a short bounded wait is
    // unavoidable.
    await page.waitForTimeout(2000);
    expect(fs.existsSync(markerPath)).toBe(false);

    // 2) Knit again -> the prompt re-appears (declining did not remember the
    //    file); accepting runs the command.
    await executeCommand(page, 'knitDocument');
    await expect(dialog).toBeVisible({ timeout: 20000 });
    await page.locator(YES_BTN).click();
    await expect.poll(() => fs.existsSync(markerPath), { timeout: 30000 }).toBe(true);

    // The custom function returns no output file, so RStudio may surface a
    // render error; clear any such modal before continuing.
    await page.keyboard.press('Escape');
    // marker is written by the custom knit function running in rsession;
    // delegate the unlink via removeSandboxFile so it works whether the
    // sandbox is local-writable or owned by a different uid.
    await removeSandboxFile(page, markerPath);

    // 3) Knit again -> the approval is remembered for the session, so no
    //    prompt appears and the command runs without a click. Re-issue knit
    //    until the marker reappears (the prior async render may still be
    //    running, in which case the backend drops the extra request), and
    //    fail loudly if the confirmation dialog ever shows.
    await expect.poll(async () => {
      if (await dialog.isVisible())
        throw new Error('confirmation dialog reappeared for an already-approved document');
      if (fs.existsSync(markerPath))
        return true;
      await executeCommand(page, 'knitDocument');
      return fs.existsSync(markerPath);
    }, { timeout: 45000, intervals: [2000] }).toBe(true);
  });
});
