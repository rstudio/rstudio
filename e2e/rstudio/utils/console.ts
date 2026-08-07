import type { Page } from 'playwright';
import type { Ace, AceEditorElement } from './ace';

/** Write `text` into the console input and put the cursor at its end, without submitting. */
export async function setConsoleInput(page: Page, text: string): Promise<void> {
  await page.evaluate((text) => {
    const el = document.getElementById('rstudio_console_input') as AceEditorElement | null;
    const editor = el?.env?.editor;
    if (!editor) throw new Error('Console Ace editor not found at #rstudio_console_input');
    editor.setValue(text, 1); // 1 = move cursor to end
    editor.focus();
  }, text);
}

// The getters below throw (rather than returning a fallback) when the console
// editor is missing: a fallback like '' is indistinguishable from a live
// console with nothing selected, so it can turn a broken test into a false
// pass. A throw also fails an enclosing expect.poll immediately instead of
// burning its timeout on a misleading diff.

/** The console input cursor's document position. */
export async function getConsoleCursorPosition(page: Page): Promise<Ace.Position> {
  return page.evaluate(() => {
    const el = document.getElementById('rstudio_console_input') as AceEditorElement | null;
    const editor = el?.env?.editor;
    if (!editor) throw new Error('Console Ace editor not found at #rstudio_console_input');
    return editor.getCursorPosition();
  });
}

/** The text currently selected in the console input. */
export async function getConsoleSelectedText(page: Page): Promise<string> {
  return page.evaluate(() => {
    const el = document.getElementById('rstudio_console_input') as AceEditorElement | null;
    const editor = el?.env?.editor;
    if (!editor) throw new Error('Console Ace editor not found at #rstudio_console_input');
    return editor.getSelectedText();
  });
}

/**
 * How many visual rows the console input currently occupies. Greater than the
 * document line count exactly when the input has soft-wrapped.
 */
export async function getConsoleScreenRowCount(page: Page): Promise<number> {
  return page.evaluate(() => {
    const el = document.getElementById('rstudio_console_input') as AceEditorElement | null;
    const editor = el?.env?.editor;
    if (!editor) throw new Error('Console Ace editor not found at #rstudio_console_input');
    return editor.session.getScreenLength();
  });
}

/**
 * Read the current browser selection's text plus the screen position of its
 * bounding rect, which the Find in Console tests use as a match identity
 * (two matches with the same text can differ only by screen position).
 */
export async function getSelectionInfo(
  page: Page,
): Promise<{ text: string; pos: string }> {
  return page.evaluate(() => {
    const sel = window.getSelection();
    if (!sel || sel.rangeCount === 0) return { text: '', pos: '' };
    const r = sel.getRangeAt(0).getBoundingClientRect();
    return { text: sel.toString(), pos: `${r.top},${r.left}` };
  });
}

/**
 * Extract only the output lines from the console panel text.
 * Console output includes echoed commands (lines starting with ">")
 * and other UI chrome. This returns only the lines between the last
 * command echo and the next ">" prompt — i.e., the actual R output.
 */
export function getOutputLines(fullText: string): string {
  const lines = fullText.split('\n');
  const outputLines: string[] = [];
  let lastPromptIndex = -1;

  // Find the last line starting with ">" (the echoed command)
  for (let i = 0; i < lines.length; i++) {
    if (lines[i].trim().startsWith('>') && lines[i].trim().length > 1) {
      lastPromptIndex = i;
    }
  }

  // Collect lines after the last command echo, stopping at the next ">" prompt
  if (lastPromptIndex >= 0) {
    for (let i = lastPromptIndex + 1; i < lines.length; i++) {
      const trimmed = lines[i].trim();
      if (trimmed.startsWith('>')) break;
      if (trimmed !== '') outputLines.push(lines[i]);
    }
  }
  return outputLines.join('\n');
}
