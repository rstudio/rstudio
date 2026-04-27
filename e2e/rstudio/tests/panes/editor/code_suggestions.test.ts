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
        `${prefix}_nes_multiline_diff.R`,
        `${prefix}_nes_multiline_apply.R`,
        `${prefix}_ghost_dismiss.R`,
        `${prefix}_nes_dismiss.R`,
        `${prefix}_nes_persist.R`,
        `${prefix}_nes_leak_a.R`,
        `${prefix}_nes_leak_b.R`,
        `${prefix}_statusbar_nav.R`,
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

      await expect(sourcePane.ghostText.first()).toBeVisible({ timeout: TIMEOUTS.ghostText });

      const ghostTextParts = await sourcePane.ghostText.allTextContents();
      const ghostTextContent = ghostTextParts.join('');
      console.log('Ghost text before accept: ' + ghostTextContent);

      await page.keyboard.press('ControlOrMeta+;');
      await sleep(2000);

      await expect(sourcePane.contentPane).toContainText(ghostTextContent, { timeout: 5000 });
      await expect(sourcePane.ghostText).toHaveCount(0, { timeout: 5000 });
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

    test('NES adjacent rename', async ({ rstudioPage: page }) => {
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
              .or(sourcePane.ghostText)
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

    test('NES multiline diff view with Discard', async ({ rstudioPage: page }) => {
      const fileName = `${prefix}_nes_multiline_diff.R`;

      // Parameter used across multiple consecutive lines in body
      const fileContent =
        'summarize <- function(nums) {\\n' +
        '  mean_val <- mean(nums)\\n' +
        '  sd_val <- sd(nums)\\n' +
        '  range_val <- range(nums)\\n' +
        '  list(mean = mean_val, sd = sd_val, range = range_val)\\n' +
        '}\\n';

      await sourceActions.createAndOpenFile(fileName, fileContent);
      await sleep(5000);

      // Rename parameter in signature to trigger multiline NES in body
      await sourceActions.selectInEditor('nums', 1, 22, 26);
      await sourcePane.aceTextInput.pressSequentially('measurements');
      await sleep(5000);

      // Wait for any NES indicator
      await expect(
        sourcePane.nesApply
          .or(sourcePane.ghostText)
          .or(sourcePane.nesInsertionPreview)
          .or(sourcePane.nesGutter)
          .first()
      ).toBeVisible({ timeout: TIMEOUTS.nesApply });
      await sleep(2000);

      // If diff view appeared (Apply visible), test the Discard button
      if (await sourcePane.nesApply.first().isVisible()) {
        console.log('  Multiline diff view detected — testing Discard');
        await expect(sourcePane.nesDiscard.first()).toBeVisible();

        // Capture editor content before discard
        const contentBefore = await sourceActions.getEditorContent();

        await sourcePane.nesDiscard.first().click();
        await sleep(2000);

        // Verify diff view is fully dismissed
        await expect(sourcePane.nesApply).toHaveCount(0, { timeout: 5000 });
        await expect(sourcePane.nesDiscard).toHaveCount(0, { timeout: 5000 });
        await expect(sourcePane.nesSuggestionContent).toHaveCount(0, { timeout: 5000 });

        // Verify editor content unchanged (suggestion was not applied)
        const contentAfter = await sourceActions.getEditorContent();
        expect(contentAfter).toBe(contentBefore);
        console.log('  Discard confirmed — diff view dismissed, content unchanged');
      } else {
        // Got a non-diff NES presentation; log and accept it so the test isn't stuck
        console.log('  WARNING: Expected multiline diff view but got a different NES type');
        await sourceActions.acceptNesRename();
      }

      await sourceActions.closeSourceAndDeleteFile(fileName);
    });

    test('NES multiline diff view with Apply', async ({ rstudioPage: page }) => {
      const fileName = `${prefix}_nes_multiline_apply.R`;

      // Same code as Discard test, different replacement name
      const fileContent =
        'summarize <- function(nums) {\\n' +
        '  mean_val <- mean(nums)\\n' +
        '  sd_val <- sd(nums)\\n' +
        '  range_val <- range(nums)\\n' +
        '  list(mean = mean_val, sd = sd_val, range = range_val)\\n' +
        '}\\n';

      await sourceActions.createAndOpenFile(fileName, fileContent);
      await sleep(5000);

      // Rename parameter in signature to trigger multiline NES in body
      // 'nums' starts at column 22: summarize <- function(nums) {
      await sourceActions.selectInEditor('nums', 1, 22, 26);
      // Use .first() because leaked NES diff view (#17361) may add a second readonly textarea
      await sourcePane.aceTextInput.first().pressSequentially('measurements');
      await sleep(5000);

      // Wait for any NES indicator
      await expect(
        sourcePane.nesApply
          .or(sourcePane.ghostText)
          .or(sourcePane.nesInsertionPreview)
          .or(sourcePane.nesGutter)
          .first()
      ).toBeVisible({ timeout: 10000 });
      await sleep(2000);

      // If diff view appeared (Apply visible), test the Apply button
      if (await sourcePane.nesApply.first().isVisible()) {
        console.log('  Multiline diff view detected — testing Apply');

        await sourcePane.nesApply.first().click();
        await sleep(2000);

        // Verify diff view is dismissed
        await expect(sourcePane.nesApply).toHaveCount(0, { timeout: 5000 });
        await expect(sourcePane.nesDiscard).toHaveCount(0, { timeout: 5000 });
        await expect(sourcePane.nesSuggestionContent).toHaveCount(0, { timeout: 5000 });

        // Verify the suggestion was applied — body should now use 'measurements'
        const content = await sourceActions.getEditorContent();
        console.log('  Editor content after Apply: ' + content.replace(/\n/g, '\\n'));
        expect(content).toContain('measurements');
        expect(content).not.toContain('nums');
        console.log('  Apply confirmed — suggestion applied to editor');
      } else {
        // Got a non-diff NES presentation; accept it
        console.log('  WARNING: Expected multiline diff view but got a different NES type');
        await sourceActions.acceptNesRename();
      }

      await sourceActions.closeSourceAndDeleteFile(fileName);
    });

    test('ghost text dismissed with Escape', async ({ rstudioPage: page }) => {
      const fileName = `${prefix}_ghost_dismiss.R`;

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

      await expect(sourcePane.ghostText.first()).toBeVisible({ timeout: TIMEOUTS.ghostText });
      console.log('  Ghost text visible — pressing Escape');

      // First Escape closes Ace autocomplete popup if active;
      // second Escape dismisses the ghost text itself
      await page.keyboard.press('Escape');
      await sleep(500);
      await page.keyboard.press('Escape');
      await sleep(1000);

      // Verify ghost text is fully cleared
      await expect(sourcePane.ghostText).toHaveCount(0, { timeout: 5000 });
      console.log('  Ghost text dismissed with Escape');

      await sourceActions.closeSourceAndDeleteFile(fileName);
    });

    test('NES dismissed with Escape', async ({ rstudioPage: page }) => {
      if (key === 'posit-assistant') test.fixme(true, 'Posit Assistant NES gutter icon persists after Escape (https://github.com/rstudio/rstudio/issues/17363)');

      const fileName = `${prefix}_nes_dismiss.R`;

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

      // Rename to trigger NES
      await sourceActions.selectInEditor('calculate_total', 6, 0, 6);
      await sourcePane.aceTextInput.pressSequentially('final_result');
      await sleep(2000);

      // Wait for any NES indicator
      await expect(
        sourcePane.nesApply
          .or(sourcePane.ghostText)
          .or(sourcePane.nesInsertionPreview)
          .or(sourcePane.nesGutter)
          .first()
      ).toBeVisible({ timeout: TIMEOUTS.nesApply });
      await sleep(1000);

      const contentBefore = await sourceActions.getEditorContent();
      console.log('  NES suggestion visible — pressing Escape');

      await page.keyboard.press('Escape');
      await sleep(2000);

      // Verify all NES indicators are cleared
      await expect(sourcePane.nesApply).toHaveCount(0, { timeout: 5000 });
      await expect(sourcePane.nesDiscard).toHaveCount(0, { timeout: 5000 });
      await expect(sourcePane.nesInsertionPreview).toHaveCount(0, { timeout: 5000 });
      await expect(sourcePane.nesGutter).toHaveCount(0, { timeout: 5000 });
      await expect(sourcePane.ghostText).toHaveCount(0, { timeout: 5000 });

      // Verify content unchanged
      const contentAfter = await sourceActions.getEditorContent();
      expect(contentAfter).toBe(contentBefore);
      console.log('  NES dismissed with Escape — clean state confirmed');

      await sourceActions.closeSourceAndDeleteFile(fileName);
    });

    test('NES suggestion survives unrelated edit', async ({ rstudioPage: page }) => {
      const fileName = `${prefix}_nes_persist.R`;

      const fileContent =
        'calculate_total <- function(price, tax_rate) {\\n' +
        '  total <- price + (price * tax_rate)\\n' +
        '  return(total)\\n' +
        '}\\n' +
        '\\n' +
        '# placeholder line\\n' +
        'order_total <- calculate_total(100, 0.08)\\n' +
        'tax_amount <- 100 * 0.08\\n' +
        'discount <- 0.10\\n' +
        'print(order_total)\\n';

      await sourceActions.createAndOpenFile(fileName, fileContent);
      await sleep(5000);

      // Rename variable on line 7 to trigger NES on line 10
      await sourceActions.selectInEditor('order_total', 7, 0, 11);
      await sourcePane.aceTextInput.pressSequentially('final_total');
      await sleep(5000);

      // Wait for NES suggestion to appear
      await expect(
        sourcePane.nesApply
          .or(sourcePane.ghostText)
          .or(sourcePane.nesInsertionPreview)
          .or(sourcePane.nesGutter)
          .first()
      ).toBeVisible({ timeout: TIMEOUTS.nesApply });
      await sleep(1000);
      console.log('  NES suggestion visible — editing unrelated line');

      // Edit an unrelated line (the placeholder comment on line 6)
      await sourceActions.selectInEditor('# placeholder line', 6, 0, 18);
      await sourcePane.aceTextInput.pressSequentially('# edited comment');
      await sleep(2000);

      // Verify NES suggestion is still visible after unrelated edit
      const stillVisible = await sourcePane.nesApply.first().isVisible().catch(() => false)
        || await sourcePane.ghostText.first().isVisible().catch(() => false)
        || await sourcePane.nesInsertionPreview.first().isVisible().catch(() => false)
        || await sourcePane.nesGutter.first().isVisible().catch(() => false);

      if (stillVisible) {
        console.log('  NES suggestion persisted after unrelated edit');
        await sourceActions.acceptNesRename();
      } else {
        console.log('  WARNING: NES suggestion was dismissed by unrelated edit');
      }

      expect(stillVisible).toBe(true);

      await sourceActions.closeSourceAndDeleteFile(fileName);
    });

    test('NES suggestion does not leak across documents', async ({ rstudioPage: page }) => {
      if (key === 'posit-assistant') test.fixme(true, 'Posit Assistant NES suggestion leaks across documents (https://github.com/rstudio/rstudio/issues/17361)');

      const fileNameA = `${prefix}_nes_leak_a.R`;
      const fileNameB = `${prefix}_nes_leak_b.R`;

      const originalCode =
        'order_total <- calculate_total(100, 0.08)\\n' +
        'tax_amount <- 100 * 0.08\\n' +
        'discount <- 0.10\\n' +
        'print(order_total)\\n';

      // --- File A: trigger and accept NES ---
      await sourceActions.createAndOpenFile(fileNameA, originalCode);
      await sleep(5000);

      // Pre-edit: rename order_total on line 1
      await sourceActions.selectInEditor('order_total', 1, 0, 11);
      await sourcePane.aceTextInput.pressSequentially('final_total');
      await sleep(5000);

      // Accept the NES suggestion (post-edit)
      await sourceActions.acceptNesRename();
      await expect(sourcePane.contentPane).toContainText('print(final_total)', { timeout: 5000 });
      console.log('  File A: NES accepted — order_total renamed to final_total');

      // Close File A
      await consoleActions.typeInConsole(".rs.api.executeCommand('saveAllSourceDocs')");
      await sleep(1000);
      await consoleActions.typeInConsole(".rs.api.executeCommand('closeAllSourceDocs')");
      await sleep(2000);

      // --- File B: same original code, no edits ---
      await sourceActions.createAndOpenFile(fileNameB, originalCode);
      await sleep(5000);

      // Check for leaked NES suggestion — none should appear
      const leaked = await sourcePane.nesApply.first().isVisible().catch(() => false)
        || await sourcePane.ghostText.first().isVisible().catch(() => false)
        || await sourcePane.nesInsertionPreview.first().isVisible().catch(() => false)
        || await sourcePane.nesGutter.first().isVisible().catch(() => false);

      if (leaked) {
        console.log('  BUG: NES suggestion leaked from File A into File B');
      } else {
        console.log('  File B: no leaked NES suggestion — OK');
      }

      expect(leaked).toBe(false);

      await sourceActions.closeSourceAndDeleteFile(fileNameB);
      await consoleActions.typeInConsole(`unlink("${fileNameA}")`);
      await sleep(500);
    });

    test('status bar navigates to most recent completion on click', async ({ rstudioPage: page }) => {
      if (key === 'copilot') test.fixme(true, 'Copilot clears status bar click handler after autocomplete dismiss (https://github.com/rstudio/rstudio/issues/17372)');
      const fileName = `${prefix}_statusbar_nav.R`;

      // Build a long file so the editor needs to scroll
      const lines: string[] = [
        'calculate_total <- function(price, tax_rate) {',
        '  total <- price + (price * tax_rate)',
        '  return(total)',
        '}',
        '',
      ];
      for (let i = 1; i <= 35; i++) {
        lines.push(`value_${i} <- ${i} * 2`);
      }
      lines.push('');
      lines.push('result <- calculate_total(100, 0.08)');
      lines.push('');

      const fileContent = lines.join('\\n');

      await sourceActions.createAndOpenFile(fileName, fileContent);
      await sleep(1000);

      // Position cursor at the end and type to trigger a ghost text suggestion
      await sourceActions.goToEnd();
      await sleep(500);
      await page.keyboard.press('Enter');
      await sleep(200);
      await page.keyboard.type('x <- calc');

      // Wait for ghost text to confirm a real suggestion arrived
      await expect(sourcePane.ghostText.first()).toBeVisible({ timeout: TIMEOUTS.ghostText });
      const ghostParts = await sourcePane.ghostText.allTextContents();
      console.log('  Ghost text: "' + ghostParts.join('') + '"');

      // Wait for the status bar to show "Completion response received"
      await expect(sourcePane.statusBarCompletionReceived).toBeVisible({ timeout: 5000 });
      console.log('  Status bar shows "Completion response received"');

      // Dismiss Ace autocomplete popup if present, without clearing ghost text
      if (await page.locator('#rstudio_popup_completions').isVisible().catch(() => false)) {
        await page.keyboard.press('Escape');
        await sleep(500);
        console.log('  Dismissed autocomplete popup');
      }

      // Scroll the viewport to the middle of the file WITHOUT moving the cursor.
      // Using keyboard shortcuts would move the cursor, triggering a new
      // completion request that replaces the click handler we want to test.
      // We scroll to the middle so that regardless of whether the completion
      // is near the top or bottom, clicking the status bar causes an
      // observable scroll change.
      await page.evaluate(`(function() {
        var editors = document.querySelectorAll('.ace_editor');
        for (var i = 0; i < editors.length; i++) {
          if (editors[i].closest('#rstudio_console_input')) continue;
          var env = editors[i].env;
          if (env && env.editor) {
            env.editor.scrollToLine(20, false);
            break;
          }
        }
      })()`);
      await sleep(1000);

      const rowBefore = await sourceActions.getFirstVisibleRow();
      console.log(`  After scrolling viewport to middle: first visible row = ${rowBefore}`);

      // Verify the status bar is still clickable (cursor: pointer = handler is set)
      const cursor = await sourcePane.footerTable.evaluate(
        el => window.getComputedStyle(el).cursor
      );
      console.log(`  Status bar cursor style: ${cursor}`);

      // Click the status bar panel directly (click handler is on the panel element)
      await sourcePane.footerTable.click({ force: true });
      await sleep(1000);

      const rowAfterClick = await sourceActions.getFirstVisibleRow();
      console.log(`  After clicking status bar: first visible row = ${rowAfterClick}`);

      // The click handler scrolls to completion.range.start.line - 5.
      // The completion could be near the top or the end of the file, but
      // either way the scroll position should change from the middle.
      expect(rowAfterClick).not.toBe(rowBefore);

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
        `${prefix}_nes_multiline_diff.R`,
        `${prefix}_nes_multiline_apply.R`,
        `${prefix}_ghost_dismiss.R`,
        `${prefix}_nes_dismiss.R`,
        `${prefix}_nes_persist.R`,
        `${prefix}_nes_leak_a.R`,
        `${prefix}_nes_leak_b.R`,
        `${prefix}_statusbar_nav.R`,
      ];
      const unlinkExpr = testFiles.map(f => `"${f}"`).join(', ');
      await consoleActions.typeInConsole(`for (f in c(${unlinkExpr})) unlink(f)`);
      await sleep(2000);
    });
  });
}
