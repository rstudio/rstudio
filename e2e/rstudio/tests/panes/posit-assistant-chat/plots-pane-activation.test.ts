import { test, expect } from '@fixtures/rstudio.fixture';
import { requireAiCredentials } from '@utils/ai-credentials';
import { executeCommand, isCommandEnabled, numModalsShowing, dismissAllModals } from '@utils/commands';
import { PLOTS_TAB } from '@pages/plots_pane.page';
import { YES_BTN } from '@pages/modals.page';
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
    // clearPlots is disabled when there is no plot history (e.g. the test
    // failed before plotting). Gate on the enabled state so this cleanup never
    // throws and masks the original failure -- executeCommand otherwise waits
    // for the command to enable and then times out.
    if (!(await isCommandEnabled(page, 'clearPlots'))) return;

    await executeCommand(page, 'clearPlots');
    // Confirm via the stable GWT "Yes" button id, polling for it rather than
    // racing a fixed timeout. dismissAll only hides dialogs, so it would not
    // actually clear the history -- the Yes click is what runs clearPlots.
    await page.locator(YES_BTN).click({ timeout: 10000 }).catch(() => {});

    // Wait for the confirm dialog and its clearPlots progress indicator to
    // drain -- i.e. the async clear finished -- so it can't race later specs in
    // this worker. Only force any still-open modal down if that doesn't happen.
    try {
      await expect.poll(() => numModalsShowing(page), { timeout: 10000 }).toBe(0);
    } catch {
      await dismissAllModals(page);
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
    // its Allow dialog appears. Match the marker only in the assistant's reply
    // bubble: the user's prompt bubble also contains the marker verbatim, so
    // scoping to the assistant role avoids depending on a prompt-text exclusion
    // (which would deadlock until timeout if the reply echoed that phrase).
    await chatActions.pollWithAllowDialogs(async () => {
      if (await chatPane.isStopButtonVisible()) return false;
      const lastReply = await chatPane.assistantMessageItem.last().innerText().catch(() => '');
      return lastReply.includes(PLOT_DONE_MARKER);
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
