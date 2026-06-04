import { test, expect } from '@fixtures/rstudio.fixture';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { PlotsPane } from '@pages/plots_pane.page';
import { CONFIRM_BTN, CANCEL_BTN, YES_BTN } from '@pages/modals.page';
import { useSuiteSandbox } from '@utils/sandbox';
import { TIMEOUTS } from '@utils/constants';
import type { Page } from 'playwright';

// GWT file-chooser accept button. base-prefs.jsonc sets native_file_dialogs=false
// suite-wide, so export flows always use the GWT file chooser (never a native OS
// dialog). The two-step save flow is: OK on the format dialog, then this button
// on the file chooser to accept the default path.
const FILE_ACCEPT_SAVE = '#rstudio_file_accept_save';

// Sandbox working directory for file-export tests so saved files are cleaned
// up by globalTeardown automatically.
useSuiteSandbox();

let consoleActions: ConsolePaneActions;
let plotsPane: PlotsPane;

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
    // confirm a successful save.
    await expect.poll(
      () => consoleActions.evalRLogical(
        `length(list.files(getwd(), pattern = "Rplot")) > 0`,
      ),
      { timeout: TIMEOUTS.fileOpen },
    ).toBe(true);
  });

  test('save plot as PDF dialog opens and accepts the save', async ({ rstudioPage: page }) => {
    await createPlot(page);

    await plotsPane.exportMenu.click();
    await plotsPane.saveAsPdfItem.click();
    await expect(plotsPane.saveAsPdfDialog).toBeVisible({ timeout: TIMEOUTS.fileOpen });
    await page.locator(CONFIRM_BTN).click();
    await page.locator(FILE_ACCEPT_SAVE).click();
    await expect(plotsPane.saveAsPdfDialog).toBeHidden();

    await expect.poll(
      () => consoleActions.evalRLogical(
        `length(list.files(getwd(), pattern = "Rplot")) > 0`,
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

      for (let i = 0; i < 8; i++) {
        await popup.setViewportSize({ width: 1280, height: 900 });
        await popup.setViewportSize({ width: 640, height: 480 });
        await popup.setViewportSize({ width: 960, height: 720 });
      }

      await popup.close();

      // The real point: R is still responsive after the resize storm.
      await consoleActions.clearConsole();
      await consoleActions.executeInConsole("cat('alive')");
      await expect(consoleActions.consolePane.consoleOutput).toContainText('alive');
    },
  );
});
