// Import Dataset (readr) end-to-end test for #17777.
//
// preview_data_import_async spawns a --vanilla R subprocess that sources
// only Tools.R + a handful of session modules. Before the fix, .rs.digest
// lived in SessionRUtil.R (not sourced in that subprocess) and was backed
// by a .Call into the embedding (also not available there), so the CSV
// preview failed with "Is this a valid CSV file? could not find function
// '.rs.digest'". .rs.digest is now a pure-base-R Adler-32 in Tools.R,
// which the subprocess already sources -- this test asserts the dialog
// renders a successful preview end to end.

import { test, expect } from '@fixtures/rstudio.fixture';
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

test.describe('Import Dataset (readr)', () => {
  const sandbox = useSuiteSandbox();
  let consoleActions: ConsolePaneActions;

  test.beforeAll(async ({ rstudioPage: page }) => {
    consoleActions = new ConsolePaneActions(page);
  });

  // https://github.com/rstudio/rstudio/issues/17777
  test('CSV preview renders without an .rs.digest lookup error', async ({ rstudioPage: page }) => {
    // Write a tiny CSV via the R session so the path is valid on the
    // rsession host (matters for Server mode where the runner and session
    // can be on different machines).
    const csvPath = `${sandbox.dir}/sample.csv`;
    await consoleActions.executeInConsole(
      `writeLines(c("a,b,c","1,2,3","4,5,6"), "${csvPath}")`,
      { wait: true },
    );

    // Open Import Dataset > From Text (readr). The presenter wraps the
    // dialog in a dependency check; accept the install prompt if readr
    // (or one of its transitive deps) isn't already on the library path.
    await executeCommand(page, 'importDatasetFromCsvUsingReadr');
    await installDepIfPrompted(page);

    const dialog = page.locator(IMPORT_DIALOG);
    await expect(dialog).toBeVisible({ timeout: TIMEOUTS.fileOpen });

    // Type the CSV path into the file chooser TextBox. The chooser polls
    // for value changes every 250ms (DataImportFileChooser.checkForTextBoxChange)
    // and flips the action button from "Browse..." to "Update" when the
    // textbox transitions empty -> non-empty.
    //
    // The textbox's class is the obfuscated form of `modelTextBox` (set via
    // styleName= in DataImportFileChooser.ui.xml, which replaces the default
    // .gwt-TextBox), so target it by ARIA name instead -- "File/URL:" comes
    // from the label-association in DataImport.ui.xml.
    const fileInput = dialog.getByRole('textbox', { name: 'File/URL:' });
    await expect(fileInput).toBeVisible({ timeout: 5000 });
    await fileInput.click();
    await fileInput.pressSequentially(csvPath);

    // Click "Update" to commit the path and kick off
    // preview_data_import_async. The textbox value change alone does not
    // trigger the preview -- it has to go through the action button's
    // click handler (DataImportFileChooser.actionButton_). The button's
    // aria-label flips from "Browse for File/URL" to "Update for File/URL"
    // via switchToUpdateMode() once the polling timer notices the new value.
    const updateBtn = dialog.getByRole('button', { name: 'Update for File/URL' });
    await expect(updateBtn).toBeVisible({ timeout: 5000 });
    await updateBtn.click();

    // Preview iframe should populate with the CSV's three columns. Pre-fix
    // this never landed because the subprocess errored out before returning
    // column data, and DataImport.java would render the "Is this a valid
    // CSV file?" error prefix instead. The grid renders each header as
    // "<name>(<type>)" once column inference completes; anchor on the name
    // prefix so the assertion doesn't depend on the inferred type string.
    const previewFrame = dialog.frameLocator(PREVIEW_FRAME);
    await expect(previewFrame.locator('th[data-col-idx="1"]'))
      .toHaveText(/^a/, { timeout: TIMEOUTS.fileOpen });
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
});
