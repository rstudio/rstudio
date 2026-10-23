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
import { SourcePaneActions } from '@actions/source_pane.actions';
import { clearPref, executeCommand, setPref } from '@utils/commands';
import { withDeadline } from '@utils/deadline';
import { TIMEOUTS } from '@utils/constants';
import { canWriteDir, closeAndDeleteSandboxFiles, writeAndOpenFile } from '@utils/files';
import { useSuiteSandbox } from '@utils/sandbox';
import {
  ensureConsoleIdle,
  executeInConsole,
  getConsolePromptCount,
  getConsolePromptText,
  waitForConsoleCommandComplete,
} from '@pages/console_pane.page';

const CONSOLE_INPUT = '#rstudio_console_input';
const BUSY_CLASS = /rstudio-console-busy/;

// Comfortably past the server's one-second busy delay, on a slow runner.
const BUSY_TIMEOUT = 15000;

/**
 * Run `trigger`, which sends the RPC named by `rpc`, and wait until the
 * session's main thread has served it. While R is busy the listener thread
 * acknowledges an RPC at once with an asyncHandle; the main thread's
 * handling arrives later as an async_completion event carrying that handle
 * on the events long-poll. Returning on the acknowledgement alone would race
 * the server. The events listener is installed before triggering, since the
 * completion can land while the acknowledgement is still being read.
 */
async function awaitServed(page: Page, rpc: RegExp, trigger: () => Promise<unknown>): Promise<void> {
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
      page.waitForResponse((r) => rpc.test(r.url()), { timeout: 10000 }),
      trigger(),
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
      `async completion of ${rpc.source}`,
    );
  } finally {
    page.off('response', onResponse);
  }
}

/**
 * Submit console input and wait until the session's main thread has taken
 * it (queued it behind whatever R is doing): an interrupt sent on the
 * acknowledgement alone could discard an empty queue and leave the input to
 * run afterwards.
 */
