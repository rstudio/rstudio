import { test, expect } from '@fixtures/rstudio.fixture';
import { requireAiCredentials } from '@utils/ai-credentials';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { ChatPaneActions } from '@actions/chat_pane.actions';
import { ChatPane } from '@pages/chat_pane.page';
import type { EnvironmentVersions } from '@pages/console_pane.page';
import { annotateVersions } from './_chat-setup';
import { getChatState, setChatUpdateCheckOverride } from '@utils/commands';

/**
 * Deprecate old Posit AI builds -- rstudio/rstudio#17145
 *
 * Tests the blocking-state transitions emitted by ChatPresenter when
 * chatCheckForUpdates returns various unsupported/unavailable results.
 *
 * Approach: rather than mocking the RPC at the HTTP layer (Playwright
 * route.fulfill returns status=0 to the renderer's XHR in dev-mode
 * Electron, which makes GWT take onCheckFailed instead of the blocking
 * callbacks we want to exercise), we install an in-rsession override
 * via the chat_set_update_check_override RPC. The override is honored
 * exactly once -- the next chat_check_for_updates returns it verbatim.
 *
 *   1. setChatUpdateCheckOverride({...}) so rsession's next reply is fixed
 *   2. postMessage('retry-manifest') so GWT does the check
 *   3. Poll window.rstudio.chat.state for the expected blocking identifier
 *
 * The rendered iframe HTML is GWT/CSS work tested at the unit level;
 * this suite covers the RPC-to-state wiring.
 *
 * Scenarios:
 *   1. Manifest unavailable     -> state "manifest-unavailable", blocked
 *   2. Unsupported protocol     -> state "unsupported-protocol", blocked
 *   3. Incompatible version     -> state "incompatible-version", blocked
 *   4. Version update required  -> state "version-update-required", blocked
 *   5. Version, no update       -> state "version-no-update", blocked
 *   6. Recovery                 -> state "ready", not blocked
 */

// Default response shape -- callers spread + override the relevant boolean
// for each test. Mirrors the field set returned by chatCheckForUpdates in
// SessionChat.cpp so rsession's override has every field the response
// schema expects.
const DEFAULT_CHECK_RESPONSE = {
  updateAvailable: false,
  isDowngrade: false,
  noCompatibleVersion: false,
  unsupportedInstalledVersion: false,
  unsupportedProtocol: false,
  manifestUnavailable: false,
  errorMessage: '',
  currentVersion: '1.0.0',
  newVersion: '',
  downloadUrl: '',
  isInitialInstall: false,
};

// ---------------------------------------------------------------------------
// Tests
// ---------------------------------------------------------------------------

