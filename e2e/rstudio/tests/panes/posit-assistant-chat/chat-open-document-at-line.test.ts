import { test, expect } from '@fixtures/rstudio.fixture';
import { CHAT_PROVIDERS, sleep } from '@utils/constants';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { AssistantOptionsActions } from '@actions/assistant_options.actions';
import { ChatPaneActions } from '@actions/chat_pane.actions';
import { SourcePaneActions } from '@actions/source_pane.actions';

const HAMLET_LINES = [
  'What a piece of work is a man!',
  'How noble in reason!',
  'How infinite in faculty!',
  'In form and moving how',
  'Express and admirable!',
  'In action how like an angel!',
  'In apprehension how like a god!',
  'The beauty of the world!',
  'The paragon of animals!',
];

// Blocked on https://github.com/rstudio/rstudio/issues/17571 -- Posit Assistant
// generates file links whose href is empty (or unparseable), so clicking them
// opens a browser to "access denied" instead of opening the file at the linked
// line. Flip the four test.fixme calls back to test once the upstream fix
// (posit-dev/assistant#1283) is in a released Assistant build.
test.describe.serial('Chat opens documents at a specific line', { tag: ['@serial'] }, () => {
  let consoleActions: ConsolePaneActions;
  let chatActions: ChatPaneActions;
  let sourceActions: SourcePaneActions;
  const fileName = `hamlet_${Date.now()}.txt`;

  test.beforeAll(async ({ rstudioPage: page }) => {
    consoleActions = new ConsolePaneActions(page);
    const assistantActions = new AssistantOptionsActions(page, consoleActions);
    chatActions = new ChatPaneActions(page, consoleActions);
    sourceActions = new SourcePaneActions(page, consoleActions);

    await consoleActions.closeAllBuffersWithoutSaving();

    const numbered = HAMLET_LINES.map((l, i) => `${i + 1}: ${l}`);
    const rVector = 'c(' + numbered.map(s => `"${s}"`).join(', ') + ')';
    await consoleActions.typeInConsole(`writeLines(${rVector}, "~/${fileName}")`);
    await sleep(500);

    await assistantActions.setChatProvider(CHAT_PROVIDERS['posit-assistant']);
    await chatActions.openChatPane();
    await chatActions.dismissSetupPrompts();
  });

  test.afterAll(async () => {
    await consoleActions.closeAllBuffersWithoutSaving();
    await consoleActions.typeInConsole(`unlink("~/${fileName}")`);
    await sleep(500);
  });

  async function promptAndClickLink(prompt: string): Promise<void> {
    const before = await chatActions.chatPane.getMessageCount();
    await chatActions.sendChatMessage(prompt);
    await chatActions.waitForResponse(before);

    const link = chatActions.chatPane.messageItem.last().locator('a').first();
    await expect(link).toBeVisible({ timeout: 10000 });
    await expect(link).toHaveAttribute('href', /.+/, { timeout: 15000 });
    await link.click();
    await sleep(500);
  }

  test.fixme('link to a specific line opens the file at that line', async () => {
    await promptAndClickLink(`Give me a link that opens ${fileName} in line 5.`);

    await expect(sourceActions.sourcePane.selectedTab)
      .toContainText(fileName, { timeout: 30000 });

    const pos = await sourceActions.getActiveEditorCursor();
    expect(pos.row).toBe(4);
  });

  test.fixme('link to another line repositions the cursor in the open file', async () => {
    await sourceActions.goToTop();
    await promptAndClickLink(`Give me a link that opens ${fileName} in line 8.`);

    const pos = await sourceActions.getActiveEditorCursor();
    expect(pos.row).toBe(7);
  });

  test.fixme('link to the line containing a specific word opens at that line', async () => {
    await sourceActions.goToTop();
    await promptAndClickLink(
      `Give me a link that opens ${fileName} in the line containing the word "apprehension".`
    );

    const pos = await sourceActions.getActiveEditorCursor();
    expect(pos.row).toBe(6);
  });

  test.fixme('link to a different line repositions cursor again', async () => {
    await sourceActions.goToTop();
    await promptAndClickLink(`Give me a link that opens ${fileName} in line 3.`);

    const pos = await sourceActions.getActiveEditorCursor();
    expect(pos.row).toBe(2);
  });
});
