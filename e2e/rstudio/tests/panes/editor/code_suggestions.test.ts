import { test, expect } from '@fixtures/rstudio.fixture';
import { TIMEOUTS, sleep, CODE_SUGGESTION_PROVIDERS } from '@utils/constants';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { AssistantOptionsActions } from '@actions/assistant_options.actions';
import { SourcePaneActions } from '@actions/source_pane.actions';
import { SourcePane } from '@pages/source_pane.page';

for (const [key, provider] of Object.entries(CODE_SUGGESTION_PROVIDERS)) {
  test.describe(provider, () => {
    const prefix = provider.toLowerCase().replace(/\s+/g, '_');

    let consoleActions: ConsolePaneActions;
    let assistantActions: AssistantOptionsActions;
    let sourceActions: SourcePaneActions;
    let sourcePane: SourcePane;

    test.beforeAll(async ({ rstudioPage: page }) => {
      consoleActions = new ConsolePaneActions(page);
      assistantActions = new AssistantOptionsActions(page, consoleActions);
      sourceActions = new SourcePaneActions(page, consoleActions);
      sourcePane = sourceActions.sourcePane;

      // Close any leftover source files and delete test files from previous runs
      await consoleActions.typeInConsole(".rs.api.executeCommand('saveAllSourceDocs')");
      await sleep(1000);
      await consoleActions.typeInConsole(".rs.api.executeCommand('closeAllSourceDocs')");
      await sleep(1000);

      // Delete all possible test files in a single R command
      const testFiles = [
        `${prefix}_ghost_text_accept.R`,
        `${prefix}_nes_trigger_accept.R`,
        `${prefix}_nes_distant_rename.R`,
        `${prefix}_nes_gap_rename.R`,
        `${prefix}_nes_multi_rename.R`,
      ];
      const unlinkExpr = testFiles.map(f => `"${f}"`).join(', ');
      await consoleActions.typeInConsole(`for (f in c(${unlinkExpr})) unlink(f)`);
      await sleep(2000);

      await assistantActions.setupAssistantOptions(provider);
    });

    test('ghost text accept', async ({ rstudioPage: page }) => {
      const fileName = `${prefix}_ghost_text_accept.R`;

      const fileContent =
        'calculate_total <- function(price, tax_rate) {\\n' +
        '  total <- price + (price * tax_rate)\\n' +
        '  return(total)\\n' +
        '}\\n' +
        '\\n' +
        'result <- calculate_total(100, 0.08)\\n';

      await sourceActions.createAndOpenFile(fileName, fileContent);
      await sleep(1000);

      await sourcePane.contentPane.click();
      await sleep(500);

      await page.keyboard.type('x <- func');

      await expect(sourcePane.copilotGhostText.first()).toBeVisible({ timeout: TIMEOUTS.ghostText });

      const ghostTextParts = await sourcePane.copilotGhostText.allTextContents();
      const ghostTextContent = ghostTextParts.join('');
      console.log('Ghost text before accept: ' + ghostTextContent);

      await page.keyboard.press('ControlOrMeta+;');
      await sleep(2000);

      await expect(sourcePane.contentPane).toContainText(ghostTextContent, { timeout: 5000 });
      await expect(sourcePane.copilotGhostText).toHaveCount(0, { timeout: 5000 });
      await sleep(2000);

      await sourceActions.closeSourceAndDeleteFile(fileName);
    });

    test('NES manual trigger and accept', async ({ rstudioPage: page }) => {
      const fileName = `${prefix}_nes_trigger_accept.R`;

      const fileContent =
        'calculate_total <- function(price, tax_rate) {\\n' +
        '  total <- price + (price * tax_rate)\\n' +
        '  return(total)\\n' +
        '}\\n' +
        '\\n' +
        'result <- calculate_total(100, 0.08)\\n' +
        'print(result)\\n';

      await sourceActions.createAndOpenFile(fileName, fileContent);
      await sleep(5000);

      await sourceActions.selectInEditor('calculate_total', 6, 0, 6);
      await sourcePane.aceTextInput.pressSequentially('final_result');
      await sleep(2000);

      await sourceActions.acceptNesRename();

      await expect(sourcePane.contentPane).toContainText('final_result', { timeout: 5000 });

      await sourceActions.closeSourceAndDeleteFile(fileName);
    });

    test.fixme('NES adjacent rename', async ({ rstudioPage: page }) => {
      const fileName = `${prefix}_nes_distant_rename.R`;

      const fileContent =
        'calculate_total <- function(price, tax_rate) {\\n' +
        '  total <- price + (price * tax_rate)\\n' +
        '  return(total)\\n' +
        '}\\n' +
        '\\n' +
        'total_price <- calculate_total(100, 0.08)\\n' +
        'print(total_price)\\n';

      await sourceActions.createAndOpenFile(fileName, fileContent);
      await sleep(5000);

      // Check for stale NES gutter icon — known RStudio bug where gutter persists across files
      if (await sourcePane.nesGutter.first().isVisible().catch(() => false)) {
        console.log('  WARNING: Stale NES gutter icon visible before any edits (known bug, https://github.com/rstudio/rstudio/issues/17108)');
      }

      await sourceActions.selectInEditor('total_price', 6, 0, 11);

      const selectedText = await sourceActions.getSelectedText('total_price');
      console.log('Selected text: "' + selectedText + '"');

      await sourcePane.aceTextInput.pressSequentially('final_price');
      await sleep(5000);

      await sourceActions.acceptNesRename();

      await expect(sourcePane.contentPane).toContainText('print(final_price)', { timeout: 5000 });
      await expect(sourcePane.contentPane).not.toContainText('total_price', { timeout: 5000 });

      await sourceActions.closeSourceAndDeleteFile(fileName);
    });

    test('NES gap rename', async ({ rstudioPage: page }) => {
      const fileName = `${prefix}_nes_gap_rename.R`;

      const fileContent =
        'calculate_total <- function(price, tax_rate) {\\n' +
        '  total <- price + (price * tax_rate)\\n' +
        '  return(total)\\n' +
        '}\\n' +
        '\\n' +
        'order_total <- calculate_total(100, 0.08)\\n' +
        'tax_amount <- 100 * 0.08\\n' +
        'discount <- 0.10\\n' +
        'print(order_total)\\n';

      await sourceActions.createAndOpenFile(fileName, fileContent);
      await sleep(5000);

      await sourceActions.selectInEditor('order_total', 6, 0, 11);

      const selectedText = await sourceActions.getSelectedText('order_total');
      console.log('Selected text: "' + selectedText + '"');

      await sourcePane.aceTextInput.pressSequentially('final_total');
      await sleep(5000);

      await sourceActions.acceptNesRename();

      await expect(sourcePane.contentPane).toContainText('print(final_total)', { timeout: 5000 });

      await sourceActions.closeSourceAndDeleteFile(fileName);
    });

    test('NES multiple renames', async ({ rstudioPage: page }) => {
      const fileName = `${prefix}_nes_multi_rename.R`;

      const fileContent =
        'calculate_total <- function(price, tax_rate) {\\n' +
        '  total <- price + (price * tax_rate)\\n' +
        '  return(total)\\n' +
        '}\\n' +
        '\\n' +
        'score <- calculate_total(100, 0.08)\\n' +
        'print(score)\\n' +
        'tax_amount <- 100 * 0.08\\n' +
        'cat(score)\\n' +
        'summary(score)\\n' +
        'message(score)\\n';

      await sourceActions.createAndOpenFile(fileName, fileContent);
      await sleep(5000);

      await sourceActions.selectInEditor('summary(score)', 6, 0, 5);

      await sourcePane.aceTextInput.pressSequentially('final_score');
      await sleep(5000);

      let accepted = 0;
      const maxAcceptances = 4;
      while (accepted < maxAcceptances) {
        try {
          await expect(
            sourcePane.nesApply
              .or(sourcePane.copilotGhostText)
              .or(sourcePane.nesInsertionPreview)
              .first()
          ).toBeVisible({ timeout: 10000 });
        } catch {
          break;
        }
        accepted++;
        console.log(`Accepting NES rename ${accepted}...`);
        await sourceActions.acceptNesRename();
      }
      console.log(`Accepted ${accepted} NES suggestion(s)`);

      const occurrences = ['print', 'cat', 'summary', 'message'];
      for (const fn of occurrences) {
        await expect(sourcePane.contentPane).toContainText(`${fn}(final_score)`, { timeout: 5000 });
      }

      await sourceActions.closeSourceAndDeleteFile(fileName);
    });

    test.afterAll(async ({ rstudioPage: page }) => {
      // Post-suite cleanup: close files and delete test artifacts
      await consoleActions.typeInConsole(".rs.api.executeCommand('saveAllSourceDocs')");
      await sleep(1000);
      await consoleActions.typeInConsole(".rs.api.executeCommand('closeAllSourceDocs')");
      await sleep(1000);

      const testFiles = [
        `${prefix}_ghost_text_accept.R`,
        `${prefix}_nes_trigger_accept.R`,
        `${prefix}_nes_distant_rename.R`,
        `${prefix}_nes_gap_rename.R`,
        `${prefix}_nes_multi_rename.R`,
      ];
      const unlinkExpr = testFiles.map(f => `"${f}"`).join(', ');
      await consoleActions.typeInConsole(`for (f in c(${unlinkExpr})) unlink(f)`);
      await sleep(2000);
    });
  });
}
