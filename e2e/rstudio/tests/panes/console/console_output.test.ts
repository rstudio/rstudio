// Console output / condition-highlighting tests ported from
// src/cpp/tests/automation/testthat/test-automation-console.R.
//
// Covers carriage-return handling, condition highlighting
// (errors/warnings/messages), the consoleLineLengthLimit truncation pref,
// post-caught-error output (#16337), and the AceEditorCommandDispatcher
// shortcuts on the console input (#16973).

import { test, expect } from '@fixtures/rstudio.fixture';
import type { Page } from 'playwright';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { useSuiteSandbox } from '@utils/sandbox';
import { AceEditorElement } from '@utils/ace';
import { writeAndOpenFile, closeAndDeleteSandboxFiles } from '@utils/files';
import { executeCommand } from '@utils/commands';
import { sleep, TIMEOUTS } from '@utils/constants';
import { heredoc } from '@utils/heredoc';

const CONSOLE_OUTPUT = '#rstudio_console_output';

async function consoleSpanTexts(page: Page): Promise<string[]> {
  return page.evaluate((sel: string) => {
    const root = document.querySelector(sel);
    if (!root) return [];
    return Array.from(root.querySelectorAll('span'))
      .map((s) => (s as HTMLElement).innerText)
      .filter((t) => t.length > 0);
  }, CONSOLE_OUTPUT);
}

async function readConsoleInput(page: Page): Promise<string> {
  return page.evaluate(() => {
    const el = document.getElementById('rstudio_console_input') as AceEditorElement | null;
    return el?.env?.editor?.getValue() ?? '';
  });
}

test.describe('Console output and annotation', () => {
  let consoleActions: ConsolePaneActions;

  test.beforeAll(async ({ rstudioPage: page }) => {
    consoleActions = new ConsolePaneActions(page);
  });

  test.beforeEach(async () => {
    await consoleActions.clearConsole();
  });

  test('carriage returns overwrite the line in console output', async () => {
    // cat()'s vector overwrites within each \r run and finalizes with \n.
    // Each finalized line should end up as " ✔ xxx yyy zzz " in plaintext.
    const literal = [
      '"\\u2714 xxx \\u001b[34myyy\\u001b[39m xxx"',
      '"\\r"',
      '"\\u2714 xxx \\u001b[31myyy\\u001b[39m zzz"',
      '"\\n"',
      '"\\u2714 xxx \\u001b[34myyy\\u001b[39m xxx"',
      '"\\r"',
      '"\\u2714 xxx \\u001b[31myyy\\u001b[39m zzz"',
      '"\\n"',
    ].join(', ');
    await consoleActions.executeInConsole(`cat(${literal})`);

    await expect.poll(async () => {
      const text = await consoleActions.consolePane.consoleOutput.innerText();
      // Skip prompt lines (start with "> ") and console-echoed input so we
      // only count the cat() output the test cares about.
      const lines = text.split('\n')
        .map((l) => l.trim())
        .filter((l) => l.length > 0 && !l.startsWith('>') && !l.startsWith('cat('));
      return lines.slice(-2);
    }).toEqual(['✔ xxx yyy zzz', '✔ xxx yyy zzz']);
  });

  test('errors are highlighted when consoleHighlightConditions is set', async ({ rstudioPage: page }) => {
    await consoleActions.executeInConsole(
      '.rs.uiPrefs$consoleHighlightConditions$set("errors_warnings_messages")',
    );
    try {
      await consoleActions.executeInConsole('stop("This is an error.")');
      await expect(consoleActions.consolePane.consoleOutput)
        .toContainText('Error: This is an error.');

      // The highlight machinery wraps the literal "Error" prefix in its own
      // span -- look it up by text since the GWT-generated class names are
      // obfuscated and change at random.
      await expect.poll(() => consoleSpanTexts(page))
        .toEqual(expect.arrayContaining(['Error']));
    } finally {
      await consoleActions.executeInConsole('.rs.uiPrefs$consoleHighlightConditions$clear()');
    }
  });

  test('warnings are highlighted with options(warn = 0) and options(warn = 1)', async ({ rstudioPage: page }) => {
    await consoleActions.executeInConsole(
      '.rs.uiPrefs$consoleHighlightConditions$set("errors_warnings_messages")',
    );
    try {
      // warn = 0 defers warnings -- the prefix is "Warning message" once R
      // surfaces them at the prompt.
      await consoleActions.executeInConsole('{ options(warn = 0); warning("This is a warning.") }');
      await expect.poll(() => consoleSpanTexts(page))
        .toEqual(expect.arrayContaining(['Warning message']));

      // warn = 1 surfaces warnings immediately; the prefix is "Warning".
      await consoleActions.clearConsole();
      await consoleActions.executeInConsole('{ options(warn = 1); warning("This is a warning.") }');
      await expect(consoleActions.consolePane.consoleOutput)
        .toContainText('Warning: This is a warning.');
      await expect.poll(() => consoleSpanTexts(page))
        .toEqual(expect.arrayContaining(['Warning']));
    } finally {
      await consoleActions.executeInConsole('options(warn = 0)');
      await consoleActions.executeInConsole('.rs.uiPrefs$consoleHighlightConditions$clear()');
    }
  });

  // https://github.com/rstudio/rstudio/issues/16031
  test('warnings are treated as errors when options(warn = 2)', async () => {
    const expr = [
      'options(warn = 2)',
      'x <- tryCatch(as.numeric("oops"), error = identity)',
      'options(warn = 0)',
      'inherits(x, "error")',
    ].join('; ');
    await consoleActions.executeInConsole(`{ ${expr} }`);

    await expect(consoleActions.consolePane.consoleOutput)
      .toContainText('[1] TRUE');
  });

  // https://github.com/rstudio/rstudio/issues/16038
  test('carriage returns do not break output annotation', async ({ rstudioPage: page }) => {
    await consoleActions.executeInConsole('{ message("M1"); cat("O1\\r"); message("M2") }');

    // The output should annotate M1 and M2; O1 was overwritten by the
    // carriage return so its span must not exist. The DOM nests spans
    // (outer wrapper + inner text span), so dedupe before checking.
    await expect.poll(async () => {
      const spans = await consoleSpanTexts(page);
      const unique = [...new Set(spans)];
      return unique.filter((t) => t === 'M1' || t === 'M2' || t === 'O1');
    }).toEqual(['M1', 'M2']);

    await consoleActions.executeInConsole('writeLines("This is some more output.")');
    await expect(consoleActions.consolePane.consoleOutput)
      .toContainText('This is some more output.');

    // Backspace via message() should likewise not survive into the final
    // rendered output -- the result is "M2" not "M1\b2".
    await consoleActions.clearConsole();
    await consoleActions.executeInConsole(
      '{ message("M1", appendLF = FALSE); message("\\b", appendLF = FALSE); message("2") }',
    );
    await expect.poll(async () => {
      const text = await consoleActions.consolePane.consoleOutput.innerText();
      const lines = text.split('\n')
        .map((l) => l.trim())
        .filter((l) => l.length > 0 && !l.startsWith('>') && !l.startsWith('{ message'));
      return lines.at(-1);
    }).toBe('M2');
  });

  test('long lines are truncated when consoleLineLengthLimit is reduced', async () => {
    await consoleActions.executeInConsole('.rs.uiPrefs$consoleLineLengthLimit$set(10L)');
    try {
      await consoleActions.executeInConsole(
        'cat(paste(rep.int("a", 1E4), collapse = ""), sep = "\\n")',
      );
      await expect(consoleActions.consolePane.consoleOutput)
        .toContainText('aaaaaaaaaa ... <truncated>');
    } finally {
      // Restore the default so the pref doesn't bleed into later tests.
      await consoleActions.executeInConsole('.rs.uiPrefs$consoleLineLengthLimit$set(2000L)');
    }
  });
});

