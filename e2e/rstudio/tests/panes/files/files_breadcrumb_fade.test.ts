// Files pane breadcrumb fade (#19172).
//
// When the path is wider than the breadcrumb bar, the bar scrolls to show the
// current folder and a fade image marks its left edge. The fade had no alt
// text, and it was only placed when the widget scrolled itself: scrolling the
// bar any other way (e.g. by focusing the root link) left it shown, covering
// the root link and taking its clicks.

import { test, expect } from '@fixtures/rstudio.fixture';

import { ConsolePaneActions } from '@actions/console_pane.actions';
import { executeCommand } from '@utils/commands';

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

    const fade = breadcrumb.locator('img');
    await expect(fade).toHaveAttribute('alt', '');

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
});
