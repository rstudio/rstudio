// Rendering a Quarto document with "(No Preview)" runs a one-shot render
// (#12838). With the preview option set to none there is nothing to serve, so
// the Render button runs `quarto render` as a background job that finishes
// when the render does, rather than `quarto preview`, which would leave a
// preview server running in the Background Jobs pane.

import { test, expect } from '@fixtures/rstudio.fixture';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { TIMEOUTS } from '@utils/constants';
import { clearPref, executeCommand, setPref, waitForSourcePaneReset } from '@utils/commands';
import { closeAndDeleteSandboxFiles, writeAndOpenFile } from '@utils/files';
import { heredoc } from '@utils/heredoc';
import { rPathLiteral } from '@utils/r';
import { useSuiteSandbox } from '@utils/sandbox';
import * as path from 'path';

const JOBS_PANEL = '#rstudio_workbench_panel_background_jobs';
const JOBS_TAB = '#rstudio_workbench_tab_background_jobs';
const CONSOLE_TAB = '#rstudio_workbench_tab_console';

// a first quarto render on a cold machine can take a while
const RENDER_TIMEOUT = 120000;

// no code chunks, so the markdown engine renders it without knitr/rmarkdown
const QMD = heredoc`
  ---
  title: "No preview"
  format: html
  ---

  Hello.
`;

// an include of a file that does not exist fails the render before any
// engine runs, so this needs neither knitr nor a code chunk
const BROKEN_QMD = heredoc`
  ---
  title: "No preview, broken"
  format: html
  ---

  Hello.

  {{< include missing.qmd >}}
`;

const SHINY_QMD = heredoc`
  ---
  title: "Shiny without preview"
  format: html
  server: shiny
  ---

  ${'```'}{r}
  shiny::textOutput("message")
  ${'```'}

  ${'```'}{r}
  #| context: server
  output$message <- shiny::renderText("Shiny is running")
  ${'```'}
`;

test.describe.serial('Quarto with no preview', () => {
  const sandbox = useSuiteSandbox();
  let consoleActions: ConsolePaneActions;
  const fileName = 'quarto_no_preview.qmd';

  test.beforeAll(async ({ rstudioPage: page }) => {
    consoleActions = new ConsolePaneActions(page);
    await setPref(page, 'rmd_viewer_type', 'none');
  });

  test.beforeEach(async ({ rstudioPage: page }) => {
    await waitForSourcePaneReset(page);
  });

  test.afterEach(async ({ rstudioPage: page }) => {
    await closeAndDeleteSandboxFiles(page, sandbox.dir, [
      fileName,
      fileName.replace(/\.qmd$/, '.html'),
    ]);
  });

  test.afterAll(async ({ rstudioPage: page }) => {
    await clearPref(page, 'rmd_viewer_type');
    await consoleActions.resetSourcePane();
  });

  test('Render runs a one-shot render job that finishes, not a preview server', async ({
    rstudioPage: page,
  }) => {
    await writeAndOpenFile(page, sandbox.dir, fileName, QMD);
    await executeCommand(page, 'quartoRenderDocument');

    const outputPath = path.join(sandbox.dir, fileName.replace(/\.qmd$/, '.html'));
    await expect.poll(
      () => consoleActions.evalRLogical(`file.exists(${rPathLiteral(outputPath)})`),
      { timeout: RENDER_TIMEOUT },
    ).toBe(true);

    // the job is named for what it does, and it ends once the render has
    const jobsPanel = page.locator(JOBS_PANEL);
    await expect(jobsPanel).toContainText(`Render: ${fileName}`, { timeout: TIMEOUTS.fileOpen });
    await expect(jobsPanel).not.toContainText(`Preview: ${fileName}`);
    await expect(jobsPanel).toContainText('Succeeded', { timeout: TIMEOUTS.fileOpen });

    // a successful render hands focus back to the console, as a preview does
    // once its server is up
    await expect(page.locator(CONSOLE_TAB)).toHaveAttribute('aria-selected', 'true', {
      timeout: TIMEOUTS.fileOpen,
    });
  });

  test('a failed render leaves its output in the Background Jobs pane', async ({ rstudioPage: page }) => {
    await writeAndOpenFile(page, sandbox.dir, fileName, BROKEN_QMD);
    await executeCommand(page, 'quartoRenderDocument');

    const jobsPanel = page.locator(JOBS_PANEL);
    await expect(jobsPanel).toContainText(`Render: ${fileName}`, { timeout: TIMEOUTS.fileOpen });
    await expect(jobsPanel).toContainText('Failed', { timeout: RENDER_TIMEOUT });
    await expect(jobsPanel).toContainText('missing.qmd');

    // unlike a successful render, a failure does not switch back to the
    // console, so the error stays in front of the user
    await expect(page.locator(JOBS_TAB)).toHaveAttribute('aria-selected', 'true');
    await expect(page.locator(CONSOLE_TAB)).not.toHaveAttribute('aria-selected', 'true');

    const outputPath = path.join(sandbox.dir, fileName.replace(/\.qmd$/, '.html'));
    expect(await consoleActions.evalRLogical(`file.exists(${rPathLiteral(outputPath)})`)).toBe(false);
  });

  test('Run Document still starts a Shiny application', async ({ rstudioPage: page }) => {
    test.setTimeout(240000);
    expect(await consoleActions.ensurePackages(['rmarkdown', 'shiny'])).toEqual([]);
    await writeAndOpenFile(page, sandbox.dir, fileName, SHINY_QMD);
    await page.getByRole('button', { name: 'Run Document', exact: true }).click();

    const jobsPanel = page.locator(JOBS_PANEL);
    await expect(jobsPanel).toContainText(`Preview: ${fileName}`, { timeout: TIMEOUTS.fileOpen });
    await expect(jobsPanel).toContainText('==> quarto serve');
    await expect(jobsPanel).toContainText('Listening on', { timeout: RENDER_TIMEOUT });
  });
});
