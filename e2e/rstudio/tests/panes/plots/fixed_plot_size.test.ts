import { test, expect } from '@fixtures/rstudio.fixture';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { PlotsPane } from '@pages/plots_pane.page';
import { CONFIRM_BTN, CANCEL_BTN } from '@pages/modals.page';
import { isCommandChecked } from '@utils/commands';
import { TIMEOUTS } from '@utils/constants';
import type { Page } from 'playwright';

// Plots can be drawn at a fixed size instead of the size of the Plots pane
// (rstudio/rstudio#4422).

let consoleActions: ConsolePaneActions;
let plotsPane: PlotsPane;

// Turns the fixed size off from R. The session tells the client about the
// change, so this also resets the toolbar and the menu's checked state.
const RESET_FIXED_SIZE = 'invisible(.rs.writeUserState("fixed_plot_size", list(' +
  'enabled = .rs.scalar(FALSE), width = .rs.scalar(7), ' +
  'height = .rs.scalar(5), units = .rs.scalar("in"))))';

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

test.describe.serial('Fixed plot size', { tag: ['@serial'] }, () => {
  test.beforeAll(async ({ rstudioPage: page }) => {
    consoleActions = new ConsolePaneActions(page);
    plotsPane = new PlotsPane(page);
    await consoleActions.executeInConsole(RESET_FIXED_SIZE);
  });

  test.afterEach(async () => {
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

  test('exporting defaults to the fixed size', async ({ rstudioPage: page }) => {
    await createPlot(page);
    await openFixedSizeDialog(page);
    await plotsPane.fixedSizeWidth.fill('5');
    await plotsPane.fixedSizeHeight.fill('4');
    await page.locator(CONFIRM_BTN).click();
    await expect.poll(() => deviceSizeIs(5, 4), { timeout: TIMEOUTS.fileOpen }).toBe(true);

    await plotsPane.exportMenu.click();
    await plotsPane.saveAsImageItem.click();
    await expect(plotsPane.saveAsImageDialog).toBeVisible({ timeout: TIMEOUTS.fileOpen });
    await expect(plotsPane.saveAsImageDialog.getByLabel('Width')).toHaveValue('480');
    await expect(plotsPane.saveAsImageDialog.getByLabel('Height')).toHaveValue('384');
    await page.locator(CANCEL_BTN).click();
    await expect(plotsPane.saveAsImageDialog).toBeHidden();
  });

  test('locator() maps clicks on a scaled plot to the plot', async ({ rstudioPage: page }) => {
    await consoleActions.executeInConsole('plot(0:1, 0:1)');
    await openFixedSizeDialog(page);
    await plotsPane.fixedSizeWidth.fill('4');
    await plotsPane.fixedSizeHeight.fill('3');
    await page.locator(CONFIRM_BTN).click();
    await expect.poll(() => deviceSizeIs(4, 3), { timeout: TIMEOUTS.fileOpen }).toBe(true);

    // the plot is centered in the pane (and scaled if the pane is small), so
    // its center is only reported as the center if the click is mapped
    await expect.poll(async () => {
      const box = await plotImageBox(page);
      return box ? Math.abs(box.width / box.height - 4 / 3) < 0.02 : false;
    }, { timeout: TIMEOUTS.fileOpen }).toBe(true);
    const box = (await plotImageBox(page))!;

    await consoleActions.executeInConsole('p <- locator(1)', { wait: false });
    await expect(page.getByRole('button', { name: 'Finish' })).toBeVisible({ timeout: TIMEOUTS.fileOpen });
    await page.mouse.click(box.x + box.width / 2, box.y + box.height / 2);

    await expect.poll(() => consoleActions.evalRLogical(
      'exists("p") && abs(grconvertX(p$x, "user", "ndc") - 0.5) < 0.02 && ' +
      'abs(grconvertY(p$y, "user", "ndc") - 0.5) < 0.02',
    ), { timeout: TIMEOUTS.fileOpen }).toBe(true);
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
