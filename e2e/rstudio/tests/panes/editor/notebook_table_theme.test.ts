import type { Page } from '@playwright/test';
import { test, expect } from '@fixtures/rstudio.fixture';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { SourcePaneActions } from '@actions/source_pane.actions';
import { executeCommand } from '@utils/commands';
import { closeAndDeleteSandboxFiles, openFile, writeAndOpenFile } from '@utils/files';
import { rStringLiteral } from '@utils/r';
import { useSuiteSandbox } from '@utils/sandbox';
import {
  DARK_THEME, DARK_THEME_HREF, LIGHT_THEME, LIGHT_THEME_HREF,
  expectThemeStylesheet,
} from '@utils/theme';

// The iframe contains an HTML fragment with no doctype. Chromium's quirks-mode
// table color can stay black after RStudio changes the body color (#13292).
const OUTPUT_FRAME = 'iframe[src*="chunk_output/"]:visible';
const WHITE = 'rgb(255, 255, 255)';
const BLACK = 'rgb(0, 0, 0)';

function notebook(code: string): string {
  return ['---', 'output: html_document', '---', '', '```{r}', code, '```', ''].join('\n');
}

test.describe('Notebook table themes', { tag: ['@parallel_safe'] }, () => {
  const sandbox = useSuiteSandbox();
  let consoleActions: ConsolePaneActions;
  let sourceActions: SourcePaneActions;
  let missingPackages: string[] = [];

  test.beforeAll(async ({ rstudioPage: page }) => {
    consoleActions = new ConsolePaneActions(page);
    sourceActions = new SourcePaneActions(page, consoleActions);
    missingPackages = await consoleActions.ensurePackages(['rmarkdown', 'knitr', 'htmltools']);
    await consoleActions.executeInConsole(
      '.rstudio.e2e.tableTheme <- .rs.api.getThemeInfo()$editor', { wait: true },
    );
  });

  test.beforeEach(() => {
    test.skip(missingPackages.length > 0, 'Required R packages are unavailable');
  });

  test.afterAll(async () => {
    await consoleActions.executeInConsole(
      '.rs.api.applyTheme(.rstudio.e2e.tableTheme); rm(.rstudio.e2e.tableTheme)',
      { wait: true },
    );
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
      '<table><tr><td id="plain">Inherited</td></tr></table>',
      '<table class="authored"><tr><td id="authored">Stylesheet</td></tr></table>',
      '<table style="color: rgb(65, 43, 21)"><tr><td id="inline">Inline</td></tr></table>',
      '<div style="color: rgb(90, 80, 70)"><table><tr><td id="parent">Parent</td></tr></table></div>',
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
    await closeAndDeleteSandboxFiles(page, sandbox.dir, [fileName]);
  });
});
