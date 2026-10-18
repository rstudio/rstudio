// Files pane breadcrumb fade (#19172).
//
// When the path is wider than the breadcrumb bar, the bar scrolls to show the
// current folder and a fade image marks its left edge. The fade had no alt
// text, and it was only placed when the widget scrolled itself: scrolling the
// bar any other way (e.g. by focusing the root link) left it shown, covering
// the root link and taking its clicks. Dark file dialogs hide the fade
// altogether, which an inline display style used to override.

import { test, expect } from '@fixtures/rstudio.fixture';

import { ConsolePaneActions } from '@actions/console_pane.actions';
import { executeCommand, getPref } from '@utils/commands';
import { DARK_THEME, DARK_THEME_HREF, LIGHT_THEME, expectThemeStylesheet } from '@utils/theme';

const DEEP_DIR = 'file.path(tempdir(), "breadcrumb_fade_19172", '
  + 'strrep("a_long_directory_name_", 3), strrep("another_long_directory_name_", 3))';

test.describe('Files pane breadcrumb fade (#19172)', () => {
  test.afterEach(async ({ rstudioPage: page }) => {
    const consolePane = new ConsolePaneActions(page);
    await consolePane.executeInConsole('.rs.api.filesPaneNavigate(path.expand("~"))', { wait: true });
    await consolePane.executeInConsole(
      'unlink(file.path(tempdir(), "breadcrumb_fade_19172"), recursive = TRUE)',
      { wait: true },
    );
  });

  test('fade is decorative and only covers the bar while it is scrolled', async ({ rstudioPage: page }) => {
    const consolePane = new ConsolePaneActions(page);
    await consolePane.executeInConsole(
      `invisible(dir.create(${DEEP_DIR}, recursive = TRUE)); .rs.api.filesPaneNavigate(${DEEP_DIR})`,
      { wait: true },
    );
    await executeCommand(page, 'activateFiles');

    const breadcrumb = page.locator('#rstudio_workbench_panel_files .breadcrumb');
    const lastCrumb = breadcrumb.locator('[aria-current="location"]');
    await expect(lastCrumb).toHaveText(/^another_long_directory_name_/);

    const fadeImage = breadcrumb.locator('img');
    await expect(fadeImage).toHaveAttribute('alt', '');
    // Dark editor themes hide the image and fade with the wrapper's own
    // background, so check the wrapper, which is shown in every theme.
    const fade = fadeImage.locator('..');

    // Scrolled to the current folder: the fade sits at the visible left edge.
    await expect.poll(() => breadcrumb.evaluate((el) => el.scrollLeft)).toBeGreaterThan(0);
    await expect(fade).toBeVisible();
    const barBox = await breadcrumb.boundingBox();
    const fadeBox = await fade.boundingBox();
    expect(Math.abs(fadeBox!.x - barBox!.x)).toBeLessThanOrEqual(1);

    // Focusing the root link scrolls the bar back to the start, where the
    // root link must be uncovered and take clicks.
    const rootCrumb = breadcrumb.getByRole('link').first();
    await rootCrumb.focus();
    await expect.poll(() => breadcrumb.evaluate((el) => el.scrollLeft)).toBe(0);
    await expect(fade).toBeHidden();
    const rootIsHit = await rootCrumb.evaluate((el) => {
      const r = el.getBoundingClientRect();
      return el.contains(document.elementFromPoint(r.x + r.width / 2, r.y + r.height / 2));
    });
    expect(rootIsHit).toBe(true);
  });

  // Desktop opens native file dialogs, which have no breadcrumb.
  test('fade stays hidden in a dark file dialog', { tag: ['@server_only'] },
    async ({ rstudioPage: page }) => {
      const consolePane = new ConsolePaneActions(page);
      const originalTheme = (await getPref(page, 'editor_theme')) as string | null;
      const dialog = page.locator('.gwt-DialogBox.rstudio-themes-dark');
      try {
        await consolePane.executeInConsole(
          `invisible(dir.create(${DEEP_DIR}, recursive = TRUE)); .rs.api.applyTheme(${JSON.stringify(DARK_THEME)})`,
          { wait: true },
        );
        await expectThemeStylesheet(page, DARK_THEME_HREF);

        // selectFile() blocks R until the dialog closes.
        await consolePane.executeInConsole(
          `.rs.api.selectFile(path = paste0(${DEEP_DIR}, "/"))`,
          { wait: false },
        );
        const breadcrumb = dialog.locator('.breadcrumb');
        await expect(breadcrumb.locator('[aria-current="location"]'))
          .toHaveText(/^another_long_directory_name_/);
        await expect.poll(() => breadcrumb.evaluate((el) => el.scrollLeft)).toBeGreaterThan(0);
        await expect(breadcrumb.locator('img').locator('..')).toBeHidden();
      } finally {
        // Cancel answers selectFile(), freeing the console; dismissing the
        // dialog any other way would leave R waiting on it.
        const cancel = dialog.getByRole('button', { name: 'Cancel', exact: true });
        if (await cancel.isVisible()) {
          await cancel.click();
          await expect(dialog).toBeHidden();
        }
        // applyTheme, not setPref: a live session is not re-themed off the pref.
        await consolePane.executeInConsole(
          `.rs.api.applyTheme(${JSON.stringify(originalTheme ?? LIGHT_THEME)})`,
          { wait: true },
        );
      }
    });
});