async function submitAndAwaitQueued(page: Page, command: string): Promise<void> {
  await awaitServed(page, /\/rpc\/console_input/, () => executeInConsole(page, command, { wait: false }));
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

  // Flag files a test's callback waits on; written in afterEach so a test
  // that failed before releasing its callback does not leave it running --
  // with the busy delay raised, the console looks idle and an interrupt is
  // not offered, so the cleanup commands would only queue behind it.
  const flags: string[] = [];
  const flagFile = (name: string): string => {
    const file = path.join(sandbox.dir, name);
    flags.push(file);
    return file;
  };

  test.afterEach(async ({ rstudioPage: page }) => {
    for (const file of flags.splice(0)) {
      try {
        fs.writeFileSync(file, '');
      } catch {
        // the sandbox is gone or not writable from here; nothing waits on it
      }
    }
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

  test('a zero busy delay does not report the idle dispatch itself as busy', async ({ rstudioPage: page }) => {
    const events = recordClientEvents(page, ['console_prompt', 'busy']);
    try {
      // The idle loop dispatches R's input handlers twenty times a second,
      // and the server's polled event handler runs inside that dispatch; a
      // delay of zero must not turn each dispatch into a busy report and a
      // reissued prompt (which resets history navigation and refreshes the
      // panes). The delay is bounded below by the dispatch grace.
      await executeInConsole(page, 'options(rstudio.idleEventBusyDelay = 0)');
      const seen = events.events.length;
      // A bounded wait is the only way to observe that nothing happens: a
      // dozen dispatches, with no callback due.
      await page.waitForTimeout(750);
      expect(events.events.slice(seen)).toEqual([]);
    } finally {
      events.stop();
    }
  });

  test('a reload while a callback runs gets the prompt back and is reported busy again', async ({ rstudioPage: page }) => {
    const consoleInput = page.locator(CONSOLE_INPUT);
    const interruptEnabled = () => page.evaluate(() => window.rstudio?.commands?.interruptR?.isEnabled() ?? false);
    await executeInConsole(page, 'later::later(function() { while (TRUE) Sys.sleep(0.05) }, 0.1)');
    await expect(consoleInput).toHaveClass(BUSY_CLASS, { timeout: BUSY_TIMEOUT });

    // The reload's client_init is served while the callback runs, and is
    // answered as the waiting read would answer it: with its prompt, so the
    // reloaded console is not left without one. The callback runs on, so R
    // is reported busy once more and the interrupt is offered again.
    await page.reload();
    await page.waitForFunction(() => window.rstudio?.ready === true, null, {
      timeout: TIMEOUTS.sessionRestart,
      polling: 50,
    });
    await expect.poll(() => getConsolePromptText(page), { timeout: BUSY_TIMEOUT }).toBe('> ');
    await expect(consoleInput).toHaveClass(BUSY_CLASS, { timeout: BUSY_TIMEOUT });
    await expect.poll(interruptEnabled, { timeout: BUSY_TIMEOUT }).toBe(true);

    await executeCommand(page, 'interruptR');
    await expect(consoleInput).not.toHaveClass(BUSY_CLASS, { timeout: BUSY_TIMEOUT });
    await executeInConsole(page, 'cat("console", "alive\\n")');
    await expect(consoleActions.consolePane.consoleOutput).toContainText('console alive');
  });

  test('a request served during a callback that returns before the busy report leaves the prompt alone', async ({ rstudioPage: page }) => {
    // The callback returns only once the test has had a request served (a
    // flag file says so), which needs a filesystem shared with the session.
    test.skip(!canWriteDir(sandbox.dir), 'the runner cannot write where the session reads (remote server)');
    const flag = flagFile(`request-served-${Date.now()}.flag`);
    await executeInConsole(page, 'options(rstudio.idleEventBusyDelay = 60)');
    const promptCountBefore = await getConsolePromptCount(page);

    // The client was never told R was busy and submitted nothing, so no
    // prompt is owed: one would reset the console's history position (and
    // the unfinished command Down-arrow brings back) for no command.
    await executeInConsole(page,
      `later::later(function() { cat("callback", "started\\n"); while (!file.exists(${JSON.stringify(flag)})) Sys.sleep(0.05); cat("callback", "done\\n") }, 0.1)`);
    await expect(consoleActions.consolePane.consoleOutput).toContainText('callback started');
    await awaitServed(page, /\/rpc\/set_user_prefs/, () => setPref(page, 'blinking_cursor', false));
    fs.writeFileSync(flag, '');

    // Only the call itself and the command below complete: two prompts. The
    // command goes in once the callback has announced its return, so it is
    // not queued behind the callback (pending input would hold any prompt
    // back by itself); a prompt reissued on the return is enqueued before
    // the command can be read, so the count after its output is decisive.
    await expect(consoleActions.consolePane.consoleOutput).toContainText('callback done');
    await executeInConsole(page, 'cat("console", "alive\\n")');
    await expect(consoleActions.consolePane.consoleOutput).toContainText('console alive');
    expect(await getConsolePromptCount(page)).toBe(promptCountBefore === null ? null : promptCountBefore + 2);
  });

  test('a notebook chunk run while a callback runs executes once the callback returns', async ({ rstudioPage: page }) => {
    // The callback returns only once the chunk's execution request has been
    // served (a flag file says so), which needs a filesystem shared with
    // the session; the chunk reports by file too, since its output goes to
    // the editor rather than the console.
    test.skip(!canWriteDir(sandbox.dir), 'the runner cannot write where the session reads (remote server)');
    const missing = await consoleActions.ensurePackages(['rmarkdown', 'knitr']);
    test.skip(missing.length > 0, `Could not install: ${missing.join(', ')}`);
    const sourceActions = new SourcePaneActions(page, consoleActions);
    const flag = flagFile(`chunk-release-${Date.now()}.flag`);
    const ran = flagFile(`chunk-ran-${Date.now()}.flag`);
    const fileName = 'idle-callback-chunk.Rmd';
    const notebook = [
      '---', 'output: html_document', '---', '',
      '```{r}', `file.create(${JSON.stringify(ran)})`, '```', '',
    ].join('\n');
    await executeInConsole(page, 'options(rstudio.idleEventBusyDelay = 60)');

    await writeAndOpenFile(page, sandbox.dir, fileName, notebook);
    try {
      await sourceActions.ensureSourceMode();
      await sourceActions.navigateToChunkByIndex(1);

      // The execution request is served while the callback runs and finds R
      // busy, so the chunk is deferred. The callback then returns to the
      // waiting read with nothing to prompt for: the deferred chunk has to
      // run all the same, not wait for the next console command.
      await executeInConsole(page,
        `later::later(function() { cat("callback", "started\\n"); while (!file.exists(${JSON.stringify(flag)})) Sys.sleep(0.05) }, 0.1)`);
      await expect(consoleActions.consolePane.consoleOutput).toContainText('callback started');
      await awaitServed(page, /\/rpc\/execute_notebook_chunks/, () => executeCommand(page, 'executeCurrentChunk'));
      fs.writeFileSync(flag, '');

      await expect.poll(() => fs.existsSync(ran), { timeout: BUSY_TIMEOUT }).toBe(true);
    } finally {
      await closeAndDeleteSandboxFiles(page, sandbox.dir, [fileName]);
    }
  });

  test('a reload during a callback that then returns before the busy report leaves the prompt not busy', async ({ rstudioPage: page }) => {
    test.skip(!canWriteDir(sandbox.dir), 'the runner cannot write where the session reads (remote server)');
    const flag = flagFile(`reload-early-${Date.now()}.flag`);
    await executeInConsole(page, 'options(rstudio.idleEventBusyDelay = 60)');
    const prompts = recordClientEvents(page, ['console_prompt']);
    try {
      await executeInConsole(page,
        `later::later(function() { cat("callback", "started\\n"); while (!file.exists(${JSON.stringify(flag)})) Sys.sleep(0.05) }, 0.1)`);
      await expect(consoleActions.consolePane.consoleOutput).toContainText('callback started');

      // The reload's client_init is answered with the waiting read's prompt
      // while R is mid-callback, so that prompt reports R busy (the chat
      // pane and notebook execution key off that field). The callback then
      // returns before R is reported busy: the prompt still has to be
      // reissued, now not busy.
      await page.reload();
      await page.waitForFunction(() => window.rstudio?.ready === true, null, {
        timeout: TIMEOUTS.sessionRestart,
        polling: 50,
      });
      await expect.poll(() => getConsolePromptText(page), { timeout: BUSY_TIMEOUT }).toBe('> ');
      fs.writeFileSync(flag, '');

      await expect.poll(() => prompts.events.at(-1)?.data, { timeout: BUSY_TIMEOUT })
        .toMatchObject({ prompt: '> ', busy: false });
      await expect(page.locator(CONSOLE_INPUT)).not.toHaveClass(BUSY_CLASS);
    } finally {
      prompts.stop();
    }
    await executeInConsole(page, 'cat("console", "alive\\n")');
    await expect(consoleActions.consolePane.consoleOutput).toContainText('console alive');
  });

  test('console input sent while a callback runs is queued, in order, for the next prompt', async ({ rstudioPage: page }) => {
    // The callback returns only once both submissions are confirmed queued
    // (a flag file says so), which needs a filesystem shared with the
    // session; it ends on its own, since an interrupt would discard them.
    test.skip(!canWriteDir(sandbox.dir), 'the runner cannot write where the session reads (remote server)');
    const flag = flagFile(`queued-in-order-${Date.now()}.flag`);
    await executeInConsole(page,
      `later::later(function() { cat("callback", "started\\n"); while (!file.exists(${JSON.stringify(flag)})) Sys.sleep(0.05) }, 0.1)`);
    await expect(consoleActions.consolePane.consoleOutput).toContainText('callback started');

    // Two submissions whose second depends on the first: queued input must
    // run in the order it was sent.
    const promptCountBefore = await getConsolePromptCount(page);
    const prompts = recordClientEvents(page, ['console_prompt']);
    try {
      await submitAndAwaitQueued(page, '.idle_callback_test <- "after callback"');
      await submitAndAwaitQueued(page, 'cat(.idle_callback_test, "ran", "second\\n")');
      fs.writeFileSync(flag, '');

      await waitForConsoleCommandComplete(page, promptCountBefore, 20000);
      await expect(consoleActions.consolePane.consoleOutput).toContainText('after callback ran second');
      await expect(consoleActions.consolePane.consoleOutput).not.toContainText('Error');

      // No prompt comes between the callback's return and the queued input
      // running, only the one once the read waits again (the REPL does not
      // prompt between queued statements): a prompt ahead of queued input
      // reads as the end of a command that has not run (a notebook chunk's
      // code is queued the same way, and its queue finishes the chunk on
      // the prompt).
      expect(prompts.events.map((e) => (e.data as { prompt?: unknown })?.prompt)).toEqual(['> ']);
    } finally {
      prompts.stop();
    }
  });

  test('Escape at a continuation prompt cancels it while the session idles', async ({ rstudioPage: page }) => {
    // Escape in an empty console asks the server whether R is busy, and
    // cancels the waiting read when it is not. The idle loop dispatches
    // R's input handlers twenty times a second; that dispatch, with no
    // callback due, must not count as R being busy, or an Escape landing
    // in it would be sent as an interrupt the dispatch swallows, leaving
    // the continuation prompt waiting for a second Escape. (The window is
    // too short to be aimed at; this pins the route it preserves.)
    await executeInConsole(page, '1 +', { wait: false });
    await expect.poll(() => getConsolePromptText(page), { timeout: 5000 }).toBe('+ ');
    await page.keyboard.press('Escape');
    await expect.poll(() => getConsolePromptText(page), { timeout: 5000 }).toBe('> ');
    await executeInConsole(page, 'cat("console", "alive\\n")');
    await expect(consoleActions.consolePane.consoleOutput).toContainText('console alive');
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

  test('an uncaught error in a callback discards queued input under the discard pref and returns the prompt', async ({ rstudioPage: page }) => {
    // The callback errors only once the test has confirmed the input queued
    // (a flag file says so), which needs a filesystem shared with the session.
    test.skip(!canWriteDir(sandbox.dir), 'the runner cannot write where the session reads (remote server)');
    const consoleInput = page.locator(CONSOLE_INPUT);
    const flag = flagFile(`error-discards-${Date.now()}.flag`);
    // The console reset R runs on an uncaught error discards queued input
    // under this pref (its default) -- the other way, besides an interrupt,
    // that input queued behind a callback is dropped. The error message is
    // assembled so the echoed command cannot satisfy the wait for it.
    await setPref(page, 'discard_pending_console_input_on_error', true);

    await executeInConsole(page,
      `later::later(function() { cat("callback", "started\\n"); while (!file.exists(${JSON.stringify(flag)})) Sys.sleep(0.05); stop(paste0("bo", "om")) }, 0.1)`);
    await expect(consoleActions.consolePane.consoleOutput).toContainText('callback started');
    await submitAndAwaitQueued(page, 'cat("discarded", "input\\n")');
    await expect(consoleInput).toHaveClass(BUSY_CLASS, { timeout: BUSY_TIMEOUT });
    fs.writeFileSync(flag, '');

    // The error ends the callback and discards the queued input; a prompt
    // still arrives, since the client has nothing else to clear its busy state.
    await expect(consoleActions.consolePane.consoleOutput).toContainText('boom');
    await expect(consoleInput).not.toHaveClass(BUSY_CLASS, { timeout: BUSY_TIMEOUT });
    await executeInConsole(page, 'cat("console", "alive\\n")');
    await expect(consoleActions.consolePane.consoleOutput).toContainText('console alive');
    await expect(consoleActions.consolePane.consoleOutput).not.toContainText('discarded input');
  });

  test('queued input survives an uncaught error in a callback with the discard pref off', async ({ rstudioPage: page }) => {
    test.skip(!canWriteDir(sandbox.dir), 'the runner cannot write where the session reads (remote server)');
    const flag = flagFile(`error-keeps-${Date.now()}.flag`);
    await setPref(page, 'discard_pending_console_input_on_error', false);

    await executeInConsole(page,
      `later::later(function() { cat("callback", "started\\n"); while (!file.exists(${JSON.stringify(flag)})) Sys.sleep(0.05); stop(paste0("bo", "om")) }, 0.1)`);
    await expect(consoleActions.consolePane.consoleOutput).toContainText('callback started');
    const promptCountBefore = await getConsolePromptCount(page);
    await submitAndAwaitQueued(page, 'cat("kept", "input\\n")');
    fs.writeFileSync(flag, '');

    // The error ends the callback; the queued input is handed back to the
    // waiting read and runs, and the REPL prompts after it as usual.
    await expect(consoleActions.consolePane.consoleOutput).toContainText('boom');
    await waitForConsoleCommandComplete(page, promptCountBefore, 20000);
    await expect(consoleActions.consolePane.consoleOutput).toContainText('kept input');
  });

  test('an uncaught error in a callback with nothing queued does not reissue the prompt', async ({ rstudioPage: page }) => {
    // The console reset R runs on an uncaught error clears the input queue
    // under this pref; with nothing queued, nothing was discarded and the
    // client was never told R is busy, so there is no prompt to restore.
    // A prompt here would reset the console's history position, refresh
    // the Environment and Files panes, and advance the prompt counter the
    // test helpers key off, although no command completed.
    await setPref(page, 'discard_pending_console_input_on_error', true);
    await executeInConsole(page, 'options(rstudio.idleEventBusyDelay = 60)');
    const promptCountBefore = await getConsolePromptCount(page);

    await executeInConsole(page, 'later::later(function() stop(paste0("bo", "om")), 0.1)', { wait: false });
    await expect(consoleActions.consolePane.consoleOutput).toContainText('boom');

    // Only the call itself and the command below complete: two prompts. The
    // check comes after the second command's output, by when a prompt
    // reissued on the error would have arrived (it is queued before R even
    // reads the next input).
    await executeInConsole(page, 'cat("console", "alive\\n")');
    await expect(consoleActions.consolePane.consoleOutput).toContainText('console alive');
    expect(await getConsolePromptCount(page)).toBe(promptCountBefore === null ? null : promptCountBefore + 2);
  });

  test('a read opened by a callback gets its own prompt, and the REPL prompt returns after it', async ({ rstudioPage: page }) => {
    const prompts = recordClientEvents(page, ['console_prompt']);
    try {
      // A callback that reads from the console: its prompt replaces the REPL's
      // while it waits. (later never runs another callback meanwhile, so the
      // busy-while-reading path cannot be driven from here.)
      await executeInConsole(page, 'later::later(function() .idle_callback_answer <<- readline("answer? "), 0.1)');
      await expect.poll(() => getConsolePromptText(page), { timeout: 5000 }).toBe('answer? ');

      // Answering ends the callback; the REPL prompt comes back on its own (a
      // new prompt event, which is what executeInConsole waits for). Without
      // that the console would sit without a prompt until something else
      // issued one. The read left R "not at its top-level prompt" when it
      // handed over the answer; the restored prompt must not report R busy
      // (notebook chunk execution and the chat pane key off that field).
      await executeInConsole(page, 'hello');
      expect(await getConsolePromptText(page)).toBe('> ');
      const restored = prompts.events.at(-1)?.data as { prompt?: unknown; busy?: unknown } | undefined;
      expect(restored?.prompt).toBe('> ');
      expect(restored?.busy).toBe(false);
    } finally {
      prompts.stop();
    }
    await executeInConsole(page, 'cat("the answer was", .idle_callback_answer, "\\n")');
    await expect(consoleActions.consolePane.consoleOutput).toContainText('the answer was hello');
  });

  test('a callback that keeps running after its read is answered is reported busy again', async ({ rstudioPage: page }) => {
    const consoleInput = page.locator(CONSOLE_INPUT);
    const interruptEnabled = () => page.evaluate(() => window.rstudio?.commands?.interruptR?.isEnabled() ?? false);

    await executeInConsole(page, 'later::later(function() { .idle_callback_answer <<- readline("answer? "); while (TRUE) Sys.sleep(0.05) }, 0.1)');
    await expect.poll(() => getConsolePromptText(page), { timeout: 5000 }).toBe('answer? ');

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
    expect(await getConsolePromptText(page)).toBe('> ');
    await executeInConsole(page, 'cat("the answer was", .idle_callback_answer, "\\n")');
    await expect(consoleActions.consolePane.consoleOutput).toContainText('the answer was hello');
  });

  test('a callback reported busy that reads and then returns at once leaves the console idle', async ({ rstudioPage: page }) => {
    // The callback reads only once the test has seen the busy report (a flag
    // file says so), which needs a filesystem shared with the session.
    test.skip(!canWriteDir(sandbox.dir), 'the runner cannot write where the session reads (remote server)');
    const consoleInput = page.locator(CONSOLE_INPUT);
    const flag = flagFile(`busy-seen-${Date.now()}.flag`);
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
      await expect.poll(() => getConsolePromptText(page), { timeout: 10000 }).toBe('answer? ');
      await expect(consoleInput).not.toHaveClass(BUSY_CLASS);

      await executeInConsole(page, 'hello');
      expect(await getConsolePromptText(page)).toBe('> ');
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
    const flag = flagFile(`read-queued-${Date.now()}.flag`);
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
      expect(await getConsolePromptText(page)).toBe('> ');
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
