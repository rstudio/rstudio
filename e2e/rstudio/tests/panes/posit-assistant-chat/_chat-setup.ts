import { test } from '@fixtures/rstudio.fixture';
import { CHAT_PROVIDERS } from '@utils/constants';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { AssistantOptionsActions } from '@actions/assistant_options.actions';
import { ChatPaneActions } from '@actions/chat_pane.actions';
import { ChatPane } from '@pages/chat_pane.page';
import type { EnvironmentVersions } from '@pages/console_pane.page';
import type { Page } from 'playwright';

export interface ChatTestActions {
  consoleActions: ConsolePaneActions;
  assistantActions: AssistantOptionsActions;
  chatActions: ChatPaneActions;
  chatPane: ChatPane;
}

// Construct the action objects used across chat tests. No side effects --
// safe to call again to refresh references after a session restart.
export function createChatActions(page: Page): ChatTestActions {
  const consoleActions = new ConsolePaneActions(page);
  const assistantActions = new AssistantOptionsActions(page, consoleActions);
  const chatActions = new ChatPaneActions(page, consoleActions);
  return { consoleActions, assistantActions, chatActions, chatPane: chatActions.chatPane };
}

// Standard setup for tests that need a working Posit Assistant chat pane:
// capture R / RStudio versions, clear the console, select Posit Assistant,
// open the chat pane, and dismiss any setup prompts. Call from beforeAll.
export async function setupPositAssistantChat(
  page: Page,
): Promise<ChatTestActions & { versions: EnvironmentVersions }> {
  const actions = createChatActions(page);
  const versions = await actions.consoleActions.getEnvironmentVersions();
  await actions.consoleActions.clearConsole();
  await actions.assistantActions.setChatProvider(CHAT_PROVIDERS['posit-assistant']);
  await actions.chatActions.openChatPane();
  await actions.chatActions.dismissSetupPrompts();
  return { ...actions, versions };
}

// Push the standard R / RStudio version annotations onto the current test.
// Call from beforeEach.
export function annotateVersions(versions: EnvironmentVersions): void {
  test.info().annotations.push(
    { type: 'R version', description: versions.r },
    { type: 'RStudio version', description: versions.rstudio },
  );
}
