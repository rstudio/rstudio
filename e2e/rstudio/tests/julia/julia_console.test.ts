import type { Page } from 'playwright';
import { test, expect } from '@fixtures/rstudio.fixture';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { executeCommand, setPref, clearPref } from '@utils/commands';
import {
  clearConsole,
  executeInConsole,
  getConsolePromptCount,
  getConsolePromptText,
  waitForConsoleCommandComplete,
} from '@pages/console_pane.page';
import { setAlwaysShowCompletionPopup, waitForCompletionPopup } from '@actions/autocomplete.actions';
import { setConsoleInput } from '@utils/console';
import { sleep } from '@utils/constants';
import { closeAndDeleteSandboxFiles, writeAndOpenFile } from '@utils/files';
import { heredoc } from '@utils/heredoc';
import {
  consoleInput,
  consoleOutput,
  JULIA_PROMPT,
  JULIA_START_TIMEOUT,
  leaveJulia,
  R_PROMPT,
  runInConsole,
  useJulia,
} from '@utils/julia';
import { useSuiteSandbox } from '@utils/sandbox';

// The Julia console (SessionJulia.R): entering and leaving the REPL,
// evaluation, interrupts, completions, and running Julia scripts and julia
// chunks from the editor. Skipped where Julia isn't installed.

const FENCE = '```';

// Printed by code about to be interrupted, once it is running. The code
// spells it in two parts, so the console's echo of the code doesn't have it.
const STARTED = 'started';
const PRINT_STARTED_JULIA = 'println("sta", "rted")';
const PRINT_STARTED_R = 'cat("sta", "rted\\n", sep = "")';

async function interpreterLabel(page: Page): Promise<string> {
  return page.evaluate(() => {
    const ids = ['rstudio_console_interpreter_version', 'rstudio_console_interpreter_version_tabbed'];
    for (const id of ids) {
      const el = document.getElementById(id);
      if (el && el.offsetParent !== null)
        return el.textContent ?? '';
    }
    return '';
  });
}

// Submit code that runs until it is interrupted, interrupt it once it is
// running (and `settleMs` later, for code that goes on to something that
// can't be seen from here), and wait for the prompt.
async function runAndInterrupt(page: Page, code: string, timeout: number, settleMs = 0): Promise<void> {
  await clearConsole(page);
  const before = await getConsolePromptCount(page);
  await executeInConsole(page, code, { wait: false });
  await expect.poll(() => consoleOutput(page), { timeout: 30_000 }).toContain(STARTED);
  await sleep(settleMs);

  await executeCommand(page, 'interruptR');
  await waitForConsoleCommandComplete(page, before, timeout);
}

