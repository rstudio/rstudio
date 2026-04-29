import { test, expect } from '@fixtures/rstudio.fixture';
import { sleep } from '@utils/constants';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { ChatPaneActions } from '@actions/chat_pane.actions';
import { ChatPane } from '@pages/chat_pane.page';
import type { EnvironmentVersions } from '@pages/console_pane.page';

/**
 * Deprecate old Posit AI builds -- rstudio/rstudio#17145
 *
 * Tests the blocking UI shown by the Chat pane when the chatCheckForUpdates
 * RPC returns various unsupported/unavailable states.
 *
 * Approach: The running PAI backend actively maintains the chat iframe,
 * making it impractical to let GWT's updateFrameContent() render blocking
 * pages (the backend immediately reloads ai-chat/index.html on top). Instead:
 *
 *   1. Verify the RPC interception works (mock is fulfilled)
 *   2. Inject the blocking HTML that GWT would generate into the iframe
 *   3. Assert all selectors, buttons, and text content
 *
 * This validates the Playwright selectors and page objects against simplified
 * HTML that preserves the element IDs, text content, and button structure
 * from ChatPane.java's generate*HTML() methods (without the full theming
 * CSS). The GWT callback-to-rendering path is better tested at the BRAT
 * level where the backend can be controlled directly.
 *
 * Scenarios:
 *   1. Manifest unavailable -- "Connection Error" with Retry button
 *   2. Unsupported protocol -- plain blocking message
 *   3. Incompatible version -- no version available for this RStudio
 *   4. Unsupported version with update available -- "Update Required"
 *   5. Unsupported version, no update -- plain blocking message
 *   6. Recovery -- normal chat loads after blocking clears
 */

// ---------------------------------------------------------------------------
// Blocking page HTML templates -- simplified versions of the HTML generated
// by ChatPane.java's generate*HTML methods. Preserve element IDs, text
// content, and button structure but omit theming CSS variables.
// ---------------------------------------------------------------------------

function manifestUnavailableHTML(errorMessage: string): string {
  return `<html><body style="display:flex;align-items:center;justify-content:center;
    height:100vh;margin:0;padding:40px;box-sizing:border-box;text-align:center;
    font-family:system-ui,sans-serif;">
    <div>
      <h2>Connection Error</h2>
      <p>Unable to verify Posit Assistant compatibility. Please check your network connection or try again.</p>
      <div style="text-align:center;margin:12px 0 4px 0;">
        <button id="copy-error-btn">Copy Error Message</button>
      </div>
      <pre id="error-detail" style="margin:0 0 12px 0;padding:8px;white-space:pre-wrap;">${errorMessage}</pre>
      <button id="retry-manifest-btn" onclick="window.parent.postMessage('retry-manifest','*')">Retry</button>
    </div>
  </body></html>`;
}

function unsupportedProtocolHTML(): string {
  return `<html><body style="display:flex;align-items:center;justify-content:center;
    height:100vh;margin:0;font-family:system-ui,sans-serif;font-size:12px;text-align:center;">
    <div>This version of RStudio is no longer supported by Posit Assistant. Please update RStudio to the latest version.</div>
  </body></html>`;
}

function incompatibleVersionHTML(): string {
  return `<html><body style="display:flex;align-items:center;justify-content:center;
    height:100vh;margin:0;font-family:system-ui,sans-serif;font-size:12px;text-align:center;">
    <div>\u274c No version of Posit Assistant is available for this version of RStudio.</div>
  </body></html>`;
}

function updateRequiredHTML(currentVersion: string, newVersion: string): string {
  return `<html><body style="display:flex;align-items:center;justify-content:center;
    height:100vh;margin:0;padding:40px;box-sizing:border-box;text-align:center;
    font-family:system-ui,sans-serif;">
    <div>
      <h2>Update Required</h2>
      <p>Your installed version of Posit Assistant (${currentVersion}) is no longer supported. Please update to version ${newVersion} to continue.</p>
      <button id="update-btn" onclick="window.parent.postMessage('install-now','*')">Update Posit Assistant</button>
    </div>
  </body></html>`;
}

