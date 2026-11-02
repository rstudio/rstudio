// Regression test for #18494: menu checkmarks and the Git/SVN changelist
// checkboxes are <img> bitmaps, which Windows contrast themes (CSS forced colors
// mode) do not recolor. Under forced colors, themeStyles.css masks each tagged
// <img> to an SVG of its glyph and fills it with CanvasText. That CSS only
// reaches the browser because it is wrapped in an @supports block that GWT's
// CSS2 parser passes through untouched; a bare forced-colors media query would
// be dropped with only a build warning. The GWT unit tests cover the tagging.
import type { Locator, Page } from '@playwright/test';
import { test, expect } from '@fixtures/rstudio.fixture';
import { EnvironmentPane } from '@pages/environment_pane.page';
import { executeCommand } from '@utils/commands';

// The memory drop-down's "Show Current Memory Usage" item is checked by default
// (show_memory_usage defaults to true).
const MENU_CHECK = 'img.rstudio-glyph-menu-check';

const CHECKBOX_GLYPHS = [
  'rstudio-glyph-checkbox-on',
  'rstudio-glyph-checkbox-off',
  'rstudio-glyph-checkbox-indeterminate',
];

const SVG_MASK = /^url\("data:image\/svg\+xml,/;

interface GlyphStyle {
  maskImage: string;
  backgroundColor: string;
}

async function glyphStyle(img: Locator): Promise<GlyphStyle> {
  return img.evaluate((el) => {
    const s = getComputedStyle(el);
    return { maskImage: s.maskImage, backgroundColor: s.backgroundColor };
  });
}

/** The active palette's CanvasText, as getComputedStyle reports colors. */
async function canvasText(page: Page): Promise<string> {
  return page.evaluate(() => {
    const probe = document.createElement('div');
    probe.style.color = 'CanvasText';
    document.body.appendChild(probe);
    const color = getComputedStyle(probe).color;
    probe.remove();
    return color;
  });
}

test.describe('Image checkmarks under contrast themes (#18494)', () => {
  test.afterEach(async ({ rstudioPage: page }) => {
    await page.emulateMedia({ forcedColors: null });
  });

  test('menu checkmark is drawn in the forced text color', async ({ rstudioPage: page }) => {
    const envPane = new EnvironmentPane(page);
    await executeCommand(page, 'activateEnvironment');
    await page.emulateMedia({ forcedColors: 'active' });

    await envPane.memoryPieBtn.click();
    const check = page.locator(MENU_CHECK).filter({ visible: true });
    await expect(check).toHaveCount(1);

    try {
      const forced = await glyphStyle(check);
      expect(forced.maskImage).toMatch(SVG_MASK);
      expect(forced.backgroundColor).toBe(await canvasText(page));

      // Contrast themes off: the bitmap renders as before, unmasked.
      await page.emulateMedia({ forcedColors: 'none' });
      expect((await glyphStyle(check)).maskImage).toBe('none');
    } finally {
      await page.keyboard.press('Escape');
    }
  });

  test('every changelist checkbox state has a forced-colors glyph', async ({
    rstudioPage: page,
  }) => {
    // Reaching a real changelist needs a version-controlled project; the cell's
    // markup is covered by TriStateCheckboxCellTests, so stand in an <img>
    // tagged the same way to check that each state's rule reaches the browser.
    await page.emulateMedia({ forcedColors: 'active' });
    const expected = await canvasText(page);
    const masks = new Set<string>();

    for (const glyph of CHECKBOX_GLYPHS) {
      await page.evaluate((cls) => {
        const img = document.createElement('img');
        img.id = 'pw-forced-colors-glyph';
        img.className = `rstudio-forced-colors-glyph ${cls}`;
        img.width = img.height = 15;
        img.alt = '';
        document.body.appendChild(img);
      }, glyph);
      const img = page.locator('#pw-forced-colors-glyph');

      try {
        const style = await glyphStyle(img);
        expect(style.maskImage, glyph).toMatch(SVG_MASK);
        expect(style.backgroundColor, glyph).toBe(expected);
        masks.add(style.maskImage);
      } finally {
        await img.evaluate((el) => el.remove());
      }
    }

    // each state draws its own glyph
    expect(masks.size).toBe(CHECKBOX_GLYPHS.length);
  });
});
