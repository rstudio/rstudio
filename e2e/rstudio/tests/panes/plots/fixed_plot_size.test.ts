import { test, expect } from '@fixtures/rstudio.fixture';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { PlotsPane } from '@pages/plots_pane.page';
import { CONFIRM_BTN, CANCEL_BTN } from '@pages/modals.page';
import { isCommandChecked } from '@utils/commands';
import { TIMEOUTS } from '@utils/constants';
import { useSuiteSandbox } from '@utils/sandbox';
import type { Page } from 'playwright';

// Plots can be drawn at a fixed size instead of the size of the Plots pane
// (rstudio/rstudio#4422).

// GWT file chooser's accept button (see plots_pane.test.ts)
const FILE_ACCEPT_SAVE = '#rstudio_file_accept_save';

// saved images land in the sandbox working directory, which is cleaned up
useSuiteSandbox();

let consoleActions: ConsolePaneActions;
let plotsPane: PlotsPane;

// R code that writes the fixed size, in inches. The session tells the client
// about the change, so this also updates the toolbar and the menu's checked
// state.
function writeFixedSize(enabled: boolean, width: number, height: number): string {
  return 'invisible(.rs.writeUserState("fixed_plot_size", list(' +
    `enabled = .rs.scalar(${enabled ? 'TRUE' : 'FALSE'}), width = .rs.scalar(${width}), ` +
    `height = .rs.scalar(${height}), units = .rs.scalar("in"))))`;
}

const RESET_FIXED_SIZE = writeFixedSize(false, 7, 5);

async function createPlot(page: Page): Promise<void> {
  await consoleActions.executeInConsole('plot(1:10)');
  await plotsPane.tab.click();
  await expect(plotsPane.plotImage).toBeVisible({ timeout: TIMEOUTS.fileOpen });
}

async function deviceSizeIs(width: number, height: number): Promise<boolean | null> {
  return consoleActions.evalRLogical(
    `isTRUE(all.equal(dev.size("in"), c(${width}, ${height})))`,
  );
}

// Accepts the GWT file chooser at its default path. The chooser lists its
// directory asynchronously, and accepting before the listing arrives closes
// it without saving anything, so wait for its breadcrumb first.
async function acceptSaveFileDialog(page: Page): Promise<void> {
  const currentDirectory = page.locator('.gwt-DialogBox [aria-current="location"]');
  await expect(currentDirectory).toBeVisible({ timeout: TIMEOUTS.fileOpen });
  await page.locator(FILE_ACCEPT_SAVE).click();
}

async function openFixedSizeDialog(page: Page): Promise<void> {
  // the size menu is on the Plots pane's toolbar, visible only when it's selected
  await plotsPane.tab.click();
  await plotsPane.sizeMenu.click();
  await plotsPane.fixedSizeItem.click();
  await expect(plotsPane.fixedSizeDialog).toBeVisible({ timeout: TIMEOUTS.fileOpen });
}

// Bounding box of the plot image inside the Plots pane's iframe.
async function plotImageBox(page: Page) {
  const img = page.frameLocator('#rstudio_plot_image_frame').locator('#img');
  return img.boundingBox();
}

// Waits for the pane to show a fixed-size plot with the given aspect ratio,
// and returns its bounding box.
async function fixedPlotImageBox(page: Page, aspect: number) {
  await expect.poll(async () => {
    const box = await plotImageBox(page);
    return box ? Math.abs(box.width / box.height - aspect) < 0.02 : false;
  }, { timeout: TIMEOUTS.fileOpen }).toBe(true);
  return (await plotImageBox(page))!;
}

// Whether the zoom window shows a plot of the given width, rendered with as
// many pixels as it's displayed at: no more, and no fewer. The window works
// out the pixel ratio from its own devicePixelRatio (which can differ from
// the main window's on a mixed-DPI setup).
async function zoomRenderedAsShown(popup: Page, plotWidth: number): Promise<boolean> {
  return popup.evaluate((plotWidth) => {
    const img = document.getElementById('plot') as HTMLImageElement | null;
    if (!img || !img.complete || !img.naturalWidth || !img.src.includes(`width=${plotWidth}&`))
      return false;
    return Math.abs(img.naturalWidth - img.clientWidth * window.devicePixelRatio) <= 2;
  }, plotWidth).catch(() => false);
}

