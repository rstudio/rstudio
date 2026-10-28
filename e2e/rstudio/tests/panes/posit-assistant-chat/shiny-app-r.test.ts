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
  // load-bearing too -- the cleanup hooks unlink that exact file. So is the
  // console-launch instruction: once the assistant chose
  // `later::later(function() shiny::runApp("app.R"))` "to keep the console
  // free", which ran the app on the idle event loop, where (before #19236)
  // the session served no requests and took no interrupt; the cleanup below
  // could not stop it and the shard was lost to the heartbeat watchdog.
  //
  // The assistant may run runApp() inside its own executeCode call, which
  // blocks until the app stops, so its turn never ends while the app runs
  // (run 40691812818). The test therefore waits on the Viewer, not on the
  // turn, and asks for no closing message.
  const PROMPT = `Create a Shiny for R web app for a tip calculator. The app can only depend on packages that are already installed--nothing that isn't already installed. The app should be a single file named app.R in the current working directory. It should have a slider from $0 to $100 for the bill amount. It should have four buttons: 10%, 15%, 20%, and 25%. The output, the tip, should be based on the value in the slider and the chosen button. The buttons and slider should both be oriented horizontally, the slider above the buttons. The bill and tip amount should be displayed above the slider. The title of the page should be "Wacky Tip Calculator for R". Then run the app in the viewer pane and make sure that it can be seen. The app MUST be viewable in the RStudio viewer pane. Do NOT set options(shiny.launch.browser) and do NOT pass a launch.browser argument to runApp--RStudio is already configured to open the app in the Viewer pane. Run the app with shiny::runApp() at the console; do NOT schedule it with later::later() or run it as a background job.`;
  const APP_TITLE = 'Wacky Tip Calculator for R';

  // Budget for the assistant to write app.R and get it running in the Viewer,
  // sized for LLM tail latency -- observed whole-test durations are 90-108s.
  //
  // Capped below the 300s no-output watchdog in scripts/run-with-heartbeat.mjs
  // (PW_HEARTBEAT_TIMEOUT_SECONDS, which only the Linux desktop engine
  // overrides). The poll is silent while the assistant works, so a budget at
  // or above that threshold lets the watchdog kill the whole shard instead of
  // this one test failing with the poll's own diagnostic. Raising it means
  // emitting progress from the poll first.
  const ASSISTANT_TURN_BUDGET_MS = 240000;

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
    // A timed-out poll leaves only seconds of the no-output watchdog's window,
    // and the cleanup below can outlast them; announcing it restarts the clock.
    console.log('[shiny-app-r] cleanup: stopping the assistant turn and the app');

    // The turn is still running when the assistant launched the app from its
    // own executeCode call. Stop it first, so interrupting R below does not
    // hand control back to a live turn that may launch the app again. A chat
    // that never loaded has no turn, though isTurnIdle() reads it as busy.
    if (chatPane && (await chatPane.chatRoot.isVisible().catch(() => false))) {
      let idle = await chatActions.isTurnIdle();
      for (let attempt = 1; attempt <= 3 && !idle; attempt++) {
        await chatPane.stopBtn.click({ timeout: 5000 }).catch((err) => {
          console.warn(`[shiny-app-r] stopping the turn (attempt ${attempt}): ${(err as Error).message}`);
        });
        // Let the cancel land before re-sampling, so it is not read as a failure.
        await expect(chatPane.stopBtn).toBeHidden({ timeout: 10000 }).catch(() => {});
        idle = await chatActions.isTurnIdle();
      }
      if (!idle) {
        console.warn('[shiny-app-r] the assistant turn is still running after 3 stop attempts');
      }
    }

    // Stop the app cleanly when the Viewer is showing it. This is best
    // effort: when the test failed before the Viewer activated (the app ran
    // but never rendered there), the stop button never appears even though
    // R is busy serving the app. Stopping the turn above can also stop the
    // app, hiding the button between the check and the click.
    const viewerStopBtn = page.locator("[id^='rstudio_tb_viewerstop']");
    if (await viewerStopBtn.isVisible().catch(() => false)) {
      await viewerStopBtn.click({ timeout: 5000 }).catch((err) => {
        console.warn(`[shiny-app-r] Viewer stop click failed: ${(err as Error).message}`);
      });
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

    // Without this the test falls back to the 120s global timeout and cuts the
    // poll below off at 40% of its budget -- a bare "Test timeout exceeded"
    // instead of the poll's own diagnostic. Derived to keep the two in sync;
    // the margin covers conversation setup and the Viewer assertions.
    test.setTimeout(ASSISTANT_TURN_BUDGET_MS + 120000);

    // Start a fresh conversation
    await chatActions.startNewConversation();

    // Send the deterministic prompt
    await chatActions.sendChatMessage(PROMPT);

    // The no-output watchdog counts from the last output, not from here, so
    // announce the wait: that resets its timer at poll start and makes the
    // budget-vs-watchdog margin exact instead of a guess about setup silence.
    console.log(`[shiny-app-r] awaiting the app in the Viewer (budget ${ASSISTANT_TURN_BUDGET_MS / 1000}s)`);

    // Poll until the app shows in the Viewer, granting Allow dialogs for each
    // tool type meanwhile. The turn may still be running (see PROMPT).
    const viewerFrame = page.frameLocator(VIEWER_FRAME);
    const appTitle = viewerFrame.getByText(APP_TITLE).first();
    await chatActions.pollWithAllowDialogs(
      () => appTitle.isVisible().catch(() => false),
      ASSISTANT_TURN_BUDGET_MS,
    );

    // Verify slider is present
    await expect(
      viewerFrame.locator('input[type="range"], .slider, [class*="slider"]').first()
    ).toBeVisible({ timeout: 10000 });

    // Verify the tip percentage choices are present. "Buttons" can come back
    // as push buttons or as radio buttons -- run 40398345386 got
    // radioButtons(choices = c("10%" = 0.10, ...)), whose inputs carry the
    // numeric value -- so match the accessible name of either role.
    for (const pct of ['10%', '15%', '20%', '25%']) {
      await expect(
        viewerFrame.getByRole('button', { name: pct }).or(viewerFrame.getByRole('radio', { name: pct })).first()
      ).toBeVisible({ timeout: 5000 });
    }

  });
});
