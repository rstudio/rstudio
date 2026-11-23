import type { Page } from 'playwright';
import { test, expect } from '@fixtures/rstudio.fixture';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { SourcePaneActions } from '@actions/source_pane.actions';
import { clearPref, dismissAllModals, executeCommand, numModalsShowing, setPref } from '@utils/commands';
import { clearConsole, executeInConsole, getConsolePromptText } from '@pages/console_pane.page';
import { closeAndDeleteSandboxFiles, writeAndOpenFile } from '@utils/files';
import { heredoc } from '@utils/heredoc';
import { R_PROMPT } from '@utils/julia';
import { useSuiteSandbox } from '@utils/sandbox';

// The Julia console is experimental: off until the user turns on the
// julia_console_enabled preference. With it off, Julia is treated as it was
// before the Console could run it. None of this starts Julia, so it runs
// where Julia isn't installed; tests/julia/julia_console.test.ts covers the
// console once it's on.

const FENCE = '```';

const RMD = heredoc`
  ---
  title: probe
  ---

  ${FENCE}{julia jlone}
  println("hello from Julia")
  ${FENCE}
`;

const RUN_COMMANDS = ['executeCode', 'executeAllCode', 'sourceActiveDocument', 'executeToCurrentLine'];

const TURNED_OFF = 'The Julia console is experimental, and is turned off.';

async function commandState(page: Page, id: string) {
  return page.evaluate((cmdId) => {
    const cmd = window.rstudio!.commands[cmdId];
    return { visible: cmd.isVisible(), enabled: cmd.isEnabled() };
  }, id);
}

async function languageMenuHasJulia(page: Page): Promise<boolean> {
  await page.locator('#rstudio_console_interpreter_version_tabbed').click();
  const items = page.locator('.gwt-MenuItem, [role=menuitem]');
  await expect(items.filter({ hasText: /^Python$/ })).toBeVisible();
  const count = await items.filter({ hasText: /^Julia$/ }).count();
  await page.keyboard.press('Escape');
  return count > 0;
}

test.describe.serial('Julia console preference', () => {
  const sandbox = useSuiteSandbox();
  let sourceActions: SourcePaneActions;

  test.beforeAll(async ({ rstudioPage: page }) => {
    const consoleActions = new ConsolePaneActions(page);
    sourceActions = new SourcePaneActions(page, consoleActions);
    await consoleActions.resetSourcePane();
    await clearPref(page, 'julia_console_enabled');
  });

  test.afterAll(async ({ rstudioPage: page }) => {
    await clearPref(page, 'julia_console_enabled');
    await clearPref(page, 'rmd_chunk_output_inline');
    await closeAndDeleteSandboxFiles(page, sandbox.dir, ['off.jl', 'on.jl', 'probe.Rmd']);
  });

  test('the console offers no Julia by default', async ({ rstudioPage: page }) => {
    expect((await commandState(page, 'consoleActivateJulia')).visible).toBe(false);
    expect(await languageMenuHasJulia(page)).toBe(false);

    await clearConsole(page);
    await executeInConsole(page, '.rs.julia.repl()');
    await expect(page.locator('#rstudio_console_output')).toContainText(TURNED_OFF);
    expect(await getConsolePromptText(page)).toBe(R_PROMPT);
  });

  test('a Julia script can only be edited by default', async ({ rstudioPage: page }) => {
    await writeAndOpenFile(page, sandbox.dir, 'off.jl', 'x = 1\n');

    for (const id of RUN_COMMANDS)
      expect((await commandState(page, id)).enabled, id).toBe(false);
    expect((await commandState(page, 'commentUncomment')).enabled).toBe(true);
  });

  test('an inline julia chunk runs through knitr by default', async ({ rstudioPage: page }) => {
    test.setTimeout(240_000);
    await writeAndOpenFile(page, sandbox.dir, 'probe.Rmd', RMD);
    await sourceActions.navigateToChunkByLabel('jlone');
    await executeCommand(page, 'executeCurrentChunk');

    // knitr's julia engine runs it from R, whatever it makes of it: the
    // Console doesn't switch to Julia, and nothing asks for JuliaConnectoR
    await expect(page.locator('.rstudio_chunk_output_jlone')).toBeVisible({ timeout: 120_000 });
    expect(await numModalsShowing(page)).toBe(0);
    expect(await getConsolePromptText(page)).toBe(R_PROMPT);
  });

  test('a julia chunk line sent to the console is refused by default', async ({ rstudioPage: page }) => {
    await setPref(page, 'rmd_chunk_output_inline', false);
    await writeAndOpenFile(page, sandbox.dir, 'probe.Rmd', RMD);
    await page.evaluate(() => window.rstudio!.documents.activeEditor()!.gotoLine(6, 0, false));
    await clearConsole(page);
    await executeCommand(page, 'executeCode');

    await expect(page.locator('.gwt-DialogBox, [role=dialog]')).toContainText(TURNED_OFF);
    await dismissAllModals(page);
    expect(await getConsolePromptText(page)).toBe(R_PROMPT);
    await expect(page.locator('#rstudio_console_output')).not.toContainText('hello from Julia');
    await clearPref(page, 'rmd_chunk_output_inline');
  });

  test('turning the preference on offers Julia, and off again takes it away', async ({ rstudioPage: page }) => {
    await setPref(page, 'julia_console_enabled', true);

    await expect.poll(async () => (await commandState(page, 'consoleActivateJulia')).visible).toBe(true);
    expect(await languageMenuHasJulia(page)).toBe(true);

    // a Julia script opened now can be run
    await writeAndOpenFile(page, sandbox.dir, 'on.jl', 'y = 2\n');
    for (const id of RUN_COMMANDS)
      expect((await commandState(page, id)).enabled, id).toBe(true);

    await clearPref(page, 'julia_console_enabled');

    await expect.poll(async () => (await commandState(page, 'consoleActivateJulia')).visible).toBe(false);
    expect(await languageMenuHasJulia(page)).toBe(false);
  });
});
