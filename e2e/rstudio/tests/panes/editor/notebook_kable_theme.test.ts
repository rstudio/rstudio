import type { Page } from '@playwright/test';
import { test, expect } from '@fixtures/rstudio.fixture';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { SourcePaneActions } from '@actions/source_pane.actions';
import { executeCommand, getPref } from '@utils/commands';
import { closeAndDeleteSandboxFiles, openFile, writeAndOpenFile } from '@utils/files';
import { rStringLiteral } from '@utils/r';
import { useSuiteSandbox } from '@utils/sandbox';
import {
  DARK_THEME, DARK_THEME_HREF, LIGHT_THEME, LIGHT_THEME_HREF,
  expectThemeStylesheet, getThemeStylesheetHref, luminance,
} from '@utils/theme';

const OUTPUT_FRAME = 'iframe[src*="chunk_output/"]:visible';
const KABLE_STYLE = '#rstudio-kable-table-theme';
const WHITE = 'rgb(255, 255, 255)';
const BLACK = 'rgb(0, 0, 0)';

function notebook(code: string): string {
  return ['---', 'output: html_document', '---', '', '```{r}', code, '```', ''].join('\n');
}

test.describe('Notebook kable themes', () => {
  const sandbox = useSuiteSandbox();
  let consoleActions: ConsolePaneActions;
  let sourceActions: SourcePaneActions;
  let missingPackages: string[] = [];
  let originalTheme: string | undefined;
  let originalThemeHref: string;

  test.beforeAll(async ({ rstudioPage: page }) => {
    consoleActions = new ConsolePaneActions(page);
    sourceActions = new SourcePaneActions(page, consoleActions);
    // Keep this outside R so workspace clearing or a restart cannot lose it.
    originalTheme = await getPref(page, 'editor_theme') as string;
    originalThemeHref = await getThemeStylesheetHref(page);
    missingPackages = await consoleActions.ensurePackages(['rmarkdown', 'knitr', 'htmltools', 'DT']);
  });

  test.beforeEach(() => {
    test.skip(missingPackages.some(name => name !== 'DT'), 'Required R packages are unavailable');
  });

  test.afterAll(async ({ rstudioPage: page }) => {
    if (originalTheme !== undefined) {
      await applyTheme(page, originalTheme, originalThemeHref);
    }
  });

  async function applyTheme(page: Page, theme: string, href: string): Promise<void> {
    await consoleActions.executeInConsole(`.rs.api.applyTheme(${rStringLiteral(theme)})`, { wait: true });
    await expectThemeStylesheet(page, href);
  }

  async function runChunk(page: Page): Promise<void> {
    await sourceActions.ensureSourceMode();
    await sourceActions.navigateToChunkByIndex(1);
    await executeCommand(page, 'executeCurrentChunk');
    await expect(page.locator(OUTPUT_FRAME)).toHaveCount(1, { timeout: 30000 });
    await expect(page.frameLocator(OUTPUT_FRAME).locator('td').first()).toBeVisible();
  }

  async function selectGalleryPage(page: Page, name: string): Promise<void> {
    const label = page.getByText(name, { exact: true });
    // Theme changes can leave gallery controls clipped by Ace's scrollbar.
    // Scroll Ace's virtual viewport, which DOM scrollIntoView cannot move.
    await label.evaluate(element => {
      const viewport = element.closest('.ace_editor')!.getBoundingClientRect();
      const target = element.getBoundingClientRect();
      window.rstudio!.documents.activeEditor()!.renderer.scrollBy(
        0,
        target.top + target.height / 2 - viewport.top - viewport.height / 2,
      );
    });
    await label.click();
  }

  for (const format of ['default', 'markdown', 'simple', 'html']) {
    test(`${format} kable follows the theme on first render, rerun, and tab switching`, async ({ rstudioPage: page }) => {
      await applyTheme(page, DARK_THEME, DARK_THEME_HREF);
      // Distinct paths prevent an earlier case's cached output from restoring
      // alongside this case's output while its new chunk is running.
      const fileName = `kable-theme-${format}.Rmd`;
      const options = format === 'default' ? '' : `, format = "${format}"`;
      await writeAndOpenFile(page, sandbox.dir, fileName,
        notebook(`knitr::kable(data.frame(value = 42)${options})`));
      await runChunk(page);
      const output = page.frameLocator(OUTPUT_FRAME);
      await expect(output.locator('td')).toHaveCSS('color', WHITE);
      await expect(output.locator(KABLE_STYLE)).toHaveCount(1);

      // Compute styles before each theme change to catch the stale quirks-mode
      // table color even if the initial load happened to render correctly.
      await applyTheme(page, LIGHT_THEME, LIGHT_THEME_HREF);
      await expect(output.locator('body')).toHaveCSS('color', BLACK);
      await expect(output.locator('td')).toHaveCSS('color', BLACK);
      await applyTheme(page, DARK_THEME, DARK_THEME_HREF);
      await expect(output.locator('td')).toHaveCSS('color', WHITE);
      await expect(output.locator(KABLE_STYLE)).toHaveCount(1);

      const oldFrame = await page.locator(OUTPUT_FRAME).elementHandle();
      await sourceActions.navigateToChunkByIndex(1);
      await executeCommand(page, 'executeCurrentChunk');
      await oldFrame!.waitForElementState('hidden');
      await expect(page.frameLocator(OUTPUT_FRAME).locator('td')).toHaveCSS('color', WHITE);

      await writeAndOpenFile(page, sandbox.dir, 'other.R', '# Another source tab\n');
      await openFile(page, `${sandbox.dir}/${fileName}`);
      await expect(page.frameLocator(OUTPUT_FRAME).locator('td')).toHaveCSS('color', WHITE);
      await expect(page.frameLocator(OUTPUT_FRAME).locator(KABLE_STYLE)).toHaveCount(1);

      // Cached notebook output must retain the marker across close/reopen.
      const closed = page.waitForResponse(response => response.url().includes('close_document'));
      await executeCommand(page, 'closeSourceDoc');
      await closed;
      await openFile(page, `${sandbox.dir}/${fileName}`);
      await expect(page.frameLocator(OUTPUT_FRAME).locator('td')).toHaveCSS('color', WHITE);
      await expect(page.frameLocator(OUTPUT_FRAME).locator(KABLE_STYLE)).toHaveCount(1);
      await closeAndDeleteSandboxFiles(page, sandbox.dir, [fileName, 'other.R']);
    });
  }

  for (const styling of ['inline', 'stylesheet', 'cascade layer']) {
    test(`HTML kable preserves ${styling} colors through theme changes`, async ({ rstudioPage: page }) => {
      await applyTheme(page, DARK_THEME, DARK_THEME_HREF);
      const fileName = `styled-kable-${styling.replaceAll(' ', '-')}.Rmd`;
      const declarations = 'color: rgb(12, 34, 56); background-color: white;';
      const tableAttributes = styling === 'inline' ? `style="${declarations}"` : 'class="authored"';
      const rule = `table.authored { ${declarations} }`;
      const stylesheet = styling === 'cascade layer' ? `@layer custom { ${rule} }` : rule;
      const code = [
        `x <- knitr::kable(data.frame(value = 42), format = "html", table.attr = ${rStringLiteral(tableAttributes)})`,
        ...(styling === 'inline' ? [] : [`x[] <- paste0(${rStringLiteral(`<style>${stylesheet}</style>`)}, x)`]),
        'x',
      ].join('\n');
      await writeAndOpenFile(page, sandbox.dir, fileName, notebook(code));
      await runChunk(page);
      const output = page.frameLocator(OUTPUT_FRAME);
      await expect(output.locator(KABLE_STYLE)).toHaveCount(1);
      await expect(output.locator('td')).toHaveCSS('color', 'rgb(12, 34, 56)');
      await applyTheme(page, LIGHT_THEME, LIGHT_THEME_HREF);
      await expect(output.locator('td')).toHaveCSS('color', 'rgb(12, 34, 56)');
      await applyTheme(page, DARK_THEME, DARK_THEME_HREF);
      await expect(output.locator('td')).toHaveCSS('color', 'rgb(12, 34, 56)');
      await expect(output.locator(KABLE_STYLE)).toHaveCount(1);
      await closeAndDeleteSandboxFiles(page, sandbox.dir, [fileName]);
    });
  }

  test('gallery pages apply inheritance only to kable output', async ({ rstudioPage: page }) => {
    await applyTheme(page, DARK_THEME, DARK_THEME_HREF);
    const fileName = 'kable-gallery.Rmd';
    const html = '<table style="color: rgb(12, 34, 56); background: white"><tr><td>Custom</td></tr></table>';
    await writeAndOpenFile(page, sandbox.dir, fileName, notebook([
      'knitr::kable(data.frame(value = 42), format = "html")',
      `htmltools::HTML(${rStringLiteral(html)})`,
    ].join('\n')));
    await runChunk(page);
    const output = page.frameLocator(OUTPUT_FRAME);
    await expect(output.locator('td')).toHaveText('Custom');
    await expect(output.locator(KABLE_STYLE)).toHaveCount(0);
    await expect(output.locator('td')).toHaveCSS('color', 'rgb(12, 34, 56)');

    await selectGalleryPage(page, 'knit_asis');
    await expect(output.locator('td')).toHaveText('42');
    await expect(output.locator('td')).toHaveCSS('color', WHITE);
    await expect(output.locator(KABLE_STYLE)).toHaveCount(1);
    await applyTheme(page, LIGHT_THEME, LIGHT_THEME_HREF);
    await expect(output.locator('td')).toHaveCSS('color', BLACK);

    await selectGalleryPage(page, 'html');
    await expect(output.locator('td')).toHaveText('Custom');
    await applyTheme(page, DARK_THEME, DARK_THEME_HREF);
    await expect(output.locator('td')).toHaveCSS('color', 'rgb(12, 34, 56)');
    await expect(output.locator(KABLE_STYLE)).toHaveCount(0);
    await selectGalleryPage(page, 'knit_asis');
    await expect(output.locator('td')).toHaveCSS('color', WHITE);
    await expect(output.locator(KABLE_STYLE)).toHaveCount(1);
    await closeAndDeleteSandboxFiles(page, sandbox.dir, [fileName]);
  });

  for (const [name, code] of [
    ['generic Markdown', 'knitr::asis_output("| value |\\n| --- |\\n| 42 |")'],
    ['custom HTML', `htmltools::HTML(${rStringLiteral('<table style="color: rgb(12, 34, 56); background: white"><tr><td>Custom</td></tr></table>')})`],
  ]) {
    test(`${name} does not receive the kable color rule`, async ({ rstudioPage: page }) => {
      await applyTheme(page, DARK_THEME, DARK_THEME_HREF);
      const fileName = `other-table-theme-${name.replaceAll(' ', '-')}.Rmd`;
      await writeAndOpenFile(page, sandbox.dir, fileName, notebook(code));
      await runChunk(page);
      const output = page.frameLocator(OUTPUT_FRAME);
      await expect(output.locator(KABLE_STYLE)).toHaveCount(0);
      if (name === 'custom HTML') {
        await expect(output.locator('td')).toHaveCSS('color', 'rgb(12, 34, 56)');
      }
      await applyTheme(page, LIGHT_THEME, LIGHT_THEME_HREF);
      await expect(output.locator(KABLE_STYLE)).toHaveCount(0);
      await applyTheme(page, DARK_THEME, DARK_THEME_HREF);
      await expect(output.locator(KABLE_STYLE)).toHaveCount(0);
      if (name === 'custom HTML') {
        await expect(output.locator('td')).toHaveCSS('color', 'rgb(12, 34, 56)');
      }
      await closeAndDeleteSandboxFiles(page, sandbox.dir, [fileName]);
    });
  }

  for (const style of ['default', 'bootstrap']) {
    test(`DT ${style} does not receive the kable color rule`, async ({ rstudioPage: page }) => {
      test.skip(missingPackages.includes('DT'), 'DT is unavailable');
      await applyTheme(page, LIGHT_THEME, LIGHT_THEME_HREF);
      const fileName = `widget-table-theme-${style}.Rmd`;
      const options = style === 'bootstrap' ? ', style = "bootstrap"' : '';
      await writeAndOpenFile(page, sandbox.dir, fileName,
        notebook(`DT::datatable(data.frame(value = 42)${options})`));
      await runChunk(page);
      const output = page.frameLocator(OUTPUT_FRAME);
      const cell = output.locator('tbody td').first();
      const originalColor = await cell.evaluate(el => getComputedStyle(el).color);
      expect(luminance(originalColor)).toBeLessThan(128);
      await expect(output.locator(KABLE_STYLE)).toHaveCount(0);
      await applyTheme(page, DARK_THEME, DARK_THEME_HREF);
      await expect(output.locator(KABLE_STYLE)).toHaveCount(0);
      // Default DT supplies its own foreground. Bootstrap's preexisting
      // white-background behavior is outside this fix's scope.
      if (style === 'default') {
        await expect(cell).toHaveCSS('color', originalColor);
      }
      await applyTheme(page, LIGHT_THEME, LIGHT_THEME_HREF);
      await expect(cell).toHaveCSS('color', originalColor);
      await expect(output.locator(KABLE_STYLE)).toHaveCount(0);
      await closeAndDeleteSandboxFiles(page, sandbox.dir, [fileName]);
    });
  }
});
