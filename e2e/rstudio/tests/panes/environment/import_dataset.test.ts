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
import { rPathLiteral } from '@utils/r';
import { TIMEOUTS } from '@utils/constants';

// The dialog's aria-label comes from the caption passed to the
// ModalDialog ctor in DataImportPresenter (constants_.importTextData()).
const IMPORT_DIALOG = '.gwt-DialogBox[aria-label="Import Text Data"]';

// The preview iframe is a GridViewerFrame titled with constants_.dataPreview().
const PREVIEW_FRAME = 'iframe[title="Data Preview"]';

// Wait for the readr preview iframe to render its first column header. On
// failure, scrape the top-level "Error" dialog into the thrown message: when
// the preview subprocess errors, the message surfaces through the progress
// indicator's onError -> GlobalDisplay.showErrorMessage
// (ModalDialogBase.addProgressIndicator), which renders a separate top-level
// "Error" dialog rather than a label inside the import dialog. The message is
// prefixed with "Is this a valid CSV file? " by enhancePreviewErrorMessage.
async function awaitPreview(
  dialog: Locator,
  previewFrame: FrameLocator,
): Promise<void> {
  const firstColHeader = previewFrame.locator('th[data-col-idx="1"]');

  const previewReady = await firstColHeader
    .waitFor({ state: 'visible', timeout: 45000 })
    .then(() => true)
    .catch(() => false);
  if (previewReady)
    return;

  const errorDialog = dialog.page().locator('.gwt-DialogBox[aria-label="Error"]');
  const errorText = (await errorDialog.textContent().catch(() => null)) ?? '';
  throw new Error(
    'readr CSV preview did not render its column headers within 45s' +
    (errorText ? ` (Error dialog: ${errorText.trim()})` : ''),
  );
}

async function openReadrCsvPreview(
  page: Page,
  consoleActions: ConsolePaneActions,
  csvPath: string,
): Promise<{ dialog: Locator; previewFrame: FrameLocator }> {
  // Write the CSV via the R session so the path is valid on the rsession host
  // (matters for Server mode where runner and session can differ). The preview
  // runs in a separate --vanilla R subprocess, so the file must be on disk
  // before the dialog opens, or vroom reports "<csv> does not exist" and the
  // preview never renders (#17985).
  //
  // Use rPathLiteral, NOT a raw "${csvPath}": on Windows sandbox.dir comes back
  // from R's tempfile() with backslashes, and embedding that into R source
  // turns it into escape sequences (\r, \a become control chars; \p, \w are a
  // hard parse error), so the writeLines never runs. rPathLiteral normalizes to
  // forward slashes (which R accepts on Windows) and quotes safely.
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
  // The file chooser passes the typed path to Java's File API; on Windows use
  // native backslash separators. createSandbox now normalizes its workdir to
  // forward slashes, so csvPath has no backslashes and the first replace is a
  // belt-and-suspenders no-op on every platform; it stays only to guard a
  // future caller that builds csvPath from a raw backslash path. The Windows
  // branch then converts to backslashes for the File API.
  const forwardPath = csvPath.replace(/\\/g, '/');
  const fillPath = os.platform() === 'win32' ? forwardPath.replace(/\//g, '\\') : forwardPath;
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
    await awaitPreview(dialog, previewFrame);

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
    await awaitPreview(dialog, previewFrame);

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
