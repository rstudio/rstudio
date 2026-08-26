/**
 * Quarto chunk and editor behavior: the `warn` option round-trip on
 * chunk run, chunk-widget visibility, variable-width nested-chunk
 * folding (#15191), the empty-quarto-block highlight regression
 * (#16463), and the multi-cursor find commands inside visual-mode
 * chunks (#16540). Multiline chunk execution (#17350) is covered by
 * `multiline_chunk_execution.test.ts`.
 */

import type { Locator } from 'playwright';
import { test, expect } from '@fixtures/rstudio.fixture';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { SourcePaneActions } from '@actions/source_pane.actions';
import { AceEditor } from '@pages/ace_editor.page';
import { useSuiteSandbox } from '@utils/sandbox';
import { executeCommand, isCommandEnabled } from '@utils/commands';

test.describe.serial('Quarto chunks', { tag: ['@serial'] }, () => {
  useSuiteSandbox();
  let consoleActions: ConsolePaneActions;
  let sourceActions: SourcePaneActions;

  test.beforeAll(async ({ rstudioPage: page }) => {
    consoleActions = new ConsolePaneActions(page);
    sourceActions = new SourcePaneActions(page, consoleActions);
    await consoleActions.clearConsole();
  });

  // Focus a chunk's embedded editor in the visual editor, addressing it by its
  // `{r <label>}` header, which the chunk editor renders as its first line:
  // unlike the body, it survives edits, so the same locator resolves before and
  // after. Force-click the hidden textarea -- an ace_content overlay intercepts
  // normal clicks on it.
  const focusChunk = (proseMirror: Locator, header: string) =>
    proseMirror
      .locator('.ace_editor')
      .filter({ hasText: header })
      .locator('textarea.ace_text-input')
      .click({ force: true });

  test('the warn option is preserved when running chunks', async ({ rstudioPage: page }) => {
    const fileName = `quarto_warn_${Date.now()}.qmd`;
    // Sentinel printed as the last line of the chunk so the test can
    // wait for "chunk finished" before checking the global warn value.
    // executeCurrentChunk dispatches via a different path than
    // executeInConsole, so the two are *not* serialized by R's input
    // queue -- without an explicit wait, a following console command
    // can land before the chunk completes.
    const sentinel = `__CHUNK_DONE_${Date.now()}__`;
    const content = [
      '---',
      'title: Chunk Warnings',
      '---',
      '',
      '```{r warning_chunk, warning=TRUE}',
      '# check current option',
      'getOption("warn")',
      '# setting a global option',
      'options(warn = 2)',
      `cat("${sentinel}\\n")`,
      '```',
    ].join('\n');

    await consoleActions.clearConsole();
    await consoleActions.executeInConsole('options(warn = 0); cat("WARN_BEFORE=", getOption("warn"), "\\n", sep = "")');
    await expect(consoleActions.consolePane.consoleOutput).toContainText('WARN_BEFORE=0');

    await sourceActions.createAndOpenFile(fileName, content);

    // Clear before running the chunk so the sentinel wait below cannot
    // match against the writeLines echo from createAndOpenFile (which
    // contains the sentinel string verbatim as part of the chunk body).
    // Use the consoleClear command rather than clearConsole() so we keep
    // source-pane focus -- navigateToChunkByLabel below clicks into the
    // source editor, which fails if focus has shifted to the console.
    await executeCommand(page, 'consoleClear');

    await sourceActions.navigateToChunkByLabel('warning_chunk');
    await executeCommand(page, 'executeCurrentChunk');
    await expect(consoleActions.consolePane.consoleOutput).toContainText(sentinel, { timeout: 30000 });

    await consoleActions.executeInConsole('cat("WARN_AFTER=", getOption("warn"), "\\n", sep = "")');
    await expect(consoleActions.consolePane.consoleOutput).toContainText('WARN_AFTER=2', { timeout: 30000 });

    await consoleActions.executeInConsole('options(warn = 0)');
    await sourceActions.closeSourceAndDeleteFile(fileName);
  });

  test('the expected chunk widgets show for multiple chunks (#11745)', async ({ rstudioPage: page }) => {
    const fileName = `quarto_widgets_${Date.now()}.qmd`;
    const content = [
      '---',
      'title: "Chunk widgets"',
      '---',
      '',
      '```{r setup, include=FALSE}',
      'knitr::opts_chunk$set(echo = TRUE)',
      '```',
      '',
      '## Quarto',
      '',
      'This is a Quarto document.',
      '',
      '```{r cars}',
      'summary(cars)',
      '```',
      '',
      '## Including Plots',
      '',
      'You can also embed plots, for example:',
      '',
      '```{r pressure, echo=FALSE}',
      'plot(pressure)',
      '```',
      '',
      'The end.',
    ].join('\n');

    await sourceActions.createAndOpenFile(fileName, content);

    const optionWidgets = page.locator('.rstudio_modify_chunk');
    const previewWidgets = page.locator('.rstudio_preview_chunk');
    const runWidgets = page.locator('.rstudio_run_chunk');

    await expect(optionWidgets).toHaveCount(3);
    await expect(previewWidgets).toHaveCount(3);
    await expect(runWidgets).toHaveCount(3);

    // The setup chunk's preview widget is hidden.
    await expect(previewWidgets.nth(0)).toHaveAttribute('aria-hidden', 'true');
    await expect(previewWidgets.nth(0)).toHaveCSS('display', 'none');

    // Other chunk widgets are visible.
    for (const locator of [previewWidgets.nth(1), previewWidgets.nth(2)]) {
      const ariaHidden = await locator.getAttribute('aria-hidden');
      expect(ariaHidden === null || ariaHidden === 'false').toBe(true);
      await expect(locator).not.toHaveCSS('display', 'none');
    }
    for (let i = 0; i < 3; i++) {
      for (const locator of [optionWidgets.nth(i), runWidgets.nth(i)]) {
        const ariaHidden = await locator.getAttribute('aria-hidden');
        expect(ariaHidden === null || ariaHidden === 'false').toBe(true);
        await expect(locator).not.toHaveCSS('display', 'none');
      }
    }

    await sourceActions.closeSourceAndDeleteFile(fileName);
  });

  test('variable-width nested chunks can be folded (#15191)', async ({ rstudioPage: page }) => {
    const fileName = `quarto_folding_${Date.now()}.qmd`;
    // A verbatim block opened with five backticks, containing a nested
    // three-backtick `{r nested}` chunk. The fold widget should span
    // the whole outer block.
    const content = [
      '---',
      'title: Folding',
      '---',
      '',
      '`````{verbatim}',
      '',
      'This is some text.',
      '',
      '```{r nested}',
      'print(1 + 1)',
      '```',
      '',
      '`````',
      '',
      '# Header',
      '',
    ].join('\n');

    await sourceActions.createAndOpenFile(fileName, content);

    const editor = new AceEditor(page, 'verbatim');
    expect(await editor.getFoldWidget(4)).toBe('start');
    expect(await editor.getFoldWidget(8)).toBe('');
    expect(await editor.getFoldWidget(10)).toBe('');
    expect(await editor.getFoldWidget(12)).toBe('end');

    const expectedRange = {
      start: { row: 4, column: 15 },
      end: { row: 12, column: 0 },
    };
    expect(await editor.getFoldWidgetRange(4)).toEqual(expectedRange);
    expect(await editor.getFoldWidgetRange(12)).toEqual(expectedRange);

    await sourceActions.closeSourceAndDeleteFile(fileName);
  });

  test(`empty quarto blocks don't break highlight in chunk (#16463)`, async ({ rstudioPage: page }) => {
    const fileName = `quarto_highlight_${Date.now()}.qmd`;
    const content = [
      '---',
      'title: Chunk Syntax Highlighting',
      '---',
      '',
      '```{r}',
      '#| echo: true',
      '2 * 2',
      '```',
    ].join('\n');

    await sourceActions.createAndOpenFile(fileName, content);

    const editor = new AceEditor(page, '#| echo: true');
    // Place cursor at end of "#| echo: true" (row 6 in 1-indexed) and
    // insert two newlines. This pushes "2 * 2" down to row 8 (0-indexed)
    // -- the regression breaks chunk tokenization when an empty body
    // line precedes a code line within the chunk.
    await editor.gotoLine(6, 13);
    await editor.insert('\n\n');

    const tokens = await editor.getTokens(8);
    expect(tokens[0].value).toBe('2');
    expect(tokens[1].value).toBe(' ');
    expect(tokens[2].value).toBe('*');
    expect(tokens[3].value).toBe(' ');
    expect(tokens[4].value).toBe('2');

    await sourceActions.closeSourceAndDeleteFile(fileName);
  });

  test('multi-cursor find commands act on the focused chunk in visual mode (#16540)', async ({ rstudioPage: page }) => {
    const fileName = `quarto_multicursor_${Date.now()}.qmd`;
    // Each chunk repeats one identifier three times, so a command that adds
    // only the next occurrence is distinguishable from one that takes them
    // all. The prose copy of `widget` must survive both.
    const content = [
      '---',
      'title: Multi-cursor',
      '---',
      '',
      'Prose mentioning a widget.',
      '',
      '```{r alpha}',
      'widget <- 1',
      'widget + widget',
      '```',
      '',
      '```{r beta}',
      'gizmo <- 2',
      'gizmo + gizmo',
      '```',
    ].join('\n');

    await sourceActions.createAndOpenFile(fileName, content);
    try {
      // Source mode first: both handlers were rerouted through
      // withActiveEditor, whose non-visual branch is still the document's own
      // editor. Assert on the selection ranges rather than typing, so the
      // document reaches the visual half below unedited.
      const sourceEditor = new AceEditor(page, '');
      await sourceEditor.find('gizmo');
      await executeCommand(page, 'quickAddNext');
      await expect.poll(() => sourceEditor.getSelectionRanges()).toHaveLength(2);

      await sourceEditor.find('gizmo');
      await executeCommand(page, 'findAll');
      await expect.poll(() => sourceEditor.getSelectionRanges()).toHaveLength(3);

      await sourceActions.ensureVisualMode();
      const proseMirror = page.locator('.ProseMirror:visible').first();
      await expect(proseMirror).toBeVisible({ timeout: 15000 });

      const prose = proseMirror.getByText('Prose mentioning a widget.');

      // Prose has no Ace instance behind it and ProseMirror has no
      // multi-cursor concept, so both commands report unavailable there
      // rather than silently doing nothing.
      await prose.click();
      await expect.poll(() => isCommandEnabled(page, 'quickAddNext')).toBe(false);
      expect(await isCommandEnabled(page, 'findAll')).toBe(false);

      await focusChunk(proseMirror, '{r alpha}');
      await expect.poll(() => isCommandEnabled(page, 'quickAddNext')).toBe(true);
      expect(await isCommandEnabled(page, 'findAll')).toBe(true);

      // The repro from the issue: select an occurrence, then Find and Add Next
      // adds the following one, and typing edits both cursors at once. The
      // third `widget` is left alone.
      const first = AceEditor.visualModeChunk(page, '{r alpha}');
      await first.find('widget');
      await executeCommand(page, 'quickAddNext');
      await page.keyboard.type('gadget');
      await expect.poll(() => first.getValue()).toContain('gadget <- 1\ngadget + widget');

      // Find All takes every occurrence in the focused chunk.
      await focusChunk(proseMirror, '{r beta}');
      const second = AceEditor.visualModeChunk(page, '{r beta}');
      await second.find('gizmo');
      await executeCommand(page, 'findAll');
      await page.keyboard.type('doohickey');
      await expect.poll(() => second.getValue()).toContain('doohickey <- 2\ndoohickey + doohickey');

      // Neither command reached the prose, and leaving the chunk makes them
      // unavailable again.
      await expect(proseMirror).toContainText('Prose mentioning a widget.');
      await prose.click();
      await expect.poll(() => isCommandEnabled(page, 'quickAddNext')).toBe(false);
    } finally {
      // Close the tab rather than toggling back to source mode: toggling
      // leaves the chunks' Ace editors mounted, which makes the source-mode
      // editor locators ambiguous for later tests in the shared IDE.
      await sourceActions.closeSourceAndDeleteFile(fileName).catch((err) => {
        console.warn(`[quarto_chunks] cleanup failed for ${fileName}: ${err}`);
      });
    }
  });

  test('the visual mode find bar is seeded from the selection (#16540)', async ({ rstudioPage: page }) => {
    const fileName = `quarto_find_seed_${Date.now()}.qmd`;
    // `sassafras` sits alone in its paragraph so a double-click at the
    // element's centre lands on it.
    const content = [
      '---',
      'title: Find seeding',
      '---',
      '',
      'sassafras',
      '',
      '```{r seed}',
      'gizmo <- 2',
      'kumquat <- 3',
      '```',
    ].join('\n');

    await sourceActions.createAndOpenFile(fileName, content);
    try {
      await sourceActions.ensureVisualMode();
      const proseMirror = page.locator('.ProseMirror:visible').first();
      await expect(proseMirror).toBeVisible({ timeout: 15000 });

      // The find box lives in the visual editor's own find bar; the source
      // editor's bar is built lazily and this document never opens it. Scope
      // to the visible bar in the source panel all the same: the console and
      // every other open editor mount a FindReplaceBar sharing these
      // automation classes (see editor.test.ts).
      const findInput = page.locator(
        "[class*='rstudio_source_panel'] .rstudio-find-replace-find-input:visible input");

      // A word selected in prose seeds the search term, as it does in source
      // mode. Double-click near the paragraph's left edge: its box spans the
      // full editor width, so the default centre point lands past the text.
      await proseMirror.getByText('sassafras').dblclick({ position: { x: 4, y: 8 } });
      await expect
        .poll(() => page.evaluate(() => window.getSelection()?.toString() ?? ''))
        .toBe('sassafras');

      await executeCommand(page, 'findReplace');
      await expect(findInput).toHaveValue('sassafras');

      // So does a selection made inside a code chunk, which the outer
      // ProseMirror selection cannot see on its own.
      const chunk = AceEditor.visualModeChunk(page, '{r seed}');
      await focusChunk(proseMirror, '{r seed}');
      await chunk.find('gizmo');
      await expect.poll(() => chunk.getSelectedText()).toBe('gizmo');
      await executeCommand(page, 'findReplace');
      await expect(findInput).toHaveValue('gizmo');

      // Use Selection for Find reads the same selection. Refocus the chunk
      // first -- opening the find bar moved focus into the search box -- and
      // take the other identifier, so the box has to change to pass. Ace's
      // find() searches forward from the cursor and reports nothing when it
      // misses, so pin the selection before dispatching: otherwise a stale
      // `gizmo` would fail below looking like a seeding regression.
      await focusChunk(proseMirror, '{r seed}');
      await chunk.find('kumquat');
      await expect.poll(() => chunk.getSelectedText()).toBe('kumquat');
      await executeCommand(page, 'findFromSelection');
      await expect(findInput).toHaveValue('kumquat');
    } finally {
      await sourceActions.closeSourceAndDeleteFile(fileName).catch((err) => {
        console.warn(`[quarto_chunks] cleanup failed for ${fileName}: ${err}`);
      });
    }
  });
});
