import { test, expect } from '@fixtures/rstudio.fixture';
import { requireAiCredentials } from '@utils/ai-credentials';
import { clearPref, executeCommand, setPref } from '@utils/commands';
import { BACKUP_BINDINGS, RESTORE_BINDINGS, writeAppBindings } from '@utils/keybindings';
import { ChatPane } from '@pages/chat_pane.page';
import type { ConsolePaneActions } from '@actions/console_pane.actions';
import type { EnvironmentVersions } from '@pages/console_pane.page';
import type { Locator, Page } from 'playwright';
import { setupPositAssistantChat, annotateVersions } from './_chat-setup';

// RStudio exposes window.rstudioChatKeydown to the Posit Assistant iframe
// (AssistantKeyForwarder.java). These tests stand in for PA's caller
// (rstudioShortcutForwarder.ts in posit-dev/assistant), so they don't depend on
// the seeded PA's version.

// A plain dict rather than KeyboardEventInit, whose type is too deep for
// Playwright's evaluate() argument serialization types.
interface KeyInit {
  key: string;
  code: string;
  keyCode: number;
  ctrlKey?: boolean;
  altKey?: boolean;
  shiftKey?: boolean;
}

interface SendResult {
  used: boolean;
  defaultPrevented: boolean;
}

// ShortcutManager matches on keyCode, so every init sets it.
const CTRL_1: KeyInit = { key: '1', code: 'Digit1', keyCode: 49, ctrlKey: true };
const CTRL_2: KeyInit = { key: '2', code: 'Digit2', keyCode: 50, ctrlKey: true };
const ALT_MINUS: KeyInit = { key: '-', code: 'Minus', keyCode: 189, altKey: true };
// Emacs mode's activateConsole binding.
const ALT_X: KeyInit = { key: 'x', code: 'KeyX', keyCode: 88, altKey: true };
// Unbound by default; the satellite tests bind it to focusNextPane.
const CTRL_ALT_SHIFT_K: KeyInit = {
  key: 'K',
  code: 'KeyK',
  keyCode: 75,
  ctrlKey: true,
  altKey: true,
  shiftKey: true,
};

// Dispatch a keydown inside the chat iframe and hand it to the parent's
// rstudioChatKeydown, as PA's forwarder does. The event is stopped at a
// throwaway element so PA's own listeners never see it.
async function sendKeyAsAssistant(
  chatInput: Locator,
  init: KeyInit,
): Promise<SendResult> {
  await chatInput.click();
  return chatInput.evaluate((input, eventInit) => {
    const host = window.parent as Window & { rstudioChatKeydown?: (e: KeyboardEvent) => boolean };
    if (typeof host.rstudioChatKeydown !== 'function') {
      throw new Error('window.parent.rstudioChatKeydown is not installed');
    }
    const doc = input.ownerDocument;
    const target = doc.createElement('div');
    doc.body.appendChild(target);
    let used = false;
    target.addEventListener('keydown', (e) => {
      used = host.rstudioChatKeydown!(e);
      e.stopPropagation();
    });
    const event = new KeyboardEvent('keydown', { ...eventInit, bubbles: true, cancelable: true });
    target.dispatchEvent(event);
    target.remove();
    return { used, defaultPrevented: event.defaultPrevented };
  }, init);
}

async function expectConsoleShortcutRuns(chatPane: ChatPane, page: Page): Promise<void> {
  const result = await sendKeyAsAssistant(chatPane.chatInput, CTRL_2);
  expect(result).toEqual({ used: true, defaultPrevented: true });
  await expect(page.locator('#rstudio_console_input .ace_text-input')).toBeFocused();
}

