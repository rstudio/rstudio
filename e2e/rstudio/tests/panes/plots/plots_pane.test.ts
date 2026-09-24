import { test, expect } from '@fixtures/rstudio.fixture';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { PlotsPane } from '@pages/plots_pane.page';
import { CONFIRM_BTN, CANCEL_BTN, YES_BTN } from '@pages/modals.page';
import { useSuiteSandbox } from '@utils/sandbox';
import { LIST_FILES_RPC, holdListFiles } from '@utils/file-dialogs';
import { TIMEOUTS } from '@utils/constants';
import type { Locator, Page, Request } from 'playwright';

// GWT file-chooser accept button. base-prefs.jsonc sets native_file_dialogs=false
// suite-wide, so export flows always use the GWT file chooser (never a native OS
// dialog). The two-step save flow is: OK on the format dialog, then this button
// on the file chooser to accept the default path.
const FILE_ACCEPT_SAVE = '#rstudio_file_accept_save';
const FILE_CANCEL_SAVE = '#rstudio_file_cancel_save';
const FILE_NEW_FOLDER = '#rstudio_file_new_folder';
const TEXT_ENTRY = '#rstudio_text_entry';

// The RPC that saves the plot once the chooser is accepted.
const SAVE_PLOT_RPC = /\/rpc\/save_plot_as(?:\?|$)/;

// Sandbox working directory for file-export tests so saved files are cleaned
// up by globalTeardown automatically.
useSuiteSandbox();

let consoleActions: ConsolePaneActions;
let plotsPane: PlotsPane;

// Opens Plots > Export > Save as Image, confirms the format dialog, and returns
// the GWT file chooser's accept button.
async function openSaveAsImageChooser(page: Page): Promise<Locator> {
  await plotsPane.exportMenu.click();
  await plotsPane.saveAsImageItem.click();
  await expect(plotsPane.saveAsImageDialog).toBeVisible({ timeout: TIMEOUTS.fileOpen });
  await page.locator(CONFIRM_BTN).click();
  return page.locator(FILE_ACCEPT_SAVE);
}

// Counts save_plot_as RPCs issued from now until stop() is called.
function countSaves(page: Page) {
  let count = 0;
  const onRequest = (request: Request) => {
    if (SAVE_PLOT_RPC.test(request.url()))
      count++;
  };
  page.on('request', onRequest);
  return {
    count: () => count,
    stop: () => page.off('request', onRequest),
  };
}

// Creates a minimal base-graphics plot and waits for the Plots pane to show it.
async function createPlot(page: Page): Promise<void> {
  await consoleActions.executeInConsole('plot(1, 1)');
  await plotsPane.tab.click();
  await expect(plotsPane.plotImage).toBeVisible({ timeout: TIMEOUTS.fileOpen });
}

