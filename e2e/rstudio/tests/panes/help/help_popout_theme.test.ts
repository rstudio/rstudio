import { test, expect } from '@fixtures/rstudio.fixture';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { executeCommand } from '@utils/commands';
import type { Page } from '@playwright/test';

// Regression test for issue #8345: the Help pane's "Show in new window"
// button used to open the raw help URL in a plain browser window, so none of
// the IDE's theming was applied and the page always rendered with R's default
// light palette. The popout now opens a satellite window hosting the help
// page in an RStudioThemedFrame, which themes the content like the Help pane
// and re-themes it live when the editor theme changes.

// 'Cobalt' ships with RStudio and is a dark theme; its stylesheet href always
// contains "cobalt" when active. 'Textmate (default)' is the light default.
const DARK_THEME = 'Cobalt';
const LIGHT_THEME = 'Textmate (default)';

// ID on the <link> element AceThemes.java injects to apply the theme CSS,
// both in top-level windows and (via RStudioThemedFrame) in themed iframes.
const ACE_THEME_LINK = '#rstudio-acethemes-linkelement';

// The satellite hosts the help page in an iframe titled "Help"
// (HelpPopoutPanel.java).
const POPOUT_HELP_FRAME = 'iframe[title="Help"]';

let consoleActions: ConsolePaneActions;

async function applyEditorTheme(page: Page, theme: string) {
  await consoleActions.executeInConsole(`.rs.api.applyTheme(${JSON.stringify(theme)})`, {
    wait: true,
  });
}

/** Relative luminance (0 = black, 255 = white) of a CSS rgb()/rgba() color. */
function luminance(cssColor: string): number {
  const match = cssColor.match(/rgba?\((\d+),\s*(\d+),\s*(\d+)/);
  if (!match)
    throw new Error(`unexpected color format: ${cssColor}`);
  const [r, g, b] = [Number(match[1]), Number(match[2]), Number(match[3])];
  return 0.2126 * r + 0.7152 * g + 0.0722 * b;
}

test.describe.serial('Help popout window theming', () => {
  let satellitePage: Page | undefined;

  test.beforeAll(async ({ rstudioPage: page }) => {
    consoleActions = new ConsolePaneActions(page);

    // remember the active editor theme R-side so afterAll can restore it
    await consoleActions.executeInConsole(
      '.rstudio.e2e.popoutThemeOrig <- .rs.api.getThemeInfo()$editor',
      { wait: true },
    );
  });

  test.afterAll(async () => {
    await satellitePage?.close().catch(() => undefined);

    await consoleActions
      .executeInConsole(
        `if (exists(".rstudio.e2e.popoutThemeOrig")) .rs.api.applyTheme(.rstudio.e2e.popoutThemeOrig) else .rs.api.applyTheme(${JSON.stringify(LIGHT_THEME)})`,
        { wait: true },
      )
      .catch((err) => {
        console.warn(`[help_popout_theme] theme restore failed: ${(err as Error).message}`);
      });
  });

  test('popped-out help page follows the editor theme', async ({ rstudioPage: page }) => {
    await applyEditorTheme(page, DARK_THEME);
    await expect
      .poll(async () =>
        page.evaluate((id) => document.querySelector(id)?.getAttribute('href') ?? '', ACE_THEME_LINK),
      )
      .toContain('cobalt');

    // open a help topic in the Help pane
    await consoleActions.executeInConsole('help("print")', { wait: true });
    const paneFrame = page.frameLocator('#rstudio_help_frame');
    await expect(paneFrame.locator('body')).toContainText('print', { timeout: 15000 });

    // pop the help page out into a satellite window
    const satellitePromise = page.context().waitForEvent('page', { timeout: 30000 });
    await executeCommand(page, 'helpPopout');
    satellitePage = await satellitePromise;
    await satellitePage.waitForLoadState('domcontentloaded');
    expect(satellitePage.url()).toContain('view=help_popout_');

    // the satellite's help iframe should be themed like the Help pane:
    // theme stylesheet injected, theming class applied, dark background
    const helpFrame = satellitePage.frameLocator(POPOUT_HELP_FRAME);
    await expect(helpFrame.locator('body')).toContainText('print', { timeout: 30000 });
    await expect
      .poll(
        () => helpFrame.locator(ACE_THEME_LINK).getAttribute('href').catch(() => ''),
        { timeout: 15000 },
      )
      .toContain('cobalt');
    await expect(helpFrame.locator('body.ace_editor_theme')).toBeVisible({ timeout: 15000 });
    await expect
      .poll(async () => {
        const color = await helpFrame
          .locator('body')
          .evaluate((el) => getComputedStyle(el).backgroundColor);
        return luminance(color);
      }, { timeout: 15000 })
      .toBeLessThan(128);
  });

  test('open popout re-themes when the editor theme changes', async ({ rstudioPage: page }) => {
    test.skip(satellitePage === undefined, 'popout satellite not opened');

    // switch back to a light theme in the main window; the satellite listens
    // for the change and should re-theme the already-loaded help page
    await applyEditorTheme(page, LIGHT_THEME);

    const helpFrame = satellitePage!.frameLocator(POPOUT_HELP_FRAME);
    await expect
      .poll(async () => {
        const color = await helpFrame
          .locator('body')
          .evaluate((el) => getComputedStyle(el).backgroundColor);
        return luminance(color);
      }, { timeout: 15000 })
      .toBeGreaterThan(128);
  });
});