// https://github.com/rstudio/rstudio/issues/16337
//
// A separate describe so the sandbox + source-pane cleanup only runs for
// the test that needs them.
test.describe('Error output after caught try()', () => {
  const sandbox = useSuiteSandbox();
  let consoleActions: ConsolePaneActions;
  const FILE = 'console_error_output.R';

  test.beforeAll(async ({ rstudioPage: page }) => {
    consoleActions = new ConsolePaneActions(page);
    await consoleActions.closeAllBuffersWithoutSaving();
  });

  test.afterEach(async ({ rstudioPage: page }) => {
    await closeAndDeleteSandboxFiles(page, sandbox.dir, [FILE]);
  });

  test('output after a caught error is not dropped', async ({ rstudioPage: page }) => {
    const contents = heredoc`
      foo <- function() {
        writeLines("Some output.")
        try(stop("try(silent = FALSE)"), silent = FALSE)
        writeLines("Some more output.")
        stop("Error.")
      }
    ` + '\n';
    await writeAndOpenFile(page, sandbox.dir, FILE, contents);

    await executeCommand(page, 'sourceActiveDocument');
    await sleep(TIMEOUTS.settleDelay);

    await consoleActions.clearConsole();
    await consoleActions.executeInConsole('foo()');

    await expect(consoleActions.consolePane.consoleOutput)
      .toContainText('Some output.');
    await expect(consoleActions.consolePane.consoleOutput)
      .toContainText('Some more output.');
  });
});

// https://github.com/rstudio/rstudio/issues/16973
test.describe('Console input AceEditorCommandDispatcher shortcuts', () => {
  let consoleActions: ConsolePaneActions;

  test.beforeAll(async ({ rstudioPage: page }) => {
    consoleActions = new ConsolePaneActions(page);
  });

  test('Ctrl+Shift+M inserts the pipe and Alt+- inserts the assignment operator', async ({ rstudioPage: page }) => {
    // Focus the console input via the activateConsole command (same effect
    // as the Ctrl+2 keyboard shortcut).
    await consoleActions.clearConsole();
    await executeCommand(page, 'activateConsole');
    await sleep(TIMEOUTS.layoutSettle);

    await page.keyboard.press('ControlOrMeta+Shift+m');
    await expect.poll(() => readConsoleInput(page)).toMatch(/\|>|%>%/);

    // Clear the console input by selecting all and deleting; then verify
    // the assignment operator shortcut.
    await page.keyboard.press('ControlOrMeta+a');
    await page.keyboard.press('Backspace');
    await expect.poll(() => readConsoleInput(page)).toBe('');

    await page.keyboard.press('Alt+-');
    await expect.poll(() => readConsoleInput(page)).toContain('<-');

    // Clean up so the typed operator doesn't bleed into the next test.
    await page.keyboard.press('ControlOrMeta+a');
    await page.keyboard.press('Backspace');
  });
});
