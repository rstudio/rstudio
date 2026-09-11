// A successful render preserves the Console pane's minimized state (#11622).
// An explicit user restore keeps it open, and a failed render opens the pane
// so its output can be read.

import type { Page } from 'playwright';
import { test, expect } from '@fixtures/rstudio.fixture';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { sleep, TIMEOUTS } from '@utils/constants';
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

// Leave time to interact with the pane while the renderer is running. The
// helper below also checks that the interaction finished before completion.
const SLOW_RMD = heredoc`
  ---
  title: "Console interaction during render"
  output: html_document
  ---

  ${FENCE}{r}
  Sys.sleep(10)
  ${FENCE}
`;

async function minimizeConsole(page: Page): Promise<void> {
  await page.locator(CONSOLE_MIN_BTN).click();
  await expect(page.locator(CONSOLE_PANE)).toBeHidden({ timeout: TIMEOUTS.fileOpen });
}

async function minimizeConsoleWithRenderTab(page: Page): Promise<void> {
  // The Render tab is created lazily. Materialize it before minimizing so
  // the next render can raise its existing pane through ensureVisible.
  await executeCommand(page, 'activateRMarkdown');
  await executeCommand(page, 'activateConsole');
  await minimizeConsole(page);
}

async function renderDocument(
  page: Page,
  fileName: string,
  succeeded: boolean,
  whileRendering?: () => Promise<void>,
): Promise<void> {
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

  const toolbar = page.getByRole('toolbar', { name: 'R Markdown Tab', includeHidden: true });
  const stop = toolbar.getByRole('button', { name: 'Stop', exact: true, includeHidden: true });
  const render = async () => {
    await executeCommand(page, 'knitDocument');
    if (whileRendering) {
      await expect(toolbar).toContainText(fileName, { timeout: TIMEOUTS.fileOpen });
      await expect(stop).toBeVisible({ timeout: TIMEOUTS.fileOpen });
      await whileRendering();
      expect(completed, 'interaction must finish before the render completes').toBeUndefined();
      await expect(stop).not.toHaveCSS('display', 'none');
    }
  };
  await Promise.all([completion, render()]);
  expect(completed?.data?.succeeded).toBe(succeeded);

  // get_events queues client-side dispatch. Wait until CompilePanel has
  // processed this document's completion too. Inspect the button's own
  // display property: toBeHidden would also pass while its pane is minimized.
  await expect(toolbar).toContainText(fileName, { timeout: TIMEOUTS.fileOpen });
  await expect(stop).toHaveCSS('display', 'none', { timeout: TIMEOUTS.fileOpen });
  await expect(page.locator('body')).not.toHaveClass(/rstudio-animating/);
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
    await clearPref(page, 'reduced_motion');
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

  test('clicking the Console tab during rendering keeps the pane open', async ({ rstudioPage: page }) => {
    fileName = `clicked_ok_${Date.now()}.Rmd`;
    await writeAndOpenFile(page, sandbox.dir, fileName, SLOW_RMD);
    await minimizeConsoleWithRenderTab(page);

    await renderDocument(page, fileName, true, async () => {
      await page.locator('#rstudio_workbench_tab_console').click();
      await expect(page.locator('#rstudio_console_input .ace_text-input')).toBeFocused();
    });
    await expect(page.locator(CONSOLE_PANE)).toBeVisible();
  });

  test('typing directly into Console during rendering keeps the pane open', async ({ rstudioPage: page }) => {
    fileName = `typed_ok_${Date.now()}.Rmd`;
    await writeAndOpenFile(page, sandbox.dir, fileName, SLOW_RMD);
    await minimizeConsoleWithRenderTab(page);

    await renderDocument(page, fileName, true, async () => {
      // Select without a mouse-down so this independently exercises keyboard
      // interaction, not the tab's mouse handler or activateConsole command.
      await page.locator('#rstudio_workbench_tab_console').dispatchEvent('click');
      const input = page.locator('#rstudio_console_input .ace_text-input');
      await input.pressSequentially('1 + 1');
      await input.press('Enter');
      await expect(page.locator('#rstudio_workbench_panel_console')).toContainText('[1] 2');
    });
    await expect(page.locator(CONSOLE_PANE)).toBeVisible();
  });

  test('automatic minimization preserves focus in another pane', async ({ rstudioPage: page }) => {
    fileName = `focused_ok_${Date.now()}.Rmd`;
    await writeAndOpenFile(page, sandbox.dir, fileName, SLOW_RMD);
    await setPref(page, 'reduced_motion', false);
    await minimizeConsoleWithRenderTab(page);

    const search = page.getByRole('textbox', { name: 'Search environment', exact: true });
    await renderDocument(page, fileName, true, async () => {
      await executeCommand(page, 'activateEnvironment');
      await search.click();
      await expect(search).toBeFocused();
    });
    await expect(page.locator(CONSOLE_PANE)).toBeHidden();
    // The 100 ms focus restoration can briefly win before the pane's 250 ms
    // animation finishes. Check the settled focus, not that transient state.
    await sleep(500);
    await expect(search).toBeFocused();
  });
});
