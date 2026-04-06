import { test, expect } from '@fixtures/rstudio.fixture';
import { sleep, CHAT_PROVIDERS } from '@utils/constants';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { AssistantOptionsActions } from '@actions/assistant_options.actions';
import { ChatPaneActions } from '@actions/chat_pane.actions';
import { ChatPane } from '@pages/chat_pane.page';
import { VIEWER_FRAME } from '@pages/viewer_pane.page';
import type { EnvironmentVersions } from '@pages/console_pane.page';

test.describe.serial('R Shiny Tip Calculator via Posit Assistant', () => {
  let chatPane: ChatPane;
  let chatActions: ChatPaneActions;
  let consoleActions: ConsolePaneActions;
  let versions: EnvironmentVersions;

  const PROMPT = `Create a Shiny for R web app for a tip calculator. The app can only depend on packages that are already installed--nothing that isn't already installed. The app should be a single file. It should have a slider from $0 to $100 for the bill amount. It should have four buttons: 10%, 15%, 20%, and 25%. The output, the tip, should be based on the value in the slider and the chosen button. The buttons and slider should both be oriented horizontally, the slider above the buttons. The bill and tip amount should be displayed above the slider. The title of the page should be "Wacky Tip Calculator for R". Then run the app in the viewer pane and make sure that it can be seen. The app MUST be viewable in the RStudio viewer pane. Then say "Wacky Tip Calculator for R has started" when the app starts running.`;

  test.beforeAll(async ({ rstudioPage: page }) => {
    consoleActions = new ConsolePaneActions(page);
    const assistantActions = new AssistantOptionsActions(page, consoleActions);
    chatActions = new ChatPaneActions(page, consoleActions);
    chatPane = chatActions.chatPane;

    versions = await consoleActions.getEnvironmentVersions();
    console.log(`R: ${versions.r}, RStudio: ${versions.rstudio}`);
    await consoleActions.clearConsole();

    // Force Shiny apps to open in the Viewer pane
    await consoleActions.typeInConsole('.rs.api.writeRStudioPreference("shiny_viewer_type", "pane")');
    await sleep(1000);

    // Clean up any leftover files from previous runs
    await consoleActions.typeInConsole('unlink("app.R")');
    await sleep(1000);

    // Clear the Viewer pane so we don't get false positives from previous content
    await consoleActions.typeInConsole(".rs.api.executeCommand('viewerClearAll')");
    await sleep(1000);

    // Dismiss the "Clear Viewer" confirmation dialog if it appears
    const clearViewerYes = page.locator('role=alertdialog >> button:has-text("Yes")');
    if (await clearViewerYes.isVisible({ timeout: 2000 }).catch(() => false)) {
      await clearViewerYes.click();
      await sleep(500);
    }

    await assistantActions.setChatProvider(CHAT_PROVIDERS['posit-assistant']);

    await chatActions.openChatPane();
    await chatActions.dismissSetupPrompts();
  });

  test.afterAll(async ({ rstudioPage: page }) => {
    // Stop any running Shiny app via the Viewer pane's stop button
    const viewerStopBtn = page.locator("[id^='rstudio_tb_viewerstop']");
    if (await viewerStopBtn.isVisible().catch(() => false)) {
      await viewerStopBtn.click();
      await expect(page.locator("[id^='rstudio_tb_interruptr']")).not.toBeVisible({ timeout: 10000 });
    }

    // Clean up created file(s)
    await consoleActions.typeInConsole('unlink("app.R")');
    await sleep(1000);
  });

  test.beforeEach(async () => {
    test.info().annotations.push(
      { type: 'R version', description: versions.r },
      { type: 'RStudio version', description: versions.rstudio },
    );
  });

  test('Create and run R Shiny tip calculator', async ({ rstudioPage: page }) => {
    // Start a fresh conversation
    await chatActions.startNewConversation();

    // Send the deterministic prompt
    await chatActions.sendChatMessage(PROMPT);

    // Poll until the assistant completes. Handle Allow dialogs for each tool type.
    await chatActions.pollWithAllowDialogs(async () => {
      const isStillProcessing = await chatPane.isStopButtonVisible();
      const messageCount = await chatPane.getMessageCount();
      if (!isStillProcessing && messageCount >= 2) {
        const lastText = await chatPane.messageItem.last().innerText().catch(() => '');
        if (lastText.includes('Wacky Tip Calculator for R has started') && !lastText.includes('Create a Shiny for R')) {
          console.log('Completion signal found: "Wacky Tip Calculator for R has started" in last message');
          return true;
        }
      }
      return false;
    }, 300000);



    // Brief settle time
    await sleep(2000);

    // Verify the Viewer pane iframe is visible
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