test.describe('Posit Assistant -- RStudio shortcuts', { tag: ['@ai', '@chat'] }, () => {
  requireAiCredentials(test, 'positai');

  let chatPane: ChatPane;
  let versions: EnvironmentVersions;

  test.beforeAll(async ({ rstudioPage: page }) => {
    ({ chatPane, versions } = await setupPositAssistantChat(page));
    await expect(chatPane.chatInput).toBeVisible({ timeout: 30000 });
  });

  test.beforeEach(async () => {
    annotateVersions(versions);
  });

  test('a Workbench shortcut runs and is cancelled', async ({ rstudioPage: page }) => {
    await expectConsoleShortcutRuns(chatPane, page);
  });

  test('an editor shortcut is left to Posit Assistant', async () => {
    // Alt+- is insertAssignmentOperator, an editor-context command.
    const result = await sendKeyAsAssistant(chatPane.chatInput, ALT_MINUS);
    expect(result).toEqual({ used: false, defaultPrevented: false });
  });
});

// Satellite pop-out on Server hits the Chrome window.open() reload path;
// chat-satellite-commands.test.ts is @desktop_only for the same reason.
test.describe.serial(
  'Posit Assistant satellite -- RStudio shortcuts',
  { tag: ['@ai', '@chat', '@desktop_only'] },
  () => {
    requireAiCredentials(test, 'positai');

    let consoleActions: ConsolePaneActions;
    let mainChatPane: ChatPane;
    let satellitePage: Page;
    let satelliteChatPane: ChatPane;

    test.beforeAll(async ({ rstudioPage: page }) => {
      ({ consoleActions, chatPane: mainChatPane } = await setupPositAssistantChat(page));
      await expect(mainChatPane.chatInput).toBeVisible({ timeout: 30000 });

      // Set before popping out, so the satellite starts with them. A custom
      // binding replaces the command's defaults, so F6 is kept in the list.
      await consoleActions.executeInConsole(BACKUP_BINDINGS);
      await consoleActions.executeInConsole(
        writeAppBindings({ focusNextPane: 'F6|Ctrl+Alt+Shift+K' }),
      );
      await setPref(page, 'editor_keybindings', 'emacs');

      const satellitePromise = page.context().waitForEvent('page', { timeout: 30000 });
      await executeCommand(page, 'popOutChat');
      satellitePage = await satellitePromise;
      await satellitePage.waitForLoadState('domcontentloaded');
      satelliteChatPane = new ChatPane(satellitePage);
      await expect(satelliteChatPane.chatInput).toBeVisible({ timeout: 30000 });
    });

    test.afterAll(async ({ rstudioPage: page }) => {
      await clearPref(page, 'editor_keybindings');
      await consoleActions.executeInConsole(RESTORE_BINDINGS);
    });

    test('a main-window shortcut is routed to the main window', async () => {
      const result = await sendKeyAsAssistant(satelliteChatPane.chatInput, CTRL_2);
      expect(result).toEqual({ used: true, defaultPrevented: true });
      await expect(consoleActions.consolePane.consoleInput).toBeFocused();
    });

    test('a custom keybinding works from the satellite', async () => {
      // used is true only if the satellite matched the binding and routed it out.
      const result = await sendKeyAsAssistant(satelliteChatPane.chatInput, CTRL_ALT_SHIFT_K);
      expect(result).toEqual({ used: true, defaultPrevented: true });
    });

    test('the editor keybinding mode applies in the satellite', async () => {
      const result = await sendKeyAsAssistant(satelliteChatPane.chatInput, ALT_X);
      expect(result).toEqual({ used: true, defaultPrevented: true });
      await expect(consoleActions.consolePane.consoleInput).toBeFocused();
    });

    test('an any-mode shortcut stays out of the satellite', async () => {
      // Ctrl+1 is activateSource: Workbench context, no windowMode.
      const result = await sendKeyAsAssistant(satelliteChatPane.chatInput, CTRL_1);
      expect(result).toEqual({ used: false, defaultPrevented: false });
    });

    test('shortcuts still work after returning chat to the main window', async ({
      rstudioPage: page,
    }) => {
      const closePromise = satellitePage.waitForEvent('close', { timeout: 30000 });
      await executeCommand(page, 'returnChatToMain');
      await closePromise;

      const chatPane = new ChatPane(page);
      await expect(chatPane.chatInput).toBeVisible({ timeout: 30000 });
      await expectConsoleShortcutRuns(chatPane, page);
    });
  },
);
