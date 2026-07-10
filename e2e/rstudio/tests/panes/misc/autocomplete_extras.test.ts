// Autocomplete tests ported from
// src/cpp/tests/automation/testthat/test-automation-completions.R.
//
// The existing autocomplete.test.ts already covers function-parameter
// completions (#13196), list `$` completions (#13291), and local-variable
// ordering (#12678). This file fills in the long tail:
//
//   - new local variables show up as prefix completions
//   - R6 active bindings are not evaluated by completion (#14784)
//   - pipe-expression completions at the start of a document (#13611)
//   - .DollarNames completions (#15115), including trailing-parens stripping
//   - code_completion_include_already_used pref (#13065)
//   - dplyr backtick-quoted column names (#15161)
//   - column quoting via the Tab-accept path (#13290)
//   - roxygen tag completions in .R / .Rmd / .qmd (#5809)
//   - pipe placeholder `_$` and `_$<prefix>` completions
//
// "Tab keypresses indent multi-line selections" (#15046) is more of an
// editor-shortcut test than a completion test; it's a candidate for a
// future editor.test.ts addition rather than living here.

import { test, expect } from '@fixtures/rstudio.fixture';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { SourcePaneActions } from '@actions/source_pane.actions';
import { AutocompleteActions } from '@actions/autocomplete.actions';
import { useSuiteSandbox } from '@utils/sandbox';
import { setPref, clearPref } from '@utils/commands';

