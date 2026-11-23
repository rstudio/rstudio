import type { Page } from 'playwright';
import { test, expect } from '@fixtures/rstudio.fixture';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { SourcePaneActions } from '@actions/source_pane.actions';
import { executeCommand } from '@utils/commands';
import { clearConsole, executeInConsole, getConsolePromptText } from '@pages/console_pane.page';
import { PLOT_IMAGE_FRAME } from '@pages/plots_pane.page';
import { closeAndDeleteSandboxFiles, writeAndOpenFile } from '@utils/files';
import { heredoc } from '@utils/heredoc';
import { consoleOutput, JULIA_PROMPT, JULIA_START_TIMEOUT, leaveJulia, runInConsole, useJulia } from '@utils/julia';
import { useSuiteSandbox } from '@utils/sandbox';

// Julia values that render as PNG are shown in the Plots pane, or in a
// notebook chunk's output (RStudio.ConsoleDisplay -> .rs.julia.showPlot),
// while text values and display() still reach the console. The image here is
// a type of the test's own that shows itself as a PNG, so no Julia plotting
// package is needed. Skipped where Julia isn't installed.

const FENCE = '```';

// 16x12 PNGs of a single color
const ORANGE_PNG = 'iVBORw0KGgoAAAANSUhEUgAAABAAAAAMCAIAAADkharWAAAAFUlEQVR42mP4v5SBJMQwqmFUA3YEAJjCOxD7lRJJAAAAAElFTkSuQmCC';
const TEAL_PNG = 'iVBORw0KGgoAAAANSUhEUgAAABAAAAAMCAIAAADkharWAAAAFElEQVR42mNgaGggDY1qGNWAHQEALzDAAaM45j0AAAAASUVORK5CYII=';

const DEFINE_IMAGE = [
  'using Base64',
  'struct TestImage; png::String; end',
  'Base.show(io::IO, ::MIME"image/png", image::TestImage) = write(io, base64decode(image.png))',
  'Base.show(io::IO, image::TestImage) = print(io, "TestImage as text")',
].join('; ') + ';';

const RMD = heredoc`
  ---
  title: plots
  ---

  ${FENCE}{julia jlplot}
  TestImage("${TEAL_PNG}")
  ${FENCE}
`;

// The current plot's image URL, or null when the pane shows none.
async function plotSource(page: Page): Promise<string | null> {
  if (await page.locator(PLOT_IMAGE_FRAME).count() === 0)
    return null;
  return page.frameLocator(PLOT_IMAGE_FRAME).locator('#img').getAttribute('src', { timeout: 2_000 }).catch(() => null);
}

// Run Julia code, and wait for the Plots pane to show a plot other than the
// one it showed before.
async function juliaPlot(page: Page, code: string): Promise<string> {
  const before = await plotSource(page);
  await clearConsole(page);
  await executeInConsole(page, code, { timeout: 60_000 });
  await expect.poll(async () => {
    const source = await plotSource(page);
    return source !== null && source !== before;
  }, { timeout: 30_000 }).toBe(true);
  return consoleOutput(page);
}

// a chunk's plots, as opposed to its output widget's own buttons
function chunkPlots(page: Page, label: string) {
  return page.locator(`.rstudio_chunk_output_${label} img:not([title])`);
}

test.describe.serial('Julia plots', () => {
  const sandbox = useSuiteSandbox();
  useJulia();

  test.afterAll(async ({ rstudioPage: page }) => {
    await leaveJulia(page);
    await executeInConsole(page, 'graphics.off()');
    await closeAndDeleteSandboxFiles(page, sandbox.dir, ['jlplots.Rmd']);
  });

  test('images go to the Plots pane, text to the console', async ({ rstudioPage: page }) => {
    test.setTimeout(JULIA_START_TIMEOUT + 180_000);

    await executeInConsole(page, '.rs.julia.repl()', { timeout: JULIA_START_TIMEOUT });
    expect(await getConsolePromptText(page)).toBe(JULIA_PROMPT);
    await executeInConsole(page, DEFINE_IMAGE);

    // an image, as the value of the input: drawn, not printed
    const output = await juliaPlot(page, `TestImage("${ORANGE_PNG}")`);
    expect(output).not.toContain('TestImage as text');

    // display() from inside a loop shows each image
    await juliaPlot(page, `for png in ("${TEAL_PNG}", "${ORANGE_PNG}"); display(TestImage(png)); end`);

    // text values, and text passed to display(), still reach the console
    expect(await runInConsole(page, '1 + 1')).toMatch(/julia> 1 \+ 1\s+2/);
    expect(await runInConsole(page, 'display("shown by display()")')).toContain('"shown by display()"');
  });

  test("a chunk's image goes in its output", async ({ rstudioPage: page }) => {
    test.setTimeout(180_000);
    await leaveJulia(page);

    // a device the console left open would take the chunk's plot
    await executeInConsole(page, 'graphics.off()');

    const consoleActions = new ConsolePaneActions(page);
    const sourceActions = new SourcePaneActions(page, consoleActions);
    await consoleActions.resetSourcePane();
    await writeAndOpenFile(page, sandbox.dir, 'jlplots.Rmd', RMD);

    await sourceActions.navigateToChunkByLabel('jlplot');
    await executeCommand(page, 'executeCurrentChunk');
    await expect(chunkPlots(page, 'jlplot').first()).toBeVisible({ timeout: 120_000 });
  });
});
