import { test, expect } from '@fixtures/rstudio.fixture';
import { requireAiCredentials } from '@utils/ai-credentials';
import { CHAT_PROVIDERS } from '@utils/constants';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { ChatPaneActions } from '@actions/chat_pane.actions';
import { ChatPane } from '@pages/chat_pane.page';
import { VIEWER_FRAME } from '@pages/viewer_pane.page';
import { ensureConsoleIdle, waitForConsoleIdle } from '@pages/console_pane.page';
import type { EnvironmentVersions } from '@pages/console_pane.page';
import { executeCommand, setPref } from '@utils/commands';
import { createChatActions, annotateVersions } from './_chat-setup';

test.describe.serial('R Shiny Tip Calculator via Posit Assistant', { tag: ['@ai', '@chat'] }, () => {
  requireAiCredentials(test, 'positai');

  let chatPane: ChatPane;
  let chatActions: ChatPaneActions;
  let consoleActions: ConsolePaneActions;
  let versions: EnvironmentVersions;
  let missingPackages: string[] = [];

  // The launch.browser prohibition is load-bearing: left to its own devices
  // the assistant sets options(shiny.launch.browser) to a window/external
  // launcher, which overrides the shiny_viewer_type pref set below and routes
  // the app away from the Viewer pane this test asserts on. The app.R name is
  // load-bearing too -- the cleanup hooks unlink that exact file.
  const PROMPT = `Create a Shiny for R web app for a tip calculator. The app can only depend on packages that are already installed--nothing that isn't already installed. The app should be a single file named app.R in the current working directory. It should have a slider from $0 to $100 for the bill amount. It should have four buttons: 10%, 15%, 20%, and 25%. The output, the tip, should be based on the value in the slider and the chosen button. The buttons and slider should both be oriented horizontally, the slider above the buttons. The bill and tip amount should be displayed above the slider. The title of the page should be "Wacky Tip Calculator for R". Then run the app in the viewer pane and make sure that it can be seen. The app MUST be viewable in the RStudio viewer pane. Do NOT set options(shiny.launch.browser) and do NOT pass a launch.browser argument to runApp--RStudio is already configured to open the app in the Viewer pane. Then say "Wacky Tip Calculator for R has started" when the app starts running.`;

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
    // Stop the app cleanly when the Viewer is showing it. This is best
    // effort: when the test failed before the Viewer activated (the app ran
    // but never rendered there), the stop button never appears even though
    // R is busy serving the app.
    const viewerStopBtn = page.locator("[id^='rstudio_tb_viewerstop']");
    if (await viewerStopBtn.isVisible().catch(() => false)) {
      await viewerStopBtn.click();
      await waitForConsoleIdle(page, 10000).catch(() => {});
    }

    // Never hand the worker off with a busy R: a leaked runApp() blocks the
    // console for every later suite in the worker (in Server mode, the rest
    // of the shard). ensureConsoleIdle interrupts regardless of what the
    // Viewer shows, escalating to the "Terminate R" dialog if R is stuck.
    await ensureConsoleIdle(page);

    // Clean up created file(s)
    await consoleActions
      .executeInConsole('unlink("app.R")', { wait: true })
      .catch((err) => {
        console.warn(
          `[shiny-app-r] cleanup unlink failed (R may be stuck): ${(err as Error).message}`,
        );
      });
  });

  test.beforeEach(async () => {
    annotateVersions(versions);
  });

  test('Create and run R Shiny tip calculator', async ({ rstudioPage: page }) => {
    test.skip(missingPackages.length > 0, `Missing packages: ${missingPackages.join(', ')}`);

    // Start a fresh conversation
    await chatActions.startNewConversation();

    // Send the deterministic prompt
    await chatActions.sendChatMessage(PROMPT);

    // Poll until the assistant completes. Handle Allow dialogs for each tool type.
    await chatActions.pollWithAllowDialogs(async () => {
      const messageCount = await chatPane.getMessageCount();
      if (messageCount >= 2 && await chatActions.isTurnIdle()) {
        const lastText = await chatPane.messageItem.last().innerText().catch(() => '');
        if (lastText.includes('Wacky Tip Calculator for R has started') && !lastText.includes('Create a Shiny for R')) {
          return true;
        }
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
