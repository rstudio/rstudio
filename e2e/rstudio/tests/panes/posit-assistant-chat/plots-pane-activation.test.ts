import { test, expect } from '@fixtures/rstudio.fixture';
import { requireAiCredentials } from '@utils/ai-credentials';
import { executeCommand } from '@utils/commands';
import { PLOTS_TAB } from '@pages/plots_pane.page';
import type { ChatPane } from '@pages/chat_pane.page';
import type { ChatPaneActions } from '@actions/chat_pane.actions';
import type { ConsolePaneActions } from '@actions/console_pane.actions';
import type { EnvironmentVersions } from '@pages/console_pane.page';
import type { Page } from 'playwright';
import { setupPositAssistantChat, annotateVersions } from './_chat-setup';

// Regression test for https://github.com/rstudio/rstudio/issues/18037.
//
// When Posit Assistant runs code that produces a plot, the Plots pane should
// be brought to the front automatically -- just like plotting from the
// console. PR #17923 moved the assistant to an interleaved execution path that
// captures plots itself and calls executeCode with capturePlot disabled; the
// session then reported ChangeSourceRPC instead of ChangeSourceREPL, so the
// Plots pane no longer activated even though the plot rendered to the device.
//
// This drives the real assistant: it selects Files (which shares the Plots
// tabset, so Plots is deselected -- the state described in the issue), asks the
// assistant to draw a base-R plot, then asserts the Plots tab becomes selected.

const PLOT_DONE_MARKER = 'PLOT_DONE_E2E';

// Force base-R plotting (no package dependencies) through the code-execution
// tool, and end with a deterministic marker so the poll can detect completion.
const PROMPT =
  'Use your code execution tool to run exactly this R code and nothing else: ' +
  'plot(1:10, main = "E2E plot test"). Do not write it to a file and do not ' +
  `load any packages. After the plot has been drawn, reply with exactly: ${PLOT_DONE_MARKER}`;

async function isPlotsTabSelected(page: Page): Promise<boolean> {
  return (await page.locator(PLOTS_TAB).getAttribute('aria-selected')) === 'true';
}

test.describe.serial('Posit Assistant activates the Plots pane', { tag: ['@ai'] }, () => {
  requireAiCredentials(test, 'positai');

  let chatPane: ChatPane;
  let chatActions: ChatPaneActions;
  let consoleActions: ConsolePaneActions;
  let versions: EnvironmentVersions;

  test.beforeAll(async ({ rstudioPage: page }) => {
    // Assistant backend cold-start can outlast the default per-test budget.
    test.setTimeout(120000);
    const actions = await setupPositAssistantChat(page);
    chatPane = actions.chatPane;
    chatActions = actions.chatActions;
    consoleActions = actions.consoleActions;
    versions = actions.versions;
  });

  test.beforeEach(() => {
    annotateVersions(versions);
  });

  test.afterAll(async ({ rstudioPage: page }) => {
    // Don't leak the device/plot this test created into later tests in the
    // worker: close the graphics device and clear the Plots pane history.
    if (!consoleActions) return;
    await consoleActions.executeInConsole('graphics.off()', { wait: true });
    await executeCommand(page, 'clearPlots');
    const clearYes = page.locator('role=alertdialog >> button:has-text("Yes")');
    if (await clearYes.isVisible({ timeout: 2000 }).catch(() => false)) {
      await clearYes.click();
      await expect(clearYes).not.toBeVisible({ timeout: 5000 });
    }
  });

  test('generating a plot brings the Plots pane to the front', async ({ rstudioPage: page }) => {
    test.setTimeout(180000);

    await chatActions.startNewConversation();

    // Close any device left open by an earlier test so the activation we assert
    // on is caused by this turn -- not a stale pending plot that background
    // rendering could surface on its own and mask the regression.
    await consoleActions.executeInConsole('graphics.off()', { wait: true });

    // Precondition: the Plots pane is NOT the selected tab. Files shares its
    // tabset (TabSet2) with Plots, so selecting Files guarantees Plots is
    // deselected -- the starting state described in the issue.
    await executeCommand(page, 'activateFiles');
    await expect.poll(() => isPlotsTabSelected(page)).toBe(false);

    await chatActions.sendChatMessage(PROMPT);

    // Drive the conversation to completion, granting executeCode permission as
    // its Allow dialog appears. The user's prompt bubble also contains the
    // marker, so guard against matching it by excluding a prompt-only phrase.
    await chatActions.pollWithAllowDialogs(async () => {
      if (await chatPane.isStopButtonVisible()) return false;
      if ((await chatPane.getMessageCount()) < 2) return false;
      const lastText = await chatPane.messageItem.last().innerText().catch(() => '');
      return lastText.includes(PLOT_DONE_MARKER) && !lastText.includes('Use your code execution tool');
    }, 150000);

    // The regression: the assistant drew a plot, so the Plots pane must be
    // brought to the front, exactly as a console plot would.
    await expect
      .poll(() => isPlotsTabSelected(page), {
        timeout: 15000,
        message:
          'Plots pane was not activated after Posit Assistant generated a plot (issue #18037)',
      })
      .toBe(true);
  });
});
