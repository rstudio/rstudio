import { test, expect } from '@fixtures/rstudio.fixture';
import * as os from 'os';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { SourcePaneActions } from '@actions/source_pane.actions';
import { installDepIfPrompted } from '@pages/modals.page';
import {
  VIEWER_TAB, VIEWER_FRAME, PUBLISH_BTN_IN_PANEL, QUARTO_CONTENT,
  switchToViewerFrame,
} from '@pages/viewer_pane.page';
import { useSuiteSandbox } from '@utils/sandbox';

test.describe('Quarto', () => {
  // Sets cwd to a per-spec sandbox; relative paths used by createAndOpenFile
  // and closeSourceAndDeleteFile land there.
  useSuiteSandbox();
  let consoleActions: ConsolePaneActions;
  let sourceActions: SourcePaneActions;

  test.beforeAll(async ({ rstudioPage: page }) => {
    consoleActions = new ConsolePaneActions(page);
    sourceActions = new SourcePaneActions(page, consoleActions);

    // Clean up leftover source files
    await consoleActions.resetSourcePane();

    await consoleActions.ensurePackages(['rmarkdown']);

    await consoleActions.clearConsole();
  });

  test('render quarto document', async ({ rstudioPage: page }) => {
    // macOS CI: panmirror visual mode reliably never mounts on macOS 26 ARM64
    // after the per-test reset added activateEnvironment + activateFiles in
    // #17950 -- the source pane shows the Visual toggle selected and the
    // editor area sits on the loading spinner until ensureVisualMode's
    // timeout. Reproduced across 180s waits, source-pane refocus before the
    // toggle, and dismissAllModals sweeps; none of those unblock the mount.
    // Tracking on the data viewer / reset PR for a proper macOS repro before
    // a fix lands. The Windows + Linux paths are unaffected.
    test.fixme(os.platform() === 'darwin' && !!process.env.CI, 'panmirror visual mode never mounts on macOS 26 ARM64 e2e CI -- see #17950 per-test reset activations');
    // Original: test_desktop_Quarto.py::test_render_quarto_doc
    const fileName = `quarto_${Date.now()}.qmd`;

    // Create file with initial "---" and open it
    await sourceActions.createAndOpenFile(fileName, '---');
    await expect(sourceActions.sourcePane.selectedTab).toContainText(fileName, { timeout: 20000 });
    await expect(sourceActions.sourcePane.publishBtn).toBeVisible({ timeout: 10000 });

    // Ensure we're in Source mode (not Visual)
    await sourceActions.ensureSourceMode();

    // Type the Quarto document content
    await sourceActions.sendText('title: "Test Quarto"');
    await page.keyboard.press('Enter');
    await sourceActions.sendText('format: html');
    await page.keyboard.press('Enter');
    await sourceActions.sendText('---');
    await page.keyboard.press('Enter');
    await page.keyboard.press('Enter');
    await sourceActions.sendText('## Quarto');
    await page.keyboard.press('Enter');
    await page.keyboard.press('Enter');
    await page.keyboard.type('Tomorrow and tomorrow and tomorrow\n\n```{r}\n1 + 1\n```');
    await page.keyboard.press('Enter');

    // Switch to Visual mode
    await sourceActions.ensureVisualMode();

    // Set preview to Viewer pane (each click auto-waits for actionability).
    await sourceActions.sourcePane.formatOptions.click();
    await sourceActions.sourcePane.viewerPaneOption.click();

    // Render via Cmd+Shift+K / Ctrl+Shift+K
    await page.keyboard.press('ControlOrMeta+Shift+k');
    await installDepIfPrompted(page);

    // Verify viewer pane shows rendered output
    console.log('Checking VIEWER_TAB...');
    await expect(page.locator(VIEWER_TAB)).toBeVisible({ timeout: 30000 });
    console.log('Checking VIEWER_FRAME...');
    await expect(page.locator(VIEWER_FRAME)).toBeVisible({ timeout: 30000 });
    console.log('Checking PUBLISH_BTN_IN_PANEL...');
    await expect(page.locator(PUBLISH_BTN_IN_PANEL)).toBeVisible({ timeout: 10000 });

    // Switch into viewer iframe and verify content
    console.log('Switching to viewer iframe...');
    const viewerFrame = switchToViewerFrame(page);
    const quartoContent = viewerFrame.locator(QUARTO_CONTENT);
    console.log('Checking for prose text...');
    await expect(quartoContent).toContainText('Tomorrow and tomorrow and tomorrow', { timeout: 30000 });
    console.log('Checking for code...');
    await expect(quartoContent).toContainText('1 + 1');
    console.log('Checking for result...');
    await expect(quartoContent).toContainText('[1] 2');

    // Cleanup
    await sourceActions.closeSourceAndDeleteFile(fileName);
    await consoleActions.executeInConsole(
      `unlink("${fileName.replace('.qmd', '.html')}")`,
      { wait: true },
    );
  });
});
