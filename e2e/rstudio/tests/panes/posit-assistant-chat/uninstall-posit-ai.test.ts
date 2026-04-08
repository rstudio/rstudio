import { test as base, expect } from '@playwright/test';
import { launchRStudio, shutdownRStudio, type DesktopSession } from '@fixtures/desktop.fixture';
import { sleep } from '@utils/constants';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { ChatPaneActions } from '@actions/chat_pane.actions';
import { CONSOLE_INPUT } from '@pages/console_pane.page';
import { YES_BTN, NO_BTN } from '@pages/modals.page';
import type { Page } from 'playwright';

/**
 * Uninstall Posit Assistant — rstudio/rstudio#17322
 *
 * Tests the "Uninstall Posit Assistant" command (Help > Diagnostics).
 * Serial because test 4 (happy path) deletes PAI and restarts RStudio,
 * affecting all subsequent tests.
 *
 * Uses desktop.fixture directly (rather than the unified rstudio.fixture)
 * because the restart in test 4 reloads the Electron window, requiring
 * manual session lifecycle management.
 *
 * Order: 1 → 2 → 3 → 4 → 5
 *   1. Command visibility (palette)
 *   2. Cancel dialog
 *   3. Post-cancel state (chat still works)
 *   4. Happy path (deletes PAI, restarts)
 *   5. Reinstall PAI (cleanup)
 *
 * The "not installed" error case is not tested separately because commit
 * 2631a71 changed the backend to return Success (triggering a restart)
 * rather than an error when PAI is already absent.
 */

const PALETTE_LIST = '#rstudio_command_palette_list';

/**
 * Invoke "Uninstall Posit Assistant" via the command palette.
 * Desktop mode uses native Electron menus (not in the DOM),
 * so the command palette is the testable UI path.
 */
async function invokeUninstallViaPalette(page: Page): Promise<void> {
  await page.keyboard.press('ControlOrMeta+Shift+P');
  await sleep(1000);

  await page.keyboard.type('Uninstall Posit Assistant');
  await sleep(500);

  const paletteItem = page.locator(`${PALETTE_LIST} >> text=Uninstall Posit Assistant`);
  await expect(paletteItem).toBeVisible({ timeout: 5000 });
  await paletteItem.click();
  await sleep(500);
}

/**
 * Wait for RStudio to complete a restart cycle.
 *
 * The Electron shell stays alive but reloads the GWT page with a new
 * R session (see session-launcher.ts launchNextSession). We detect the
 * reload by checking that a window flag set before restart has cleared,
 * then wait for the new GWT app to initialize.
 *
 * Callers must set `window.__preRestart = true` before triggering the
 * restart so the flag-clearing check works.
 */
async function waitForRestart(page: Page): Promise<void> {
  await page.waitForFunction(
    '!window.__preRestart && typeof window.desktopHooks?.invokeCommand === "function"',
    null,
    { timeout: 60000 }
  );

  // Dismiss any save dialog that may appear after restart
  try {
    const dontSaveBtn = page.locator(
      "button:has-text('Don\\'t Save'), button:has-text('Do not Save'), #rstudio_dlg_no"
    );
    await dontSaveBtn.click({ timeout: 3000 });
  } catch {
    // No dialog
  }

  // desktopHooks used intentionally--console/R session not ready yet after restart
  await page.evaluate("window.desktopHooks.invokeCommand('activateConsole')");
  await sleep(2000);
  await page.waitForSelector(CONSOLE_INPUT, { state: 'visible', timeout: 30000 });
}

