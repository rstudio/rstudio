// Regression test for https://github.com/rstudio/rstudio/issues/17521
// R Notebook previews are rendered in a background R process whose global
// environment is empty. Inline R code (`r expr`) is evaluated live at render
// time, so expressions referencing the user's objects must be evaluated in
// the user's session, with their outputs handed to the background render.

import type { Page } from '@playwright/test';
import { test, expect } from '@fixtures/rstudio.fixture';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { SourcePaneActions } from '@actions/source_pane.actions';
import { useSuiteSandbox } from '@utils/sandbox';
import { executeCommand, setPref, clearPref } from '@utils/commands';
import { heredoc } from '@utils/heredoc';

// A timestamp distinctive enough to grep for in the rendered .nb.html.
const INLINE_TIMESTAMP = '2026-06-01 12:34:56';

function notebookSource(): string {
  // The heredoc reads template parts in raw form, so escaped backticks
  // (\`) would keep their backslashes; interpolate backticks instead.
  const tick = '`';
  const fence = tick.repeat(3);
  return heredoc`
    ---
    title: Inline Chunk Test
    output: html_notebook
    ---

    ${fence}{r define}
    dfSamples <- data.frame(date_received = as.POSIXct("${INLINE_TIMESTAMP}", tz = "UTC"))
    ${fence}

    Latest sample received: ${tick}r as.character(max(dfSamples$date_received))${tick}
  `;
}

test.describe('Notebook inline chunks', { tag: ['@parallel_safe'] }, () => {
  // Sets cwd to a per-spec sandbox; relative paths used by createAndOpenFile,
  // the .nb.html checks, and closeSourceAndDeleteFile land there.
  useSuiteSandbox();
  let consoleActions: ConsolePaneActions;
  let sourceActions: SourcePaneActions;
  let missingPackages: string[] = [];

  test.beforeAll(async ({ rstudioPage: page }) => {
    // A cold-cache package install can outlast the global per-test timeout;
    // keep the headroom this install hook's ensurePackages() budget assumes.
    test.setTimeout(300000);
    consoleActions = new ConsolePaneActions(page);
    sourceActions = new SourcePaneActions(page, consoleActions);

    await consoleActions.resetSourcePane();
    missingPackages = await consoleActions.ensurePackages(['rmarkdown'], 180_000);
    await consoleActions.clearConsole();
  });

  test.beforeEach(() => {
    test.skip(
      missingPackages.length > 0,
      `required R package(s) not available: ${missingPackages.join(', ')}`,
    );
  });

  // Open a fresh notebook, define the object inline code references in the
  // global environment, then dirty the document and save. Saving an
  // html_notebook document triggers the background .nb.html render.
  // Notebooks stay dirty after save by design (MODE_UNCOMMITTED), so save
  // fire-and-forget rather than via saveDocument's wait-for-clean.
  async function defineObjectAndSave(page: Page, fileName: string): Promise<void> {
    await sourceActions.createAndOpenFile(fileName, notebookSource());
    await expect(sourceActions.sourcePane.selectedTab).toContainText(fileName, { timeout: 20000 });

    await consoleActions.executeInConsole(
      `dfSamples <- data.frame(date_received = as.POSIXct("${INLINE_TIMESTAMP}", tz = "UTC"))`,
      { wait: true },
    );
    await consoleActions.clearConsole();

    await sourceActions.goToEnd();
    await page.keyboard.press('Enter');
    await page.keyboard.type('Postscript.');
    await executeCommand(page, 'saveSourceDoc');
  }

  async function cleanup(fileName: string, nbName: string): Promise<void> {
    await consoleActions.executeInConsole(`unlink("${nbName}"); rm(list = "dfSamples")`, { wait: true });
    await sourceActions.closeSourceAndDeleteFile(fileName);
  }

  test('inline code can use global environment objects in preview (#17521)', async ({ rstudioPage: page }) => {
    const fileName = `notebook_inline_${Date.now()}.Rmd`;
    const nbName = fileName.replace(/\.Rmd$/, '.nb.html');

    await defineObjectAndSave(page, fileName);

    // Wait for the background render to produce a .nb.html containing the
    // inline expression's value. Polling through the console keeps this
    // working when the rsession filesystem is remote (Server mode). If the
    // render fails instead, the file never gains the content and this times
    // out -- with the "Error creating notebook" message left in the console
    // output attached to the failure.
    const checkExpr =
      `cat("NB_RESULT:", tryCatch(` +
      `any(grepl("${INLINE_TIMESTAMP}", readLines("${nbName}", warn = FALSE), fixed = TRUE)),` +
      ` error = function(e) FALSE), "\\n")`;
    const consoleOutput = consoleActions.consolePane.consoleOutput;
    await expect
      .poll(
        async () => {
          await consoleActions.clearConsole();
          await consoleActions.executeInConsole(checkExpr, { wait: true });
          return await consoleOutput.textContent();
        },
        { timeout: 60000 },
      )
      .toContain('NB_RESULT: TRUE');

    await cleanup(fileName, nbName);
  });

  test('disabling the inline execution pref evaluates inline code in the background process', async ({ rstudioPage: page }) => {
    const fileName = `notebook_inline_pref_${Date.now()}.Rmd`;
    const nbName = fileName.replace(/\.Rmd$/, '.nb.html');

    // Escape hatch: with the pref disabled, inline code is evaluated in the
    // background process, where the user's global environment is not
    // available -- the render reports an error even though the object
    // exists in this session.
    await setPref(page, 'notebook_execute_inline_chunks', false);
    try {
      await defineObjectAndSave(page, fileName);

      const consoleOutput = consoleActions.consolePane.consoleOutput;
      await expect(consoleOutput).toContainText('Error creating notebook', { timeout: 60000 });
      // match only the object name: the surrounding error wording belongs
      // to R/knitr and may change across versions
      await expect(consoleOutput).toContainText(/dfSamples/, { timeout: 5000 });
    } finally {
      await clearPref(page, 'notebook_execute_inline_chunks');
    }

    await cleanup(fileName, nbName);
  });
});
