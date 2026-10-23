// R code dispatched from the idle event loop -- a later::later() callback,
// e.g. shiny::runApp() scheduled that way "to keep the console free" -- used to
// leave the session deaf until the callback returned (#19236): the console
// looked idle, so no request was served (interrupt included) and the IDE
// could only be recovered by killing rsession. The session now keeps serving
// requests while such a callback runs, reports R as busy after a moment so the
// interrupt is offered, honours the interrupt, and queues console input sent
// meanwhile for the next prompt.

import * as fs from 'fs';
import * as path from 'path';
import type { Page, Response } from '@playwright/test';
import { test, expect } from '@fixtures/rstudio.fixture';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { clearPref, executeCommand, setPref } from '@utils/commands';
import { withDeadline } from '@utils/deadline';
import { canWriteDir } from '@utils/files';
import { useSuiteSandbox } from '@utils/sandbox';
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

/**
 * Submit console input and wait until the session's main thread has taken
 * it. The listener thread acknowledges the console_input RPC at once with an
 * asyncHandle; the main thread's handling (queuing the input behind whatever
 * R is doing) arrives later as an async_completion event carrying that
 * handle on the events long-poll. Returning on the acknowledgement alone
 * would race the server: an interrupt sent then could discard an empty queue
 * and leave the input to run afterwards. The events listener is installed
 * before submitting, since the completion can land while the
 * acknowledgement is still being read.
 */
async function submitAndAwaitQueued(page: Page, command: string): Promise<void> {
  const events: string[] = [];
  let wanted: string | undefined;
  let found: (() => void) | undefined;
  const onResponse = async (response: Response): Promise<void> => {
    if (!/\/events\/get_events/.test(response.url()))
      return;
    const text = await response.text().catch(() => '');
    events.push(text);
    if (wanted !== undefined && text.includes(wanted))
      found?.();
  };
  page.on('response', onResponse);
  try {
    const [ack] = await Promise.all([
      page.waitForResponse((r) => /\/rpc\/console_input/.test(r.url()), { timeout: 10000 }),
      executeInConsole(page, command, { wait: false }),
    ]);
    const body = await ack.json().catch(() => ({}));
    const handle = body?.asyncHandle;
    if (typeof handle !== 'string')
      return; // answered inline: the main thread has already handled it
    await withDeadline(
      new Promise<void>((resolve) => {
        found = resolve;
        wanted = handle;
        if (events.some((text) => text.includes(handle)))
          resolve();
      }),
      10000,
      'async completion of console_input',
    );
  } finally {
    page.off('response', onResponse);
  }
}

/**
 * Record client events of the given types as they arrive on the events
 * long-poll (each get_events response carries an array of {type, data}).
 * Installed before the behavior under test so nothing is missed; the
 * console's own busy class is cleared by any prompt, so an assertion about
 * the busy EVENT (which is what the workbench tracks) has to read the wire.
 */
function recordClientEvents(page: Page, types: string[]): { events: { type: string; data: unknown }[]; stop(): void } {
  const events: { type: string; data: unknown }[] = [];
  const onResponse = async (response: Response): Promise<void> => {
    if (!/\/events\/get_events/.test(response.url()))
      return;
    const body = await response.json().catch(() => null) as { result?: unknown } | null;
    if (!Array.isArray(body?.result))
      return;
    for (const event of body.result as { type?: unknown; data?: unknown }[]) {
      if (typeof event?.type === 'string' && types.includes(event.type))
        events.push({ type: event.type, data: event.data });
    }
  };
  page.on('response', onResponse);
  return { events, stop: () => page.off('response', onResponse) };
}

