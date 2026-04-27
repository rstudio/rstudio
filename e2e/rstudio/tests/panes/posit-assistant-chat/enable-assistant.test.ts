import { test, expect } from '@fixtures/rstudio.fixture';
import { sleep, CHAT_PROVIDERS } from '@utils/constants';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { AssistantOptionsActions } from '@actions/assistant_options.actions';
import { AssistantOptions } from '@pages/assistant_options.page';
import type { EnvironmentVersions } from '@pages/console_pane.page';

test.describe.serial('Enable Posit Assistant', () => {
  let consoleActions: ConsolePaneActions;
  let assistantActions: AssistantOptionsActions;
  let assistantOptions: AssistantOptions;
  let versions: EnvironmentVersions;

  test.beforeAll(async ({ rstudioPage: page }) => {
    consoleActions = new ConsolePaneActions(page);
    assistantActions = new AssistantOptionsActions(page, consoleActions);
    assistantOptions = assistantActions.assistantOptions;

    versions = await consoleActions.getEnvironmentVersions();
    await consoleActions.clearConsole();
  });

  test.beforeEach(async () => {
    test.info().annotations.push(
      { type: 'R version', description: versions.r },
      { type: 'RStudio version', description: versions.rstudio },
    );
  });

  test('enable Posit Assistant and verify persistence', async ({ rstudioPage: page }) => {
    // First, set to "(None)" to ensure we're starting clean
    await consoleActions.typeInConsole(".rs.api.executeCommand('showOptions')");
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
    console.log(`Chat provider value after re-open: "${value}"`);
  });
});
