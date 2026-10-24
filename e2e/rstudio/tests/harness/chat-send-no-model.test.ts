import { test, expect, type Page } from '@playwright/test';
import { ChatPaneActions } from '@actions/chat_pane.actions';
import { ConsolePaneActions } from '@actions/console_pane.actions';

// Harness self-test for sendChatMessage's wait on the Send button. A composer
// with no model keeps Send disabled, described by "Please pick a model" (the
// shape Posit Assistant renders since posit-dev/assistant#2779); that state
// gets the outage check and a failure naming it, while any other disabled
// Send keeps the plain timeout. skipIfServiceGone is stubbed out: what it
// does needs the live service, and when it runs is sendChatMessage's call.

const NO_MODEL_REASON = '<span id="reason">Please pick a model</span>';

function composer(sendDisabled: boolean, extra = ''): string {
  return `
    <div id="root">
      <div class="tiptap-input-editor" contenteditable="true" style="min-height: 2em"></div>
      <button type="button" ${sendDisabled ? 'disabled aria-describedby="reason"' : ''}
              onclick="document.body.dataset.sent = 'yes'">
        <svg class="lucide-arrow-up" width="16" height="16"></svg>
      </button>
      ${extra}
    </div>`;
}

async function showComposer(page: Page, html: string): Promise<ChatPaneActions> {
  await page.setContent(`<iframe title="Posit Assistant" srcdoc="${html.replace(/"/g, '&quot;')}"></iframe>`);
  const actions = new ChatPaneActions(page, new ConsolePaneActions(page));
  await expect(actions.chatPane.chatInput).toBeVisible();
  return actions;
}

test('sends once Send is enabled', async ({ page }) => {
  const actions = await showComposer(page, composer(false));
  let serviceChecks = 0;
  actions.skipIfServiceGone = async () => { serviceChecks++; };

  await actions.sendChatMessage('hello');

  await expect(actions.chatPane.frame.locator('body')).toHaveAttribute('data-sent', 'yes');
  expect(serviceChecks).toBe(0);
});

test('a Send disabled for lack of a model is checked for an outage and named', async ({ page }) => {
  test.setTimeout(60000);
  const actions = await showComposer(page, composer(true, NO_MODEL_REASON));
  let serviceChecks = 0;
  actions.skipIfServiceGone = async () => { serviceChecks++; };

  await expect(actions.sendChatMessage('hello')).rejects.toThrow(/no model to send with/);
  expect(serviceChecks).toBe(1);
});

test('a Send disabled for another reason keeps the plain timeout', async ({ page }) => {
  test.setTimeout(60000);
  const actions = await showComposer(page, composer(true));
  let serviceChecks = 0;
  actions.skipIfServiceGone = async () => { serviceChecks++; };

  await expect(actions.sendChatMessage('hello')).rejects.toThrow(/toBeEnabled/);
  expect(serviceChecks).toBe(0);
});
