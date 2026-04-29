import { test, expect } from '@fixtures/rstudio.fixture';
import { CHAT_PROVIDERS } from '@utils/constants';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { AssistantOptionsActions } from '@actions/assistant_options.actions';
import { ChatPaneActions } from '@actions/chat_pane.actions';
import { ChatPane } from '@pages/chat_pane.page';
import type { EnvironmentVersions } from '@pages/console_pane.page';
import type { Response } from 'playwright';

test.describe('Chat WebSocket URL', () => {
  let consoleActions: ConsolePaneActions;
  let chatActions: ChatPaneActions;
  let chatPane: ChatPane;
  let versions: EnvironmentVersions;

  // Captured from the chat_get_backend_url / chat_get_backend_status RPC.
  let wsPath = '';

  test.beforeAll(async ({ rstudioPage: page }) => {
    consoleActions = new ConsolePaneActions(page);
    const assistantActions = new AssistantOptionsActions(page, consoleActions);
    chatActions = new ChatPaneActions(page, consoleActions);
    chatPane = chatActions.chatPane;

    versions = await consoleActions.getEnvironmentVersions();
    await consoleActions.clearConsole();

    // Listen for the RPC that delivers the WebSocket path to the frontend.
    // The GWT frontend calls chat_get_backend_status after starting the
    // backend; the "ready" response includes the url field built by
    // buildWebSocketUrl() -- the value that was broken in #17249.
    //
    // The listener must be registered before setChatProvider because
    // setting the provider triggers the backend startup and RPC flow.
    page.on('response', async (response: Response) => {
      try {
        if (!/\/rpc\/chat_get_backend_(url|status)/.test(response.url())) return;
        const body = await response.json();
        const url: string | undefined = body?.result?.url;
        if (url && url.includes('ai-chat') && !wsPath) {
          wsPath = url;
          console.log(`Captured WebSocket path from RPC: ${wsPath}`);
        }
      } catch {
        // Not JSON or body already consumed
      }
    });

    await assistantActions.setChatProvider(CHAT_PROVIDERS['posit-assistant']);
  });

  test.beforeEach(async () => {
    test.info().annotations.push(
      { type: 'R version', description: versions.r },
      { type: 'RStudio version', description: versions.rstudio },
    );
  });

  test('WebSocket URL includes correct path prefix', async ({ rstudioPage: page }) => {
    await chatActions.openChatPane();
    await chatActions.dismissSetupPrompts();

    // Chat root visible means the backend delivered the URL and the
    // iframe's WebSocket connected successfully.
    await expect(chatPane.chatRoot).toBeVisible({ timeout: 30000 });

    console.log(`WebSocket path: "${wsPath}"`);
    console.log(`Page URL: ${page.url()}`);

    // Verify we captured the URL from the RPC response
    expect(wsPath, 'RPC response should contain the ai-chat WebSocket path').toContain('/ai-chat');

    // On Server behind a subpath proxy (e.g., www-root-path=/rstudio),
    // the page URL path starts with the root prefix. The WebSocket path
    // must include the same prefix so cookies are scoped correctly.
    // See: https://github.com/rstudio/rstudio/issues/17249
    //
    // On Desktop or root-level Server, there's no prefix to check.
    const pageUrl = new URL(page.url());
    const pathSegments = pageUrl.pathname.split('/').filter(Boolean);

    // The first segment is the root path prefix unless it's a known
    // RStudio path segment ('s' for Workbench sessions, 'p' for port-mapped).
    if (pathSegments.length > 0 && !['s', 'p'].includes(pathSegments[0])) {
      const rootPrefix = '/' + pathSegments[0];
      expect(wsPath).toContain(rootPrefix);
      console.log(`Verified WebSocket path includes root prefix: ${rootPrefix}`);
    }

    // Prove the WebSocket actually works -- send a message and get a response.
    // If the URL is wrong, the WebSocket won't connect and this will fail.
    const initialCount = await chatPane.getMessageCount();
    await chatActions.sendChatMessage('Say hello in one word');
    const newCount = await chatActions.waitForResponse(initialCount);
    expect(newCount).toBeGreaterThan(initialCount);
    console.log(`Chat round-trip succeeded: ${initialCount} → ${newCount} messages`);
  });
});
