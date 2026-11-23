import { test, expect } from '@fixtures/rstudio.fixture';
import { executeCommand } from '@utils/commands';
import { clearConsole, executeInConsole, getConsolePromptText } from '@pages/console_pane.page';
import { consoleOutput, JULIA_PROMPT, JULIA_START_TIMEOUT, R_PROMPT, runInConsole, useJulia } from '@utils/julia';

// RStudio's own switch into and out of the Julia console (adaptToLanguage in
// SessionModuleContext.cpp) isn't echoed: the prompt shows where the Console
// is. Input typed by hand still is. Skipped where Julia isn't installed.

test.describe.serial('Julia console language switch', () => {
  useJulia();

  test('switching languages by command echoes nothing', async ({ rstudioPage: page }) => {
    test.setTimeout(JULIA_START_TIMEOUT + 120_000);

    // into Julia
    await clearConsole(page);
    await executeCommand(page, 'consoleActivateJulia');
    await expect.poll(() => getConsolePromptText(page), { timeout: JULIA_START_TIMEOUT }).toBe(JULIA_PROMPT);
    expect(await consoleOutput(page)).not.toContain('.rs.julia.repl()');

    await executeInConsole(page, 'println("in ", "Julia")');
    await expect(page.locator('#rstudio_console_output')).toContainText('in Julia');

    // and back to R
    await clearConsole(page);
    await executeCommand(page, 'consoleActivateR');
    await expect.poll(() => getConsolePromptText(page), { timeout: 60_000 }).toBe(R_PROMPT);
    expect(await consoleOutput(page)).not.toMatch(/julia> quit/);

    // typed by hand, the entry still shows
    expect(await runInConsole(page, '.rs.julia.repl()', 120_000)).toContain('.rs.julia.repl()');
  });
});
