import { test, expect } from '@fixtures/rstudio.fixture';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { useSuiteSandbox } from '@utils/sandbox';
import { writeAndOpenFile, closeAndDeleteSandboxFiles } from '@utils/files';
import { executeCommand } from '@utils/commands';
import { heredoc } from '@utils/heredoc';
import type { AceEditorElement } from '@utils/ace';
import type { ElementHandle, Page } from '@playwright/test';

// https://github.com/rstudio/rstudio/issues/2129
//
// Chunk execution in an R Markdown notebook must follow the focused editor
// view: Run Line inside a chunk goes through the notebook queue (not the
// console), and Run Current Chunk resolves the chunk under the focused
// view's cursor, not the primary view's.
test.describe('Split editor with R Markdown chunks', () => {
  const sandbox = useSuiteSandbox();
  let consoleActions: ConsolePaneActions;

  const FILE = 'split_editor_rmd.Rmd';

  // The chunk fences are interpolated because heredoc reads template parts
  // raw -- an escaped backtick would land in the file as a literal
  // backslash-backtick.
  const FENCE = '```';
  const CONTENT = heredoc`
    ---
    title: split editor rmd
    ---

    ${FENCE}{r chunk-one}
    chunk_one <- 101
    ${FENCE}

    ${FENCE}{r chunk-two}
    chunk_two <- 202
    ${FENCE}
  `;

  const CHUNK_ONE_LINE = 6;  // 1-based line of 'chunk_one <- 101'
  const CHUNK_TWO_LINE = 10; // 1-based line of 'chunk_two <- 202'

  // The active tab's editor views. Hidden tabs' editors fail the :visible
  // filter, and the split view carries the rstudio_editor_split_view class.
  // Element handles (rather than text-filtered locators) because Ace only
  // renders visible rows: a locator filtered on document text stops matching
  // a view as soon as it scrolls the probed line offscreen.
  async function docViewHandles(page: Page): Promise<ElementHandle[]> {
    return await page.$$("[class*='rstudio_source_panel'] .ace_editor:visible");
  }

  function viewEditor(handle: ElementHandle) {
    return {
      focusAndGoto: (line: number) =>
        handle.evaluate((el, ln) => {
          const editor = (el as AceEditorElement).env?.editor;
          editor?.focus();
          editor?.gotoLine(ln, 0, false);
        }, line),
      hasFocus: () => handle.evaluate((el) => el.contains(document.activeElement)),
      // Non-null entries in the session's sparse lineWidgets array. Chunk
      // toolbars and chunk output widgets are Ace line widgets, so a chunk
      // executed through the notebook queue adds one to the hosting view,
      // while console execution adds none.
      lineWidgetCount: () =>
        handle.evaluate((el) => {
          const session = (el as AceEditorElement).env?.editor?.session as unknown as {
            lineWidgets?: unknown[];
          };
          if (!session?.lineWidgets)
            return 0;
          return session.lineWidgets.filter((w) => w != null).length;
        }),
    };
  }

  async function openAndSplit(page: Page) {
    await writeAndOpenFile(page, sandbox.dir, FILE, CONTENT);
    await expect.poll(async () => (await docViewHandles(page)).length).toBe(1);

    await executeCommand(page, 'splitEditorDown');
    await expect.poll(async () => (await docViewHandles(page)).length).toBe(2);

    const handles = await docViewHandles(page);
    const isSplit = await Promise.all(handles.map((handle) =>
      handle.evaluate((el) => el.classList.contains('rstudio_editor_split_view'))));
    expect(isSplit.filter(Boolean)).toHaveLength(1);

    const primary = viewEditor(handles[isSplit.indexOf(false)]);
    const split = viewEditor(handles[isSplit.indexOf(true)]);

    // Primary cursor in chunk-one; split view keeps focus in chunk-two, so
    // a run that reads the wrong view is caught by which chunk executes.
    await primary.focusAndGoto(CHUNK_ONE_LINE);
    await split.focusAndGoto(CHUNK_TWO_LINE);
    await expect.poll(() => split.hasFocus()).toBe(true);

    await consoleActions.evalRLogical(
      '{ rm(list = intersect(c("chunk_one", "chunk_two"), ls(globalenv())), envir = globalenv()); TRUE }');

    return { primary, split };
  }

  // Waits for either chunk variable, then reports which chunk ran, so a
  // misroute fails with a readable message instead of a poll timeout.
  async function pollWhichRan(): Promise<string> {
    await expect
      .poll(() => consoleActions.evalRLogical('exists("chunk_one") || exists("chunk_two")'),
            { timeout: 30000 })
      .toBe(true);
    const one = await consoleActions.evalRLogical('exists("chunk_one")');
    const two = await consoleActions.evalRLogical('exists("chunk_two")');
    return `chunk_one=${one} chunk_two=${two}`;
  }

  test.beforeAll(async ({ rstudioPage: page }) => {
    consoleActions = new ConsolePaneActions(page);
    await consoleActions.resetSourcePane();
  });

  test.afterEach(async ({ rstudioPage: page }) => {
    await executeCommand(page, 'removeEditorSplit');
    await closeAndDeleteSandboxFiles(page, sandbox.dir, [FILE]);
  });

  test('Run Line from the split view executes its chunk inline', async ({ rstudioPage: page }) => {
    const { primary, split } = await openAndSplit(page);
    const widgetsBefore = await primary.lineWidgetCount();

    // Run Line: must execute the split view's line, through the notebook
    // queue rather than the console.
    await page.keyboard.press('Control+Enter');

    expect(await pollWhichRan()).toBe('chunk_one=false chunk_two=true');

    // Inline execution creates a chunk output widget. It renders in the
    // primary view only for now: chunk output line widgets live on the
    // primary Ace session. When output fan-out to all views lands, the
    // split view expectation below should change to match.
    await expect.poll(() => primary.lineWidgetCount()).toBe(widgetsBefore + 1);
    expect(await split.lineWidgetCount()).toBe(0);
  });

  test('Run Current Chunk runs the chunk under the focused view cursor', async ({ rstudioPage: page }) => {
    const { primary } = await openAndSplit(page);
    const widgetsBefore = await primary.lineWidgetCount();

    await executeCommand(page, 'executeCurrentChunk');

    expect(await pollWhichRan()).toBe('chunk_one=false chunk_two=true');
    await expect.poll(() => primary.lineWidgetCount()).toBe(widgetsBefore + 1);
  });
});
