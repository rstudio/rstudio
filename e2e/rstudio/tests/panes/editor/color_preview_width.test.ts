// Color previews must not change the width of the text they decorate (#17215).
//
// The preview box used to be drawn with a 1px border offset by -1px margins.
// Chromium floors border widths to whole device pixels while margins stay
// fractional, so at any non-integer effective zoom (Windows display scaling,
// RStudio's own zoom levels) each box came out slightly narrower than its
// text and the error accumulated along the line, pushing the cursor and
// bracket highlighting off the character grid. The box is now drawn with an
// outline, which does not take part in layout.

import type { Page } from 'playwright';
import { test, expect } from '@fixtures/rstudio.fixture';
import { clearPref, setPref, waitForSourcePaneReset } from '@utils/commands';
import { closeAndDeleteSandboxFiles, writeAndOpenFile } from '@utils/files';
import { heredoc } from '@utils/heredoc';
import { useSuiteSandbox } from '@utils/sandbox';

const R_FILE = 'color_preview_width.R';
const COLOR_COUNT = 12;
const HEX = Array(COLOR_COUNT).fill('"#000000"').join(', ');
const PLAIN = Array(COLOR_COUNT).fill('".000000"').join(', ');

// row 0 gets a preview box on every string; row 1 is the same text with the
// '#' swapped for '.', so the two rows have identical character counts and
// must end at the same x position.
const CONTENT = heredoc`
  colors <- c(${HEX})
  colors <- c(${PLAIN})
`;

// A zoom whose product with both common device scale factors (1 and 2) is a
// non-integer, so a 1px border would be floored to fewer device pixels than
// its matching -1px margin.
const ZOOM = 1.25;

async function rowEndDifference(page: Page): Promise<number> {
  return page.evaluate(() => {
    const editor = window.rstudio?.documents.activeEditor();
    if (!editor) return NaN;
    const lines = editor.container.querySelectorAll('.ace_text-layer .ace_line');
    const right = (line: Element) => line.lastElementChild?.getBoundingClientRect().right ?? NaN;
    return right(lines[0]) - right(lines[1]);
  });
}

test.describe('Color preview width', () => {
  const sandbox = useSuiteSandbox();

  test.beforeAll(async ({ rstudioPage: page }) => {
    await setPref(page, 'color_preview', true);
  });

  test.beforeEach(async ({ rstudioPage: page }) => {
    await waitForSourcePaneReset(page);
  });

  test.afterEach(async ({ rstudioPage: page }) => {
    await page.evaluate(() => {
      document.body.style.zoom = '';
    });
    await closeAndDeleteSandboxFiles(page, sandbox.dir, [R_FILE]);
  });

  test.afterAll(async ({ rstudioPage: page }) => {
    await clearPref(page, 'color_preview');
  });

  test('a row of color previews ends where the same text without previews ends, at fractional zoom', async ({
    rstudioPage: page,
  }) => {
    await writeAndOpenFile(page, sandbox.dir, R_FILE, CONTENT);

    await expect
      .poll(() =>
        page.evaluate(() => {
          const editor = window.rstudio?.documents.activeEditor();
          return editor?.container.querySelectorAll('.ace_text-layer .ace_string.ace_color').length ?? 0;
        }),
      )
      .toBe(COLOR_COUNT);

    // sanity check at the default zoom, where border snapping is exact
    expect(Math.abs(await rowEndDifference(page))).toBeLessThan(0.5);

    await page.evaluate((zoom) => {
      document.body.style.zoom = String(zoom);
    }, ZOOM);

    await expect.poll(() => rowEndDifference(page).then(Math.abs)).toBeLessThan(0.5);
  });
});
