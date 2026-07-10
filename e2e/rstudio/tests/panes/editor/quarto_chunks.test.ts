/**
 * Quarto chunk and editor behavior: the `warn` option round-trip on
 * chunk run, chunk-widget visibility, variable-width nested-chunk
 * folding (#15191), and the empty-quarto-block highlight regression
 * (#16463). Multiline chunk execution (#17350) is covered by
 * `multiline_chunk_execution.test.ts`.
 */

import { test, expect } from '@fixtures/rstudio.fixture';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { SourcePaneActions } from '@actions/source_pane.actions';
import { AceEditor } from '@pages/ace_editor.page';
import { useSuiteSandbox } from '@utils/sandbox';
import { executeCommand } from '@utils/commands';

test.describe.serial('Quarto chunks', { tag: ['@serial'] }, () => {
  useSuiteSandbox();
  let consoleActions: ConsolePaneActions;
  let sourceActions: SourcePaneActions;

  test.beforeAll(async ({ rstudioPage: page }) => {
    consoleActions = new ConsolePaneActions(page);
    sourceActions = new SourcePaneActions(page, consoleActions);
    await consoleActions.clearConsole();
  });

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
});
