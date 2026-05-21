import { test, expect } from '@fixtures/rstudio.fixture';
import { sleep, CHAT_PROVIDERS } from '@utils/constants';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { AssistantOptionsActions } from '@actions/assistant_options.actions';
import { AssistantOptions } from '@pages/assistant_options.page';
import type { EnvironmentVersions } from '@pages/console_pane.page';
import { executeCommand } from '@utils/commands';
import { createChatActions, annotateVersions } from './_chat-setup';

test.describe.serial('Enable Posit Assistant', { tag: ['@ai'] }, () => {
  let consoleActions: ConsolePaneActions;
  let assistantActions: AssistantOptionsActions;
  let assistantOptions: AssistantOptions;
  let versions: EnvironmentVersions;

  test.beforeAll(async ({ rstudioPage: page }) => {
    ({ consoleActions, assistantActions } = createChatActions(page));
    assistantOptions = assistantActions.assistantOptions;

    versions = await consoleActions.getEnvironmentVersions();
    await consoleActions.clearConsole();
  });

  test.beforeEach(async () => {
    annotateVersions(versions);
  });

  test('enable Posit Assistant and verify persistence', async ({ rstudioPage: page }) => {
    // First, set to "(None)" to ensure we're starting clean
    await executeCommand(page, 'showOptions');
    await page.waitForSelector('#rstudio_preferences_confirm', { timeout: 15000 });

    await expect(assistantOptions.assistantTab).toBeVisible({ timeout: 15000 });
    await assistantOptions.assistantTab.click();
    await expect(assistantOptions.assistantPanel).toBeVisible();
    await sleep(1000);

    await assistantOptions.chatProviderSelect.selectOption({ label: '(None)' });
    await sleep(1000);
    await assistantOptions.optionsOkButton.click();
    await expect(assistantOptions.optionsOkButton).toBeHidden({ timeout: 15000 });

    // Now enable Posit Assistant
    await assistantActions.setChatProvider(CHAT_PROVIDERS['posit-assistant']);

    // Reopen options and verify the selection persisted
    const value = await assistantActions.getChatProviderValue();
    expect(value).toBe('posit');
  });
});
