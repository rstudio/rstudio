import { test, expect } from '@fixtures/rstudio.fixture';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { SourcePaneActions } from '@actions/source_pane.actions';
import { installDepIfPrompted } from '@pages/modals.page';
import {
  VIEWER_TAB, VIEWER_FRAME, PUBLISH_BTN_IN_PANEL, CONTAINER_IMG, MAIN_CONTAINER,
  switchToViewerFrame,
} from '@pages/viewer_pane.page';
import { useSuiteSandbox } from '@utils/sandbox';
import { executeCommand, saveDocument } from '@utils/commands';
import type { Page, Locator } from 'playwright';

/**
 * Click the notebook Preview button until `ready` succeeds. Preview is known to
 * occasionally do nothing on the first click (no render kicks off and no
 * .nb.html is written), so a single click isn't reliable -- retry until the
 * expected viewer state appears.
 */
async function previewUntil(
  page: Page,
  previewBtn: Locator,
  ready: () => Promise<void>,
  attempts = 3,
): Promise<void> {
  let lastError: unknown;
  for (let attempt = 1; attempt <= attempts; attempt++) {
    await previewBtn.click();
    await installDepIfPrompted(page, 2000);
    try {
      await ready();
      return;
    } catch (error) {
      lastError = error;
      console.log(`Preview attempt ${attempt} produced no viewer output; retrying.`);
    }
  }
  throw lastError;
}

test.describe('R Notebook', () => {
  // Sets cwd to a per-spec sandbox; relative paths used by createAndOpenFile
  // and closeSourceAndDeleteFile land there.
  useSuiteSandbox();
  let consoleActions: ConsolePaneActions;
  let sourceActions: SourcePaneActions;
  let missingPackages: string[] = [];

  test.beforeAll(async ({ rstudioPage: page }) => {
    consoleActions = new ConsolePaneActions(page);
    sourceActions = new SourcePaneActions(page, consoleActions);

    await consoleActions.resetSourcePane();

    // 180s: rmarkdown's transitive install (knitr, markdown, etc.) regularly
    // outlasts a minute on a cold CI runner. Capturing failures here surfaces a
    // missing dep as a clean skip below rather than an opaque preview timeout.
    missingPackages = await consoleActions.ensurePackages(['rmarkdown'], 180_000);

    await consoleActions.clearConsole();
  });

  // The single test below drives a notebook preview that needs rmarkdown
  // installed; skipping at this level keeps the report honest.
  test.beforeEach(() => {
    test.skip(
      missingPackages.length > 0,
      `required R package(s) not available: ${missingPackages.join(', ')}`,
    );
  });

  test('create new R Notebook and preview', async ({ rstudioPage: page }) => {
    // Original: test_desktop_RNotebook.py::test_creating_new_r_notebook_and_preview
    const fileName = `rnotebook_test_${Date.now()}.Rmd`;

    // output: html_notebook is what makes this a Notebook (Preview, not Knit).
    const notebookContent = [
      '---',
      'title: "R Notebook"',
      'output: html_notebook',
      '---',
      '',
      'Friends, Romans, countrymen, lend me your ears; I come to bury Caesar, not to praise him.',
      '',
      '```{r}',
      'plot(cars)',
      '```',
    ].join('\n');

    await sourceActions.createAndOpenFile(fileName, notebookContent);
    await expect(sourceActions.sourcePane.selectedTab).toContainText(fileName, { timeout: 20000 });
    await expect(sourceActions.sourcePane.publishBtn).toBeVisible({ timeout: 10000 });
    await expect(sourceActions.sourcePane.footerTable).toContainText('R Markdown');

    // Notebooks render inline chunk output in source mode; ensure we're there so
    // the executed chunk's plot appears in the editor (verified below).
    await sourceActions.ensureSourceMode();

    // Preview displays the notebook's .nb.html sidecar, which is written by the
    // editor's save hook -- not by the writeLines that created the file on disk.
    // So the file opens clean, no .nb.html exists, and Preview has nothing to
    // show. Dirty the doc and save through the editor to generate the sidecar.
    await sourceActions.goToEnd();
    await page.keyboard.press('Enter');
    await saveDocument(page);

    // Set preview to Viewer pane, then preview (retrying the click -- the first
    // preview of a freshly opened notebook often does nothing).
    await sourceActions.sourcePane.formatOptions.click();
    await sourceActions.sourcePane.viewerPaneOption.click();
    await previewUntil(page, sourceActions.sourcePane.previewBtn, async () => {
      await expect(page.locator(VIEWER_FRAME)).toBeVisible({ timeout: 20000 });
    });

    // Viewer pane shows the rendered notebook.
    await expect(page.locator(VIEWER_TAB)).toBeVisible({ timeout: 30000 });
    await expect(page.locator(PUBLISH_BTN_IN_PANEL)).toBeVisible({ timeout: 10000 });

    // First preview: the chunk hasn't run, so there's no cached plot output --
    // the title renders but no image exists yet.
    const viewerFrame = switchToViewerFrame(page);
    await expect(viewerFrame.locator(MAIN_CONTAINER).first()).toContainText('R Notebook', { timeout: 30000 });
    // The body prose renders even before the chunk runs; the title only proves
    // the YAML header parsed, so assert the paragraph text too.
    await expect(viewerFrame.locator(MAIN_CONTAINER).first()).toContainText('Friends, Romans, countrymen, lend me your ears');
    await expect(viewerFrame.locator(CONTAINER_IMG)).toHaveCount(0);

    // Run the (only, unlabeled) chunk via the chunk-navigation commands.
    await consoleActions.clearConsole();
    await sourceActions.navigateToChunkByIndex(1);
    await executeCommand(page, 'executeCurrentChunk');
    await expect(consoleActions.consolePane.consoleOutput).toContainText('plot(cars)', { timeout: 10000 });

    // The executed chunk renders its plot inline in the editor.
    await expect(sourceActions.sourcePane.chunkImage).toBeVisible({ timeout: 30000 });

    // Preview again: the regenerated notebook now carries the cached plot.
    await previewUntil(page, sourceActions.sourcePane.previewBtn, async () => {
      await expect(switchToViewerFrame(page).locator(CONTAINER_IMG).first()).toBeVisible({ timeout: 20000 });
    });
    await expect(switchToViewerFrame(page).locator(MAIN_CONTAINER).first()).toContainText('R Notebook');

    // Cleanup: close the tab, delete the .Rmd and the .nb.html preview artifact.
    await sourceActions.closeSourceAndDeleteFile(fileName);
    await consoleActions.executeInConsole(`unlink("${fileName.replace('.Rmd', '.nb.html')}")`, { wait: true });
  });
});
