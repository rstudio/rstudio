import { test, expect } from '@fixtures/rstudio.fixture';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { PlotsPane, PLOT_IMAGE_FRAME } from '@pages/plots_pane.page';
import { TIMEOUTS } from '@utils/constants';
import type { Page } from 'playwright';

// Issue 19337: the Plots pane image has alt text giving its place in the plot
// history, followed by the author's alt text, and the same text is announced
// through the polite status live region when a different plot is shown.

// Long enough to outlast the debounced announcement (typing_status_delay_ms).
const NO_ANNOUNCEMENT_WINDOW_MS = 3000;

let consoleActions: ConsolePaneActions;
let plotsPane: PlotsPane;
let missingPackages: string[] = [];

function plotImage(page: Page) {
  return page.frameLocator(PLOT_IMAGE_FRAME).locator('#img');
}

// Records every text the status live region is given, from now on. The region
// keeps its last text, so reading it can't tell a new announcement from an
// old one.
async function recordAnnouncements(page: Page): Promise<void> {
  await page.evaluate(() => {
    const w = window as unknown as { __plotAnnouncements: string[] };
    w.__plotAnnouncements = [];
    const region = document.querySelector('[role=status]');
    if (!region)
      throw new Error('No status live region found');
    new MutationObserver(() => {
      const text = (region.textContent ?? '').trim();
      if (text)
        w.__plotAnnouncements.push(text);
    }).observe(region, { childList: true, subtree: true, characterData: true });
  });
}

async function announcements(page: Page): Promise<string[]> {
  return page.evaluate(() =>
    (window as unknown as { __plotAnnouncements: string[] }).__plotAnnouncements);
}

async function clearAnnouncements(page: Page): Promise<void> {
  await page.evaluate(() => {
    (window as unknown as { __plotAnnouncements: string[] }).__plotAnnouncements = [];
  });
}

async function expectAnnounced(page: Page, text: string): Promise<void> {
  await expect.poll(() => announcements(page), { timeout: TIMEOUTS.fileOpen })
    .toContain(text);
}

// The empty starting state also passes, so watch for the whole window rather
// than returning on the first passing sample.
async function expectNoPlotAnnouncement(page: Page): Promise<void> {
  const deadline = Date.now() + NO_ANNOUNCEMENT_WINDOW_MS;
  while (Date.now() < deadline) {
    const plotAnnouncements = (await announcements(page)).filter((a) => a.startsWith('Plot '));
    expect(plotAnnouncements).toEqual([]);
    await page.waitForTimeout(250);
  }
}

test.describe.serial('Plot alt text', { tag: ['@serial'] }, () => {
  test.beforeAll(async ({ rstudioPage: page }) => {
    consoleActions = new ConsolePaneActions(page);
    plotsPane = new PlotsPane(page);
    missingPackages = await consoleActions.ensurePackages(['ggplot2']);
    await consoleActions.executeInConsole('try(while (dev.cur() > 1) dev.off(), silent = TRUE)');
    await recordAnnouncements(page);
  });

  test.beforeEach(async ({ rstudioPage: page }) => {
    // every test builds on the plot history the ggplot2 test starts
    test.skip(missingPackages.length > 0, `Missing: ${missingPackages.join(', ')}`);
    await clearAnnouncements(page);
  });

  test.afterAll(async () => {
    await consoleActions.executeInConsole('.rs.removePref("disabled_aria_live_announcements")');
    await consoleActions.executeInConsole('try(while (dev.cur() > 1) dev.off(), silent = TRUE)');
  });

  test('a ggplot2 plot uses its author alt text', async ({ rstudioPage: page }) => {
    await consoleActions.executeInConsole(
      'print(ggplot2::ggplot(mtcars, ggplot2::aes(mpg, wt)) + ggplot2::geom_point() + ' +
      'ggplot2::labs(alt = "Weight falls as fuel economy rises"))');

    const expected = 'Plot 1 of 1: Weight falls as fuel economy rises';
    await expect(plotImage(page)).toHaveAttribute('alt', expected, { timeout: TIMEOUTS.fileOpen });
    await expectAnnounced(page, expected);
  });

  test('a later base plot does not inherit the earlier alt text', async ({ rstudioPage: page }) => {
    await consoleActions.executeInConsole('plot(1:10)');

    await expect(plotImage(page)).toHaveAttribute('alt', 'Plot 2 of 2', { timeout: TIMEOUTS.fileOpen });
    await expectAnnounced(page, 'Plot 2 of 2');
  });

  test('moving through the plot history announces the plot shown', async ({ rstudioPage: page }) => {
    await plotsPane.tab.click();
    await plotsPane.previousPlotBtn.click();

    const expected = 'Plot 1 of 2: Weight falls as fuel economy rises';
    await expect(plotImage(page)).toHaveAttribute('alt', expected, { timeout: TIMEOUTS.fileOpen });
    await expectAnnounced(page, expected);
  });

  test('redrawing the same plot is not announced', async ({ rstudioPage: page }) => {
    const img = plotImage(page);
    const src = await img.getAttribute('src');

    await plotsPane.tab.click();
    await plotsPane.refreshPlotBtn.click();

    // the refresh draws a new image, so this proves a new plots state arrived
    await expect(img).not.toHaveAttribute('src', src ?? '', { timeout: TIMEOUTS.fileOpen });
    await expect(img).toHaveAttribute('alt', 'Plot 1 of 2: Weight falls as fuel economy rises');
    await expectNoPlotAnnouncement(page);
  });

  // e.g. a ggplot2 plot whose build is slow enough for the blank page to be
  // shown first
  test('alt text drawn after the plot is shown is announced', async ({ rstudioPage: page }) => {
    await consoleActions.executeInConsole('plot(1:3)');
    await expect(plotImage(page)).toHaveAttribute('alt', 'Plot 3 of 3', { timeout: TIMEOUTS.fileOpen });
    await expectAnnounced(page, 'Plot 3 of 3');

    await consoleActions.executeInConsole(
      'grid::grid.draw(structure(grid::rectGrob(width = 0.1, height = 0.1), `alt-label` = "Three points"))');

    const expected = 'Plot 3 of 3: Three points';
    await expect(plotImage(page)).toHaveAttribute('alt', expected, { timeout: TIMEOUTS.fileOpen });
    await expectAnnounced(page, expected);
  });

  test('the announcement can be turned off', async ({ rstudioPage: page }) => {
    await consoleActions.executeInConsole(
      '.rs.writeUiPref("disabled_aria_live_announcements", list("plot_changed"))');
    await consoleActions.executeInConsole('plot(1:5)');

    await expect(plotImage(page)).toHaveAttribute('alt', 'Plot 4 of 4', { timeout: TIMEOUTS.fileOpen });
    await expectNoPlotAnnouncement(page);
  });
});