test.describe.serial('Deprecate old Posit AI builds -- #17145', { tag: ['@ai', '@serial'] }, () => {
  requireAiCredentials(test, 'positai');

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
    await expect(chatPane.chatInput).toBeVisible({ timeout: 60000 });
    await consoleActions.clearConsole();
  });

  test.beforeEach(async () => {
    annotateVersions(versions);
  });

  test.afterEach(async ({ rstudioPage: page }) => {
    // Clear the per-test override so an unrelated chat_check_for_updates
    // (e.g. from a downstream test or session resume) doesn't pick it up.
    await setChatUpdateCheckOverride(page, null);
  });

  /**
   * Force a re-check via retry-manifest and wait until ChatPresenter has
   * published the expected state. ChatPresenter writes window.rstudio.chat
   * synchronously inside each show... callback, so polling it is race-free
   * regardless of what the live chat backend does to the iframe content.
   *
   * Anchors on the LAST history entry rather than the current state so a
   * transient transition (e.g. a stale poll callback re-emitting "ready"
   * after the blocking state fires) can't satisfy the assertion. The
   * matching entry must also have `blocked: true`.
   */
  async function triggerRecheckAndExpectState(
    page: import('playwright').Page,
    expectedState: NonNullable<Awaited<ReturnType<typeof getChatState>>>,
  ): Promise<void> {
    await Promise.all([
      page.waitForRequest('**/rpc/chat_check_for_updates', { timeout: 5000 }),
      chatPane.frame.locator('body').evaluate(() => {
        window.parent.postMessage('retry-manifest', '*');
      }),
    ]);
    await expect.poll(
      () => page.evaluate(() => {
        const h = window.rstudio?.chat?.history;
        return h && h.length > 0 ? h[h.length - 1] : null;
      }),
      { timeout: 5000 },
    ).toEqual(expect.objectContaining({ state: expectedState, blocked: true }));
  }

  // ---------------------------------------------------------------------------
  // Test 1: Manifest unavailable
  // ---------------------------------------------------------------------------
  test('manifest unavailable transitions chat to blocked state', async ({ rstudioPage: page }) => {
    await setChatUpdateCheckOverride(page, {
      ...DEFAULT_CHECK_RESPONSE,
      manifestUnavailable: true,
      errorMessage: 'Network error: unable to download manifest',
    });

    await triggerRecheckAndExpectState(page, 'manifest-unavailable');
  });

  // ---------------------------------------------------------------------------
  // Test 2: Unsupported protocol
  // ---------------------------------------------------------------------------
  test('unsupported protocol transitions chat to blocked state', async ({ rstudioPage: page }) => {
    await setChatUpdateCheckOverride(page, {
      ...DEFAULT_CHECK_RESPONSE,
      unsupportedProtocol: true,
    });

    await triggerRecheckAndExpectState(page, 'unsupported-protocol');
  });

  // ---------------------------------------------------------------------------
  // Test 3: No compatible version available
  // ---------------------------------------------------------------------------
  test('incompatible version transitions chat to blocked state', async ({ rstudioPage: page }) => {
    await setChatUpdateCheckOverride(page, {
      ...DEFAULT_CHECK_RESPONSE,
      noCompatibleVersion: true,
    });

    await triggerRecheckAndExpectState(page, 'incompatible-version');
  });

  // ---------------------------------------------------------------------------
  // Test 4: Unsupported version with update available
  // ---------------------------------------------------------------------------
  test('unsupported version with update transitions chat to blocked state', async ({ rstudioPage: page }) => {
    await setChatUpdateCheckOverride(page, {
      ...DEFAULT_CHECK_RESPONSE,
      unsupportedInstalledVersion: true,
      updateAvailable: true,
      currentVersion: '1.0.0',
      newVersion: '2.0.0',
    });

    await triggerRecheckAndExpectState(page, 'version-update-required');
  });

  // ---------------------------------------------------------------------------
  // Test 5: Unsupported version, no update available
  // ---------------------------------------------------------------------------
  test('unsupported version without update transitions chat to blocked state', async ({ rstudioPage: page }) => {
    await setChatUpdateCheckOverride(page, {
      ...DEFAULT_CHECK_RESPONSE,
      unsupportedInstalledVersion: true,
      updateAvailable: false,
      currentVersion: '1.0.0',
    });

    await triggerRecheckAndExpectState(page, 'version-no-update');
  });

  // ---------------------------------------------------------------------------
  // Test 6: Recovery -- normal chat loads after blocking
  // ---------------------------------------------------------------------------
  test('recovery from blocking state loads normal chat', async ({ rstudioPage: page }) => {
    // No override; rsession returns the real response. retry-manifest ->
    // checkForUpdates(true) -> onNoUpdateAvailable -> startBackend ->
    // loadChatUI -> state "ready".
    await Promise.all([
      page.waitForRequest('**/rpc/chat_check_for_updates', { timeout: 5000 }),
      chatPane.frame.locator('body').evaluate(() => {
        window.parent.postMessage('retry-manifest', '*');
      }),
    ]);
    await chatActions.dismissSetupPrompts();

    // Anchor on the last history entry so a mid-startup transient
    // (e.g. "starting") that briefly emits the right state-name without
    // the right blocked-flag can't satisfy the assertion.
    await expect.poll(
      () => page.evaluate(() => {
        const h = window.rstudio?.chat?.history;
        return h && h.length > 0 ? h[h.length - 1] : null;
      }),
      { timeout: 30000 },
    ).toEqual(expect.objectContaining({ state: 'ready', blocked: false }));
  });
});