test.describe.serial('Plots pane', { tag: ['@serial'] }, () => {
  test.beforeAll(async ({ rstudioPage: page }) => {
    consoleActions = new ConsolePaneActions(page);
    plotsPane = new PlotsPane(page);
    await consoleActions.resetSourcePane();
  });

  test.afterEach(async () => {
    // Close all open graphics devices to reset plot history between tests
    // without needing to interact with the confirmation dialog.
    await consoleActions.executeInConsole('try(while (dev.cur() > 1) dev.off(), silent = TRUE)');
  });

  test(
    'zoom in plot opens a popup containing the plot image',
    { tag: ['@desktop_only'] },
    async ({ rstudioPage: page }) => {
      await createPlot(page);

      const [popup] = await Promise.all([
        page.context().waitForEvent('page'),
        plotsPane.zoomPlotBtn.click(),
      ]);
      await expect(popup.locator('#plot')).toBeVisible({ timeout: TIMEOUTS.fileOpen });
      await popup.close();
    },
  );

  test('removing a plot clears the plots pane', async ({ rstudioPage: page }) => {
    await createPlot(page);

    await plotsPane.removePlotBtn.click();
    await page.locator(YES_BTN).click();
    // When no plots remain the toolbar buttons are disabled -- the iframe
    // stays in the DOM (src="about:blank") so visibility is not a useful signal.
    await expect(plotsPane.removePlotBtn).toBeDisabled();
  });

  test('clearing all plots clears the plots pane', async ({ rstudioPage: page }) => {
    await createPlot(page);

    await plotsPane.clearPlotsBtn.click();
    await page.locator(YES_BTN).click();
    await expect(plotsPane.clearPlotsBtn).toBeDisabled();
  });

  test('publish button is visible in the plots toolbar', async ({ rstudioPage: page }) => {
    await createPlot(page);
    await expect(plotsPane.publishBtn).toBeVisible();
  });

  test('refreshing a plot leaves the plot image visible', async ({ rstudioPage: page }) => {
    await createPlot(page);

    await plotsPane.refreshPlotBtn.click();
    await expect(plotsPane.plotImage).toBeVisible({ timeout: TIMEOUTS.fileOpen });
  });

  test('export dropdown lists save-image, save-PDF, and copy-to-clipboard options', async ({
    rstudioPage: page,
  }) => {
    await createPlot(page);

    await plotsPane.exportMenu.click();
    await expect(plotsPane.saveAsImageItem).toBeVisible();
    await expect(plotsPane.saveAsPdfItem).toBeVisible();
    await expect(plotsPane.copyToClipboardItem).toBeVisible();
    await page.keyboard.press('Escape');
  });

  test('save plot as image dialog opens and accepts the save', async ({ rstudioPage: page }) => {
    await createPlot(page);

    await plotsPane.exportMenu.click();
    await plotsPane.saveAsImageItem.click();
    await expect(plotsPane.saveAsImageDialog).toBeVisible({ timeout: TIMEOUTS.fileOpen });
    // Two-step save: OK confirms format/size, then the GWT file chooser appears
    // and FILE_ACCEPT_SAVE accepts the default path.
    await page.locator(CONFIRM_BTN).click();
    await page.locator(FILE_ACCEPT_SAVE).click();
    await expect(plotsPane.saveAsImageDialog).toBeHidden();

    // Verify a file was actually written -- the dialog closing alone doesn't
    // confirm a successful save. Match the extension so a pre-existing Rplot.pdf
    // from a later test can't satisfy this assertion vacuously.
    await expect.poll(
      () => consoleActions.evalRLogical(
        `length(list.files(getwd(), pattern = "Rplot.*\\\\.png$")) > 0`,
      ),
      { timeout: TIMEOUTS.fileOpen },
    ).toBe(true);
  });

  test('file chooser accepted before its listing arrives saves once it does', async ({ rstudioPage: page }) => {
    await createPlot(page);
    await consoleActions.executeInConsole('.pngs_before <- list.files(getwd(), pattern = "Rplot.*\\\\.png$")');

    // Hold the chooser's directory listing so the accept click is guaranteed
    // to land before it arrives (#18922: the click used to close the chooser
    // without saving).
    const listing = await holdListFiles(page);
    const saves = countSaves(page);

    try {
      const acceptButton = await openSaveAsImageChooser(page);
      await acceptButton.click();

      // the chooser stays open with the accept pending, and nothing is saved
      await expect.poll(() => listing.held(), { timeout: TIMEOUTS.fileOpen }).toBeGreaterThan(0);
      await expect(acceptButton).toBeVisible();
      expect(saves.count()).toBe(0);

      // the pending accept saves the plot by itself once the listing arrives
      listing.release();
      await expect(acceptButton).toBeHidden({ timeout: TIMEOUTS.fileOpen });
      await expect(plotsPane.saveAsImageDialog).toBeHidden();
      expect(saves.count()).toBe(1);
      await expect.poll(
        () => consoleActions.evalRLogical(
          'length(setdiff(list.files(getwd(), pattern = "Rplot.*\\\\.png$"), .pngs_before)) == 1',
        ),
        { timeout: TIMEOUTS.fileOpen },
      ).toBe(true);
    } finally {
      saves.stop();
      await listing.cleanup();
    }
  });

  test('file chooser cancelled with an accept pending does not save', async ({ rstudioPage: page }) => {
    await createPlot(page);
    await consoleActions.executeInConsole('.pngs_before <- list.files(getwd(), pattern = "Rplot.*\\\\.png$")');

    // Accept while the listing is held, then cancel: the listing still reaches
    // the closed chooser, and the deferred accept must not run from it.
    const listing = await holdListFiles(page);
    const saves = countSaves(page);

    try {
      const acceptButton = await openSaveAsImageChooser(page);
      await acceptButton.click();
      await expect.poll(() => listing.held(), { timeout: TIMEOUTS.fileOpen }).toBeGreaterThan(0);
      await expect(acceptButton).toBeVisible();
      await page.locator(FILE_CANCEL_SAVE).click();
      await expect(acceptButton).toBeHidden();

      // cancelling the chooser returns to the format dialog; a deferred accept
      // would issue the save as soon as the client handles the listing
      // response, and dismissing that dialog is ordered after it
      listing.release();
      await listing.settled();
      await expect(plotsPane.saveAsImageDialog).toBeVisible();
      await page.locator(CANCEL_BTN).click();
      await expect(plotsPane.saveAsImageDialog).toBeHidden();
      expect(saves.count()).toBe(0);
      expect(
        await consoleActions.evalRLogical(
          'length(setdiff(list.files(getwd(), pattern = "Rplot.*\\\\.png$"), .pngs_before)) == 0',
        ),
      ).toBe(true);
    } finally {
      saves.stop();
      await listing.cleanup();
    }
  });

  test('save plot as PDF dialog opens and accepts the save', async ({ rstudioPage: page }) => {
    await createPlot(page);

    await plotsPane.exportMenu.click();
    await plotsPane.saveAsPdfItem.click();
    await expect(plotsPane.saveAsPdfDialog).toBeVisible({ timeout: TIMEOUTS.fileOpen });
    await page.locator(CONFIRM_BTN).click();
    await page.locator(FILE_ACCEPT_SAVE).click();
    await expect(plotsPane.saveAsPdfDialog).toBeHidden();

    // RStudio increments the filename (Rplot01.pdf, Rplot02.pdf, ...) when a
    // previous export already exists in the directory. Match any Rplot*.pdf.
    await expect.poll(
      () => consoleActions.evalRLogical(
        `length(list.files(getwd(), pattern = "Rplot.*\\\\.pdf$")) > 0`,
      ),
      { timeout: TIMEOUTS.fileOpen },
    ).toBe(true);
  });

  test('copy plot to clipboard dialog opens and can be cancelled', async ({
    rstudioPage: page,
  }) => {
    await createPlot(page);

    await plotsPane.exportMenu.click();
    await plotsPane.copyToClipboardItem.click();
    await expect(plotsPane.copyToClipboardDialog).toBeVisible({ timeout: TIMEOUTS.fileOpen });
    await page.locator(CANCEL_BTN).click();
    await expect(plotsPane.copyToClipboardDialog).toBeHidden();
  });

  test('grid graphics renders a plot without crashing R', async ({ rstudioPage: page }) => {
    await consoleActions.executeInConsole('grid::grid.newpage()');
    await plotsPane.tab.click();
    await expect(plotsPane.plotImage).toBeVisible({ timeout: TIMEOUTS.fileOpen });
  });

  test(
    'resizing the zoomed plot popup does not crash R',
    { tag: ['@desktop_only'] },
    async ({ rstudioPage: page }) => {
      await createPlot(page);

      // Guards against rstudio/rstudio#14697 (crash on repeated resize of the
      // zoomed-plot popup). We drive viewport resizes on the popup page rather
      // than OS-level window.resizeTo(), which exercises the same resize-event
      // handlers without requiring Electron-specific JS evaluation.
      const [popup] = await Promise.all([
        page.context().waitForEvent('page'),
        plotsPane.zoomPlotBtn.click(),
      ]);
      await expect(popup.locator('#plot')).toBeVisible({ timeout: TIMEOUTS.fileOpen });

      // Confirm a resize actually drives the popup window before stressing it --
      // otherwise a silent setViewportSize no-op would leave the loop below
      // exercising nothing. innerWidth must track the requested size.
      await popup.setViewportSize({ width: 640, height: 480 });
      const widthSmall = await popup.evaluate(() => window.innerWidth);
      await popup.setViewportSize({ width: 1280, height: 900 });
      const widthLarge = await popup.evaluate(() => window.innerWidth);
      expect(widthLarge).toBeGreaterThan(widthSmall);

      for (let i = 0; i < 8; i++) {
        await popup.setViewportSize({ width: 1280, height: 900 });
        await popup.setViewportSize({ width: 640, height: 480 });
        await popup.setViewportSize({ width: 960, height: 720 });
      }

      await popup.close();

      // The real point: R is still responsive after the resize storm. evalRLogical
      // reads R's "[1] TRUE" output -- a token the echoed console input can't
      // contain -- so this proves a genuine round-trip, not just an echo match.
      expect(await consoleActions.evalRLogical('TRUE')).toBe(true);
    },
  );

  // Last in the suite: a save remembers its directory for later exports
  // (ExportPlotUtils.setDefaultSaveDirectory), so choosers opened after this
  // test would start in the new folder rather than the sandbox directory.
  test('file chooser accepted before a new folder is listed saves into it', async ({ rstudioPage: page }) => {
    await createPlot(page);
    const folder = `pw-new-folder-${Date.now()}`;

    // let the chooser list its directory first: only the new folder's listing
    // is held, so the New Folder button and its mkdir run normally
    const listed = page.waitForResponse(LIST_FILES_RPC);
    const acceptButton = await openSaveAsImageChooser(page);
    await listed;
    const listing = await holdListFiles(page);
    const saves = countSaves(page);

    try {
      await page.locator(FILE_NEW_FOLDER).click();
      const prompt = page.getByRole('dialog', { name: 'New Folder' });
      await expect(prompt).toBeVisible();
      await prompt.locator(TEXT_ENTRY).fill(folder);
      // by role: the format dialog behind the chooser already owns the OK id
      await prompt.getByRole('button', { name: 'OK' }).click();

      // the folder exists and its listing is in flight; accepting now must
      // wait for it rather than resolve the filename against the parent
      await expect.poll(() => listing.held(), { timeout: TIMEOUTS.fileOpen }).toBeGreaterThan(0);
      await acceptButton.click();
      await expect(acceptButton).toBeVisible();
      expect(saves.count()).toBe(0);

      listing.release();
      await expect(acceptButton).toBeHidden({ timeout: TIMEOUTS.fileOpen });
      await expect(plotsPane.saveAsImageDialog).toBeHidden();
      expect(saves.count()).toBe(1);
      await expect.poll(
        () => consoleActions.evalRLogical(
          `length(list.files(file.path(getwd(), "${folder}"), pattern = "Rplot.*\\\\.png$")) == 1`,
        ),
        { timeout: TIMEOUTS.fileOpen },
      ).toBe(true);
    } finally {
      saves.stop();
      await listing.cleanup();
    }
  });
});
