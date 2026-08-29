/**
 * Quarto chunk and editor behavior: the `warn` option round-trip on
 * chunk run, chunk-widget visibility, variable-width nested-chunk
 * folding (#15191), the empty-quarto-block highlight regression
 * (#16463), and the multi-cursor find commands inside visual-mode
 * chunks (#16540). Multiline chunk execution (#17350) is covered by
 * `multiline_chunk_execution.test.ts`.
 */

import type { Locator, Page } from 'playwright';
import { test, expect } from '@fixtures/rstudio.fixture';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { SourcePaneActions } from '@actions/source_pane.actions';
import { AceEditor } from '@pages/ace_editor.page';
import { useSuiteSandbox } from '@utils/sandbox';
import { executeCommand, isCommandEnabled, saveDocument } from '@utils/commands';

test.describe.serial('Quarto chunks', { tag: ['@serial'] }, () => {
  useSuiteSandbox();
  let consoleActions: ConsolePaneActions;
  let sourceActions: SourcePaneActions;

  test.beforeAll(async ({ rstudioPage: page }) => {
    consoleActions = new ConsolePaneActions(page);
    sourceActions = new SourcePaneActions(page, consoleActions);
    await consoleActions.clearConsole();
  });

  // Focus a chunk's embedded editor through Ace itself. A forced click on its
  // hidden textarea is not a reliable focus handoff after the find input has
  // focus: the click can complete without Ace's focus handler running.
  const focusChunk = (page: Page, proseMirror: Locator, header: string) =>
    AceEditor.visualModeChunk(page, header, proseMirror).focus();

  // Showing or seeding the visual find bar runs the search on a 300ms buffer
  // (PanmirrorFindReplaceWidget.timeBufferedFind_) that ends by re-selecting
  // the current match, which would undo a selection or focus change made in
  // the meantime -- e.g. pull the selection out of a footnote and close its
  // editor, or move it out of the chunk a command is about to act on. Wait
  // for that pass before moving on: it marks the selected match (an empty
  // span when the match is inside a chunk), and any editor transaction since
  // the last pass clears the marks.
  const awaitFindSettled = (proseMirror: Locator) =>
    expect(proseMirror.locator('.pm-find-text.pm-selected-text')).toHaveCount(1);

  // Each editor tab owns a copy of the visual toolbar. Resolve the button
  // through the tab panel containing the ProseMirror instance under test so
  // split editors and background tabs cannot make the locator ambiguous.
  const visualEditorPanel = (proseMirror: Locator) =>
    proseMirror.locator("xpath=ancestor::div[@role='tabpanel'][1]");

  const findReplaceButton = (proseMirror: Locator) =>
    visualEditorPanel(proseMirror).getByRole('button', { name: 'Find/Replace' });

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
      '',
      '```{r gamma}',
      'sprocket <- 3',
      'sprocket + sprocket',
      '```',
    ].join('\n');

    await sourceActions.createAndOpenFile(fileName, content);
    try {
      // Source mode first: both handlers were rerouted through
      // withActiveEditor, whose non-visual branch is still the document's own
      // editor. Assert on the selection ranges rather than typing, so the
      // document reaches the visual half below unedited.
      await sourceActions.ensureSourceMode();
      const sourceEditor = new AceEditor(page, '');
      await sourceEditor.find('gizmo');
      await expect.poll(() => sourceEditor.getSelectedText()).toBe('gizmo');
      await executeCommand(page, 'quickAddNext');
      await expect.poll(() => sourceEditor.getSelectionRanges()).toHaveLength(2);

      await sourceEditor.find('gizmo');
      await expect.poll(() => sourceEditor.getSelectedText()).toBe('gizmo');
      await executeCommand(page, 'findAll');
      await expect.poll(() => sourceEditor.getSelectionRanges()).toHaveLength(3);

      await sourceActions.ensureVisualMode();
      const proseMirror = page.locator('.ProseMirror:visible').first();
      await expect(proseMirror).toBeVisible({ timeout: 15000 });

      const prose = proseMirror.getByText('Prose mentioning a widget.');
      const findInput = page.locator(
        "[class*='rstudio_source_panel'] .rstudio-find-replace-find-input:visible input");

      // Prose has no Ace instance behind it and ProseMirror has no
      // multi-cursor concept, so both commands report unavailable there
      // rather than silently doing nothing.
      await prose.click();
      await expect.poll(() => isCommandEnabled(page, 'quickAddNext')).toBe(false);
      expect(await isCommandEnabled(page, 'findAll')).toBe(false);

      await focusChunk(page, proseMirror, '{r alpha}');
      await expect.poll(() => isCommandEnabled(page, 'quickAddNext')).toBe(true);
      expect(await isCommandEnabled(page, 'findAll')).toBe(true);

      // The repro from the issue: select an occurrence, then Find and Add Next
      // adds the following one, and typing edits both cursors at once. The
      // third `widget` is left alone.
      const first = AceEditor.visualModeChunk(page, '{r alpha}', proseMirror);
      await first.find('widget');
      await expect.poll(() => first.getSelectedText()).toBe('widget');

      // Opening the find bar blurs Ace and clears VisualMode.activeEditor_, but
      // deliberately leaves code commands enabled. Dispatch must restore the
      // chunk before it tries to add another cursor.
      await executeCommand(page, 'findReplace');
      await expect(findInput).toBeFocused();
      await awaitFindSettled(proseMirror);
      expect(await isCommandEnabled(page, 'quickAddNext')).toBe(true);
      await executeCommand(page, 'quickAddNext');
      await page.keyboard.type('gadget');
      await expect.poll(() => first.getValue()).toContain('gadget <- 1\ngadget + widget');

      // Find All takes every occurrence in the focused chunk.
      await focusChunk(page, proseMirror, '{r beta}');
      const second = AceEditor.visualModeChunk(page, '{r beta}', proseMirror);
      await second.find('gizmo');
      await expect.poll(() => second.getSelectedText()).toBe('gizmo');
      await executeCommand(page, 'findAll');
      await page.keyboard.type('doohickey');
      await expect.poll(() => second.getValue()).toContain('doohickey <- 2\ndoohickey + doohickey');

      // The Command Palette is the route that matters for these two -- findAll
      // has no shortcut and no menu entry, and quickAddNext's Cmd+D is
      // disableModes="default,vim,emacs" -- and it takes focus out of the chunk
      // on the way, which withActiveEditor has to survive.
      await focusChunk(page, proseMirror, '{r gamma}');
      const third = AceEditor.visualModeChunk(page, '{r gamma}', proseMirror);
      await third.find('sprocket');
      await expect.poll(() => third.getSelectedText()).toBe('sprocket');

      // Start the palette from the find input, so closing it restores focus to
      // the non-editor control rather than directly to the chunk. Find All
      // must perform the editing-surface handoff itself.
      await executeCommand(page, 'findReplace');
      await expect(findInput).toBeFocused();
      await awaitFindSettled(proseMirror);
      await expect.poll(() => isCommandEnabled(page, 'findAll')).toBe(true);

      const palette = page.locator('#rstudio_command_palette_search');
      await executeCommand(page, 'showCommandPalette');
      await expect(palette).toBeVisible({ timeout: 15000 });
      await palette.pressSequentially('Find All');
      // Click the row rather than pressing Enter, which runs whichever entry is
      // highlighted first and need not be this one.
      const findAllEntry = page
        .locator('#rstudio_command_palette_list')
        .getByText('Find All', { exact: true });
      await expect(findAllEntry).toBeVisible({ timeout: 15000 });
      await findAllEntry.click();
      await expect(palette).toBeHidden();

      // Cursor count rather than typed text: it reads the same wherever the
      // palette left focus, so a miss means the command did not reach the
      // chunk rather than that the keystrokes went elsewhere.
      await expect.poll(() => third.getSelectionRanges()).toHaveLength(3);

      // Neither command reached the prose, and leaving the chunk makes them
      // unavailable again.
      await expect(proseMirror).toContainText('Prose mentioning a widget.');
      await prose.click();
      await expect.poll(() => isCommandEnabled(page, 'quickAddNext')).toBe(false);
    } finally {
      // Close the tab rather than toggling back to source mode: toggling
      // leaves the chunks' Ace editors mounted, which makes the source-mode
      // editor locators ambiguous for later tests in the shared IDE.
      await sourceActions.closeSourceAndDeleteFile(fileName);
    }
  });

  test('format reload disables blurred chunk commands (#16540)', async ({ rstudioPage: page }) => {
    const sourceFile = `quarto_format_reload_source_${Date.now()}.R`;
    const visualFile = `quarto_format_reload_commands_${Date.now()}.qmd`;
    const content = [
      '---',
      'title: Format reload commands',
      'from: markdown+smart',
      '---',
      '',
      '```{r reload}',
      'widget <- 1',
      'widget + widget',
      '```',
    ].join('\n');

    await sourceActions.createAndOpenFile(sourceFile, 'widget <- 1\nwidget + widget');
    await sourceActions.createAndOpenFile(visualFile, content);
    try {
      await sourceActions.ensureVisualMode();
      const proseMirror = page.locator('.ProseMirror:visible').first();
      await expect(proseMirror).toBeVisible({ timeout: 15000 });
      const findInput = page.locator(
        "[class*='rstudio_source_panel'] .rstudio-find-replace-find-input:visible input");

      // Change a format-affecting YAML field in Panmirror's embedded YAML Ace
      // editor. The idle sync will offer to rebuild the visual editor.
      const yaml = AceEditor.visualModeChunk(
        page,
        'from: markdown+smart',
        proseMirror,
      );
      await yaml.find('markdown+smart');
      await expect.poll(() => yaml.getSelectedText()).toBe('markdown+smart');
      await yaml.insert('markdown-smart');

      // Blur the YAML editor through Find before the reload starts. Chunk
      // commands intentionally remain enabled across this ordinary blur.
      await executeCommand(page, 'findReplace');
      await expect(findInput).toBeFocused();
      await expect.poll(() => isCommandEnabled(page, 'quickAddNext')).toBe(true);
      expect(await isCommandEnabled(page, 'findAll')).toBe(true);

      const reload = visualEditorPanel(proseMirror).getByText('Reload Now', {
        exact: true,
      });
      await expect(reload).toBeVisible({ timeout: 15000 });
      await reload.click();

      // Teardown has no active chunk destroy hook to change global command
      // state. The reload path itself must disable commands dispatched through
      // the active editor, and visual dispatch must not fall through to the
      // hidden source editor.
      await expect.poll(() => isCommandEnabled(page, 'quickAddNext')).toBe(false);
      expect(await isCommandEnabled(page, 'findAll')).toBe(false);
      expect(await isCommandEnabled(page, 'codeCompletion')).toBe(false);
      await expect(page.locator('.ProseMirror:visible').first()).toBeVisible({
        timeout: 15000,
      });

      // Command enabled state is global. Activating a regular source target
      // after the visual reload must restore the commands through the normal
      // supported-command lifecycle, not wait for another visual chunk focus.
      const sourceTabs = page.locator(
        "[class*='rstudio_source_panel'] .gwt-TabLayoutPanelTab");
      const sourceTab = sourceTabs.filter({ hasText: sourceFile }).first();
      await sourceTab.evaluate((element) => (element as HTMLElement).click());
      await expect(sourceActions.sourcePane.selectedTab).toContainText(sourceFile);
      await expect.poll(() => isCommandEnabled(page, 'quickAddNext')).toBe(true);
      expect(await isCommandEnabled(page, 'findAll')).toBe(true);
      expect(await isCommandEnabled(page, 'codeCompletion')).toBe(true);

      const sourceEditor = new AceEditor(page, '');
      await sourceEditor.focus();
      await sourceEditor.find('widget');
      await expect.poll(() => sourceEditor.getSelectedText()).toBe('widget');
      await executeCommand(page, 'quickAddNext');
      await expect.poll(() => sourceEditor.getSelectionRanges()).toHaveLength(2);
    } finally {
      await consoleActions.resetSourcePane();
      await consoleActions.executeInConsole(
        `unlink(c("${sourceFile}", "${visualFile}"))`,
        { wait: true },
      );
    }
  });

  test('closing an inactive visual document preserves source command state (#16540)', async ({ rstudioPage: page }) => {
    const sourceFile = `quarto_command_state_${Date.now()}.R`;
    const visualFile = `quarto_command_owner_${Date.now()}.qmd`;

    await sourceActions.createAndOpenFile(sourceFile, 'widget <- 1\nwidget + widget');
    await sourceActions.createAndOpenFile(visualFile, [
      '---',
      'title: Command owner',
      '---',
      '',
      '```{r owner}',
      'widget <- 1',
      '```',
    ].join('\n'));

    try {
      await sourceActions.ensureVisualMode();
      await saveDocument(page);
      const proseMirror = page.locator('.ProseMirror:visible').first();
      await expect(proseMirror).toBeVisible({ timeout: 15000 });
      await focusChunk(page, proseMirror, '{r owner}');
      await expect.poll(() => isCommandEnabled(page, 'quickAddNext')).toBe(true);

      // Activate the R file through a synthetic tab click. Unlike a pointer
      // click, this does not itself take DOM focus away from the chunk, which
      // exercises the stale activeEditor_ state the destroy hook must handle.
      const sourceTabs = page.locator(
        "[class*='rstudio_source_panel'] .gwt-TabLayoutPanelTab");
      const sourceTab = sourceTabs.filter({ hasText: sourceFile }).first();
      await sourceTab.evaluate((element) => (element as HTMLElement).click());
      await expect(sourceActions.sourcePane.selectedTab).toContainText(sourceFile);
      await expect.poll(() => isCommandEnabled(page, 'quickAddNext')).toBe(true);

      // Closing the now-inactive visual target destroys its chunks. That
      // target must not disable the global AppCommand state owned by this R
      // editor.
      await executeCommand(page, 'closeOtherSourceDocs');
      await expect(sourceTabs.filter({ hasText: visualFile })).toHaveCount(0);
      await expect.poll(() => isCommandEnabled(page, 'quickAddNext')).toBe(true);

      const sourceEditor = new AceEditor(page, '');
      await sourceEditor.focus();
      await sourceEditor.find('widget');
      await expect.poll(() => sourceEditor.getSelectedText()).toBe('widget');
      await executeCommand(page, 'quickAddNext');
      await expect.poll(() => sourceEditor.getSelectionRanges()).toHaveLength(2);
    } finally {
      await consoleActions.resetSourcePane().catch((err) => {
        console.warn(`[quarto_chunks] cleanup failed for command-state tabs: ${err}`);
      });
      await consoleActions
        .executeInConsole(`unlink(c("${sourceFile}", "${visualFile}"))`, { wait: true })
        .catch((err) => {
          console.warn(`[quarto_chunks] cleanup failed for command-state files: ${err}`);
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
      '# Outline Sentinel',
      '',
      'sassafras',
      '',
      'Footnote body.[^fruit]',
      '',
      '[^fruit]: mangosteen',
      '',
      '```{r seed}',
      'gizmo <- 2',
      'kumquat <- 3',
      '```',
    ].join('\n');

    // Selectable text outside the document, for the non-document selection
    // checks below. Printed before the document opens so the console keeps
    // the focus disruption away from the editor interactions.
    await consoleActions.executeInConsole('cat("Console Sentinel\\n")', { wait: true });

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
      const focusSeedChunk = async () => {
        // Make the focus handoff explicit. Coming directly from the find input,
        // a forced textarea click can race the parent ProseMirror focus event,
        // leaving the code commands disabled even though Ace has DOM focus.
        await proseMirror.getByText('sassafras').click({ position: { x: 4, y: 8 } });
        await expect.poll(() => isCommandEnabled(page, 'quickAddNext')).toBe(false);
        await focusChunk(page, proseMirror, '{r seed}');
        await expect.poll(() => isCommandEnabled(page, 'quickAddNext')).toBe(true);
      };

      // A word selected in prose seeds the search term, as it does in source
      // mode. Double-click near the paragraph's left edge: its box spans the
      // full editor width, so the default centre point lands past the text.
      await proseMirror.getByText('sassafras').dblclick({ position: { x: 4, y: 8 } });
      await expect
        .poll(() => page.evaluate(() => window.getSelection()?.toString() ?? ''))
        .toBe('sassafras');

      await executeCommand(page, 'findReplace');
      await expect(findInput).toHaveValue('sassafras');
      await awaitFindSettled(proseMirror);

      // So does a selection made inside a code chunk, which the outer
      // ProseMirror selection cannot see on its own. Ace mirrors it into its
      // hidden textarea, which must not read as a find input selection.
      const chunk = AceEditor.visualModeChunk(page, '{r seed}', proseMirror);
      await focusSeedChunk();
      await chunk.find('gizmo');
      await expect.poll(() => chunk.getSelectedText()).toBe('gizmo');
      await executeCommand(page, 'findReplace');
      await expect(findInput).toHaveValue('gizmo');
      await awaitFindSettled(proseMirror);

      // A menu, command palette, or other non-editor control can blur the
      // chunk before dispatching Find. The command must restore the editing
      // surface before it reads the retained Ace selection. Stand in with the
      // toolbar button, not the find input: seeding left the input's text
      // selected, and a focused input selection is one the command must keep
      // (asserted below), so focusing the input would not exercise this path.
      // Each step takes the other identifier, so the box has to change to
      // pass. Ace's find() searches forward from the cursor and reports
      // nothing when it misses, so pin the selection before dispatching:
      // otherwise a stale term would fail looking like a seeding regression.
      const findButton = findReplaceButton(proseMirror);
      await focusSeedChunk();
      await chunk.find('kumquat');
      await expect.poll(() => chunk.getSelectedText()).toBe('kumquat');
      await findButton.focus();
      await executeCommand(page, 'findReplace');
      await expect(findInput).toHaveValue('kumquat');
      await awaitFindSettled(proseMirror);

      // Use Selection for Find reads the same selection. As with Find, a menu
      // or palette can own focus by the time the command handler runs; Use
      // Selection must restore the chunk itself.
      await focusSeedChunk();
      await chunk.find('gizmo');
      await expect.poll(() => chunk.getSelectedText()).toBe('gizmo');
      await findButton.focus();
      await executeCommand(page, 'findFromSelection');
      await expect(findInput).toHaveValue('gizmo');
      await awaitFindSettled(proseMirror);

      // Neither guard in getSearchSelection() seeds, and both leave the box as
      // it was. A multi-line selection is refused, as source mode's find bar
      // refuses one.
      await focusSeedChunk();
      await chunk.execCommand('selectall');
      await expect.poll(() => chunk.getSelectedText()).toContain('\n');
      const retainedSearchTerm = await findInput.inputValue();
      await executeCommand(page, 'findFromSelection');
      await expect(findInput).toHaveValue(retainedSearchTerm);

      // So is an empty one -- Ctrl+F with nothing selected, the common case --
      // but the bar still opens. First leave a valid single-line chunk
      // selection active: if the prose handoff fails to clear activeEditor_,
      // Find will incorrectly seed `gizmo` and this assertion will fail.
      await chunk.find('gizmo');
      await expect.poll(() => chunk.getSelectedText()).toBe('gizmo');
      await findButton.click();
      await expect(findInput).toBeHidden();

      // Positioned like the dblclicks above: the paragraph box spans the full
      // editor width, so a centre click lands past the text and moves nothing.
      await proseMirror.getByText('sassafras').click({ position: { x: 4, y: 8 } });
      await expect
        .poll(() => page.evaluate(() => window.getSelection()?.toString() ?? ''))
        .toBe('');
      await executeCommand(page, 'findReplace');
      await expect(findInput).toBeVisible();
      await expect(findInput).toHaveValue(retainedSearchTerm);
      await awaitFindSettled(proseMirror);

      // After interacting with a chunk, Panmirror's tracked prose selection
      // can lag behind the live browser selection. Select prose again, move
      // focus to a non-editor control, and verify Find reads the current word.
      await proseMirror.getByText('sassafras').dblclick({ position: { x: 4, y: 8 } });
      await expect
        .poll(() => page.evaluate(() => window.getSelection()?.toString() ?? ''))
        .toBe('sassafras');
      await findButton.focus();
      await executeCommand(page, 'findReplace');
      await expect(findInput).toHaveValue('sassafras');
      await awaitFindSettled(proseMirror);

      // Input selections do not appear in window.getSelection(). With the
      // find input focused and a distinct value selected in it, the chunk's
      // retained `gizmo` selection -- what the editor would otherwise seed --
      // must not replace it: the focused input wins. A DOM range cannot stand
      // in for a stale editor selection here, as placing one in the editor
      // moves focus out of the input.
      const selectFindInput = async () => {
        await focusSeedChunk();
        await chunk.find('gizmo');
        await expect.poll(() => chunk.getSelectedText()).toBe('gizmo');
        await findInput.fill('current-search-term');
        await findInput.selectText();
        await expect
          .poll(() => findInput.evaluate((input: HTMLInputElement) => ({
            active: document.activeElement === input,
            selected: input.selectionEnd! - input.selectionStart!,
          })))
          .toEqual({ active: true, selected: 'current-search-term'.length });
      };

      await selectFindInput();
      await executeCommand(page, 'findReplace');
      await expect(findInput).toHaveValue('current-search-term');

      await selectFindInput();
      await executeCommand(page, 'findFromSelection');
      await expect(findInput).toHaveValue('current-search-term');

      // Only a text selection in the input counts. Use Selection handed focus
      // back to the chunk, which still holds `gizmo`; once that handoff has
      // landed, focus the input with a collapsed selection, and the same
      // command seeds from the chunk.
      await expect.poll(() => chunk.isFocused()).toBe(true);
      await expect.poll(() => chunk.getSelectedText()).toBe('gizmo');
      await findInput.focus();
      await findInput.evaluate((input: HTMLInputElement) => {
        input.setSelectionRange(input.value.length, input.value.length);
      });
      await expect
        .poll(() => findInput.evaluate((input: HTMLInputElement) => ({
          active: document.activeElement === input,
          selected: input.selectionEnd! - input.selectionStart!,
        })))
        .toEqual({ active: true, selected: 0 });
      await executeCommand(page, 'findReplace');
      await expect(findInput).toHaveValue('gizmo');
      // Let the buffered find land on the chunk match now, or it fires after
      // the footnote click below and closes the footnote editor.
      await awaitFindSettled(proseMirror);

      // The footnote steps go last. Panmirror's footnote editor is a 160px
      // panel pinned to the editor's bottom edge that stays open while the
      // selection is in a footnote, and the buffered find keeps re-selecting
      // the footnote match; in CI's short windows nothing that needs the
      // document body can reliably follow them.
      // Footnote editing uses a second `.pm-content` root. Its selection is
      // still document content and must seed Find just like the main body.
      const footnote = proseMirror.locator('.pm-footnote').first();
      await expect(footnote).toBeVisible();
      await footnote.click();
      const noteContent = visualEditorPanel(proseMirror).locator(
        '.notes .pm-content:visible');
      const noteText = noteContent.getByText('mangosteen', { exact: true });
      await expect(noteText).toBeVisible();
      await noteText.dblclick({ position: { x: 4, y: 8 } });
      await expect
        .poll(() => page.evaluate(() => window.getSelection()?.toString() ?? ''))
        .toBe('mangosteen');
      await findButton.focus();
      await executeCommand(page, 'findReplace');
      await expect(findInput).toHaveValue('mangosteen');
      await awaitFindSettled(proseMirror);

      // A live text selection outside document content must never become
      // the search term. RStudio's chrome, the outline included, is
      // user-select: none, and Selection.toString() -- what getSelectedText()
      // reads -- is empty for a range in it, so the only such selection a
      // user can make lies in another pane. Select the console sentinel; the
      // visual editor stays the active document throughout.
      const consoleSentinel = consoleActions.consolePane.consoleOutput
        .getByText('Console Sentinel', { exact: true })
        .last();
      await expect(consoleSentinel).toBeVisible();

      const selectConsoleSentinel = async () => {
        await consoleSentinel.evaluate((element) => {
          const range = document.createRange();
          range.selectNodeContents(element);
          const selection = window.getSelection();
          selection?.removeAllRanges();
          selection?.addRange(range);
        });
        await expect
          .poll(() => page.evaluate(() => (window.getSelection()?.toString() ?? '').trim()))
          .toBe('Console Sentinel');
      };

      // Such a selection is invalid, rather than an invitation to fall back
      // to a stale document selection. Preserve the footnote term through Use
      // Selection and direct Find.
      await selectConsoleSentinel();
      await executeCommand(page, 'findFromSelection');
      await expect(findInput).toHaveValue('mangosteen');

      await selectConsoleSentinel();
      await executeCommand(page, 'findReplace');
      await expect(findInput).toHaveValue('mangosteen');

      await findButton.click();
      await expect(findInput).toBeHidden();

      // Keep a valid chunk selection active, then create the console
      // selection programmatically so Ace does not blur. Toolbar mouse-down
      // must prefer the live non-document selection over stale activeEditor_.
      await focusSeedChunk();
      await chunk.find('gizmo');
      await expect.poll(() => chunk.getSelectedText()).toBe('gizmo');
      await selectConsoleSentinel();
      await findButton.click();
      await expect(findInput).toHaveValue('mangosteen');
    } finally {
      await sourceActions.closeSourceAndDeleteFile(fileName).catch((err) => {
        console.warn(`[quarto_chunks] cleanup failed for ${fileName}: ${err}`);
      });
    }
  });

  test('the visual mode find bar toolbar preserves a prose selection (#16540)', async ({ rstudioPage: page }) => {
    const fileName = `quarto_find_toolbar_prose_${Date.now()}.qmd`;
    const content = [
      '---',
      'title: Find toolbar prose',
      '---',
      '',
      'persimmon',
      '',
      'nectarine',
    ].join('\n');

    await sourceActions.createAndOpenFile(fileName, content);
    try {
      await sourceActions.ensureVisualMode();
      const proseMirror = page.locator('.ProseMirror:visible').first();
      await expect(proseMirror).toBeVisible({ timeout: 15000 });
      const findInput = page.locator(
        "[class*='rstudio_source_panel'] .rstudio-find-replace-find-input:visible input");
      const findButton = findReplaceButton(proseMirror);

      await proseMirror.getByText('persimmon').dblclick({ position: { x: 4, y: 8 } });
      await expect
        .poll(() => page.evaluate(() => window.getSelection()?.toString() ?? ''))
        .toBe('persimmon');

      // Mouse-down must capture the term before the toolbar takes focus.
      await findButton.click();
      await expect(findInput).toHaveValue('persimmon');
      await awaitFindSettled(proseMirror);

      // Keyboard activation has no mouse-down. Close the bar, select a
      // different term so a stale value cannot pass, and open it with Enter.
      await findButton.click();
      await expect(findInput).toBeHidden();
      await proseMirror.getByText('nectarine').dblclick({ position: { x: 4, y: 8 } });
      await expect
        .poll(() => page.evaluate(() => window.getSelection()?.toString() ?? ''))
        .toBe('nectarine');
      await findButton.focus();
      await page.keyboard.press('Enter');
      await expect(findInput).toHaveValue('nectarine');
    } finally {
      await sourceActions.closeSourceAndDeleteFile(fileName).catch((err) => {
        console.warn(`[quarto_chunks] cleanup failed for ${fileName}: ${err}`);
      });
    }
  });

  test('the visual mode find bar toolbar preserves a chunk selection (#16540)', async ({ rstudioPage: page }) => {
    const fileName = `quarto_find_toolbar_chunk_${Date.now()}.qmd`;
    const content = [
      '---',
      'title: Find toolbar chunk',
      '---',
      '',
      '```{r toolbar}',
      'gizmo <- 2',
      'kumquat <- 3',
      '```',
    ].join('\n');

    await sourceActions.createAndOpenFile(fileName, content);
    try {
      await sourceActions.ensureVisualMode();
      const proseMirror = page.locator('.ProseMirror:visible').first();
      await expect(proseMirror).toBeVisible({ timeout: 15000 });
      const findInput = page.locator(
        "[class*='rstudio_source_panel'] .rstudio-find-replace-find-input:visible input");
      const findButton = findReplaceButton(proseMirror);
      const chunk = AceEditor.visualModeChunk(page, '{r toolbar}', proseMirror);

      await focusChunk(page, proseMirror, '{r toolbar}');
      await expect.poll(() => isCommandEnabled(page, 'quickAddNext')).toBe(true);
      await chunk.find('gizmo');
      await expect.poll(() => chunk.getSelectedText()).toBe('gizmo');

      // The same capture has to run before the embedded Ace editor blurs.
      await findButton.click();
      await expect(findInput).toHaveValue('gizmo');
      await awaitFindSettled(proseMirror);

      // Enter / Space go through ToolbarButton.click() without a mouse-down.
      // Refocus Ace, select another term, then verify the keyboard path
      // restores the chunk before it reads the selection.
      await findButton.click();
      await expect(findInput).toBeHidden();
      await focusChunk(page, proseMirror, '{r toolbar}');
      await chunk.find('kumquat');
      await expect.poll(() => chunk.getSelectedText()).toBe('kumquat');
      await findButton.focus();
      await page.keyboard.press('Enter');
      await expect(findInput).toHaveValue('kumquat');
    } finally {
      await sourceActions.closeSourceAndDeleteFile(fileName).catch((err) => {
        console.warn(`[quarto_chunks] cleanup failed for ${fileName}: ${err}`);
      });
    }
  });
});
