import { test, expect, type Page } from '@playwright/test';
import { ChatPaneActions, providerFailure } from '@actions/chat_pane.actions';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { ChatPane } from '@pages/chat_pane.page';

// Harness self-test for providerFailure, which decides whether a chat turn
// that ended in an error was Posit AI failing on its side (skip) or something
// a product change could cause (fail). The strings are Posit Assistant's own
// (databot's formatApiError), and the DOM below is the shape it rendered in
// run 34994843398. The poll tests at the end cover which turn's failure
// pollWithAllowDialogs holds against the test.

test('recognizes a request that failed without an HTTP status', () => {
  const network = [
    'Error making request',
    'Error making request: read ETIMEDOUT',
    // Appended to a reply that had already started streaming.
    'Let me read that fileError making request',
    '[An error occurred: terminated]',
    '[An error occurred: Failed after 3 attempts. Last error: Cannot connect to API: Connect Timeout Error '
      + '(attempted address: gateway.posit.ai:443, timeout: 10000ms)]',
  ];
  for (const text of network) {
    expect(providerFailure(text), text).toEqual({ message: expect.any(String), side: 'network' });
  }
  expect(providerFailure('Error making request: read ETIMEDOUT')?.message).toBe('Error making request: read ETIMEDOUT');
});

test('recognizes the service answering with a failure', () => {
  const service = [
    'Too many requests. Please wait a moment and try again.',
    'The AI service encountered an internal error. Please try again.',
    'The AI service is temporarily unavailable (HTTP 503). Please try again.',
    'The AI model is currently overloaded. Please try again in a few moments.',
    'Error making request: Overloaded',
  ];
  for (const text of service) {
    expect(providerFailure(text), text).toEqual({ message: text, side: 'service' });
  }
});

test('leaves failures a product change could cause alone', () => {
  const notProviderFailures = [
    '',
    'The answer is 42.',
    // Other HTTP statuses: a malformed request, a missing model, an account problem.
    'Error making request (HTTP 400): prompt is too long',
    'Error making request (HTTP 404)',
    'Your Posit AI Pass credits have been depleted.',
    // A status-less error whose detail isn't a network failure.
    'Error making request: invalid tool schema',
    '[An error occurred: object of type \'closure\' is not subsettable]',
    // A network error reaching something local, which no outage explains.
    'Error making request: connect ECONNREFUSED 127.0.0.1:53211',
    '[An error occurred: connect ECONNREFUSED [::1]:8787]',
    // The failure is not how the reply ends.
    'The pane shows "Error making request" when a call fails.',
    'Error making request\n\nRetrying worked: the answer is 42.',
  ];
  for (const text of notProviderFailures) {
    expect(providerFailure(text), text).toBeNull();
  }
});

const USER_MESSAGE = '<div data-testid="chat-message-user"><p>Who is the Norse god of mischief?</p></div>';

// The error detail renders as a code span inside the reply's content div.
function failedReply(messageId: string): string {
  return `
    <div class="chat-message-assistant">
      <div class="message-content relative" data-message-id="${messageId}">
        <div class="space-y-4"><p>Error making request: <code>read ETIMEDOUT</code></p></div>
      </div>
    </div>`;
}

async function showConversation(page: Page, chat: string): Promise<void> {
  await page.setContent(`<iframe title="Posit Assistant" srcdoc="${chat.replace(/"/g, '&quot;')}"></iframe>`);
}

test('reads the failure from the rendered chat message', async ({ page }) => {
  await showConversation(page, USER_MESSAGE + failedReply('m1'));

  const chatPane = new ChatPane(page);
  await expect(chatPane.messageItem).toHaveCount(2);

  const text = await chatPane.lastMessageText();
  expect(providerFailure(text)).toEqual({ message: 'Error making request: read ETIMEDOUT', side: 'network' });
});

/**
 * Run pollWithAllowDialogs over `chat` for two rounds, appending `arriving`
 * to the conversation in between, and return how many times the poll looked
 * at a provider failure. skipIfProviderFailed is stubbed out: what it does
 * with a failure needs the live service, and which failures reach it is the
 * poll's own decision.
 */
async function providerFailureLooks(
  page: Page,
  chat: string,
  arriving: string,
  options?: { watchForProviderFailure?: boolean }
): Promise<number> {
  await showConversation(page, chat);
  const actions = new ChatPaneActions(page, new ConsolePaneActions(page));
  await expect(actions.chatPane.messageItem.first()).toBeVisible();

  let looks = 0;
  actions.skipIfProviderFailed = async () => {
    const failed = providerFailure(await actions.chatPane.lastMessageText()) !== null;
    if (failed) {
      looks++;
    }
    return failed;
  };

  let rounds = 0;
  await actions.pollWithAllowDialogs(
    async () => {
      rounds++;
      if (rounds === 1 && arriving !== '') {
        await actions.chatPane.frame.locator('body').evaluate(
          (body, html) => body.insertAdjacentHTML('beforeend', html),
          arriving
        );
      }
      return rounds === 2;
    },
    30000,
    undefined,
    options
  );
  return looks;
}

test('a poll ignores a failure that was showing when it began', async ({ page }) => {
  expect(await providerFailureLooks(page, USER_MESSAGE + failedReply('m1'), '')).toBe(0);
});

test('a poll that began on an old failure still watches the turn after it', async ({ page }) => {
  // The poll starts before the new turn's messages have rendered.
  const looks = await providerFailureLooks(
    page,
    USER_MESSAGE + failedReply('m1'),
    USER_MESSAGE + failedReply('m2')
  );
  expect(looks).toBe(1);
});

test('a poll watches for a failure unless told not to', async ({ page }) => {
  expect(await providerFailureLooks(page, USER_MESSAGE, failedReply('m1'))).toBe(1);

  const unwatched = await providerFailureLooks(
    page,
    USER_MESSAGE,
    failedReply('m1'),
    { watchForProviderFailure: false }
  );
  expect(unwatched).toBe(0);
});
