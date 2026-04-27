// Regression test for https://github.com/rstudio/rstudio/issues/16985
// When a user defines a str() method for a custom class in the global
// environment, the Environment pane was dropping the first list element.
// .rs.valueContentsImpl() unconditionally removed the first line of str()
// output assuming it was a "List of N" header, but user-defined str methods
// can cause str.default() to omit that header, so a data line got dropped.

import { test, expect } from '@fixtures/rstudio.fixture';
import { sleep } from '@utils/constants';
import { ConsolePaneActions } from '@actions/console_pane.actions';

/**
 * Helper: call .rs.valueContents() on an R expression and return the
 * concatenated result lines, separated by "|||".
 */
async function getValueContents(
  consoleActions: ConsolePaneActions,
  rExpr: string
): Promise<string> {
  const marker = `__VC_${Date.now()}__`;
  await consoleActions.clearConsole();
  await consoleActions.typeInConsole(
    `cat("${marker}", paste(.rs.valueContents(${rExpr}), collapse = "|||"), "${marker}")`
  );
  await sleep(2000);

  const output = await consoleActions.consolePane.consoleOutput.innerText();
  const match = output.match(new RegExp(`${marker}\\s+([\\s\\S]+?)\\s+${marker}`));
  expect(match, `marker not found in console output for: ${rExpr}`).toBeTruthy();
  return match![1];
}

test.describe('Environment pane valueContents', { tag: ['@parallel_safe'] }, () => {
  let consoleActions: ConsolePaneActions;

  test.beforeAll(async ({ rstudioPage: page }) => {
    consoleActions = new ConsolePaneActions(page);
    await consoleActions.clearConsole();
  });

  // -----------------------------------------------------------------------
  // Regression: user-defined str method drops first list element (#16985)
  // -----------------------------------------------------------------------
  test('first list element is not dropped with user-defined str method (#16985)', async () => {
    await consoleActions.typeInConsole('obj <- structure(list(x = 10, y = 20), class = "myclass")');
    await sleep(1000);
    await consoleActions.typeInConsole('str.myclass <- function(object, ...) { cat("myclass:\\n"); NextMethod() }');
    await sleep(1000);

    // Before the fix, "$ x" was dropped; only "$ y" appeared.
    const contents = await getValueContents(consoleActions, 'obj');
    expect(contents).toContain('$ x');
    expect(contents).toContain('$ y');

    await consoleActions.typeInConsole('rm(obj, str.myclass)');
    await sleep(500);
  });

  // -----------------------------------------------------------------------
  // Standard list (no user str method) — header should be stripped normally
  // -----------------------------------------------------------------------
  test('standard list displays all elements correctly', async () => {
    await consoleActions.typeInConsole('plain_list <- list(a = 1, b = 2, c = 3)');
    await sleep(1000);

    const contents = await getValueContents(consoleActions, 'plain_list');
    expect(contents).toContain('$ a');
    expect(contents).toContain('$ b');
    expect(contents).toContain('$ c');
    // The "List of 3" header should have been stripped
    expect(contents).not.toContain('List of');

    await consoleActions.typeInConsole('rm(plain_list)');
    await sleep(500);
  });

  // -----------------------------------------------------------------------
  // Data frame — header should be stripped normally
  // -----------------------------------------------------------------------
  test('data frame displays all columns correctly', async () => {
    await consoleActions.typeInConsole('df <- data.frame(name = c("a", "b"), val = c(1, 2))');
    await sleep(1000);

    const contents = await getValueContents(consoleActions, 'df');
    expect(contents).toContain('$ name');
    expect(contents).toContain('$ val');
    // The header (e.g. "'data.frame': 2 obs. of 2 variables:") should be stripped
    expect(contents).not.toContain('data.frame');

    await consoleActions.typeInConsole('rm(df)');
    await sleep(500);
  });

  // -----------------------------------------------------------------------
  // Nested list (>150 str lines) — truncation with correct first element
  // -----------------------------------------------------------------------
  test('nested list truncates correctly and keeps first element', async () => {
    // str() defaults to list.len = 99, so a flat list caps at ~100 lines.
    // A nested list where each element expands to multiple str lines
    // (sub-header + 4 values = 5 lines × 40 groups + 1 header = 201 lines)
    // triggers the n > 150 truncation path in .rs.valueContentsImpl().
    await consoleActions.typeInConsole(
      'nested <- setNames(lapply(1:40, function(i) list(a = i, b = i, c = i, d = i)), paste0("g_", 1:40))'
    );
    await sleep(1000);

    const marker = `__NEST_${Date.now()}__`;
    await consoleActions.clearConsole();
    await consoleActions.typeInConsole(
      `vc <- .rs.valueContents(nested); cat("${marker}", length(vc), vc[1], vc[length(vc)], "${marker}")`
    );
    await sleep(2000);

    const output = await consoleActions.consolePane.consoleOutput.innerText();
    const match = output.match(new RegExp(`${marker}\\s+([\\s\\S]+?)\\s+${marker}`));
    expect(match, 'marker not found in console output').toBeTruthy();

    const contents = match![1];
    // 149 data lines + 1 truncation line = 150 (header stripped)
    expect(contents).toMatch(/\b150\b/);
    // First element present
    expect(contents).toContain('$ g_1');
    // Truncation message at the end
    expect(contents).toContain('lines omitted');

    await consoleActions.typeInConsole('rm(nested, vc)');
    await sleep(500);
  });
});
