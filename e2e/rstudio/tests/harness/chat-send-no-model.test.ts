import { test, expect, type Page } from '@playwright/test';
import { ChatPaneActions } from '@actions/chat_pane.actions';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { TIMEOUTS } from '@utils/constants';

// Harness self-test for sendChatMessage's wait on the Send button. A composer
// with no model keeps Send disabled, described by "Please pick a model" (the
// shape Posit Assistant renders since posit-dev/assistant#2779); that state
// gets the outage check and a failure naming it, while any other disabled
// Send keeps the plain timeout. skipIfServiceGone is stubbed out: what it
// does needs the live service, and when it runs is sendChatMessage's call.

const NO_MODEL_REASON = '<span id="reason">Please pick a model</span>';

// Enables Send once typing starts, but later than click()'s own wait for an
// enabled element (TIMEOUTS.action) would last.
const ENABLE_AFTER_TYPING = `oninput="setTimeout(() => document.querySelector('button').disabled = false, ${TIMEOUTS.action + 1000})"`;

// Removes Send once typing starts, as a composer re-render would, while the
// wait for it to enable is still running.
const REMOVE_AFTER_TYPING = `oninput="setTimeout(() => document.querySelector('button')?.remove(), 1000)"`;

const SEND_HANDLERS = {
  'disabled': '',
  'enabled-after-typing': ENABLE_AFTER_TYPING,
  'removed-after-typing': REMOVE_AFTER_TYPING,
};

interface ComposerOptions {
  send: keyof typeof SEND_HANDLERS;
  describedBy?: string;
  extra?: string;
}

function composer({ send, describedBy, extra = '' }: ComposerOptions): string {
  return `
    <div id="root">
      ${extra}
      <div class="tiptap-input-editor" contenteditable="true" style="min-height: 2em"
           ${SEND_HANDLERS[send]}></div>
      <button type="button" disabled ${describedBy ? `aria-describedby="${describedBy}"` : ''}
              onclick="document.body.dataset.sent = 'yes'">
        <svg class="lucide-arrow-up" width="16" height="16"></svg>
      </button>
    </div>`;
}

async function showComposer(page: Page, html: string): Promise<ChatPaneActions> {
  await page.setContent(`<iframe title="Posit Assistant" srcdoc="${html.replace(/"/g, '&quot;')}"></iframe>`);
  const actions = new ChatPaneActions(page, new ConsolePaneActions(page));
  await expect(actions.chatPane.chatInput).toBeVisible();
  return actions;
}

test('waits for Send to enable after the prompt is typed', async ({ page }) => {
  test.setTimeout(60000);
  const actions = await showComposer(page, composer({ send: 'enabled-after-typing' }));
  let serviceChecks = 0;
  actions.skipIfServiceGone = async () => { serviceChecks++; };

  await actions.sendChatMessage('hello');

  await expect(actions.chatPane.frame.locator('body')).toHaveAttribute('data-sent', 'yes');
  expect(serviceChecks).toBe(0);
});

test('a Send disabled for lack of a model is checked for an outage and named', async ({ page }) => {
  test.setTimeout(60000);
  const actions = await showComposer(
    page, composer({ send: 'disabled', describedBy: 'reason', extra: NO_MODEL_REASON }));
  let serviceChecks = 0;
  actions.skipIfServiceGone = async () => { serviceChecks++; };

  await expect(actions.sendChatMessage('hello')).rejects.toThrow(/no model to send with/);
  expect(serviceChecks).toBe(1);
});

test('a Send disabled for another reason keeps the plain timeout', async ({ page }) => {
  test.setTimeout(60000);
  // The phrase elsewhere in the chat is not Send's description.
  const message = '<p>Please pick a model that fits the data.</p>';
  const actions = await showComposer(page, composer({ send: 'disabled', extra: message }));
  let serviceChecks = 0;
  actions.skipIfServiceGone = async () => { serviceChecks++; };

  await expect(actions.sendChatMessage('hello')).rejects.toThrow(/toBeEnabled/);
  expect(serviceChecks).toBe(0);
});

test('a Send that leaves the page keeps the plain timeout', async ({ page }) => {
  test.setTimeout(60000);
  const actions = await showComposer(page, composer({ send: 'removed-after-typing' }));
  let serviceChecks = 0;
  actions.skipIfServiceGone = async () => { serviceChecks++; };

  await expect(actions.sendChatMessage('hello')).rejects.toThrow(/toBeEnabled/);
  expect(serviceChecks).toBe(0);
});
