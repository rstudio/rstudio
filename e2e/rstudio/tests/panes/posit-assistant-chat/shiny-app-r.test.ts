import { test, expect } from '@fixtures/rstudio.fixture';
import { requireAiCredentials } from '@utils/ai-credentials';
import { CHAT_PROVIDERS } from '@utils/constants';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { ChatPaneActions } from '@actions/chat_pane.actions';
import { ChatPane } from '@pages/chat_pane.page';
import { VIEWER_FRAME } from '@pages/viewer_pane.page';
import type { EnvironmentVersions } from '@pages/console_pane.page';
import { executeCommand, setPref } from '@utils/commands';
import { createChatActions, annotateVersions } from './_chat-setup';

test.describe.serial('R Shiny Tip Calculator via Posit Assistant', { tag: ['@ai'] }, () => {
  requireAiCredentials(test, 'positai');

  let chatPane: ChatPane;
  let chatActions: ChatPaneActions;
  let consoleActions: ConsolePaneActions;
  let versions: EnvironmentVersions;
  let missingPackages: string[] = [];

  const PROMPT = `Create a Shiny for R web app for a tip calculator. The app can only depend on packages that are already installed--nothing that isn't already installed. The app should be a single file. It should have a slider from $0 to $100 for the bill amount. It should have four buttons: 10%, 15%, 20%, and 25%. The output, the tip, should be based on the value in the slider and the chosen button. The buttons and slider should both be oriented horizontally, the slider above the buttons. The bill and tip amount should be displayed above the slider. The title of the page should be "Wacky Tip Calculator for R". Then run the app in the viewer pane and make sure that it can be seen. The app MUST be viewable in the RStudio viewer pane. Then say "Wacky Tip Calculator for R has started" when the app starts running.`;

  test.beforeAll(async ({ rstudioPage: page }) => {
    // A cold-cache package install can outlast the global per-test timeout;
    // keep the headroom this install hook's ensurePackages() budget assumes.
    test.setTimeout(300000);
    const actions = createChatActions(page);
    consoleActions = actions.consoleActions;
    chatActions = actions.chatActions;
    chatPane = actions.chatPane;

    versions = await consoleActions.getEnvironmentVersions();
    await consoleActions.clearConsole();

    // The prompt forbids the assistant from installing packages, so shiny + bslib
    // must be present in the sandbox-redirected user library before the test runs.
    missingPackages = await consoleActions.ensurePackages(['shiny', 'bslib', 'rstudioapi'], 180000);

    // Force Shiny apps to open in the Viewer pane (setPref is synchronous on
    // the bridge -- no settling needed).
    await setPref(page, 'shiny_viewer_type', 'pane');

    // Clean up any leftover files from previous runs
    await consoleActions.executeInConsole('unlink("app.R")', { wait: true });

    // Clear the Viewer pane so we don't get false positives from previous content.
    // viewerClearAll may show a confirmation dialog; wait for it to either
    // surface or settle.
    await executeCommand(page, 'viewerClearAll');
    const clearViewerYes = page.locator('role=alertdialog >> button:has-text("Yes")');
    if (await clearViewerYes.isVisible({ timeout: 2000 }).catch(() => false)) {
      await clearViewerYes.click();
      await expect(clearViewerYes).not.toBeVisible({ timeout: 5000 });
    }

    await actions.assistantActions.setChatProvider(CHAT_PROVIDERS['posit-assistant']);

    await chatActions.openChatPane();
    await chatActions.dismissSetupPrompts();
  });

  test.afterAll(async ({ rstudioPage: page }) => {
    // Stop any running Shiny app via the Viewer pane's stop button
    const viewerStopBtn = page.locator("[id^='rstudio_tb_viewerstop']");
    const interruptBtn = page.locator("[id^='rstudio_tb_interruptr']");

    if (await viewerStopBtn.isVisible().catch(() => false)) {
      await viewerStopBtn.click();

      const stoppedCleanly = await interruptBtn
        .waitFor({ state: 'hidden', timeout: 5000 })
        .then(() => true)
        .catch(() => false);

      if (!stoppedCleanly) {
        // R is still busy -- confirm "Terminate R" dialog if present,
        // otherwise click the Interrupt R button directly.
        const terminateDialog = page.locator('[role="alertdialog"]:has-text("Terminate R")');
        if (await terminateDialog.isVisible({ timeout: 2000 }).catch(() => false)) {
          await terminateDialog.locator('button:has-text("Yes")').click();
        } else {
          await interruptBtn.click();
        }
        await interruptBtn.waitFor({ state: 'hidden', timeout: 10000 }).catch(() => {
          console.warn('Interrupt R did not complete within 10s; subsequent tests may be affected');
        });
      }
    }

    // Dismiss any remaining "Terminate R" dialog before touching the console.
    const terminateDialog = page.locator('[role="alertdialog"]:has-text("Terminate R")');
    if (await terminateDialog.isVisible({ timeout: 1000 }).catch(() => false)) {
      await terminateDialog.locator('button:has-text("Yes")').click();
      await interruptBtn.waitFor({ state: 'hidden', timeout: 10000 }).catch(() => {
        console.warn('Interrupt R did not complete within 10s; subsequent tests may be affected');
      });
    }

    // Clean up created file(s)
    await consoleActions.executeInConsole('unlink("app.R")', { wait: true });
  });

  test.beforeEach(async () => {
    annotateVersions(versions);
  });

  test('Create and run R Shiny tip calculator', async ({ rstudioPage: page }) => {
    test.skip(missingPackages.length > 0, `Missing packages: ${missingPackages.join(', ')}`);

    // Budget the whole test body above the sum of its explicit waits, so a slow
    // but successful run is never preempted -- preemption would cap the
    // completion poll and hide its more informative timeout. The explicit waits
    // are ~45s of composer setup (startNewConversation + sendChatMessage's three
    // 15s visibility/editable waits) + the 300s completion poll + up to ~90s of
    // trailing Viewer-pane assertions, ~435s in total, so budget 540s to clear
    // it with margin. The fail-fast path below still surfaces a broken
    // executeCode in seconds, so this large ceiling only applies to a genuinely
    // slow cold Shiny launch.
    test.setTimeout(540000);

    // Start a fresh conversation
    await chatActions.startNewConversation();

    // Send the deterministic prompt
    await chatActions.sendChatMessage(PROMPT);

    // Poll until the assistant completes. Handle Allow dialogs for each tool
    // type. Fail fast if the turn finishes without the success marker -- e.g.
    // the r.getLogger executeCode outage (assistant #1893) makes write succeed
    // but every executeCode fail, so the app never runs and the marker never
    // arrives.
    let finishedWithoutMarker = 0;
    await chatActions.pollWithAllowDialogs(async () => {
      if ((await chatPane.getMessageCount()) < 2) return false;

      const lastText = await chatPane.messageItem.last().innerText().catch(() => '');
      // Keep the prompt-text guard so the user's own message is never mistaken
      // for the assistant's success marker.
      if (lastText.includes('Wacky Tip Calculator for R has started') &&
          !lastText.includes('Create a Shiny for R')) {
        return true;
      }

      // No success marker yet. Only conclude the assistant gave up once the
      // turn has truly ended -- the composer is editable again (idle, ready for
      // input). A missing Stop button alone is also true right after sending
      // and during the many tool-step transitions this prompt drives (write +
      // bash + several executeCode), so gate on the positive idle signal,
      // confirmed across one re-check, to avoid failing a still-progressing run.
      if (!(await chatPane.isChatInputReady())) {
        finishedWithoutMarker = 0;
        return false;
      }
      finishedWithoutMarker += 1;
      if (finishedWithoutMarker >= 2) {
        throw new Error(
          `Assistant finished without launching the Shiny app -- executeCode may ` +
          `have failed ("r.getLogger is not a function", assistant #1893). ` +
          `Last reply: ${lastText.slice(0, 500)}`,
        );
      }
      return false;
    }, 300000);



    // Verify the Viewer pane iframe is visible (expect auto-waits up to 30s
    // -- the iframe may take a beat to mount after the assistant signals
    // completion).
    const viewerIframe = page.locator(VIEWER_FRAME);
    await expect(viewerIframe).toBeVisible({ timeout: 30000 });

    // Verify elements inside the Viewer iframe
    const viewerFrame = page.frameLocator(VIEWER_FRAME);
    await expect(
      viewerFrame.locator('text=Wacky Tip Calculator for R').first()
    ).toBeVisible({ timeout: 30000 });

    // Verify slider is present
    await expect(
      viewerFrame.locator('input[type="range"], .slider, [class*="slider"]').first()
    ).toBeVisible({ timeout: 10000 });

    // Verify tip percentage buttons are present
    for (const pct of ['10%', '15%', '20%', '25%']) {
      await expect(
        viewerFrame.locator(`button:has-text("${pct}"), input[value="${pct}"]`).first()
      ).toBeVisible({ timeout: 5000 });
    }

  });
});
