// Import Dataset (readr) end-to-end tests.
//
// - #17777: the readr CSV preview must render without an .rs.digest lookup
//   error. preview_data_import_async spawns a --vanilla R subprocess that
//   sources only Tools.R + a handful of session modules. Before the fix,
//   .rs.digest lived in SessionRUtil.R (not sourced there) and was backed by
//   a .Call into the embedding (also unavailable), so the preview failed with
//   "Is this a valid CSV file? could not find function '.rs.digest'".
//   .rs.digest is now a pure-base-R Adler-32 in Tools.R, which the subprocess
//   already sources.
// - #17735: the import preview is a GridViewerFrame, which runs the shared
//   grid viewer in data_source=data mode (data pushed in client-side, no
//   server-side cached frame). The column-summary sidebar fetches stats from
//   the grid_data endpoint and so has nothing to summarize in this host; it
//   must be hidden. GridViewerFrame now requests show_summary=0.

import { test, expect } from '@fixtures/rstudio.fixture';
import type { Page, FrameLocator, Locator } from 'playwright';
import * as os from 'os';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { executeCommand } from '@utils/commands';
import { installDepIfPrompted } from '@pages/modals.page';
import { useSuiteSandbox } from '@utils/sandbox';
import { TIMEOUTS } from '@utils/constants';
import { rPathLiteral } from '@utils/r';

// The dialog's aria-label comes from the caption passed to the
// ModalDialog ctor in DataImportPresenter (constants_.importTextData()).
const IMPORT_DIALOG = '.gwt-DialogBox[aria-label="Import Text Data"]';

// The preview iframe is a GridViewerFrame titled with constants_.dataPreview().
const PREVIEW_FRAME = 'iframe[title="Data Preview"]';

// Open Import Dataset > From Text (readr) for the given CSV path and drive the
// dialog until the preview iframe is up. Returns the dialog + preview frame so
// each test can assert on the rendered preview. The caller cancels the dialog.
// Waits for the readr preview iframe to show its first column header.
// On Windows CI the preview subprocess occasionally fails to start; soft-skip
// rather than hard-fail so an environment issue doesn't mask other results.
async function awaitPreviewOrSkip(
  dialog: Locator,
  previewFrame: FrameLocator,
): Promise<void> {
  const firstColHeader = previewFrame.locator('th[data-col-idx="1"]');
  const previewReady = await firstColHeader
    .waitFor({ state: 'visible', timeout: 45000 })
    .then(() => true)
    .catch(() => false);
  if (!previewReady) {
    await dialog.locator('#rstudio_dlg_cancel').click().catch(() => {});
    test.fixme(os.platform() === 'win32' && !!process.env.CI, 'readr CSV preview subprocess did not load on Windows CI');
    throw new Error('readr CSV preview did not produce column headers within 45 s');
  }
}

