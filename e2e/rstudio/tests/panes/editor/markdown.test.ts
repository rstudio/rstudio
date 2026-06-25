import { test, expect } from '@fixtures/rstudio.fixture';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { SourcePaneActions } from '@actions/source_pane.actions';
import { installDepIfPrompted } from '@pages/modals.page';
import {
  VIEWER_TAB, VIEWER_FRAME, PUBLISH_BTN_IN_PANEL, CONTAINER,
  switchToViewerFrame,
} from '@pages/viewer_pane.page';
import { useSuiteSandbox } from '@utils/sandbox';
import { executeCommand } from '@utils/commands';

test.describe('Markdown', () => {
  // Sets cwd to a per-spec sandbox; the relative .md path used by
  // createAndOpenFile and closeSourceAndDeleteFile lands there.
  useSuiteSandbox();
  let consoleActions: ConsolePaneActions;
  let sourceActions: SourcePaneActions;
  let missingPackages: string[] = [];

  test.beforeAll(async ({ rstudioPage: page }) => {
    // A cold-cache install can outrun the 120s global timeout; give this hook
    // the headroom its ensurePackages() call needs.
    test.setTimeout(300000);
    consoleActions = new ConsolePaneActions(page);
    sourceActions = new SourcePaneActions(page, consoleActions);

    await consoleActions.resetSourcePane();

    // Only the HTML-preview test needs rmarkdown; it skips itself if the
    // install fails. 180s covers the transitive install on a cold CI runner.
    missingPackages = await consoleActions.ensurePackages(['rmarkdown'], 180_000);

    await consoleActions.clearConsole();
  });

  test('create new markdown file', async ({ rstudioPage: page }) => {
    // Original: test_desktop_Markdown.py::test_creating_new_markdown_file
    await executeCommand(page, 'newMarkdownDoc');

    // The suite keeps an Untitled placeholder tab open, so the new doc may be
    // Untitled2 rather than Untitled1 -- assert the stable parts (it's an
    // Untitled doc, and its file type is Markdown).
    await expect(sourceActions.sourcePane.selectedTab).toContainText('Untitled', { timeout: 10000 });
    await expect(sourceActions.sourcePane.footerTable).toContainText('Markdown');

    await consoleActions.resetSourcePane();
  });

  test('preview markdown file as HTML', async ({ rstudioPage: page }) => {
    // Original: test_desktop_Markdown.py::test_preview_markdown_file_as_html
    test.skip(missingPackages.length > 0, `required R package(s) not available: ${missingPackages.join(', ')}`);

    const fileName = `markdown_doc_${Date.now()}.md`;

    // Exercises heading + italic + bold + list so the rendered preview text
    // (asserted below) reflects real markdown structure.
    const markdownContent = [
      '#### Double, double toil and trouble;',
      '',
      '*Fire* burn, and **cauldron** bubble.',
      '',
      '- Fillet of a fenny snake',
      '- In the cauldron boil and bake.',
    ].join('\n');

    await sourceActions.createAndOpenFile(fileName, markdownContent);
    await expect(sourceActions.sourcePane.selectedTab).toContainText(fileName, { timeout: 20000 });
    await expect(sourceActions.sourcePane.publishBtn).toBeVisible({ timeout: 10000 });

    // Set preview to Viewer pane, then preview the .md as HTML.
    await sourceActions.sourcePane.formatOptions.click();
    await sourceActions.sourcePane.viewerPaneOption.click();
    await executeCommand(page, 'previewHTML');
    await installDepIfPrompted(page);

    await expect(sourceActions.sourcePane.footerTable).toContainText('Markdown');

    // Viewer pane shows the rendered HTML.
    await expect(page.locator(VIEWER_TAB)).toBeVisible({ timeout: 30000 });
    await expect(page.locator(VIEWER_FRAME)).toBeVisible({ timeout: 30000 });
    await expect(page.locator(PUBLISH_BTN_IN_PANEL)).toBeVisible({ timeout: 10000 });

    // Verify the rendered markdown text inside the viewer iframe. The
    // formatting markers (####, *, **, -) are stripped in the rendered output.
    const viewerFrame = switchToViewerFrame(page);
    const container = viewerFrame.locator(CONTAINER).first();
    await expect(container).toContainText('Double, double toil and trouble;', { timeout: 30000 });
    await expect(container).toContainText('Fire burn, and cauldron bubble.');
    await expect(container).toContainText('Fillet of a fenny snake');
    await expect(container).toContainText('In the cauldron boil and bake.');

    // Cleanup: close the tab and delete the .md. previewHTML renders to a
    // session tempfile, so there's no sibling .html next to the source to remove.
    await sourceActions.closeSourceAndDeleteFile(fileName);
  });
});
