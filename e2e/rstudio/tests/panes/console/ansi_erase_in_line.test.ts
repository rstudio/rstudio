import { test, expect } from '@fixtures/rstudio.fixture';
import { sleep } from '@utils/constants';
import { getOutputLines } from '@utils/console';
import { ConsolePaneActions } from '@actions/console_pane.actions';

test.describe('ANSI Erase in Line (CSI K) - #17070', () => {
  let consoleActions: ConsolePaneActions;

  test.beforeAll(async ({ rstudioPage: page }) => {
    consoleActions = new ConsolePaneActions(page);
  });

  test.beforeEach(async () => {
    await consoleActions.clearConsole();
  });

  test('EL0 implicit - erase cursor to end of line', async () => {
    await consoleActions.typeInConsole('cat("AAAAAAAAAA\\rBBB\\033[K\\n")');

    await expect(consoleActions.consolePane.consoleOutput).toContainText('BBB');
    const output = getOutputLines(
      await consoleActions.consolePane.consoleOutput.innerText(),
    );
    expect(output).not.toMatch(/BBB.*A/);
  });

  test('EL0 explicit - erase cursor to end of line', async () => {
    await consoleActions.typeInConsole('cat("AAAAAAAAAA\\rBBB\\033[0K\\n")');

    await expect(consoleActions.consolePane.consoleOutput).toContainText('BBB');
    const output = getOutputLines(
      await consoleActions.consolePane.consoleOutput.innerText(),
    );
    expect(output).not.toMatch(/BBB.*A/);
  });

  test('EL0 progress bar - no trailing artifacts', async () => {
    await consoleActions.typeInConsole(
      'for (i in 1:5) { cat(sprintf("\\rStep %d of 5: %s\\033[K", i, strrep(".", i))); Sys.sleep(0.3) }; cat("\\n")',
    );

    // Loop runs for ~1.5s; allow headroom for the final render.
    await expect(consoleActions.consolePane.consoleOutput).toContainText(
      'Step 5 of 5: .....',
      { timeout: 10000 },
    );
  });

  test('EL1 - erase beginning of line to cursor', async () => {
    await consoleActions.typeInConsole('cat("ABCDEF\\033[3D\\033[1K\\n")');

    await expect(consoleActions.consolePane.consoleOutput).toContainText('EF');
    const output = getOutputLines(
      await consoleActions.consolePane.consoleOutput.innerText(),
    );
    expect(output).not.toMatch(/[ABCD]/);
  });

  test('EL2 - erase entire line', async () => {
    await consoleActions.typeInConsole('cat("Hello World\\033[2KGONE\\n")');

    await expect(consoleActions.consolePane.consoleOutput).toContainText('GONE');
    const output = getOutputLines(
      await consoleActions.consolePane.consoleOutput.innerText(),
    );
    expect(output).not.toContain('Hello World');
  });

  test('EL on empty line - no errors', async () => {
    // Pure side-effect check (no positive marker to wait on).
    await consoleActions.typeInConsole('cat("\\033[K\\n")');
    await sleep(1000);

    const output = getOutputLines(
      await consoleActions.consolePane.consoleOutput.innerText(),
    );
    expect(output).not.toMatch(/[Ee]rror/);
  });

  test('EL with colored text - no leftover colored text', async () => {
    await consoleActions.typeInConsole(
      'cat("\\033[31mRed Text\\033[0m Uncolored\\rOverwrite\\033[K\\n")',
    );

    await expect(consoleActions.consolePane.consoleOutput).toContainText('Overwrite');
    const output = getOutputLines(
      await consoleActions.consolePane.consoleOutput.innerText(),
    );
    expect(output).not.toContain('Red Text');
    expect(output).not.toContain('Uncolored');
  });
});
