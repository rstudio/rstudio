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

// The iframe contains an HTML fragment with no doctype. Chromium's quirks-mode
// table color can stay black after RStudio changes the body color (#13292).
const OUTPUT_FRAME = 'iframe[src*="chunk_output/"]:visible';
const WHITE = 'rgb(255, 255, 255)';
const BLACK = 'rgb(0, 0, 0)';

function notebook(code: string): string {
  return ['---', 'output: html_document', '---', '', '```{r}', code, '```', ''].join('\n');
}

test.describe('Notebook table themes', () => {
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

  test('kable text follows the theme on first render, rerun, and tab switching', async ({ rstudioPage: page }) => {
    await applyTheme(page, DARK_THEME, DARK_THEME_HREF);
    const fileName = 'table-theme.Rmd';
    await writeAndOpenFile(page, sandbox.dir, fileName,
      notebook('knitr::kable(data.frame(value = 42), format = "markdown")'));
    await runChunk(page);
    const output = page.frameLocator(OUTPUT_FRAME);
    await expect(output.locator('td')).toHaveCSS('color', WHITE);

    // Force styles to be computed before changing the theme. This catches the
    // stale table color even when the initial load happened to render correctly.
    await applyTheme(page, LIGHT_THEME, LIGHT_THEME_HREF);
    await expect(output.locator('body')).toHaveCSS('color', BLACK);
    await expect(output.locator('td')).toHaveCSS('color', BLACK);
    await applyTheme(page, DARK_THEME, DARK_THEME_HREF);
    await expect(output.locator('td')).toHaveCSS('color', WHITE);

    const oldFrame = await page.locator(OUTPUT_FRAME).elementHandle();
    await sourceActions.navigateToChunkByIndex(1);
    await executeCommand(page, 'executeCurrentChunk');
    await oldFrame!.waitForElementState('hidden');
    await expect(page.frameLocator(OUTPUT_FRAME).locator('td')).toHaveCSS('color', WHITE);

    await writeAndOpenFile(page, sandbox.dir, 'other.R', '# Another source tab\n');
    await openFile(page, `${sandbox.dir}/${fileName}`);
    await expect(page.frameLocator(OUTPUT_FRAME).locator('td')).toHaveCSS('color', WHITE);
    await closeAndDeleteSandboxFiles(page, sandbox.dir, [fileName, 'other.R']);
  });

  test('table inheritance preserves authored colors and applies to later tables', async ({ rstudioPage: page }) => {
    await applyTheme(page, DARK_THEME, DARK_THEME_HREF);
    const html = [
      '<style>table.authored { color: rgb(12, 34, 56); }</style>',
      '<style>@layer widget { table.layered { color: rgb(23, 45, 67); } }</style>',
      '<table><tr><td id="plain">Inherited</td></tr></table>',
      '<table class="authored"><tr><td id="authored">Stylesheet</td></tr></table>',
      '<table style="color: rgb(65, 43, 21)"><tr><td id="inline">Inline</td></tr></table>',
      '<div style="color: rgb(90, 80, 70)"><table><tr><td id="parent">Parent</td></tr></table></div>',
      '<table class="layered"><tr><td id="layered">Cascade layer</td></tr></table>',
    ].join('');
    const fileName = 'authored-table-theme.Rmd';
    await writeAndOpenFile(page, sandbox.dir, fileName,
      notebook(`htmltools::HTML(${rStringLiteral(html)})`));
    await runChunk(page);
    const output = page.frameLocator(OUTPUT_FRAME);
    await expect(output.locator('#plain')).toHaveCSS('color', WHITE);
    await expect(output.locator('#authored')).toHaveCSS('color', 'rgb(12, 34, 56)');
    await expect(output.locator('#inline')).toHaveCSS('color', 'rgb(65, 43, 21)');
    await expect(output.locator('#parent')).toHaveCSS('color', 'rgb(90, 80, 70)');
    await expect(output.locator('#layered')).toHaveCSS('color', 'rgb(23, 45, 67)');

    // HTML widgets may add tables after the iframe's load handler has run.
    await output.locator('body').evaluate(body => {
      const table = body.ownerDocument.createElement('table');
      const cell = table.insertRow().insertCell();
      cell.id = 'later';
      cell.textContent = 'Added after load';
      body.appendChild(table);
    });
    await expect(output.locator('#later')).toHaveCSS('color', WHITE);
    await applyTheme(page, LIGHT_THEME, LIGHT_THEME_HREF);
    await expect(output.locator('#plain')).toHaveCSS('color', BLACK);
    await expect(output.locator('#later')).toHaveCSS('color', BLACK);
    await expect(output.locator('#authored')).toHaveCSS('color', 'rgb(12, 34, 56)');
    await expect(output.locator('#inline')).toHaveCSS('color', 'rgb(65, 43, 21)');
    await expect(output.locator('#layered')).toHaveCSS('color', 'rgb(23, 45, 67)');
    await closeAndDeleteSandboxFiles(page, sandbox.dir, [fileName]);
  });

  test('painted surfaces stay readable while authored colors retain precedence', async ({ rstudioPage: page }) => {
    await applyTheme(page, DARK_THEME, DARK_THEME_HREF);
    const table = (id: string, attributes = '') =>
      `<table ${attributes}><tr><td id="${id}">Value</td></tr></table>`;
    const html = [
      '<style>.light { background-color: white; } .dark { background-color: rgb(0, 34, 64); }</style>',
      table('transparent'),
      table('table-background', 'class="light"'),
      '<table><tr class="light"><td id="row-background">Light row</td></tr>',
      '<tr class="dark"><td id="dark-row">Dark row</td></tr></table>',
      '<table><tr><td class="light" id="cell-background">Light cell</td></tr></table>',
      '<div class="light">', table('wrapper-background'), '</div>',
      '<div class="dark"><div style="background-color: rgba(255,255,255,0.9)">',
      table('translucent-background'), '</div></div>',
      '<div style="color: rgb(90,80,70)"><div class="light">',
      table('authored-parent'), '</div></div>',
      '<table style="color: rgb(12,34,56)"><tr class="light">',
      '<td id="authored-table-background">Authored table</td></tr></table>',
    ].join('');
    const fileName = 'painted-table-theme.Rmd';
    await writeAndOpenFile(page, sandbox.dir, fileName,
      notebook(`htmltools::HTML(${rStringLiteral(html)})`));
    await runChunk(page);
    const output = page.frameLocator(OUTPUT_FRAME);
    const lightCells = '#table-background, #row-background, #cell-background, ' +
      '#wrapper-background, #translucent-background';

    async function expectSurfaceColors(): Promise<void> {
      for (const cell of await output.locator(lightCells).all())
        await expect(cell).toHaveCSS('color', BLACK);
      await expect(output.locator('#dark-row')).toHaveCSS('color', WHITE);
      await expect(output.locator('#authored-parent')).toHaveCSS('color', 'rgb(90, 80, 70)');
      await expect(output.locator('#authored-table-background')).toHaveCSS('color', 'rgb(12, 34, 56)');
    }

    await expect(output.locator('#transparent')).toHaveCSS('color', WHITE);
    await expectSurfaceColors();
    await applyTheme(page, LIGHT_THEME, LIGHT_THEME_HREF);
    await expect(output.locator('#transparent')).toHaveCSS('color', BLACK);
    await expectSurfaceColors();
    await applyTheme(page, DARK_THEME, DARK_THEME_HREF);

    // Cover output that paints a surface after its initial load, then changes
    // or removes that background without rerunning the chunk.
    await output.locator('body').evaluate(body => {
      const table = body.ownerDocument.createElement('table');
      table.className = 'light';
      table.insertRow().insertCell().id = 'dynamic-background';
      body.appendChild(table);
    });
    const dynamic = output.locator('#dynamic-background');
    await expect(dynamic).toHaveCSS('color', BLACK);
    await dynamic.evaluate(cell => cell.closest('table')!.className = 'dark');
    await expect(dynamic).toHaveCSS('color', WHITE);
    await dynamic.evaluate(cell => cell.closest('table')!.className = 'light');
    await expect(dynamic).toHaveCSS('color', BLACK);
    await dynamic.evaluate(cell => cell.closest('table')!.removeAttribute('class'));
    await expect(dynamic).toHaveCSS('color', WHITE);
    await dynamic.evaluate(cell => {
      const table = cell.closest('table')!;
      table.className = 'light';
      const wrapper = cell.ownerDocument.createElement('div');
      wrapper.style.color = 'rgb(90, 80, 70)';
      table.replaceWith(wrapper);
      wrapper.appendChild(table);
    });
    await expect(dynamic).toHaveCSS('color', 'rgb(90, 80, 70)');
    await closeAndDeleteSandboxFiles(page, sandbox.dir, [fileName]);
  });

  for (const [name, style, expectedColor = BLACK] of [
    ['stylesheet body', '<style>body { background-color: white; }</style>'],
    ['stylesheet root', '<style>html { background-color: white; }</style>'],
    ['inline body', '<body style="background-color: white">'],
    ['body class', '<style>.custom { background-color: white; color: black; }</style><body class="custom">'],
    ['authored root color', '<style>html { background-color: white; color: rgb(23,45,67); }</style>', 'rgb(23, 45, 67)'],
    ['authored body color', '<style>body { background-color: white; color: rgb(12,34,56); }</style>', 'rgb(12, 34, 56)'],
  ]) {
    test(`${name} background retains readable table text`, async ({ rstudioPage: page }) => {
      await applyTheme(page, DARK_THEME, DARK_THEME_HREF);
      const fileName = 'document-table-theme.Rmd';
      const html = style + '<table><tr><td>Value</td></tr></table>';
      await writeAndOpenFile(page, sandbox.dir, fileName,
        notebook(`htmltools::HTML(${rStringLiteral(html)})`));
      await runChunk(page);
      const cell = page.frameLocator(OUTPUT_FRAME).locator('td');
      await expect(cell).toHaveCSS('color', expectedColor);
      await applyTheme(page, LIGHT_THEME, LIGHT_THEME_HREF);
      await expect(cell).toHaveCSS('color', expectedColor);
      await applyTheme(page, DARK_THEME, DARK_THEME_HREF);
      await expect(cell).toHaveCSS('color', expectedColor);
      await closeAndDeleteSandboxFiles(page, sandbox.dir, [fileName]);
    });
  }

  for (const style of ['default', 'bootstrap']) {
    test(`DT ${style} keeps readable text through theme changes`, async ({ rstudioPage: page }) => {
      test.skip(missingPackages.includes('DT'), 'DT is unavailable');
      await applyTheme(page, LIGHT_THEME, LIGHT_THEME_HREF);
      const fileName = 'widget-table-theme.Rmd';
      const options = style === 'bootstrap' ? ', style = "bootstrap"' : '';
      await writeAndOpenFile(page, sandbox.dir, fileName,
        notebook(`DT::datatable(data.frame(value = 42)${options})`));
      await runChunk(page);
      const cell = page.frameLocator(OUTPUT_FRAME).locator('tbody td').first();
      const originalColor = await cell.evaluate(el => getComputedStyle(el).color);
      expect(luminance(originalColor)).toBeLessThan(128);
      await applyTheme(page, DARK_THEME, DARK_THEME_HREF);
      await expect(cell).toHaveCSS('color', originalColor);
      await applyTheme(page, LIGHT_THEME, LIGHT_THEME_HREF);
      await expect(cell).toHaveCSS('color', originalColor);
      await closeAndDeleteSandboxFiles(page, sandbox.dir, [fileName]);
    });
  }

});
