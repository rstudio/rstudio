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
import { useSuiteSandbox } from '@utils/sandbox';
import * as fs from 'fs';
import * as path from 'path';

const JOBS_PANEL = '#rstudio_workbench_panel_background_jobs';

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

test.describe.serial('Quarto render without preview', () => {
  const sandbox = useSuiteSandbox();
  let consoleActions: ConsolePaneActions;
  const fileName = 'quarto_no_preview.qmd';

  test.beforeAll(async ({ rstudioPage: page }) => {
    consoleActions = new ConsolePaneActions(page);
    await setPref(page, 'rmd_viewer_type', 'none');
  });

  test.beforeEach(async ({ rstudioPage: page }) => {
    await waitForSourcePaneReset(page);
    await writeAndOpenFile(page, sandbox.dir, fileName, QMD);
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
    await executeCommand(page, 'quartoRenderDocument');

    const outputPath = path.join(sandbox.dir, fileName.replace(/\.qmd$/, '.html'));
    await expect.poll(() => fs.existsSync(outputPath), { timeout: RENDER_TIMEOUT }).toBe(true);

    // the job is named for what it does, and it ends once the render has
    const jobsPanel = page.locator(JOBS_PANEL);
    await expect(jobsPanel).toContainText(`Render: ${fileName}`, { timeout: TIMEOUTS.fileOpen });
    await expect(jobsPanel).not.toContainText(`Preview: ${fileName}`);
    await expect(jobsPanel).toContainText('Succeeded', { timeout: TIMEOUTS.fileOpen });
  });
});
