// A successful render preserves the Console pane's minimized state (#11622).
// An explicit user restore keeps it open, and a failed render opens the pane
// so its output can be read.

import type { Page } from 'playwright';
import { test, expect } from '@fixtures/rstudio.fixture';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { TIMEOUTS } from '@utils/constants';
import { clearPref, executeCommand, setPref } from '@utils/commands';
import { closeAndDeleteSandboxFiles, writeAndOpenFile } from '@utils/files';
import { heredoc } from '@utils/heredoc';
import { useSuiteSandbox } from '@utils/sandbox';

const CONSOLE_PANE = '#rstudio_Console_pane';
const CONSOLE_MIN_BTN = `${CONSOLE_PANE} .rstudio_panel_min_btn_console`;

// rmarkdown::render of a trivial document plus pandoc; generous on CI.
const RENDER_TIMEOUT = 90000;

// heredoc reads its template raw, so a chunk fence has to be interpolated
const FENCE = '```';

async function minimizeConsole(page: Page): Promise<void> {
  await page.locator(CONSOLE_MIN_BTN).click();
  await expect(page.locator(CONSOLE_PANE)).toBeHidden({ timeout: TIMEOUTS.fileOpen });
}

async function renderDocument(page: Page, fileName: string, succeeded: boolean): Promise<void> {
  type RenderEvent = {
    type: string;
    data?: { target_file?: string; succeeded?: boolean };
  };
  let completed: RenderEvent | undefined;

  // Observe the session's completion event without typing into the Console
  // or assuming that the runner can access files on the rsession host.
  const completion = page.waitForResponse(
    async response => {
      if (!new URL(response.url()).pathname.endsWith('/events/get_events'))
        return false;
      const body = await response.json().catch(() => null) as { result?: RenderEvent[] } | null;
      const event = body?.result?.find(event =>
        event.type === 'rmd_render_completed' &&
        event.data?.target_file?.replace(/\\/g, '/').split('/').pop() === fileName,
      );
      if (!event)
        return false;
      completed = event;
      return true;
    },
    { timeout: RENDER_TIMEOUT },
  );

  await Promise.all([completion, executeCommand(page, 'knitDocument')]);
  expect(completed?.data?.succeeded).toBe(succeeded);

  // get_events queues client-side dispatch. Wait until CompilePanel has
  // processed this document's completion too. Inspect the button's own
  // display property: toBeHidden would also pass while its pane is minimized.
  const toolbar = page.getByRole('toolbar', { name: 'R Markdown Tab', includeHidden: true });
  await expect(toolbar).toContainText(fileName, { timeout: TIMEOUTS.fileOpen });
  await expect(toolbar.getByRole('button', { name: 'Stop', exact: true, includeHidden: true }))
    .toHaveCSS('display', 'none', { timeout: TIMEOUTS.fileOpen });
}

test.describe.serial('Console pane stays minimized across a render', () => {
  const sandbox = useSuiteSandbox();
  let consoleActions: ConsolePaneActions;
  let missingPackages: string[] = [];
  let fileName = '';

  test.beforeAll(async ({ rstudioPage: page }) => {
    test.setTimeout(300000);
    consoleActions = new ConsolePaneActions(page);
    await consoleActions.resetSourcePane();
    missingPackages = await consoleActions.ensurePackages(['rmarkdown'], 180_000);
    await consoleActions.clearConsole();

    // keep the rendered document out of a preview window
    await setPref(page, 'rmd_viewer_type', 'none');
  });

  test.beforeEach(() => {
    test.skip(
      missingPackages.length > 0,
      `required R package(s) not available: ${missingPackages.join(', ')}`,
    );
  });

  test.afterEach(async ({ rstudioPage: page }) => {
    // un-minimize the Console pane for whatever runs next
    await executeCommand(page, 'activateConsolePane');
    await expect(page.locator(CONSOLE_PANE)).toBeVisible({ timeout: TIMEOUTS.fileOpen });
    if (fileName) {
      const stem = fileName.replace(/\.Rmd$/, '');
      await closeAndDeleteSandboxFiles(page, sandbox.dir, [fileName, `${stem}.html`]);
      fileName = '';
    }
  });

  test.afterAll(async ({ rstudioPage: page }) => {
    await clearPref(page, 'rmd_viewer_type');
  });

  test('a successful render minimizes the pane again', async ({ rstudioPage: page }) => {
    fileName = `minimized_ok_${Date.now()}.Rmd`;
    const rmd = heredoc`
      ---
      title: "Minimized console"
      output: html_document
      ---

      Body.
    `;
    await writeAndOpenFile(page, sandbox.dir, fileName, rmd);
    await minimizeConsole(page);

    await renderDocument(page, fileName, true);
    await expect(page.locator(CONSOLE_PANE)).toBeHidden({ timeout: TIMEOUTS.fileOpen });
  });

  test('a successful render keeps a console restored with Ctrl+2 open', async ({ rstudioPage: page }) => {
    fileName = `restored_ok_${Date.now()}.Rmd`;
    const rmd = heredoc`
      ---
      title: "Restored console"
      output: html_document
      ---

      Body.
    `;
    await writeAndOpenFile(page, sandbox.dir, fileName, rmd);
    await minimizeConsole(page);

    // Ctrl+2 uses activateConsole, which must count as the user restoring
    // the pane even though it raises the Console through ensureVisible.
    await page.keyboard.press('Control+2');
    await expect(page.locator(CONSOLE_PANE)).toBeVisible({ timeout: TIMEOUTS.fileOpen });

    await renderDocument(page, fileName, true);
    await expect(page.locator(CONSOLE_PANE)).toBeVisible({ timeout: TIMEOUTS.fileOpen });
  });

  test('a failed render leaves the pane open', async ({ rstudioPage: page }) => {
    fileName = `minimized_fail_${Date.now()}.Rmd`;
    const rmd = heredoc`
      ---
      title: "Minimized console"
      output: html_document
      ---

      ${FENCE}{r}
      stop("render should fail")
      ${FENCE}
    `;
    await writeAndOpenFile(page, sandbox.dir, fileName, rmd);
    await minimizeConsole(page);

    await renderDocument(page, fileName, false);

    // The failed render has completed and its output remains visible.
    const consolePane = page.locator(CONSOLE_PANE);
    await expect(consolePane).toContainText('render should fail', { timeout: RENDER_TIMEOUT });
    await expect(consolePane).toContainText('Execution halted', { timeout: RENDER_TIMEOUT });
    await expect(consolePane).toBeVisible();
  });
});
