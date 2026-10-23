// R code dispatched from the idle event loop -- a later::later() callback,
// e.g. shiny::runApp() scheduled that way "to keep the console free" -- used to
// leave the session deaf until the callback returned (#19236): the console
// looked idle, so no request was served (interrupt included) and the IDE
// could only be recovered by killing rsession. The session now keeps serving
// requests while such a callback runs, reports R as busy after a moment so the
// interrupt is offered, honours the interrupt, and queues console input sent
// meanwhile for the next prompt.

import { test, expect } from '@fixtures/rstudio.fixture';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { clearPref, executeCommand, setPref } from '@utils/commands';
import { withDeadline } from '@utils/deadline';
import {
  ensureConsoleIdle,
  executeInConsole,
  getConsolePromptCount,
  waitForConsoleCommandComplete,
} from '@pages/console_pane.page';

const CONSOLE_INPUT = '#rstudio_console_input';
const BUSY_CLASS = /rstudio-console-busy/;

// Comfortably past the server's one-second busy delay, on a slow runner.
const BUSY_TIMEOUT = 15000;

test.describe('Idle event loop callbacks', () => {
  let consoleActions: ConsolePaneActions;

  test.beforeAll(async ({ rstudioPage: page }) => {
    consoleActions = new ConsolePaneActions(page);
    const missing = await consoleActions.ensurePackages(['later']);
    test.skip(missing.length > 0, `Could not install: ${missing.join(', ')}`);
  });

  test.beforeEach(async () => {
    await consoleActions.clearConsole();
  });

  test.afterEach(async ({ rstudioPage: page }) => {
    // A failed assertion can leave the callback running; interrupting it
    // here is what keeps the rest of the worker usable.
    await ensureConsoleIdle(page);
    await clearPref(page, 'blinking_cursor');
  });

  test('requests are served and the interrupt works while a callback runs', async ({ rstudioPage: page }) => {
    const consoleInput = page.locator(CONSOLE_INPUT);

    // The call itself returns at once; the callback starts 100ms later and
    // runs until interrupted.
    await executeInConsole(page, 'later::later(function() { while (TRUE) Sys.sleep(0.05) }, 0.1)');

    // Nothing was typed, yet R is busy: the client is told so a moment in.
    await expect(consoleInput).toHaveClass(BUSY_CLASS, { timeout: BUSY_TIMEOUT });

    // A pref write is a plain RPC whose completion the bridge reports; it
    // never came back while the callback ran. Bounded so a regression is a
    // fast, descriptive failure rather than a hung hook.
    await withDeadline(setPref(page, 'blinking_cursor', false), 15000, 'pref write while a callback runs');

    // The interrupt reaches the callback, and the prompt comes back.
    await executeCommand(page, 'interruptR');
    await expect(consoleInput).not.toHaveClass(BUSY_CLASS, { timeout: BUSY_TIMEOUT });

    await executeInConsole(page, 'cat("console", "alive\\n")');
    await expect(consoleActions.consolePane.consoleOutput).toContainText('console alive');
  });

  test('console input sent while a callback runs is queued, in order, for the next prompt', async ({ rstudioPage: page }) => {
    // The callback announces itself so the input below is sent while it is
    // running, and ends on its own: an interrupt would discard queued input.
    await executeInConsole(page, 'later::later(function() { cat("callback", "started\\n"); Sys.sleep(3) }, 0.1)');
    await expect(consoleActions.consolePane.consoleOutput).toContainText('callback started');

    // Two submissions whose second depends on the first: queued input must
    // run in the order it was sent.
    const promptCountBefore = await getConsolePromptCount(page);
    await executeInConsole(page, '.idle_callback_test <- "after callback"', { wait: false });
    await executeInConsole(page, 'cat(.idle_callback_test, "ran", "second\\n")', { wait: false });

    await waitForConsoleCommandComplete(page, promptCountBefore, 20000);
    await expect(consoleActions.consolePane.consoleOutput).toContainText('after callback ran second');
    await expect(consoleActions.consolePane.consoleOutput).not.toContainText('Error');
  });

  test('a read opened by a callback gets its own prompt, and the REPL prompt returns after it', async ({ rstudioPage: page }) => {
    // The prompt sits in the cell before the input's, in the shell's input row.
    const promptText = () => page.evaluate(() =>
      document.getElementById('rstudio_console_input')?.closest('tr')?.querySelector('td')?.textContent ?? '');

    // A callback that reads from the console: its prompt replaces the REPL's
    // while it waits. (later never runs another callback meanwhile, so the
    // busy-while-reading path cannot be driven from here.)
    await executeInConsole(page, 'later::later(function() .idle_callback_answer <<- readline("answer? "), 0.1)');
    await expect.poll(promptText, { timeout: 5000 }).toBe('answer? ');

    // Answering ends the callback; the REPL prompt comes back on its own (a
    // new prompt event, which is what executeInConsole waits for). Without
    // that the console would sit without a prompt until something else
    // issued one.
    await executeInConsole(page, 'hello');
    expect(await promptText()).toBe('> ');
    await executeInConsole(page, 'cat("the answer was", .idle_callback_answer, "\\n")');
    await expect(consoleActions.consolePane.consoleOutput).toContainText('the answer was hello');
  });
});