test.describe.serial('Julia console', () => {
  const sandbox = useSuiteSandbox();
  useJulia();

  test.beforeAll(async ({ rstudioPage: page }) => {
    await new ConsolePaneActions(page).resetSourcePane();
  });

  test.afterAll(async ({ rstudioPage: page }) => {
    await setAlwaysShowCompletionPopup(page, false);
    await clearPref(page, 'rmd_chunk_output_inline');
    await leaveJulia(page);
    await closeAndDeleteSandboxFiles(page, sandbox.dir, ['probe.jl', 'documented.jl', 'probe.Rmd', 'probe4.Rmd']);
  });

  test('the REPL evaluates as the Julia REPL does', async ({ rstudioPage: page }) => {
    test.setTimeout(JULIA_START_TIMEOUT + 120_000);

    const banner = await runInConsole(page, '.rs.julia.repl()', JULIA_START_TIMEOUT);
    expect(await getConsolePromptText(page)).toBe(JULIA_PROMPT);
    expect(banner).toContain('JuliaConnectoR');
    await expect.poll(() => interpreterLabel(page)).toMatch(/^Julia \d+\.\d+/);

    expect(await runInConsole(page, '1 + 1')).toMatch(/julia> 1 \+ 1\s+2/);
    expect(await runInConsole(page, 'x = [1, 2, 3];')).not.toMatch(/3-element/);
    expect(await runInConsole(page, 'x .* 2')).toContain('3-element Vector{Int64}');

    // multi-line input is buffered until it is complete
    const definition = await runInConsole(page, heredoc`
      function f(x)
          x + 1
      end`);
    expect(definition).toContain('f (generic function with 1 method)');
    expect(await runInConsole(page, 'f(41)')).toMatch(/\b42\b/);

    // Up Arrow recalls the Julia command just run
    await setConsoleInput(page, '');
    await page.keyboard.press('ArrowUp');
    expect(await consoleInput(page)).toBe('f(41)');
    await setConsoleInput(page, '');

    // indentation within a string is kept, as Julia dedents it itself
    expect(await runInConsole(page, 'S = """\n    a\n      b\n    """')).toContain('"a\\n  b\\n"');

    // soft scope at top level, as in the Julia REPL
    expect(await runInConsole(page, 'n = 0; for i in 1:3; n += i; end; n')).toMatch(/\b6\b/);

    // errors are reported as the Julia REPL reports them
    const error = await runInConsole(page, 'undefined_thing + 1');
    expect(error).toContain('UndefVarError');
    expect(error).not.toContain('RConnector');

    // help mode
    expect(await runInConsole(page, '?sqrt')).toContain('sqrt(x)');
  });

  test('shell mode runs a command', async ({ rstudioPage: page }) => {
    expect(await runInConsole(page, ';echo hello-from-shell')).toContain('hello-from-shell');
  });

  test("R's working directory follows Julia's, and Julia keeps its state", async ({ rstudioPage: page }) => {
    await runInConsole(page, 'cd(mkpath(joinpath(pwd(), "jl_cd_probe")))');
    await executeInConsole(page, 'exit');
    expect(await getConsolePromptText(page)).toBe(R_PROMPT);
    await expect.poll(() => interpreterLabel(page)).toMatch(/^R \d/);
    expect(await runInConsole(page, 'cat(basename(getwd()), "\\n")')).toContain('jl_cd_probe');
    await executeInConsole(page, 'setwd(dirname(getwd()))');

    // state survives leaving and re-entering
    await executeInConsole(page, '.rs.julia.repl()', { timeout: 60_000 });
    expect(await runInConsole(page, 'f(1)')).toMatch(/\b2\b/);
  });

  test('an interrupt stops Julia code, and Julia carries on', async ({ rstudioPage: page }) => {
    test.setTimeout(240_000);

    for (const code of [
      `${PRINT_STARTED_JULIA}; sleep(30)`,
      `${PRINT_STARTED_JULIA}; k = 0; while true; println("tick ", k += 1); sleep(0.001); end`,
    ]) {
      await runAndInterrupt(page, code, 20_000);
      expect(await consoleOutput(page), code).toContain('InterruptException');
      expect(await getConsolePromptText(page)).toBe(JULIA_PROMPT);

      // and the connection still works
      expect(await runInConsole(page, '40 + 2'), code).toMatch(/\b42\b/);
    }
  });

  test('an interrupt stops a loop that never yields', async ({ rstudioPage: page }) => {
    test.setTimeout(120_000);

    // Julia's output only reaches R when Julia's main task yields, which
    // this loop never does: the sleep ahead of it lets the marker out. Once
    // the marker shows, nothing tells the sleep from the loop, so the
    // interrupt waits out the sleep.
    const code = `${PRINT_STARTED_JULIA}; sleep(0.2); y = 0; while true; global y += length(string(y)); end`;
    await runAndInterrupt(page, code, 20_000, 1_000);
    expect(await consoleOutput(page)).toContain('InterruptException');
    expect(await getConsolePromptText(page)).toBe(JULIA_PROMPT);
    expect(await runInConsole(page, '40 + 2')).toMatch(/\b42\b/);
  });

  test('an interrupt stops a command Julia runs', async ({ rstudioPage: page }) => {
    test.setTimeout(120_000);

    // as at a terminal
    await runAndInterrupt(page, `${PRINT_STARTED_JULIA}; run(\`sleep 30\`)`, 10_000);
    expect(await getConsolePromptText(page)).toBe(JULIA_PROMPT);
    expect(await runInConsole(page, '40 + 2')).toMatch(/\b42\b/);
  });

  test('interrupting R leaves an idle Julia running', async ({ rstudioPage: page }) => {
    test.setTimeout(120_000);

    // RStudio interrupts R by signalling its whole process group
    await executeInConsole(page, 'exit');
    await runAndInterrupt(page, `${PRINT_STARTED_R}; Sys.sleep(30)`, 20_000);

    // the same Julia process: its state is still there
    await executeInConsole(page, '.rs.julia.repl()', { timeout: 60_000 });
    expect(await runInConsole(page, 'f(1)')).toMatch(/\b2\b/);
  });

  test('completions come from Julia, with its documentation', async ({ rstudioPage: page }) => {
    await setAlwaysShowCompletionPopup(page, true);

    for (const [input, expected] of [['printl', 'println'], ['Base.Mat', 'Math'], ['using Statis', 'Statistics']]) {
      await setConsoleInput(page, input);
      await page.keyboard.press('Tab');
      await waitForCompletionPopup(page);
      await expect(page.locator('#rstudio_popup_completions')).toContainText(expected);
      await page.keyboard.press('Escape');
    }

    // the help pane shows Julia's documentation, not R's
    await setConsoleInput(page, 'printl');
    await page.keyboard.press('Tab');
    await waitForCompletionPopup(page);
    const help = page.locator('[id^="rstudio_popup_completions_help"]:visible');
    await expect(help).toContainText('followed by a newline', { timeout: 30_000 });
    await page.keyboard.press('Escape');

    // a name R would quote goes in as Julia spells it
    await setConsoleInput(page, 'pushfirs');
    await page.keyboard.press('Tab');
    await waitForCompletionPopup(page);
    await page.keyboard.press('Enter');
    expect(await consoleInput(page)).toBe('pushfirst!()');

    // a macro isn't given parentheses; a token that is already its only
    // completion shows no popup, so complete from part of one
    await setConsoleInput(page, '@tim');
    await page.keyboard.press('Tab');
    await waitForCompletionPopup(page);
    await page.keyboard.press('Enter');
    expect(await consoleInput(page)).toMatch(/^@time[dv]?$/);

    await setConsoleInput(page, '');
    await setAlwaysShowCompletionPopup(page, false);
  });

  test('a Julia script runs and sources in the Julia console', async ({ rstudioPage: page }) => {
    test.setTimeout(240_000);

    // leave for R first: running Julia code from the editor enters the REPL
    await executeInConsole(page, 'exit');
    expect(await getConsolePromptText(page)).toBe(R_PROMPT);

    const script = heredoc`
      jl_answer = 6 * 7
      function g(x)
          return x^2
      end
      println("g(9) = ", g(9))
    `;
    await writeAndOpenFile(page, sandbox.dir, 'probe.jl', script);
    await clearConsole(page);
    const before = await getConsolePromptCount(page);
    await executeCommand(page, 'executeCode');
    await waitForConsoleCommandComplete(page, before, 120_000);
    await expect.poll(() => getConsolePromptText(page), { timeout: 60_000 }).toBe(JULIA_PROMPT);
    await expect.poll(() => consoleOutput(page), { timeout: 30_000 }).toMatch(/\b42\b/);

    // Source runs the whole file with include()
    await clearConsole(page);
    await executeCommand(page, 'sourceActiveDocument');
    await expect.poll(() => consoleOutput(page), { timeout: 60_000 }).toContain('g(9) = 81');
  });

  test('a docstring run with the definition after it documents it', async ({ rstudioPage: page }) => {
    test.setTimeout(120_000);

    // leave for R first: the file is written and opened with R code
    await leaveJulia(page);

    // a selection runs line by line (Run All would include() the file): the
    // docstring waits for the line after it, as it would in a file
    const script = heredoc`
      "Add one to x."
      jl_add_one(x) = x + 1
      println("documented: ", occursin("Add one", string(@doc jl_add_one)))
    `;
    await writeAndOpenFile(page, sandbox.dir, 'documented.jl', script);
    await page.evaluate(() => {
      const editor = window.rstudio!.documents.activeEditor()!;
      editor.selectAll();
      editor.focus();
    });
    await clearConsole(page);
    await executeCommand(page, 'executeCode');
    await expect.poll(() => consoleOutput(page), { timeout: 60_000 }).toMatch(/documented: (true|false)/);
    const output = await consoleOutput(page);
    expect(output).toContain('documented: true');

    // the docstring is echoed, but isn't a value of its own
    expect(output.split('"Add one to x."').length - 1).toBe(1);

    // typed at the prompt, a string is a value, as at Julia's own prompt
    expect(await runInConsole(page, '"just a string"')).toMatch(/julia> "just a string"\s+"just a string"/);
  });

  test('R and julia chunks run in turn in the console', async ({ rstudioPage: page }) => {
    test.setTimeout(240_000);

    // on Server the harness opens files with R code at the console
    await leaveJulia(page);
    await setPref(page, 'rmd_chunk_output_inline', false);

    // heredoc keeps its text raw: fences come in through ${FENCE}, and "\n"
    // reaches R as written
    const rmd = heredoc`
      ---
      title: probe
      ---

      ${FENCE}{r}
      r_value <- 10
      cat("from R:", r_value, "\n")
      ${FENCE}

      ${FENCE}{julia}
      println("from Julia: ", jl_answer + 1)
      ${FENCE}

      ${FENCE}{r}
      cat("back in R:", r_value * 2, "\n")
      ${FENCE}
    `;
    await writeAndOpenFile(page, sandbox.dir, 'probe.Rmd', rmd);
    await clearConsole(page);
    await executeCommand(page, 'executeAllCode');
    await expect.poll(() => consoleOutput(page), { timeout: 90_000 }).toContain('back in R: 20');
    const output = await consoleOutput(page);
    expect(output).toContain('from R: 10');
    expect(output).toContain('from Julia: 43');
    await expect.poll(() => getConsolePromptText(page), { timeout: 30_000 }).toBe(R_PROMPT);
  });

  test('Run Current Chunk takes the language from the chunk header', async ({ rstudioPage: page }) => {
    test.setTimeout(120_000);
    await leaveJulia(page);
    await setPref(page, 'rmd_chunk_output_inline', false);

    // a four-backtick julia chunk that starts with a Quarto option: it runs
    // in Julia, not R
    const fence4 = FENCE + '`';
    const rmd = heredoc`
      ---
      title: probe
      ---

      ${fence4}{julia}
      #| label: four
      jl_four = 4
      ${fence4}
    `;
    await writeAndOpenFile(page, sandbox.dir, 'probe4.Rmd', rmd);
    await page.evaluate(() => {
      const editor = window.rstudio!.documents.activeEditor()!;
      editor.gotoLine(7, 0, false);
      editor.focus();
    });
    await executeCommand(page, 'executeCurrentChunk');
    await expect.poll(() => getConsolePromptText(page), { timeout: 60_000 }).toBe(JULIA_PROMPT);
    expect(await runInConsole(page, 'jl_four + 1')).toMatch(/\b5\b/);
    await executeInConsole(page, 'exit');
    expect(await runInConsole(page, 'cat(exists("jl_four"), "\\n")')).toContain('FALSE');
  });
});
