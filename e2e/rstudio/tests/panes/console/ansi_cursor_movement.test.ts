// UI-layer coverage for the ANSI cursor-movement escape sequences handled by
// VirtualConsole (#17768 / PR #17786): cursor up/down (CSI A/B), cursor
// next/previous line (CSI E/F), and cursor horizontal absolute (CSI G), plus
// the change that makes the horizontal moves -- cursor right/left (CSI C/D) and
// backspace -- stay within the current line instead of sliding across newlines.
//
// VirtualConsoleTests.java exercises the parser directly and exhaustively;
// these tests verify the whole pipeline (R cat() -> session -> client event ->
// rendered console DOM) for the cases most likely to regress end-to-end.
//
// Each scenario is a single cat() so the escapes arrive together. Movement is
// written relative to the end of the freshly printed text, then a marker
// character is written so the final rendered lines are unambiguous: the line
// that moved is overwritten, the others survive intact.

import { test, expect } from '@fixtures/rstudio.fixture';
import type { Locator } from 'playwright';
import { getOutputLines } from '@utils/console';
import { ConsolePaneActions } from '@actions/console_pane.actions';

// Rendered output block (between the echoed command and the next prompt), one
// entry per line, trailing whitespace trimmed so space-padded overwrites don't
// make exact comparisons brittle.
async function renderedLines(consoleOutput: Locator): Promise<string[]> {
  const text = await consoleOutput.innerText();
  const out = getOutputLines(text);
  return out.length ? out.split('\n').map((line) => line.replace(/\s+$/, '')) : [];
}

async function expectRenderedLines(consoleOutput: Locator, expected: string[]): Promise<void> {
  await expect.poll(() => renderedLines(consoleOutput)).toEqual(expected);
}

test.describe('ANSI cursor movement (CSI A/B/E/F/G) - #17768', () => {
  let consoleActions: ConsolePaneActions;

  test.beforeAll(async ({ rstudioPage: page }) => {
    consoleActions = new ConsolePaneActions(page);
  });

  test.beforeEach(async () => {
    await consoleActions.clearConsole();
  });

  test('CSI A rewrites the line above', async () => {
    // Up one row, back to column 0, overwrite "first" -> "FIRST".
    await consoleActions.executeInConsole('cat("first\\nsecond\\033[1A\\rFIRST\\n")');
    await expectRenderedLines(consoleActions.consolePane.consoleOutput, ['FIRST', 'second']);
  });

  test('CSI A then CSI B target a middle line', async () => {
    // Up two rows then down one lands on the middle line; rewrite "L2" -> "XX".
    await consoleActions.executeInConsole('cat("L1\\nL2\\nL3\\033[2A\\033[1B\\rXX\\n")');
    await expectRenderedLines(consoleActions.consolePane.consoleOutput, ['L1', 'XX', 'L3']);
  });

  test('CSI F moves to the start of a previous line', async () => {
    // Previous-line x2 from "ccc" lands at column 0 of "aaa"; write "X".
    await consoleActions.executeInConsole('cat("aaa\\nbbb\\nccc\\033[2FX\\n")');
    await expectRenderedLines(consoleActions.consolePane.consoleOutput, ['Xaa', 'bbb', 'ccc']);
  });

  test('CSI E moves to the start of a following line', async () => {
    // Up to "aaa", then next-line down to "bbb" (column 0); write "X".
    await consoleActions.executeInConsole('cat("aaa\\nbbb\\nccc\\033[2F\\033[1EX\\n")');
    await expectRenderedLines(consoleActions.consolePane.consoleOutput, ['aaa', 'Xbb', 'ccc']);
  });

  test('CSI G column 1 redraws the current line (progress-bar style)', async () => {
    // Go to column 1, redraw the whole line, erase any leftover with CSI K.
    await consoleActions.executeInConsole('cat("Loading 0%\\033[GLoading 99%\\033[K\\n")');
    await expect(consoleActions.consolePane.consoleOutput).toContainText('Loading 99%');
    await expectRenderedLines(consoleActions.consolePane.consoleOutput, ['Loading 99%']);
  });

  test('CSI G moves to an explicit column', async () => {
    // Column 4 (1-based) sits on "D"; overwrite "DEF" with "XYZ".
    await consoleActions.executeInConsole('cat("ABCDEFGH\\033[4GXYZ\\n")');
    await expectRenderedLines(consoleActions.consolePane.consoleOutput, ['ABCXYZGH']);
  });
});

test.describe('ANSI horizontal moves stay within the line - #17768', () => {
  let consoleActions: ConsolePaneActions;

  test.beforeAll(async ({ rstudioPage: page }) => {
    consoleActions = new ConsolePaneActions(page);
  });

  test.beforeEach(async () => {
    await consoleActions.clearConsole();
  });

  test('CSI D (cursor back) stops at the start of the current line', async () => {
    // Left 9 from the end of "def" clamps at "def" start, not up into "abc",
    // so the marker overwrites "d" -> "Xef" (old quirk would have hit "abc").
    await consoleActions.executeInConsole('cat("abc\\ndef\\033[9DX\\n")');
    await expectRenderedLines(consoleActions.consolePane.consoleOutput, ['abc', 'Xef']);
  });

  test('backspace stops at the start of the current line', async () => {
    await consoleActions.executeInConsole('cat("abc\\ndef\\b\\b\\b\\b\\bX\\n")');
    await expectRenderedLines(consoleActions.consolePane.consoleOutput, ['abc', 'Xef']);
  });

  test('CSI C (cursor forward) stops at the end of the current line', async () => {
    // Up to "abc", then forward 9 clamps at its end (not down into "def"); the
    // write appends to "abc" -> "abcX".
    await consoleActions.executeInConsole('cat("abc\\ndef\\033[1A\\033[9CX\\n")');
    await expectRenderedLines(consoleActions.consolePane.consoleOutput, ['abcX', 'def']);
  });
});