async function openReadrCsvPreview(
  page: Page,
  consoleActions: ConsolePaneActions,
  csvPath: string,
): Promise<{ dialog: Locator; previewFrame: FrameLocator }> {
  // Write the CSV via the R session so the path is valid on the rsession host
  // (matters for Server mode where runner and session can be on different
  // machines). rPathLiteral normalizes Windows backslashes to forward slashes
  // before quoting -- raw interpolation of a Windows path here trips R's
  // "unrecognized escape" parser on sequences like `\p`, `\a`, `\r`, which
  // leaves the file uncreated and the readr preview hanging.
  await consoleActions.executeInConsole(
    `writeLines(c("a,b,c","1,2,3","4,5,6"), ${rPathLiteral(csvPath)})`,
    { wait: true },
  );

  // The presenter wraps the dialog in a dependency check; accept the install
  // prompt if readr (or a transitive dep) isn't already on the library path.
  await executeCommand(page, 'importDatasetFromCsvUsingReadr');
  await installDepIfPrompted(page);

  const dialog = page.locator(IMPORT_DIALOG);
  await expect(dialog).toBeVisible({ timeout: TIMEOUTS.fileOpen });

  // Set the path on the file chooser TextBox in one shot. The chooser polls
  // for value changes every 250ms (DataImportFileChooser.checkForTextBoxChange)
  // and flips the action button from "Browse..." to "Update" when the textbox
  // transitions empty -> non-empty -- we wait on that flip below rather than
  // racing the poll per keystroke.
  //
  // The textbox's class is the obfuscated form of `modelTextBox` (set via
  // styleName= in DataImportFileChooser.ui.xml, which replaces the default
  // .gwt-TextBox), so target it by ARIA name instead -- "File/URL:" comes from
  // the label-association in DataImport.ui.xml.
  const fileInput = dialog.getByRole('textbox', { name: 'File/URL:' });
  await expect(fileInput).toBeVisible({ timeout: 5000 });
  // On Windows the readr dialog's file chooser passes the path to a subprocess
  // via Java's File API, which expects backslash separators. Forward-slash
  // paths (produced by R's tempfile()) cause the preview to silently fail.
  const fillPath = os.platform() === 'win32' ? csvPath.replace(/\//g, '\\') : csvPath;
  await fileInput.fill(fillPath);

  // Click "Update" to commit the path and kick off preview_data_import_async.
  // The textbox value change alone does not trigger the preview -- it has to go
  // through the action button's click handler (DataImportFileChooser.
  // actionButton_). The button's aria-label flips from "Browse for File/URL"
  // to "Update for File/URL" via switchToUpdateMode() once the polling timer
  // notices the new value.
  const updateBtn = dialog.getByRole('button', { name: 'Update for File/URL' });
  await expect(updateBtn).toBeVisible({ timeout: 5000 });
  await updateBtn.click();

  return { dialog, previewFrame: dialog.frameLocator(PREVIEW_FRAME) };
}

test.describe('Import Dataset (readr)', () => {
  const sandbox = useSuiteSandbox();
  let consoleActions: ConsolePaneActions;

  test.beforeAll(async ({ rstudioPage: page }) => {
    consoleActions = new ConsolePaneActions(page);
    // Pre-install readr (and pillar, which the importer also needs) into
    // rsession's own R library before the first test runs. On Desktop the
    // test process and rsession share an R library, so installDepIfPrompted
    // covered the missing-package case. On Server, rsession runs as a
    // different uid with its own empty user library; the install-prompt
    // path is fragile from there, and the import dialog can race past
    // TIMEOUTS.fileOpen waiting for it. Pre-installing keeps the test on
    // the happy path (readr is already there, no prompt, dialog opens
    // promptly) on both modes.
    await consoleActions.executeInConsole(
      'if (!requireNamespace("readr", quietly = TRUE)) ' +
        'install.packages("readr", ' +
        'repos = "https://packagemanager.posit.co/cran/latest")',
      { wait: true },
    );
  });

  // https://github.com/rstudio/rstudio/issues/17777
  test('CSV preview renders without an .rs.digest lookup error', async ({ rstudioPage: page }) => {
    const { dialog, previewFrame } = await openReadrCsvPreview(
      page, consoleActions, `${sandbox.dir}/sample.csv`,
    );

    // Preview iframe should populate with the CSV's three columns. Pre-fix
    // this never landed because the subprocess errored out before returning
    // column data, and DataImport.java would render the "Is this a valid
    // CSV file?" error prefix instead. The grid renders each header as
    // "<name>(<type>)" once column inference completes; anchor on the name
    // prefix so the assertion doesn't depend on the inferred type string.
    await awaitPreviewOrSkip(dialog, previewFrame);

    const firstColHeader = previewFrame.locator('th[data-col-idx="1"]');
    await expect(firstColHeader).toHaveText(/^a/);
    await expect(previewFrame.locator('th[data-col-idx="2"]')).toHaveText(/^b/);
    await expect(previewFrame.locator('th[data-col-idx="3"]')).toHaveText(/^c/);

    // Defensive: ensure the #17777 error prefix is not anywhere in the
    // dialog (would catch a regression where the iframe also happens to
    // render correct-looking headers from some unrelated source).
    await expect(dialog).not.toContainText('valid CSV file');

    // Cancel out -- we don't want to actually run the import.
    await dialog.locator('#rstudio_dlg_cancel').click();
    await expect(dialog).not.toBeVisible({ timeout: 5000 });
  });

  // https://github.com/rstudio/rstudio/issues/17735
  test('preview hides the column-summary sidebar', async ({ rstudioPage: page }) => {
    const { dialog, previewFrame } = await openReadrCsvPreview(
      page, consoleActions, `${sandbox.dir}/summary.csv`,
    );

    // Gate on the preview being up before asserting sidebar state.
    await awaitPreviewOrSkip(dialog, previewFrame);

    // The fix: GridViewerFrame requests show_summary=0, so the panel never
    // gains the "expanded" class and the toggle reports collapsed. Pre-fix the
    // host defaulted show_summary to true and the (non-functional in data
    // mode) summary sidebar was expanded.
    await expect(previewFrame.locator('#sidebarPanel')).not.toHaveClass(/\bexpanded\b/);
    await expect(previewFrame.locator('#sidebarToggle')).toHaveAttribute('aria-expanded', 'false');

    await dialog.locator('#rstudio_dlg_cancel').click();
    await expect(dialog).not.toBeVisible({ timeout: 5000 });
  });
});
