import { test, expect } from '@fixtures/rstudio.fixture';
import { AceEditor } from '@pages/ace_editor.page';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { SourcePaneActions } from '@actions/source_pane.actions';
import { installDepIfPrompted, clickConfirmIfVisible, RMARKDOWN_MODAL, TEMPLATE_OPTION, TEMPLATE_LIST, CONFIRM_BTN, CANCEL_BTN } from '@pages/modals.page';
import {
  VIEWER_TAB, VIEWER_FRAME, PUBLISH_BTN_IN_PANEL, CONTAINER,
  switchToViewerFrame,
} from '@pages/viewer_pane.page';
import { clearWorkspace } from '@pages/environment_pane.page';
import { useSuiteSandbox } from '@utils/sandbox';
import { executeCommand, saveDocument, setPref } from '@utils/commands';

test.describe('RMarkdown', () => {
  // Sets cwd to a per-spec sandbox; all relative file paths used by
  // createAndOpenFile / closeSourceAndDeleteFile / file.edit / unlink land there.
  useSuiteSandbox();
  let consoleActions: ConsolePaneActions;
  let sourceActions: SourcePaneActions;
  let missingPackages: string[] = [];

  test.beforeAll(async ({ rstudioPage: page }) => {
    consoleActions = new ConsolePaneActions(page);
    sourceActions = new SourcePaneActions(page, consoleActions);

    await consoleActions.resetSourcePane();

    // Bumped from the default 60s -- on slow CI runners rmarkdown's transitive
    // install (knitr, markdown, etc.) regularly outlasts a minute. The previous
    // call discarded the return value; if install timed out, every test below
    // would then fail with an opaque knit/template/spellcheck timeout 60+
    // seconds later. Capture failures and surface them as a clear test.skip
    // in each test so the cause is visible in one place.
    // thematic and bslib back the "Theming with bslib and thematic" RMarkdown
    // template the rmd-templates test selects. Without them, opening the
    // template surfaces a non-modal "Package thematic required" notification
    // banner that blocks panmirror from mounting -- ensureVisualMode then
    // times out waiting for .ProseMirror. installDepIfPrompted only handles
    // modal install dialogs, not the inline banner, so the cleanest fix is
    // to pre-install the packages here.
    missingPackages = await consoleActions.ensurePackages(
      ['rmarkdown', 'remotes', 'thematic', 'bslib'],
      180_000,
    );

    await consoleActions.clearConsole();
  });

  // Every test in this suite drives at least one .rmd path (knit, chunk run,
  // spellcheck, templates) that needs rmarkdown installed. Skipping at the
  // beforeEach level keeps the report honest -- a missing dep shows up as
  // "skipped: rmarkdown not available", not as 4-6 different opaque timeouts.
  test.beforeEach(() => {
    test.skip(
      missingPackages.length > 0,
      `required R package(s) not available: ${missingPackages.join(', ')}`,
    );
  });

  test('create new rmarkdown and knit to HTML', async ({ rstudioPage: page }) => {
    // Original: test_desktop_RMarkdown.py::test_creating_new_rmarkdown_and_knitting
    const fileName = `rmarkdown_newandknit_${Date.now()}.rmd`;

    await sourceActions.createAndOpenFile(fileName, 'Is this a dagger which I see before me?');
    await expect(sourceActions.sourcePane.selectedTab).toContainText(fileName, { timeout: 20000 });
    await expect(sourceActions.sourcePane.publishBtn).toBeVisible({ timeout: 10000 });

    // Set preview to Viewer pane
    await sourceActions.sourcePane.formatOptions.click();
    await sourceActions.sourcePane.viewerPaneOption.click();

    // Knit to HTML
    await sourceActions.sourcePane.knitOptions.click();
    await sourceActions.sourcePane.knitHtml.click();
    await installDepIfPrompted(page);

    // Verify viewer pane shows rendered output
    await expect(page.locator(VIEWER_TAB)).toBeVisible({ timeout: 30000 });
    await expect(page.locator(VIEWER_FRAME)).toBeVisible({ timeout: 30000 });
    await expect(page.locator(PUBLISH_BTN_IN_PANEL)).toBeVisible({ timeout: 10000 });

    // Switch into viewer iframe and verify content
    const viewerFrame = switchToViewerFrame(page);
    const container = viewerFrame.locator(CONTAINER).first();
    await expect(container).toContainText('Is this a dagger which I see before me?', { timeout: 30000 });

    // Cleanup
    await sourceActions.closeSourceAndDeleteFile(fileName);
    await consoleActions.executeInConsole(`unlink("${fileName.replace('.rmd', '.html')}")`);
  });

  test('empty chunk does not define variables', async ({ rstudioPage: page }) => {
    // Original: test_desktop_RMarkdown.py::test_empty_chunk_does_not_define_variables

    // Clear workspace
    await executeCommand(page, 'activateEnvironment');
    await clearWorkspace(page);

    // Create an rmd file with a chunk
    const fileName = `rmarkdown_novariables_${Date.now()}.rmd`;
    await sourceActions.createAndOpenFile(fileName, "```{r}\nprint('full of sound and fury, signifying nothing')\n```");
    await expect(sourceActions.sourcePane.selectedTab).toContainText(fileName, { timeout: 20000 });

    // Save the file
    await saveDocument(page);

    // Clear the console so the chunk's print() output is the only thing in
    // there afterwards -- waiting for that output is the deterministic
    // readiness signal for "R restarted AND ran the chunk", which avoids
    // having to time the restart or watch the interrupt button.
    await consoleActions.clearConsole();

    // Run all chunks via restart R. 60s covers the worst-case restart +
    // chunk run window.
    await executeCommand(page, 'restartRRunAllChunks');
    const consoleOutput = consoleActions.consolePane.consoleOutput;
    await expect(consoleOutput).toContainText(
      'full of sound and fury, signifying nothing',
      { timeout: 60000 },
    );

    // Verify no variables in global env
    await consoleActions.clearConsole();
    await consoleActions.executeInConsole('ls(envir = globalenv())', { wait: true });
    await expect(consoleOutput).toContainText('character(0)', { timeout: 10000 });

    // Cleanup
    await sourceActions.closeSourceAndDeleteFile(fileName);
  });

  test('continue comment newline preference', async ({ rstudioPage: page }) => {
    // Original: test_desktop_RMarkdown.py::test_continue_comment_newline_preference

    // Enable the preference (setPref is a JS-side bridge call, no R round-trip)
    await setPref(page, 'continue_comments_on_newline', true);

    // Create an RMarkdown file, verify no comment is inserted on newline
    const rmdFileName = `rmarkdown_commentnewline_${Date.now()}.rmd`;
    await consoleActions.executeInConsole(`file.create("${rmdFileName}")`, { wait: true });
    await consoleActions.executeInConsole(`file.edit("${rmdFileName}")`);
    await expect(sourceActions.sourcePane.selectedTab).toContainText(rmdFileName, { timeout: 20000 });

    await sourceActions.sendText('# Section');
    await page.keyboard.press('Enter');
    await expect(sourceActions.sourcePane.contentPane).toHaveText('# Section');

    await consoleActions.resetSourcePane();
    await consoleActions.executeInConsole(`unlink("${rmdFileName}")`, { wait: true });

    // Check that an R file DOES insert a comment on newline
    const rFileName = `rmarkdown_commentnewline_${Date.now()}.r`;
    await consoleActions.executeInConsole(`file.create("${rFileName}")`, { wait: true });
    await consoleActions.executeInConsole(`file.edit("${rFileName}")`);
    await expect(sourceActions.sourcePane.selectedTab).toContainText(rFileName, { timeout: 20000 });

    await sourceActions.sendText('# This is a Comment');
    await page.keyboard.press('Enter');
    // Ace inserts a "# " continuation after the newline when the
    // continue_comments_on_newline pref is set. innerText collapses the
    // newline, so the polled text reads as "# This is a Comment# ".
    await expect(sourceActions.sourcePane.contentPane).toContainText(/# This is a Comment.*# /);

    // Cleanup
    await sourceActions.closeSourceAndDeleteFile(rFileName);
  });

  test('spellcheck in source mode', async ({ rstudioPage: page }) => {
    // Original: test_desktop_RMarkdown.py::test_rmd_spellcheck_in_editor_and_visual_mode (part 1)
    const fileName = `rmarkdown_spelling_src_${Date.now()}.Rmd`;

    const rmarkdownSource = '---\ntitle: RMarkdown Spelling\noutput: html_document\neditor:\n  mode: source\n---\n\nThis is missssssspelled.\nThis is spelled correctly.';
    await sourceActions.createAndOpenFile(fileName, rmarkdownSource);
    await expect(sourceActions.sourcePane.publishBtn).toBeVisible({ timeout: 10000 });

    // Navigate to file and run spellcheck. The toBeVisible/toHaveValue below
    // already poll, so the prior sleeps after navigate/checkSpelling were
    // redundant slack.
    await consoleActions.executeInConsole(`rstudioapi::navigateToFile("${fileName}", line=1L)`, { wait: true });
    await executeCommand(page, 'checkSpelling');

    await expect(page.locator('#rstudio_spelling_not_in_dict')).toBeVisible({ timeout: 30000 });
    await expect(page.locator('#rstudio_spelling_not_in_dict')).toHaveValue('missssssspelled');
    await expect(page.locator('select[aria-label="Suggestions"] option[value="misspelling"]')).toBeVisible({ timeout: 30000 });
    await page.locator(CANCEL_BTN).click();

    // Cleanup
    await sourceActions.closeSourceAndDeleteFile(fileName);
  });

  test('spellcheck in visual mode', async ({ rstudioPage: page }) => {
    // Original: test_desktop_RMarkdown.py::test_rmd_spellcheck_in_editor_and_visual_mode (part 2)
    const fileName = `rmarkdown_spelling_vis_${Date.now()}.Rmd`;

    const rmarkdownSource = '---\ntitle: RMarkdown Spelling\noutput: html_document\neditor:\n  mode: source\n---\n\nThis is missssssspelled.\nThis is spelled correctly.';
    await sourceActions.createAndOpenFile(fileName, rmarkdownSource);
    await expect(sourceActions.sourcePane.publishBtn).toBeVisible({ timeout: 10000 });

    // Switch to visual mode
    await consoleActions.executeInConsole(`rstudioapi::navigateToFile("${fileName}", line=1L)`, { wait: true });
    await sourceActions.ensureVisualMode();

    // Run spellcheck in visual mode
    await executeCommand(page, 'checkSpelling');

    await expect(page.locator('#rstudio_spelling_not_in_dict')).toBeVisible({ timeout: 30000 });
    await expect(page.locator('#rstudio_spelling_not_in_dict')).toHaveValue('missssssspelled');
    await expect(page.locator('select[aria-label="Suggestions"] option[value="misspelling"]')).toBeVisible({ timeout: 30000 });
    await page.locator(CANCEL_BTN).click();

    // Cleanup
    await sourceActions.closeSourceAndDeleteFile(fileName);
  });

  test('rmd templates display', async ({ rstudioPage: page }) => {
    // Original: test_desktop_RMarkdown.py::test_rmd_templates_displays

    // Open New R Markdown dialog
    await executeCommand(page, 'newRMarkdownDoc');
    await installDepIfPrompted(page);

    // Check templates
    const rmdModal = page.locator(RMARKDOWN_MODAL);
    await expect(rmdModal).toBeVisible({ timeout: 10000 });
    await page.locator(`xpath=${TEMPLATE_OPTION}`).click();

    const templateList = page.locator(TEMPLATE_LIST);
    await expect(templateList).toBeVisible({ timeout: 5000 });
    await expect(templateList).toContainText('Custom theming');

    // Confirm to create the file
    await page.locator(CONFIRM_BTN).click();
    await sourceActions.ensureVisualMode();
    await expect(page.locator('.ProseMirror')).toContainText('Theming with bslib and thematic', { timeout: 10000 });

    // Cleanup
    await consoleActions.resetSourcePane();
  });

  test.skip('visual mode go to next and prev chunk', async ({ rstudioPage: page }) => {
    // Skipped: https://github.com/rstudio/rstudio/issues/13271

    // Create rmarkdown file via command
    await executeCommand(page, 'newRMarkdownDoc');
    await installDepIfPrompted(page);
    await clickConfirmIfVisible(page);
    await expect(sourceActions.sourcePane.selectedTab).toContainText('Untitled', { timeout: 10000 });
    await expect(sourceActions.sourcePane.footerTable).toContainText('R Markdown');

    // Switch to visual mode
    await sourceActions.sourcePane.visualMdToggle.click();
    await clickConfirmIfVisible(page, 5000);

    // Wait for visual toolbar
    await expect(sourceActions.sourcePane.secondaryToolbar).toBeVisible({ timeout: 10000 });
    await consoleActions.clearConsole();

    // Navigate from chunk-to-chunk via goToNextChunk. The command schedules
    // the cursor move asynchronously, so poll the cursor row after each call
    // to make sure we've actually advanced before the next navigation fires.
    const editor = new AceEditor(page, '');
    async function advanceChunk(direction: 'next' | 'prev'): Promise<void> {
      const before = (await editor.getCursorPosition()).row;
      await executeCommand(page, direction === 'next' ? 'goToNextChunk' : 'goToPrevChunk');
      await expect
        .poll(async () => (await editor.getCursorPosition()).row, { timeout: 5000 })
        .not.toBe(before);
    }

    // Go to next chunk 3 times to reach plot(pressure) chunk
    await advanceChunk('next');
    await advanceChunk('next');
    await advanceChunk('next');
    await executeCommand(page, 'executeCurrentChunk');
    await expect(consoleActions.consolePane.consoleOutput).toContainText('plot(pressure)', { timeout: 10000 });

    // Go to previous chunk and verify summary(cars) runs but NOT plot(pressure)
    await consoleActions.clearConsole();
    await advanceChunk('prev');
    await executeCommand(page, 'executeCurrentChunk');
    const consoleOutput = consoleActions.consolePane.consoleOutput;
    await expect(consoleOutput).toContainText('summary(cars)', { timeout: 10000 });
    await expect(consoleOutput).toContainText('speed');
    await expect(consoleOutput).toContainText('dist');
    await expect(consoleOutput).not.toContainText('plot(pressure)');

    // Cleanup
    await consoleActions.resetSourcePane();
  });
});