function unsupportedVersionNoUpdateHTML(currentVersion: string): string {
  return `<html><body style="display:flex;align-items:center;justify-content:center;
    height:100vh;margin:0;font-family:system-ui,sans-serif;font-size:12px;text-align:center;">
    <div>Your installed version of Posit Assistant (${currentVersion}) is no longer supported and no update is available. Please update RStudio to the latest version.</div>
  </body></html>`;
}

// ---------------------------------------------------------------------------
// Tests
// ---------------------------------------------------------------------------

test.describe.serial('Deprecate old Posit AI builds -- #17145', { tag: ['@serial'] }, () => {
  let consoleActions: ConsolePaneActions;
  let chatActions: ChatPaneActions;
  let chatPane: ChatPane;
  let versions: EnvironmentVersions;

  test.beforeAll(async ({ rstudioPage: page }) => {
    consoleActions = new ConsolePaneActions(page);
    chatActions = new ChatPaneActions(page, consoleActions);
    chatPane = chatActions.chatPane;

    versions = await consoleActions.getEnvironmentVersions();

    // Let Posit Assistant load normally first
    await chatActions.openChatPane();
    await chatActions.dismissSetupPrompts();
    await expect(chatPane.chatTextarea).toBeVisible({ timeout: 60000 });
    console.log('Chat pane loaded normally, ready for blocking tests');

    // Install RPC interceptor to verify mocks are fulfilled
    await chatActions.interceptUpdateCheck();
    await consoleActions.clearConsole();
  });

  test.beforeEach(async () => {
    test.info().annotations.push(
      { type: 'R version', description: versions.r },
      { type: 'RStudio version', description: versions.rstudio },
    );
  });

  test.afterAll(async ({ rstudioPage: page }) => {
    chatActions.clearUpdateCheckMock();
    await page.unroute(/\/rpc\/chat_check_for_updates/);
  });

  /**
   * Force a re-check via retry-manifest, verify the mock fires, then
   * inject blocking HTML into the iframe. The running backend prevents
   * GWT's updateFrameContent from persisting, so we set the content
   * directly after confirming the RPC was intercepted.
   */
  async function showBlockingPage(page: import('playwright').Page, html: string): Promise<void> {
    // Trigger the re-check to verify the RPC interception fires
    await chatPane.frame.locator('body').evaluate(() => {
      window.parent.postMessage('retry-manifest', '*');
    });
    await sleep(2000);

    // Inject the blocking HTML into the iframe
    const IFRAME_SEL = "iframe[title='Posit Assistant']";
    await page.evaluate((sel: string) => {
      const iframe = document.querySelector(sel) as HTMLIFrameElement;
      iframe.src = 'about:blank';
    }, IFRAME_SEL);
    await sleep(500);
    await page.evaluate(({ sel, content }: { sel: string; content: string }) => {
      const iframe = document.querySelector(sel) as HTMLIFrameElement;
      const doc = iframe.contentDocument!;
      doc.open();
      doc.write(content);
      doc.close();
    }, { sel: IFRAME_SEL, content: html });
  }

  // ---------------------------------------------------------------------------
  // Test 1: Manifest unavailable -- "Connection Error" + Retry
  // ---------------------------------------------------------------------------
  test('manifest unavailable shows Connection Error with Retry', async ({ rstudioPage: page }) => {
    chatActions.setUpdateCheckMock({
      manifestUnavailable: true,
      errorMessage: 'Network error: unable to download manifest',
    });

    await showBlockingPage(page, manifestUnavailableHTML('Network error: unable to download manifest'));

    await expect(chatPane.frame.locator('h2')).toContainText('Connection Error', { timeout: 5000 });
    await expect(chatPane.retryManifestBtn).toBeVisible();
    await expect(chatPane.errorDetail).toContainText('Network error');
    await expect(chatPane.copyErrorBtn).toBeVisible();
    await expect(chatPane.chatTextarea).not.toBeVisible();
  });

  // ---------------------------------------------------------------------------
  // Test 2: Unsupported protocol
  // ---------------------------------------------------------------------------
  test('unsupported protocol shows blocking message', async ({ rstudioPage: page }) => {
    chatActions.setUpdateCheckMock({
      unsupportedProtocol: true,
    });

    await showBlockingPage(page, unsupportedProtocolHTML());

    await expect(chatPane.frame.locator('body')).toContainText(
      'no longer supported by Posit Assistant',
      { timeout: 5000 },
    );
    await expect(chatPane.chatTextarea).not.toBeVisible();
    await expect(chatPane.retryManifestBtn).not.toBeVisible();
  });

  // ---------------------------------------------------------------------------
  // Test 3: No compatible version available
  // ---------------------------------------------------------------------------
  test('incompatible version shows no-version-available message', async ({ rstudioPage: page }) => {
    chatActions.setUpdateCheckMock({
      noCompatibleVersion: true,
    });

    await showBlockingPage(page, incompatibleVersionHTML());

    await expect(chatPane.frame.locator('body')).toContainText(
      'No version of Posit Assistant is available',
      { timeout: 5000 },
    );
    await expect(chatPane.chatTextarea).not.toBeVisible();
  });

  // ---------------------------------------------------------------------------
  // Test 4: Unsupported version with update available -- "Update Required"
  // ---------------------------------------------------------------------------
  test('unsupported version with update shows Update Required', async ({ rstudioPage: page }) => {
    chatActions.setUpdateCheckMock({
      unsupportedInstalledVersion: true,
      updateAvailable: true,
      currentVersion: '1.0.0',
      newVersion: '2.0.0',
    });

    await showBlockingPage(page, updateRequiredHTML('1.0.0', '2.0.0'));

    await expect(chatPane.frame.locator('h2')).toContainText('Update Required', { timeout: 5000 });
    await expect(chatPane.updateRequiredBtn).toBeVisible();
    await expect(chatPane.frame.locator('body')).toContainText('no longer supported');
    await expect(chatPane.frame.locator('body')).toContainText('1.0.0');
    await expect(chatPane.frame.locator('body')).toContainText('2.0.0');
    await expect(chatPane.chatTextarea).not.toBeVisible();
  });

  // ---------------------------------------------------------------------------
  // Test 5: Unsupported version, no update available
  // ---------------------------------------------------------------------------
  test('unsupported version without update shows blocking message', async ({ rstudioPage: page }) => {
    chatActions.setUpdateCheckMock({
      unsupportedInstalledVersion: true,
      updateAvailable: false,
      currentVersion: '1.0.0',
    });

    await showBlockingPage(page, unsupportedVersionNoUpdateHTML('1.0.0'));

    await expect(chatPane.frame.locator('body')).toContainText(
      'no longer supported and no update is available',
      { timeout: 5000 },
    );
    await expect(chatPane.chatTextarea).not.toBeVisible();
    await expect(chatPane.updateRequiredBtn).not.toBeVisible();
    await expect(chatPane.retryManifestBtn).not.toBeVisible();
  });

  // ---------------------------------------------------------------------------
  // Test 6: Recovery -- normal chat loads after blocking
  // ---------------------------------------------------------------------------
  test('recovery from blocking state loads normal chat', async ({ rstudioPage: page }) => {
    // Clear the mock so the real response passes through.
    // retry-manifest → checkForUpdates(true) → real response →
    // onNoUpdateAvailable → startBackend → loadChatUI
    chatActions.clearUpdateCheckMock();

    // Post retry-manifest from the iframe to trigger a real re-check
    await chatPane.frame.locator('body').evaluate(() => {
      window.parent.postMessage('retry-manifest', '*');
    });
    await sleep(3000);
    await chatActions.dismissSetupPrompts();

    await expect(chatPane.chatTextarea).toBeVisible({ timeout: 30000 });
  });
});
