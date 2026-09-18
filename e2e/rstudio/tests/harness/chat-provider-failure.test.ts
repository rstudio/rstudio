import { test, expect } from '@playwright/test';
import { providerFailure } from '@actions/chat_pane.actions';
import { ChatPane } from '@pages/chat_pane.page';

// Harness self-test for providerFailure, which decides whether a chat turn
// that ended in an error was Posit AI failing on its side (skip) or something
// a product change could cause (fail). The strings are Posit Assistant's own
// (databot's formatApiError), and the DOM below is the shape it rendered in
// run 34994843398.

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

test('reads the failure from the rendered chat message', async ({ page }) => {
  // The error detail renders as a code span inside the reply's content div.
  const chat = `
    <div data-testid="chat-message-user"><p>Who is the Norse god of mischief?</p></div>
    <div class="chat-message-assistant">
      <div class="message-content relative" data-message-id="m1">
        <div class="space-y-4"><p>Error making request: <code>read ETIMEDOUT</code></p></div>
      </div>
    </div>`;
  await page.setContent(`<iframe title="Posit Assistant" srcdoc="${chat.replace(/"/g, '&quot;')}"></iframe>`);

  const chatPane = new ChatPane(page);
  await expect(chatPane.messageItem).toHaveCount(2);

  const text = await chatPane.lastMessageText();
  expect(providerFailure(text)).toEqual({ message: 'Error making request: read ETIMEDOUT', side: 'network' });
});
