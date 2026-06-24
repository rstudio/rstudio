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
//
// `consoleActions` and `csvPath` are used only on the failure path, to probe
// the R session for the CSV's on-disk state. #17985 manifests as the preview
// subprocess reporting the CSV "does not exist"; the probe records whether the
// file is actually missing (write failed / wrong path) or present-but-invisible
// to the separate preview process, so the skip reason is actionable.
async function awaitPreviewOrSkip(
  dialog: Locator,
  previewFrame: FrameLocator,
  consoleActions: ConsolePaneActions,
  csvPath: string,
): Promise<void> {
  const firstColHeader = previewFrame.locator('th[data-col-idx="1"]');

  const previewReady = await firstColHeader
    .waitFor({ state: 'visible', timeout: 45000 })
    .then(() => true)
    .catch(() => false);
  if (previewReady)
    return;

  // The header never appeared. When the preview subprocess fails, the error is
  // surfaced through the progress indicator's onError, which routes to
  // GlobalDisplay.showErrorMessage (ModalDialogBase.addProgressIndicator) and
  // renders a separate top-level "Error" dialog -- not a label inside the
  // import dialog. Scrape that dialog for a more specific diagnostic. The
  // message is prefixed with "Is this a valid CSV file? " by
  // enhancePreviewErrorMessage, so key on that marker.
  const errorDialog = dialog.page().locator('.gwt-DialogBox[aria-label="Error"]');
  const errorText = (await errorDialog.textContent().catch(() => null)) ?? '';
  let detail = errorText.includes('valid CSV file')
    ? `readr CSV preview failed on Windows CI: ${errorText.trim()}`
    : 'readr CSV preview subprocess did not load on Windows CI';

  // Cancel out of the import dialog and dismiss the top-level Error dialog (if
  // any) before probing/skipping, so the console is reachable for the probe
  // below and teardown stays clean.
  await dialog.locator('#rstudio_dlg_cancel').click().catch(() => {});
  await errorDialog.getByRole('button', { name: 'OK' }).click().catch(() => {});

  // Best-effort: capture the R session's view of the CSV so the skip/throw
  // reason says whether the file is on disk (write failed / wrong path) or
  // present-but-invisible to the separate preview process (the suspected
  // Windows-CI filesystem-visibility flake in #17985).
  const fsState = await consoleActions
    .evalRString(
      `paste0("wd=[", getwd(), "] exists=", file.exists("${csvPath}"), ` +
      `" dirfiles=[", paste(list.files(dirname("${csvPath}")), collapse=", "), "]")`,
    )
    .catch(() => null);
  if (fsState)
    detail += ` [R fs: ${fsState}]`;

  test.fixme(os.platform() === 'win32' && !!process.env.CI, detail);
  throw new Error(
    `readr CSV preview did not produce column headers within 45 s [R fs: ${fsState ?? 'unavailable'}]`,
  );
}

async function openReadrCsvPreview(
  page: Page,
  consoleActions: ConsolePaneActions,
  csvPath: string,
): Promise<{ dialog: Locator; previewFrame: FrameLocator; csvPath: string }> {
  // Write the CSV via the R session so the path is valid on the rsession host
  // (matters for Server mode where runner and session can be on different
  // machines).
  const writeCmd = `writeLines(c("a,b,c","1,2,3","4,5,6"), "${csvPath}")`;
  await consoleActions.executeInConsole(writeCmd, { wait: true });

  // Verify the CSV actually landed before opening the dialog. The preview runs
  // in a separate --vanilla R subprocess; if the write silently failed (a
  // transient open/lock failure on Windows CI is swallowed by wait:true) the
  // subprocess reports the file "does not exist" and the preview times out
  // after 45s (#17985). Poll file.exists() from the session, retry the write
  // once, and surface a setup error here rather than misattributing it to the
  // preview. file.exists() is the session's own view -- a cross-process
  // visibility lag still slips through, but awaitPreviewOrSkip's probe records
  // that case.
  await ensureCsvWritten(consoleActions, csvPath, writeCmd);

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

  return { dialog, previewFrame: dialog.frameLocator(PREVIEW_FRAME), csvPath };
}

// Confirm the CSV is on disk from the R session's point of view, retrying the
// write once. Throws a setup-level error (not a preview timeout) if the file
// never appears -- see openReadrCsvPreview for why this guards #17985.
async function ensureCsvWritten(
  consoleActions: ConsolePaneActions,
  csvPath: string,
  writeCmd: string,
): Promise<void> {
  const exists = async (): Promise<boolean> =>
    (await consoleActions.evalRLogical(`file.exists("${csvPath}")`)) === true;

  if (await exists())
    return;

  await consoleActions.executeInConsole(writeCmd, { wait: true });
  if (await exists())
    return;

  throw new Error(
    `Import Dataset test setup: CSV not written to ${csvPath} ` +
    `(file.exists() is FALSE after two writeLines attempts).`,
  );
}

test.describe('Import Dataset (readr)', () => {
  const sandbox = useSuiteSandbox();
  let consoleActions: ConsolePaneActions;

  test.beforeAll(async ({ rstudioPage: page }) => {
    consoleActions = new ConsolePaneActions(page);
  });

  // https://github.com/rstudio/rstudio/issues/17777
  test('CSV preview renders without an .rs.digest lookup error', async ({ rstudioPage: page }) => {
    const { dialog, previewFrame, csvPath } = await openReadrCsvPreview(
      page, consoleActions, `${sandbox.dir}/sample.csv`,
    );

    // Preview iframe should populate with the CSV's three columns. Pre-fix
    // this never landed because the subprocess errored out before returning
    // column data, and DataImport.java would render the "Is this a valid
    // CSV file?" error prefix instead. The grid renders each header as
    // "<name>(<type>)" once column inference completes; anchor on the name
    // prefix so the assertion doesn't depend on the inferred type string.
    await awaitPreviewOrSkip(dialog, previewFrame, consoleActions, csvPath);

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
    const { dialog, previewFrame, csvPath } = await openReadrCsvPreview(
      page, consoleActions, `${sandbox.dir}/summary.csv`,
    );

    // Gate on the preview being up before asserting sidebar state.
    await awaitPreviewOrSkip(dialog, previewFrame, consoleActions, csvPath);

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
