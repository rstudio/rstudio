import { test, expect } from '@fixtures/rstudio.fixture';
import { sleep } from '@utils/constants';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { SourcePaneActions } from '@actions/source_pane.actions';
import { installDepIfPrompted, clickConfirmIfVisible, RMARKDOWN_MODAL, TEMPLATE_OPTION, TEMPLATE_LIST, CONFIRM_BTN, CANCEL_BTN } from '@pages/modals.page';
import {
  VIEWER_TAB, VIEWER_FRAME, PUBLISH_BTN_IN_PANEL, CONTAINER,
  switchToViewerFrame,
} from '@pages/viewer_pane.page';
import { clearWorkspace } from '@pages/environment_pane.page';
import { CONSOLE_INPUT } from '@pages/console_pane.page';
import { useSuiteSandbox } from '@utils/sandbox';

test.describe('RMarkdown', () => {
  // Sets cwd to a per-spec sandbox; all relative file paths used by
  // createAndOpenFile / closeSourceAndDeleteFile / file.edit / unlink land there.
  useSuiteSandbox();
  let consoleActions: ConsolePaneActions;
  let sourceActions: SourcePaneActions;

  test.beforeAll(async ({ rstudioPage: page }) => {
    consoleActions = new ConsolePaneActions(page);
    sourceActions = new SourcePaneActions(page, consoleActions);

    await consoleActions.closeAllBuffersWithoutSaving();

    await consoleActions.ensurePackages(['rmarkdown', 'remotes']);

    await consoleActions.clearConsole();
  });

  test('create new rmarkdown and knit to HTML', async ({ rstudioPage: page }) => {
    // Original: test_desktop_RMarkdown.py::test_creating_new_rmarkdown_and_knitting
    const fileName = `rmarkdown_newandknit_${Date.now()}.rmd`;

    await sourceActions.createAndOpenFile(fileName, 'Is this a dagger which I see before me?');
    await expect(sourceActions.sourcePane.selectedTab).toContainText(fileName, { timeout: 20000 });
    await expect(sourceActions.sourcePane.publishBtn).toBeVisible({ timeout: 10000 });

    // Set preview to Viewer pane
    await sourceActions.sourcePane.formatOptions.click();
    await sleep(500);
    await sourceActions.sourcePane.viewerPaneOption.click();
    await sleep(500);

    // Knit to HTML
    await sourceActions.sourcePane.knitOptions.click();
    await sleep(500);
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
    await consoleActions.typeInConsole(`unlink("${fileName.replace('.rmd', '.html')}")`);
    await sleep(500);
  });

  test('empty chunk does not define variables', async ({ rstudioPage: page }) => {
    // Original: test_desktop_RMarkdown.py::test_empty_chunk_does_not_define_variables

    // Clear workspace
    await consoleActions.typeInConsole(".rs.api.executeCommand('activateEnvironment')");
    await sleep(2000);
    await clearWorkspace(page);

    // Create an rmd file with a chunk
    const fileName = `rmarkdown_novariables_${Date.now()}.rmd`;
    await sourceActions.createAndOpenFile(fileName, "```{r}\\nprint('full of sound and fury, signifying nothing')\\n```");
    await expect(sourceActions.sourcePane.selectedTab).toContainText(fileName, { timeout: 20000 });

    // Save the file
    await consoleActions.typeInConsole(".rs.api.executeCommand('saveSourceDoc')");
    await sleep(1000);

    // Run all chunks via restart R
    await consoleActions.typeInConsole(".rs.api.executeCommand('restartRRunAllChunks')");
    await sleep(5000);
    await expect(page.locator("[id^='rstudio_tb_interruptr']")).not.toBeVisible({ timeout: 30000 });

    // Wait for R to be ready after restart
    await page.waitForSelector(CONSOLE_INPUT, { state: 'visible', timeout: 15000 });

    // Verify output
    const consoleOutput = consoleActions.consolePane.consoleOutput;
    await expect(consoleOutput).toContainText('full of sound and fury, signifying nothing', { timeout: 20000 });

    // Verify no variables in global env
    await consoleActions.clearConsole();
    await consoleActions.typeInConsole('ls(envir = globalenv())');
    await sleep(2000);
    await expect(consoleOutput).toContainText('character(0)', { timeout: 10000 });

    // Cleanup
    await sourceActions.closeSourceAndDeleteFile(fileName);
  });

  test('continue comment newline preference', async ({ rstudioPage: page }) => {
    // Original: test_desktop_RMarkdown.py::test_continue_comment_newline_preference

    // Enable the preference
    await consoleActions.typeInConsole('.rs.api.writeRStudioPreference("continue_comments_on_newline", TRUE)');
    await sleep(1000);

    // Create an RMarkdown file, verify no comment is inserted on newline
    const rmdFileName = `rmarkdown_commentnewline_${Date.now()}.rmd`;
    await consoleActions.typeInConsole(`file.create("${rmdFileName}")`);
    await sleep(1000);
    await consoleActions.typeInConsole(`file.edit("${rmdFileName}")`);
    await expect(sourceActions.sourcePane.selectedTab).toContainText(rmdFileName, { timeout: 20000 });

    await sourceActions.sendText('# Section');
    await sleep(1000);
    await page.keyboard.press('Enter');
    await sleep(1000);
    await expect(sourceActions.sourcePane.contentPane).toHaveText('# Section');

    await consoleActions.closeAllBuffersWithoutSaving();
    await consoleActions.typeInConsole(`unlink("${rmdFileName}")`);
    await sleep(500);

    // Check that an R file DOES insert a comment on newline
    const rFileName = `rmarkdown_commentnewline_${Date.now()}.r`;
    await consoleActions.typeInConsole(`file.create("${rFileName}")`);
    await sleep(1000);
    await consoleActions.typeInConsole(`file.edit("${rFileName}")`);
    await expect(sourceActions.sourcePane.selectedTab).toContainText(rFileName, { timeout: 20000 });

    await sourceActions.sendText('# This is a Comment');
    await sleep(1000);
    await page.keyboard.press('Enter');
    await sleep(1000);
    const editorText = await sourceActions.sourcePane.contentPane.innerText();
    expect(editorText).toContain('# This is a Comment');
    expect(editorText).toMatch(/# This is a Comment[\s\S]*# /);

    // Cleanup
    await sourceActions.closeSourceAndDeleteFile(rFileName);
  });

  test('spellcheck in source mode', async ({ rstudioPage: page }) => {
    // Original: test_desktop_RMarkdown.py::test_rmd_spellcheck_in_editor_and_visual_mode (part 1)
    const fileName = `rmarkdown_spelling_src_${Date.now()}.Rmd`;

    const rmarkdownSource = '---\\ntitle: RMarkdown Spelling\\noutput: html_document\\neditor:\\n  mode: source\\n---\\n\\nThis is missssssspelled.\\nThis is spelled correctly.';
    await sourceActions.createAndOpenFile(fileName, rmarkdownSource);
    await expect(sourceActions.sourcePane.publishBtn).toBeVisible({ timeout: 10000 });

    // Navigate to file and run spellcheck
    await consoleActions.typeInConsole(`rstudioapi::navigateToFile("${fileName}", line=1L)`);
    await sleep(2000);
    await consoleActions.typeInConsole(".rs.api.executeCommand('checkSpelling')");
    await sleep(5000);

    await expect(page.locator('#rstudio_spelling_not_in_dict')).toBeVisible({ timeout: 30000 });
    await expect(page.locator('#rstudio_spelling_not_in_dict')).toHaveValue('missssssspelled');
    await expect(page.locator('select[aria-label="Suggestions"] option[value="misspelling"]')).toBeVisible({ timeout: 30000 });
    await page.locator(CANCEL_BTN).click();
    await sleep(1000);

    // Cleanup
    await sourceActions.closeSourceAndDeleteFile(fileName);
  });

  test('spellcheck in visual mode', async ({ rstudioPage: page }) => {
    // Original: test_desktop_RMarkdown.py::test_rmd_spellcheck_in_editor_and_visual_mode (part 2)
    const fileName = `rmarkdown_spelling_vis_${Date.now()}.Rmd`;

    const rmarkdownSource = '---\\ntitle: RMarkdown Spelling\\noutput: html_document\\neditor:\\n  mode: source\\n---\\n\\nThis is missssssspelled.\\nThis is spelled correctly.';
    await sourceActions.createAndOpenFile(fileName, rmarkdownSource);
    await expect(sourceActions.sourcePane.publishBtn).toBeVisible({ timeout: 10000 });

    // Switch to visual mode
    await consoleActions.typeInConsole(`rstudioapi::navigateToFile("${fileName}", line=1L)`);
    await sleep(1000);
    await sourceActions.ensureVisualMode();

    // Run spellcheck in visual mode
    await consoleActions.typeInConsole(".rs.api.executeCommand('checkSpelling')");
    await sleep(5000);

    await expect(page.locator('#rstudio_spelling_not_in_dict')).toBeVisible({ timeout: 30000 });
    await expect(page.locator('#rstudio_spelling_not_in_dict')).toHaveValue('missssssspelled');
    await expect(page.locator('select[aria-label="Suggestions"] option[value="misspelling"]')).toBeVisible({ timeout: 30000 });
    await page.locator(CANCEL_BTN).click();
    await sleep(1000);

    // Cleanup
    await sourceActions.closeSourceAndDeleteFile(fileName);
  });

  test('rmd templates display', async ({ rstudioPage: page }) => {
    // Original: test_desktop_RMarkdown.py::test_rmd_templates_displays

    // Open New R Markdown dialog
    await consoleActions.typeInConsole(".rs.api.executeCommand('newRMarkdownDoc')");
    await installDepIfPrompted(page);

    // Check templates
    const rmdModal = page.locator(RMARKDOWN_MODAL);
    await expect(rmdModal).toBeVisible({ timeout: 10000 });
    await page.locator(`xpath=${TEMPLATE_OPTION}`).click();
    await sleep(1000);

    const templateList = page.locator(TEMPLATE_LIST);
    await expect(templateList).toBeVisible({ timeout: 5000 });
    await expect(templateList).toContainText('Custom theming');

    // Confirm to create the file
    await page.locator(CONFIRM_BTN).click();
    await sleep(2000);
    await sourceActions.ensureVisualMode();
    await expect(page.locator('.ProseMirror')).toContainText('Theming with bslib and thematic', { timeout: 10000 });

    // Cleanup
    await consoleActions.closeAllBuffersWithoutSaving();
  });

  test.skip('visual mode go to next and prev chunk', async ({ rstudioPage: page }) => {
    // Skipped: https://github.com/rstudio/rstudio/issues/13271

    // Create rmarkdown file via command
    await consoleActions.typeInConsole(".rs.api.executeCommand('newRMarkdownDoc')");
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
    await sleep(3000);

    // Go to next chunk 3 times to reach plot(pressure) chunk
    await consoleActions.typeInConsole(".rs.api.executeCommand('goToNextChunk')");
    await sleep(2000);
    await consoleActions.typeInConsole(".rs.api.executeCommand('goToNextChunk')");
    await sleep(2000);
    await consoleActions.typeInConsole(".rs.api.executeCommand('goToNextChunk')");
    await sleep(2000);
    await consoleActions.typeInConsole(".rs.api.executeCommand('executeCurrentChunk')");
    await expect(consoleActions.consolePane.consoleOutput).toContainText('plot(pressure)', { timeout: 10000 });

    // Go to previous chunk and verify summary(cars) runs but NOT plot(pressure)
    await consoleActions.clearConsole();
    await consoleActions.typeInConsole(".rs.api.executeCommand('goToPrevChunk')");
    await sleep(2000);
    await consoleActions.typeInConsole(".rs.api.executeCommand('executeCurrentChunk')");
    const consoleOutput = consoleActions.consolePane.consoleOutput;
    await expect(consoleOutput).toContainText('summary(cars)', { timeout: 10000 });
    await expect(consoleOutput).toContainText('speed');
    await expect(consoleOutput).toContainText('dist');
    await expect(consoleOutput).not.toContainText('plot(pressure)');

    // Cleanup
    await consoleActions.closeAllBuffersWithoutSaving();
  });
});