test.describe.serial('Fixed plot size', { tag: ['@serial'] }, () => {
  test.beforeAll(async ({ rstudioPage: page }) => {
    consoleActions = new ConsolePaneActions(page);
    plotsPane = new PlotsPane(page);
    await consoleActions.executeInConsole(RESET_FIXED_SIZE);
  });

  test.afterEach(async ({ rstudioPage: page }) => {
    // a locator() test that fails mid-click leaves R waiting for a click,
    // and the console reset below would wait for a prompt that never comes
    const finish = page.getByRole('button', { name: 'Finish' });
    if (await finish.isVisible())
      await finish.click();

    await consoleActions.executeInConsole(RESET_FIXED_SIZE);
    await consoleActions.executeInConsole('try(while (dev.cur() > 1) dev.off(), silent = TRUE)');
  });

  test('plots follow the pane by default', async ({ rstudioPage: page }) => {
    await createPlot(page);

    await expect(plotsPane.sizeMenu).toContainText('Fit to Pane');
    expect(await isCommandChecked(page, 'fitPlotToPane')).toBe(true);
    expect(await isCommandChecked(page, 'useFixedPlotSize')).toBe(false);
  });

  test('a fixed size draws the plot at that size, scaled to fit the pane', async ({
    rstudioPage: page,
  }) => {
    await createPlot(page);

    await openFixedSizeDialog(page);
    await plotsPane.fixedSizeWidth.fill('4');
    await plotsPane.fixedSizeHeight.fill('3');
    await page.locator(CONFIRM_BTN).click();
    await expect(plotsPane.fixedSizeDialog).toBeHidden();

    await expect(plotsPane.sizeMenu).toContainText('4 x 3 in');
    await expect.poll(() => deviceSizeIs(4, 3), { timeout: TIMEOUTS.fileOpen }).toBe(true);
    expect(await isCommandChecked(page, 'useFixedPlotSize')).toBe(true);
    expect(await isCommandChecked(page, 'fitPlotToPane')).toBe(false);

    // the image keeps the plot's aspect ratio rather than filling the pane,
    // and is never drawn larger than the plot
    await expect.poll(async () => {
      const box = await plotImageBox(page);
      return box ? Math.abs(box.width / box.height - 4 / 3) < 0.02 : false;
    }, { timeout: TIMEOUTS.fileOpen }).toBe(true);
    const box = await plotImageBox(page);
    expect(box!.width).toBeLessThanOrEqual(4 * 96 + 1);

    // resizing the pane leaves the device alone; wait for the new pane size
    // to reach the session before checking
    const viewport = page.viewportSize();
    if (viewport) {
      const metricsSent = page.waitForResponse(
        (r) => r.url().includes('set_workbench_metrics'),
        { timeout: TIMEOUTS.fileOpen },
      );
      await page.setViewportSize({ width: viewport.width - 200, height: viewport.height - 100 });
      await metricsSent;
      expect(await deviceSizeIs(4, 3)).toBe(true);
      await page.setViewportSize(viewport);
    }
  });

  test('changing units in the dialog keeps the size', async ({ rstudioPage: page }) => {
    await openFixedSizeDialog(page);
    await plotsPane.fixedSizeWidth.fill('4');
    await plotsPane.fixedSizeHeight.fill('2');

    await plotsPane.fixedSizeUnits.selectOption('cm');
    await expect(plotsPane.fixedSizeWidth).toHaveValue('10.16');
    await expect(plotsPane.fixedSizeHeight).toHaveValue('5.08');

    await plotsPane.fixedSizeUnits.selectOption('px');
    await expect(plotsPane.fixedSizeWidth).toHaveValue('384');
    await expect(plotsPane.fixedSizeHeight).toHaveValue('192');

    await page.locator(CONFIRM_BTN).click();
    await expect(plotsPane.sizeMenu).toContainText('384 x 192 px');

    await createPlot(page);
    await expect.poll(() => deviceSizeIs(4, 2), { timeout: TIMEOUTS.fileOpen }).toBe(true);
  });

  test('sizes outside the supported range are rejected', async ({ rstudioPage: page }) => {
    await openFixedSizeDialog(page);
    await plotsPane.fixedSizeWidth.fill('0.5');
    await plotsPane.fixedSizeHeight.fill('3');
    await page.locator(CONFIRM_BTN).click();

    // the error message is shown over the dialog, which stays open
    const error = page.getByRole('alertdialog', { name: 'Invalid Plot Size' })
      .or(page.getByRole('dialog', { name: 'Invalid Plot Size' }));
    await expect(error).toBeVisible();
    await error.getByRole('button', { name: 'OK' }).click();
    await expect(plotsPane.fixedSizeDialog).toBeVisible();

    await page.locator(CANCEL_BTN).click();
    await expect(plotsPane.fixedSizeDialog).toBeHidden();
    await expect(plotsPane.sizeMenu).toContainText('Fit to Pane');
    expect(await isCommandChecked(page, 'fitPlotToPane')).toBe(true);
  });

  test('the toolbar shows the size the session applies', async ({ rstudioPage: page }) => {
    await plotsPane.tab.click();

    // a saved size outside the supported range is clamped
    await consoleActions.executeInConsole(writeFixedSize(true, 100, 0.5));
    await expect(plotsPane.sizeMenu).toContainText('30 x 1 in');
    await expect.poll(() => deviceSizeIs(30, 1), { timeout: TIMEOUTS.fileOpen }).toBe(true);

    // a saved size the session can't read isn't applied
    await consoleActions.executeInConsole(
      'invisible(.rs.writeUserState("fixed_plot_size", list(enabled = .rs.scalar(TRUE), ' +
      'width = .rs.scalar(4), height = .rs.scalar(3), units = .rs.scalar(1))))',
    );
    await expect(plotsPane.sizeMenu).toContainText('Fit to Pane');
    await expect.poll(() => deviceSizeIs(30, 1), { timeout: TIMEOUTS.fileOpen }).toBe(false);
    expect(await isCommandChecked(page, 'fitPlotToPane')).toBe(true);
    expect(await isCommandChecked(page, 'useFixedPlotSize')).toBe(false);
  });

  test('exporting defaults to the fixed size', async ({ rstudioPage: page }) => {
    await createPlot(page);
    await openFixedSizeDialog(page);
    // larger than the test window, which the export dialogs used to clamp to
    await plotsPane.fixedSizeWidth.fill('15');
    await plotsPane.fixedSizeHeight.fill('20');
    await page.locator(CONFIRM_BTN).click();
    await expect.poll(() => deviceSizeIs(15, 20), { timeout: TIMEOUTS.fileOpen }).toBe(true);

    // Save as Image keeps the fixed size, and hides the preview it can't show
    await plotsPane.exportMenu.click();
    await plotsPane.saveAsImageItem.click();
    await expect(plotsPane.saveAsImageDialog).toBeVisible({ timeout: TIMEOUTS.fileOpen });
    await expect(plotsPane.saveAsImageDialog.getByLabel('Width')).toHaveValue('1440');
    await expect(plotsPane.saveAsImageDialog.getByLabel('Height')).toHaveValue('1920');
    await expect(plotsPane.saveAsImageDialog.locator('iframe')).toBeHidden();

    // the dialog warns when the image would be over the session's 100
    // megapixel limit, which 15 x 20 in reaches at 600 DPI
    const sizeText = plotsPane.saveAsImageDialog.locator('#rstudio_export_plot_size_text');
    await plotsPane.saveAsImageDialog.locator('#rstudio_export_plot_resolution').selectOption('600');
    await expect(sizeText).toHaveText(
      '15 x 20 in, 9000 x 12000 pixels (too large to save; reduce the size or resolution)',
    );

    // saving anyway fails with the session's advice, not a system error code
    await page.locator(CONFIRM_BTN).click();
    await acceptSaveFileDialog(page);
    const errorDialog = page.locator('.gwt-DialogBox[aria-label="Error"]');
    await expect(errorDialog).toBeVisible({ timeout: TIMEOUTS.fileOpen });
    await expect(errorDialog).toContainText(
      'The image would be 9000 x 12000 pixels; reduce its size or resolution',
    );
    await errorDialog.getByRole('button', { name: 'OK' }).click();
    await expect(errorDialog).toBeHidden();

    await plotsPane.saveAsImageDialog.locator('#rstudio_export_plot_resolution').selectOption('300');
    await expect(sizeText).toHaveText('15 x 20 in, 4500 x 6000 pixels');
    await page.locator(CANCEL_BTN).click();
    await expect(plotsPane.saveAsImageDialog).toBeHidden();

    // Copy to Clipboard copies from its preview, so its size is limited to
    // the window (see ExportPlotSizeEditor.getMaxSize)
    const maxSize = await page.evaluate(() => ({
      width: document.documentElement.clientWidth - 100,
      height: document.documentElement.clientHeight - 200,
    }));
    await plotsPane.exportMenu.click();
    await plotsPane.copyToClipboardItem.click();
    await expect(plotsPane.copyToClipboardDialog).toBeVisible({ timeout: TIMEOUTS.fileOpen });
    const width = Number(await plotsPane.copyToClipboardDialog.getByLabel('Width').inputValue());
    const height = Number(await plotsPane.copyToClipboardDialog.getByLabel('Height').inputValue());
    expect(width).toBeLessThan(1440);
    expect(width).toBeLessThanOrEqual(maxSize.width);
    expect(height).toBeLessThan(1920);
    expect(height).toBeLessThanOrEqual(maxSize.height);
    await expect(plotsPane.copyToClipboardDialog.locator('iframe')).toBeVisible();
    await page.locator(CANCEL_BTN).click();
    await expect(plotsPane.copyToClipboardDialog).toBeHidden();
  });

  // The plots in these tests are portrait, which the pane isn't, so that
  // their aspect ratio shows the pane has laid them out.

  test('locator() ignores clicks beside a centered plot', async ({ rstudioPage: page }) => {
    // the default margins don't fit in a 1 in wide plot
    await consoleActions.executeInConsole('par(mar = rep(0, 4)); plot(0:1, 0:1)');
    await consoleActions.executeInConsole(writeFixedSize(true, 1, 1.5));
    await expect.poll(() => deviceSizeIs(1, 1.5), { timeout: TIMEOUTS.fileOpen }).toBe(true);

    // the plot is shown at its own size, centered, with room beside it
    const box = await fixedPlotImageBox(page, 1 / 1.5);
    const frame = (await plotsPane.plotImage.boundingBox())!;
    expect(Math.abs(box.width - 96)).toBeLessThanOrEqual(1);
    expect(box.x - frame.x).toBeGreaterThan(20);

    // the click beside the plot is ignored, so only the second one is
    // reported, as the plot's center
    await consoleActions.executeInConsole('p <- locator(1)', { wait: false });
    await expect(page.getByRole('button', { name: 'Finish' })).toBeVisible({ timeout: TIMEOUTS.fileOpen });
    await page.mouse.click(box.x - 10, box.y + box.height / 2);
    await page.mouse.click(box.x + box.width / 2, box.y + box.height / 2);

    await expect.poll(() => consoleActions.evalRLogical(
      'exists("p") && abs(grconvertX(p$x, "user", "ndc") - 0.5) < 0.02 && ' +
      'abs(grconvertY(p$y, "user", "ndc") - 0.5) < 0.02',
    ), { timeout: TIMEOUTS.fileOpen }).toBe(true);
  });

  test('locator() maps clicks on a scaled-down plot to the plot', async ({ rstudioPage: page }) => {
    await consoleActions.executeInConsole('plot(0:1, 0:1)');
    await consoleActions.executeInConsole(writeFixedSize(true, 15, 20));
    await expect.poll(() => deviceSizeIs(15, 20), { timeout: TIMEOUTS.fileOpen }).toBe(true);

    // the plot is larger than the pane, so it's scaled down to fit
    const box = await fixedPlotImageBox(page, 15 / 20);
    expect(box.height).toBeLessThan(20 * 96);

    // a quarter of the way in from the left and from the bottom
    await consoleActions.executeInConsole('p <- locator(1)', { wait: false });
    await expect(page.getByRole('button', { name: 'Finish' })).toBeVisible({ timeout: TIMEOUTS.fileOpen });
    await page.mouse.click(box.x + box.width * 0.25, box.y + box.height * 0.75);

    await expect.poll(() => consoleActions.evalRLogical(
      'exists("p") && abs(grconvertX(p$x, "user", "ndc") - 0.25) < 0.02 && ' +
      'abs(grconvertY(p$y, "user", "ndc") - 0.25) < 0.02',
    ), { timeout: TIMEOUTS.fileOpen }).toBe(true);
  });

  test(
    'the zoom window renders a fixed-size plot at the size it shows it',
    { tag: ['@desktop_only'] },
    async ({ rstudioPage: page }) => {
      await createPlot(page);
      await consoleActions.executeInConsole(writeFixedSize(true, 15, 20));
      await expect.poll(() => deviceSizeIs(15, 20), { timeout: TIMEOUTS.fileOpen }).toBe(true);

      const [popup] = await Promise.all([
        page.context().waitForEvent('page'),
        plotsPane.zoomPlotBtn.click(),
      ]);
      await expect(popup.locator('#plot')).toBeVisible({ timeout: TIMEOUTS.fileOpen });

      // resizing reloads the window at its new size, which is smaller than
      // the 1440 x 1920 px plot
      await popup.setViewportSize({ width: 480, height: 640 });
      await expect.poll(() => zoomRenderedAsShown(popup, 15 * 96), {
        timeout: TIMEOUTS.fileOpen,
      }).toBe(true);

      // a smaller plot is enlarged to fit the window, and rendered sharp
      await consoleActions.executeInConsole(writeFixedSize(true, 1.5, 2));
      await expect.poll(() => zoomRenderedAsShown(popup, 1.5 * 96), {
        timeout: TIMEOUTS.fileOpen,
      }).toBe(true);

      await popup.close();
    },
  );

  test('saving an image at a resolution keeps its size in inches', async ({ rstudioPage: page }) => {
    await createPlot(page);
    await openFixedSizeDialog(page);
    await plotsPane.fixedSizeWidth.fill('4');
    await plotsPane.fixedSizeHeight.fill('3');
    await page.locator(CONFIRM_BTN).click();
    await expect.poll(() => deviceSizeIs(4, 3), { timeout: TIMEOUTS.fileOpen }).toBe(true);

    await plotsPane.exportMenu.click();
    await plotsPane.saveAsImageItem.click();
    const dialog = plotsPane.saveAsImageDialog;
    await expect(dialog).toBeVisible({ timeout: TIMEOUTS.fileOpen });

    const resolution = dialog.locator('#rstudio_export_plot_resolution');
    const sizeText = dialog.locator('#rstudio_export_plot_size_text');
    await resolution.selectOption('300');
    await expect(sizeText).toHaveText('4 x 3 in, 1200 x 900 pixels');

    // vector formats have no resolution
    const format = dialog.getByLabel('Image format:');
    await format.selectOption('svg');
    await expect(resolution).toBeDisabled();
    await expect(sizeText).toHaveText('4 x 3 in');
    await format.selectOption('png');
    await expect(resolution).toBeEnabled();

    await page.locator(CONFIRM_BTN).click();
    await acceptSaveFileDialog(page);
    await expect(dialog).toBeHidden();

    // the newest saved PNG is 1200 x 900 pixels and records 300 DPI, so it
    // is inserted into other documents at 4 x 3 inches
    const checkSavedImage = 'local({' +
      'f <- list.files(getwd(), pattern = "^Rplot.*[.]png$", full.names = TRUE); ' +
      'if (!length(f)) return(FALSE); ' +
      'b <- readBin(f[which.max(file.mtime(f))], "raw", 1e7); ' +
      'n <- function(i) sum(as.integer(b[i:(i + 3)]) * 256^(3:0)); ' +
      'p <- grepRaw("pHYs", b); ' +
      'n(17) == 1200 && n(21) == 900 && length(p) == 1 && round(n(p + 4) * 0.0254) == 300' +
      '})';
    await expect.poll(() => consoleActions.evalRLogical(checkSavedImage), {
      timeout: TIMEOUTS.fileOpen,
    }).toBe(true);
  });

  test('Fit to Pane draws plots at the size of the pane again', async ({ rstudioPage: page }) => {
    await createPlot(page);
    await openFixedSizeDialog(page);
    await plotsPane.fixedSizeWidth.fill('4');
    await plotsPane.fixedSizeHeight.fill('3');
    await page.locator(CONFIRM_BTN).click();
    await expect.poll(() => deviceSizeIs(4, 3), { timeout: TIMEOUTS.fileOpen }).toBe(true);

    await plotsPane.sizeMenu.click();
    await plotsPane.fitToPaneItem.click();

    await expect(plotsPane.sizeMenu).toContainText('Fit to Pane');
    await expect.poll(() => deviceSizeIs(4, 3), { timeout: TIMEOUTS.fileOpen }).toBe(false);

    // the image fills the pane again
    await expect.poll(async () => {
      const box = await plotImageBox(page);
      const frame = await plotsPane.plotImage.boundingBox();
      return box && frame ? Math.abs(box.width - frame.width) <= 1 : false;
    }, { timeout: TIMEOUTS.fileOpen }).toBe(true);
  });
});
