import { test, expect } from '@fixtures/rstudio.fixture';
import { sleep } from '@utils/constants';
import { getOutputLines } from '@utils/console';
import { ConsolePaneActions } from '@actions/console_pane.actions';

test.describe('ANSI Erase in Line (CSI K) - #17070', () => {
  let consoleActions: ConsolePaneActions;

  test.beforeAll(async ({ rstudioPage: page }) => {
    consoleActions = new ConsolePaneActions(page);
  });

  test('EL0 implicit - erase cursor to end of line', async ({ rstudioPage: page }) => {
    await consoleActions.clearConsole();
    await consoleActions.typeInConsole('cat("AAAAAAAAAA\\rBBB\\033[K\\n")');
    await sleep(1000);

    const fullText = await consoleActions.consolePane.consoleOutput.innerText();
    const output = getOutputLines(fullText);
    expect(output).toContain('BBB');
    expect(output).not.toMatch(/BBB.*A/);
  });

  test('EL0 explicit - erase cursor to end of line', async ({ rstudioPage: page }) => {
    await consoleActions.clearConsole();
    await consoleActions.typeInConsole('cat("AAAAAAAAAA\\rBBB\\033[0K\\n")');
    await sleep(1000);

    const fullText = await consoleActions.consolePane.consoleOutput.innerText();
    const output = getOutputLines(fullText);
    expect(output).toContain('BBB');
    expect(output).not.toMatch(/BBB.*A/);
  });

  test('EL0 progress bar - no trailing artifacts', async ({ rstudioPage: page }) => {
    await consoleActions.clearConsole();
    await consoleActions.typeInConsole('for (i in 1:5) { cat(sprintf("\\rStep %d of 5: %s\\033[K", i, strrep(".", i))); Sys.sleep(0.3) }; cat("\\n")');
    await sleep(3000);

    const fullText = await consoleActions.consolePane.consoleOutput.innerText();
    const output = getOutputLines(fullText);
    expect(output).toContain('Step 5 of 5: .....');
  });

  test('EL1 - erase beginning of line to cursor', async ({ rstudioPage: page }) => {
    await consoleActions.clearConsole();
    await consoleActions.typeInConsole('cat("ABCDEF\\033[3D\\033[1K\\n")');
    await sleep(1000);

    const fullText = await consoleActions.consolePane.consoleOutput.innerText();
    const output = getOutputLines(fullText);
    expect(output).toContain('EF');
    expect(output).not.toMatch(/[ABCD]/);

  });

  test('EL2 - erase entire line', async ({ rstudioPage: page }) => {
    await consoleActions.clearConsole();
    await consoleActions.typeInConsole('cat("Hello World\\033[2KGONE\\n")');
    await sleep(1000);

    const fullText = await consoleActions.consolePane.consoleOutput.innerText();
    const output = getOutputLines(fullText);
    expect(output).toContain('GONE');
    expect(output).not.toContain('Hello World');
  });

  test('EL on empty line - no errors', async ({ rstudioPage: page }) => {
    await consoleActions.clearConsole();
    await consoleActions.typeInConsole('cat("\\033[K\\n")');
    await sleep(1000);

    const fullText = await consoleActions.consolePane.consoleOutput.innerText();
    const output = getOutputLines(fullText);
    expect(output).not.toMatch(/[Ee]rror/);
  });

  test('EL with colored text - no leftover colored text', async ({ rstudioPage: page }) => {
    await consoleActions.clearConsole();
    await consoleActions.typeInConsole('cat("\\033[31mRed Text\\033[0m Uncolored\\rOverwrite\\033[K\\n")');
    await sleep(1000);

    const fullText = await consoleActions.consolePane.consoleOutput.innerText();
    const output = getOutputLines(fullText);
    expect(output).toContain('Overwrite');
    expect(output).not.toContain('Red Text');
    expect(output).not.toContain('Uncolored');
  });
});