test.describe('Idle event loop callbacks', () => {
  let consoleActions: ConsolePaneActions;
  const sandbox = useSuiteSandbox();

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
    await clearPref(page, 'discard_pending_console_input_on_error');
    await executeInConsole(page, 'options(rstudio.idleEventBusyDelay = NULL)');
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

  test('an interrupt before R is reported busy discards queued input and returns the prompt', async ({ rstudioPage: page }) => {
    const consoleInput = page.locator(CONSOLE_INPUT);
    // The interrupt's console reset discards queued input under this pref
    // (its default); the test relies on that, so say so.
    await setPref(page, 'discard_pending_console_input_on_error', true);
    // Keep the busy report out of the picture however slow the runner: this
    // is the path where the interrupt lands before it.
    await executeInConsole(page, 'options(rstudio.idleEventBusyDelay = 60)');

    await executeInConsole(page, 'later::later(function() { cat("callback", "started\\n"); while (TRUE) Sys.sleep(0.05) }, 0.1)');
    await expect(consoleActions.consolePane.consoleOutput).toContainText('callback started');

    // Submitting marks the client busy, and the server queues the input
    // behind the callback. Escape in the (now empty) console sends the
    // interrupt at once -- ahead of the busy report, which is what enables
    // the interrupt command (the client's own busy state does not).
    await submitAndAwaitQueued(page, 'cat("discarded", "input\\n")');
    await expect(consoleInput).toHaveClass(BUSY_CLASS, { timeout: BUSY_TIMEOUT });
    const busyReported = await page.evaluate(() => window.rstudio?.commands?.interruptR?.isEnabled() ?? null);
    expect(busyReported, 'the delayed busy report must not have fired').toBe(false);
    await page.keyboard.press('Escape');

    // The interrupt ends the callback and discards the queued input; a prompt
    // still arrives, since the client has nothing else to clear its busy state.
    await expect(consoleInput).not.toHaveClass(BUSY_CLASS, { timeout: BUSY_TIMEOUT });
    await executeInConsole(page, 'cat("console", "alive\\n")');
    await expect(consoleActions.consolePane.consoleOutput).toContainText('console alive');
    await expect(consoleActions.consolePane.consoleOutput).not.toContainText('discarded input');
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

  test('a callback that keeps running after its read is answered is reported busy again', async ({ rstudioPage: page }) => {
    const consoleInput = page.locator(CONSOLE_INPUT);
    const promptText = () => page.evaluate(() =>
      document.getElementById('rstudio_console_input')?.closest('tr')?.querySelector('td')?.textContent ?? '');
    const interruptEnabled = () => page.evaluate(() => window.rstudio?.commands?.interruptR?.isEnabled() ?? false);

    await executeInConsole(page, 'later::later(function() { .idle_callback_answer <<- readline("answer? "); while (TRUE) Sys.sleep(0.05) }, 0.1)');
    await expect.poll(promptText, { timeout: 5000 }).toBe('answer? ');

    // Answering the read hands R the answer -- which marks console input as
    // executing, as for any read -- and the callback runs on. The client is
    // busy from its own submit, but only the server's busy report offers the
    // interrupt; it has to come even though the read's prompt cleared the
    // client's state, and the executing flag must not suppress it.
    await executeInConsole(page, 'hello', { wait: false });
    await expect(consoleInput).toHaveClass(BUSY_CLASS, { timeout: BUSY_TIMEOUT });
    await expect.poll(interruptEnabled, { timeout: BUSY_TIMEOUT }).toBe(true);

    await executeCommand(page, 'interruptR');
    await expect(consoleInput).not.toHaveClass(BUSY_CLASS, { timeout: BUSY_TIMEOUT });
    expect(await promptText()).toBe('> ');
    await executeInConsole(page, 'cat("the answer was", .idle_callback_answer, "\\n")');
    await expect(consoleActions.consolePane.consoleOutput).toContainText('the answer was hello');
  });

  test('a callback reported busy that reads and then returns at once leaves the console idle', async ({ rstudioPage: page }) => {
    // The callback reads only once the test has seen the busy report (a flag
    // file says so), which needs a filesystem shared with the session.
    test.skip(!canWriteDir(sandbox.dir), 'the runner cannot write where the session reads (remote server)');
    const consoleInput = page.locator(CONSOLE_INPUT);
    const promptText = () => page.evaluate(() =>
      document.getElementById('rstudio_console_input')?.closest('tr')?.querySelector('td')?.textContent ?? '');
    const flag = path.join(sandbox.dir, `busy-seen-${Date.now()}.flag`);
    const busy = recordClientEvents(page, ['busy']);
    // R's own busy callback adds events of its own around a read, so the
    // sequence is not fixed; what matters is that busy was reported, and
    // that the last word once the callback has returned is "not busy".
    const busyValues = () => busy.events.map((e) => (e.data as { value?: unknown })?.value);
    try {
      // Busy is reported before the read; the read's prompt clears the
      // client's busy state and re-arms the report; the callback then returns
      // well before it would fire again. The busy=false event still has to
      // come: the workbench tracks that event, not the console's prompt.
      await executeInConsole(page,
        `later::later(function() { while (!file.exists(${JSON.stringify(flag)})) Sys.sleep(0.05); .idle_callback_answer <<- readline("answer? "); Sys.sleep(0.2) }, 0.1)`);
      await expect.poll(() => busyValues().includes(true), { timeout: BUSY_TIMEOUT }).toBe(true);
      await expect(consoleInput).toHaveClass(BUSY_CLASS, { timeout: BUSY_TIMEOUT });
      fs.writeFileSync(flag, '');
      await expect.poll(promptText, { timeout: 10000 }).toBe('answer? ');
      await expect(consoleInput).not.toHaveClass(BUSY_CLASS);

      await executeInConsole(page, 'hello');
      expect(await promptText()).toBe('> ');
      await expect(consoleInput).not.toHaveClass(BUSY_CLASS);
      await expect.poll(() => busyValues().at(-1), { timeout: 5000 }).toBe(false);
    } finally {
      busy.stop();
    }
    await executeInConsole(page, 'cat("the answer was", .idle_callback_answer, "\\n")');
    await expect(consoleActions.consolePane.consoleOutput).toContainText('the answer was hello');
  });

  test('input queued during a callback that then reads it brings the prompt back', async ({ rstudioPage: page }) => {
    // The callback waits for a file the runner writes once the input is
    // confirmed queued, so the read always finds it queued; that needs a
    // filesystem shared with the session.
    test.skip(!canWriteDir(sandbox.dir), 'the runner cannot write where the session reads (remote server)');
    const consoleInput = page.locator(CONSOLE_INPUT);
    const promptText = () => page.evaluate(() =>
      document.getElementById('rstudio_console_input')?.closest('tr')?.querySelector('td')?.textContent ?? '');
    const flag = path.join(sandbox.dir, `read-queued-${Date.now()}.flag`);
    const prompts = recordClientEvents(page, ['console_prompt']);
    try {
      // Input submitted while the callback runs is queued; the callback's
      // readline() then takes it straight from the queue, with no prompt of
      // its own. The client is busy from its submit, and only the prompt
      // restored when the callback returns clears that.
      // The input is a number: a bare word can raise a completion popup, and
      // the Escape that dismisses one is an interrupt in an empty console.
      await executeInConsole(page,
        `later::later(function() { cat("callback", "started\\n"); while (!file.exists(${JSON.stringify(flag)})) Sys.sleep(0.05); .idle_callback_answer <<- readline("answer? ") }, 0.1)`);
      await expect(consoleActions.consolePane.consoleOutput).toContainText('callback started');
      await submitAndAwaitQueued(page, '42');
      await expect(consoleInput).toHaveClass(BUSY_CLASS, { timeout: BUSY_TIMEOUT });
      fs.writeFileSync(flag, '');

      await expect(consoleInput).not.toHaveClass(BUSY_CLASS, { timeout: BUSY_TIMEOUT });
      expect(await promptText()).toBe('> ');
      // The read never prompted: the only prompts seen are the REPL's.
      const promptTexts = prompts.events.map((e) => (e.data as { prompt?: unknown })?.prompt);
      expect(promptTexts).not.toContain('answer? ');
    } finally {
      prompts.stop();
    }
    await executeInConsole(page, 'cat("the answer was", .idle_callback_answer, "\\n")');
    await expect(consoleActions.consolePane.consoleOutput).toContainText('the answer was 42');
  });
});
