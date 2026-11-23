import { test, expect } from '@fixtures/rstudio.fixture';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { clearPref, executeCommand, setPref } from '@utils/commands';
import { clearConsole, executeInConsole, getConsolePromptText } from '@pages/console_pane.page';
import { setConsoleInput } from '@utils/console';
import { closeAndDeleteSandboxFiles, writeAndOpenFile } from '@utils/files';
import { heredoc } from '@utils/heredoc';
import {
  consoleInput,
  consoleOutput,
  JULIA_PROMPT,
  JULIA_START_TIMEOUT,
  R_PROMPT,
  runInConsole,
  useJulia,
} from '@utils/julia';
import { sleep } from '@utils/constants';
import { useSuiteSandbox } from '@utils/sandbox';

// Typing at the Julia prompt: the console's input stays an R editor, so quote
// pairing, signature tooltips and completions have to follow the prompt's
// language. Skipped where Julia isn't installed.

const FENCE = '```';

// R's signature tooltip for sum(
const R_SIGNATURE = 'na.rm = FALSE';

test.describe.serial('Julia console editing', () => {
  const sandbox = useSuiteSandbox();
  useJulia();

  test.beforeAll(async ({ rstudioPage: page }) => {
    await new ConsolePaneActions(page).resetSourcePane();
  });

  test.afterAll(async ({ rstudioPage: page }) => {
    await setConsoleInput(page, '');
    await clearPref(page, 'rmd_chunk_output_inline');
    await closeAndDeleteSandboxFiles(page, sandbox.dir, ['probe_sel.Rmd']);
  });

  test('the R prompt pairs quotes and shows R signatures', async ({ rstudioPage: page }) => {
    // the control for the next test: what the Julia prompt must not do
    await setConsoleInput(page, '');
    await page.locator('#rstudio_console_input').click();
    await page.keyboard.type("x'");
    expect(await consoleInput(page)).toBe("x''");

    await setConsoleInput(page, '');
    await page.keyboard.type('sum(');
    await expect(page.getByText(R_SIGNATURE).first()).toBeVisible({ timeout: 10_000 });
    await page.keyboard.press('Escape');
    await setConsoleInput(page, '');
  });

  test('the Julia prompt types as Julia does', async ({ rstudioPage: page }) => {
    test.setTimeout(JULIA_START_TIMEOUT + 120_000);

    await executeInConsole(page, '.rs.julia.repl()', { timeout: JULIA_START_TIMEOUT });
    expect(await getConsolePromptText(page)).toBe(JULIA_PROMPT);
    await executeInConsole(page, 'A = [1 2; 3 4];');

    // A' is the adjoint, not the start of a character literal
    await clearConsole(page);
    await page.locator('#rstudio_console_input').click();
    await page.keyboard.type("A'");
    expect(await consoleInput(page)).toBe("A'");
    await page.keyboard.press('Enter');
    await expect.poll(() => consoleOutput(page), { timeout: 30_000 }).toContain('adjoint');

    // No R signature for sum( at the Julia prompt. The R prompt shows one
    // within moments (the test above), so sample for as long: a poll would
    // pass on its first look, before a tooltip could have come up.
    await setConsoleInput(page, '');
    await page.keyboard.type('sum(');
    for (let i = 0; i < 10; i++) {
      expect(await page.getByText(R_SIGNATURE).count()).toBe(0);
      await sleep(200);
    }
    await setConsoleInput(page, '');

    // a member typed with the popup already open is completed in its module
    await page.keyboard.type('Bas', { delay: 50 });
    const popup = page.locator('#rstudio_popup_completions');
    await expect(popup).toBeVisible({ timeout: 10_000 });
    await page.keyboard.type('e.Mat', { delay: 150 });
    await expect(popup).toContainText('Math', { timeout: 10_000 });
    await page.keyboard.press('Escape');
    await setConsoleInput(page, '');
  });

  test('whole lines selected to a chunk fence run in Julia', async ({ rstudioPage: page }) => {
    test.setTimeout(180_000);

    await executeInConsole(page, 'exit');
    expect(await getConsolePromptText(page)).toBe(R_PROMPT);
    await setPref(page, 'rmd_chunk_output_inline', false);

    const rmd = heredoc`
      ---
      title: probe
      ---

      ${FENCE}{julia}
      jl_sel = 7
      ${FENCE}
    `;
    await writeAndOpenFile(page, sandbox.dir, 'probe_sel.Rmd', rmd);

    // as Shift+Down from the start of 'jl_sel = 7' selects it: to column 0
    // of the closing fence
    await page.evaluate(() => {
      const editor = window.rstudio!.documents.activeEditor()!;
      editor.gotoLine(6, 0, false);
      editor.selection.selectDown();
      editor.focus();
    });
    await executeCommand(page, 'executeCode');
    await expect.poll(() => getConsolePromptText(page), { timeout: 60_000 }).toBe(JULIA_PROMPT);

    expect(await runInConsole(page, 'jl_sel + 1')).toMatch(/\b8\b/);
    await executeInConsole(page, 'exit');
    expect(await runInConsole(page, 'cat(exists("jl_sel"), "\\n")')).toContain('FALSE');
  });
});