test.describe('Autocomplete extras', () => {
  // setwd into a per-spec sandbox so the relative filenames used by
  // getCompletionsInEditor land in a clean workdir.
  useSuiteSandbox();
  let consoleActions: ConsolePaneActions;
  let autocomplete: AutocompleteActions;

  test.beforeAll(async ({ rstudioPage: page }) => {
    consoleActions = new ConsolePaneActions(page);
    const sourceActions = new SourcePaneActions(page, consoleActions);
    autocomplete = new AutocompleteActions(page, consoleActions, sourceActions);
    await consoleActions.resetSourcePane();
    await consoleActions.executeInConsole('rm(list = ls())', { wait: true });
  });

  test.afterEach(async ({ rstudioPage: page }) => {
    // Dismiss lingering popups / partial console input.
    await page.keyboard.press('Escape');
    await page.keyboard.press('Escape');
    await consoleActions.resetSourcePane();
  });

  test('new local variables show up as prefix completions', async () => {
    const items = await autocomplete.getCompletionsInConsole(
      ['foobar <- 42', 'foobaz <- 42'],
      'foo',
    );
    expect(items).toEqual(expect.arrayContaining(['foobar', 'foobaz']));
  });

  // https://github.com/rstudio/rstudio/issues/14784
  test('autocompletion does not trigger R6 active bindings', async ({ rstudioPage: page }) => {
    const setup = [
      'library(R6)',
      'Test <- R6Class("Test", active = list(active_test = function(value) print("active")), private = list(private_test = function(value) print("private")), public = list(public_test = function(value) print("public")))',
      'n <- Test$new()',
      'nms <- .rs.getNames(n)',
    ];
    for (const code of setup) {
      await consoleActions.executeInConsole(code, { wait: true });
    }

    // .rs.getNames itself must not have evaluated the active binding.
    const before = await consoleActions.consolePane.consoleOutput.innerText();
    expect(before).not.toMatch(/\[1\]\s*"active"/);

    // Triggering completions on `n` (Tab) must also not evaluate it. Wait for
    // the completion popup to appear (proof that Tab triggered completion)
    // before dismissing; that's the precondition the test is about.
    await consoleActions.typeInConsole('n');
    await page.keyboard.press('Tab');
    const popup = page.locator('#rstudio_popup_completions');
    await expect(popup).toBeVisible({ timeout: 5000 });
    await page.keyboard.press('Escape');
    await expect(popup).not.toBeVisible({ timeout: 2000 });
    await page.keyboard.press('Backspace');

    const after = await consoleActions.consolePane.consoleOutput.innerText();
    expect(after).not.toMatch(/\[1\]\s*"active"/);
  });

  // https://github.com/rstudio/rstudio/issues/13611
  test('completions work inside a piped expression at the start of a document', async () => {
    const items = await autocomplete.getCompletionsInEditor(
      [],
      '\nmtcars |> mutate(x = mean())',
      2,
      26,
    );
    expect(items.slice(0, 2)).toEqual(['x =', '... =']);
  });

  // https://github.com/rstudio/rstudio/issues/15115
  test('.DollarNames completions preserve type-derived display', async () => {
    // Case 1: $-names from a names()-based .DollarNames method.
    const cls1Setup = [
      'className <- basename(tempfile(pattern = "class-"))',
      'registerS3method(".DollarNames", className, function(x, pattern) names(x))',
      'cls1_obj <- structure(list(apple = identity, banana = identity), class = className)',
    ];
    const items1 = await autocomplete.getCompletionsInConsole(cls1Setup, 'cls1_obj$');
    expect(items1).toEqual(expect.arrayContaining(['apple', 'banana']));

    // Case 2: names with trailing "()" -- the popup display strips the
    // parens, leaving just the function name.
    const cls2Setup = [
      'className <- basename(tempfile(pattern = "class-"))',
      'registerS3method(".DollarNames", className, function(x, pattern) c("example1()", "example2()"))',
      'cls2_obj <- structure(list(), class = className)',
    ];
    const items2 = await autocomplete.getCompletionsInConsole(cls2Setup, 'cls2_obj$');
    expect(items2).toEqual(expect.arrayContaining(['example1', 'example2']));
  });

  // https://github.com/rstudio/rstudio/issues/13065
  test('code_completion_include_already_used pref shows already-used args', async () => {
    const content = 'mtcars |> write()';

    // Default pref (off): already-used `x` arg is hidden from completions.
    const before = await autocomplete.getCompletionsInEditor([], content, 1, 16);
    expect(before.slice(0, 4)).toEqual(['file =', 'ncolumns =', 'append =', 'sep =']);

    await setPref(consoleActions.page, 'code_completion_include_already_used', true);
    try {
      const after = await autocomplete.getCompletionsInEditor([], content, 1, 16);
      expect(after.slice(0, 5)).toEqual(['x =', 'file =', 'ncolumns =', 'append =', 'sep =']);
    } finally {
      await clearPref(consoleActions.page, 'code_completion_include_already_used');
    }
  });

  // https://github.com/rstudio/rstudio/issues/15161
  test('dplyr piped backtick-quoted column names are listed unquoted', async () => {
    const missing = await consoleActions.ensurePackages(['dplyr']);
    test.skip(missing.length > 0, `Missing: ${missing.join(', ')}`);

    const content = 'library(dplyr)\nmtcars |> rename(`zzz A` = 1, `zzz B` = 2) |> select()';
    const items = await autocomplete.getCompletionsInEditor([], content, 2, 53);
    expect(items).toEqual(expect.arrayContaining(['zzz A', 'zzz B']));
  });

  // https://github.com/rstudio/rstudio/issues/13290
  test('column names with special chars are properly quoted on accept', async ({ rstudioPage: page }) => {
    await consoleActions.executeInConsole(
      'cols_q <- list(apple = "apple", "2024" = "2024")',
      { wait: true },
    );

    // Typing `$` auto-opens the autocomplete popup; an explicit Tab here
    // would just accept the highlighted entry ("apple") before we get a
    // chance to navigate to "2024".
    await consoleActions.typeInConsole('cols_q$');
    await expect(page.locator('#rstudio_popup_completions')).toBeVisible({ timeout: 5000 });

    // Second item ("2024") needs Down to highlight, then Enter to accept
    // the completion, then Enter again to submit the line.
    await page.keyboard.press('ArrowDown');
    await page.keyboard.press('Enter');
    await page.keyboard.press('Enter');
    await expect(consoleActions.consolePane.consoleOutput).toContainText('[1] "2024"');
  });

  // https://github.com/rstudio/rstudio/issues/5809 -- roxygen tag completions.
  // Same content (a single `#' @` line above an identity2 function), checked
  // in three host file types so the completion provider hooks survive each
  // mode.
  for (const ext of ['R', 'Rmd', 'qmd'] as const) {
    test(`roxygen tag completions appear in .${ext} files`, async () => {
      const content = ext === 'R'
        ? "#' @\nidentity2 <- function(x) x"
        : `---\ntitle: Test\n---\n\n\`\`\`{r}\n#' @\nidentity2 <- function(x) x\n\`\`\``;
      const line = ext === 'R' ? 1 : 6;
      const col = 5;
      const items = await autocomplete.getCompletionsInEditor([], content, line, col, ext);
      expect(items.map((s) => s.trim())).toEqual(expect.arrayContaining(['@param']));
    });
  }

  test('pipe placeholder _$ lists columns from the upstream tibble', async () => {
    const missing = await consoleActions.ensurePackages(['dplyr']);
    test.skip(missing.length > 0, `Missing: ${missing.join(', ')}`);

    // No prefix after _$: list every column produced by the upstream chain.
    const content1 = 'library(dplyr)\nmtcars |>\n   mutate(x = 42) |>\n   _$';
    const items1 = await autocomplete.getCompletionsInEditor([], content1, 4, 5);
    expect(items1).toEqual(expect.arrayContaining(['x', 'mpg']));

    // Prefix after _$ narrows the list to matching column names.
    const content2 = 'library(dplyr)\nmtcars |>\n   mutate(x = 42) |>\n   _$c';
    const items2 = await autocomplete.getCompletionsInEditor([], content2, 4, 7);
    expect(items2).toEqual(expect.arrayContaining(['carb', 'cyl']));
  });
});