base.describe.serial('Uninstall Posit Assistant - #17322', { tag: ['@serial'] }, () => {
  base.skip(process.env.RSTUDIO_EDITION === 'server', 'Uses Desktop restart — Server not supported');

  let session: DesktopSession;
  let page: Page;
  let consoleActions: ConsolePaneActions;
  let chatActions: ChatPaneActions;

  base.beforeAll(async () => {
    session = await launchRStudio();
    page = session.page;
    consoleActions = new ConsolePaneActions(page);
    chatActions = new ChatPaneActions(page, consoleActions);

    // Ensure PAI is installed before the suite
    await chatActions.openChatPane();
    await chatActions.dismissSetupPrompts();
  });

  base.afterAll(async () => {
    if (session) {
      await shutdownRStudio(session);
    }
  });

  // -----------------------------------------------------------------------
  // Test 1: Command visibility (palette)
  // -----------------------------------------------------------------------
  base('Uninstall Posit Assistant visible in command palette', async () => {
    await page.keyboard.press('ControlOrMeta+Shift+P');
    await sleep(1000);

    await page.keyboard.type('Uninstall Posit Assistant');
    await sleep(500);

    const paletteItem = page.locator(`${PALETTE_LIST} >> text=Uninstall Posit Assistant`);
    await expect(paletteItem).toBeVisible({ timeout: 5000 });

    // Close palette without invoking
    await page.keyboard.press('Escape');
    await sleep(500);
  });

  // -----------------------------------------------------------------------
  // Test 2: Cancel dialog dismisses without action
  // -----------------------------------------------------------------------
  base('Cancel dialog dismisses without action', async () => {
    await invokeUninstallViaPalette(page);

    // Confirmation dialog should appear with Yes/No buttons
    const noBtn = page.locator(NO_BTN);
    await expect(noBtn).toBeVisible({ timeout: 5000 });

    // Verify the confirmation message
    const dialogText = await page.locator('.gwt-DialogBox').innerText();
    expect(dialogText).toContain('remove Posit Assistant and restart RStudio');

    // Click No to cancel
    await noBtn.click();
    await sleep(1000);

    // Session should still be alive
    await expect(page.locator(CONSOLE_INPUT)).toBeVisible();
  });

  // -----------------------------------------------------------------------
  // Test 3: Post-cancel state — chat still works
  // -----------------------------------------------------------------------
  base('Chat still works after cancelling uninstall', async () => {
    await chatActions.openChatPane();
    await chatActions.dismissSetupPrompts();

    await expect(chatActions.chatPane.chatRoot).toBeVisible({ timeout: 30000 });
    await expect(chatActions.chatPane.chatTextarea).toBeVisible({ timeout: 15000 });
  });

  // -----------------------------------------------------------------------
  // Test 4: Happy path — uninstall, restart, verify PAI gone
  // -----------------------------------------------------------------------
  base('Confirm uninstall deletes PAI and restarts', async () => {
    // Mark the window so waitForRestart can detect the page reload
    await page.evaluate('window.__preRestart = true');

    await invokeUninstallViaPalette(page);

    const yesBtn = page.locator(YES_BTN);
    await expect(yesBtn).toBeVisible({ timeout: 5000 });
    await yesBtn.click();

    // RStudio restarts: Electron stays alive, page reloads with new R session
    await waitForRestart(page);

    // Reinitialize actions on the reloaded page
    consoleActions = new ConsolePaneActions(page);
    chatActions = new ChatPaneActions(page, consoleActions);

    // Verify PAI is not installed — chat pane should show Install button
    await chatActions.openChatPane();
    await expect(chatActions.chatPane.installBtn).toBeVisible({ timeout: 15000 });
  });

  // -----------------------------------------------------------------------
  // Test 5: Reinstall PAI (cleanup — leave things as we found them)
  // -----------------------------------------------------------------------
  base('Reinstall Posit Assistant for cleanup', async () => {
    // Install button should already be visible from test 4
    await expect(chatActions.chatPane.installBtn).toBeVisible({ timeout: 15000 });
    await chatActions.chatPane.installBtn.click();
    console.log('Clicked Install — waiting for PAI installation...');

    await expect(chatActions.chatPane.chatRoot).toBeVisible({ timeout: 120000 });
    console.log('PAI reinstalled successfully');
  });
});
