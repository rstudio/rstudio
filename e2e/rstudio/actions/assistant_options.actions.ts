import type { Page } from 'playwright';
import { expect } from '@playwright/test';
import { AssistantOptions } from '../pages/assistant_options.page';
import { ConsolePaneActions } from './console_pane.actions';
import { sleep } from '../utils/constants';
import { executeCommand } from '../utils/commands';

export class AssistantOptionsActions {
  readonly page: Page;
  readonly assistantOptions: AssistantOptions;
  readonly consolePaneActions: ConsolePaneActions;

  constructor(page: Page, consolePaneActions: ConsolePaneActions) {
    this.page = page;
    this.assistantOptions = new AssistantOptions(page);
    this.consolePaneActions = consolePaneActions;
  }

  /** Accept the "Update Posit Assistant" dialog if it appears, then dismiss "Installation Complete" */
  private async acceptUpdateDialog(): Promise<void> {
    // Caller already settled (selectOption + sleep), so an Update dialog --
    // if it's going to appear -- is rendered by now. isVisible() snapshot
    // returns instantly when absent.
    const updateBtn = this.page.locator('#rstudio_dlg_yes');
    if (!(await updateBtn.isVisible())) {
      return;
    }
    await updateBtn.click();
    console.log('Accepted Posit Assistant update dialog');

    // Wait for "Installation Complete" dialog and dismiss it. This wait IS
    // legitimate -- the install runs asynchronously and the OK button only
    // appears once it completes.
    const installOkBtn = this.page.locator('#rstudio_dlg_ok');
    await installOkBtn.click({ timeout: 30000 });
    console.log('Dismissed Installation Complete dialog');
    await sleep(500);
  }

  async setupAssistantOptions(provider: string): Promise<void> {
    await executeCommand(this.page, 'showOptions');
    await this.page.waitForSelector('#rstudio_preferences_confirm', { timeout: 15000 });

    await expect(this.assistantOptions.assistantTab).toBeVisible({ timeout: 15000 });
    await this.assistantOptions.assistantTab.click();
    await expect(this.assistantOptions.assistantPanel).toBeVisible();
    await sleep(1000);

    console.log(`Configuring code assistant: ${provider}`);
    const options = await this.assistantOptions.codeAssistantSelect.locator('option').all();
    let matchedLabel: string | undefined;
    for (const option of options) {
      const label = await option.textContent();
      if (label?.startsWith(provider)) {
        matchedLabel = label;
        break;
      }
    }
    if (!matchedLabel) {
      throw new Error(`No code assistant option starting with "${provider}"`);
    }
    await this.assistantOptions.codeAssistantSelect.selectOption({ label: matchedLabel });
    await sleep(1000);
    await this.acceptUpdateDialog();

    await this.assistantOptions.showCodeSuggestionsSelect.selectOption({ label: 'Automatically' });
    await sleep(1000);

    if (!(await this.assistantOptions.enableNesCheckbox.isChecked())) {
      await this.assistantOptions.enableNesCheckbox.click();
    }
    await sleep(1000);

    await sleep(1000);
    await this.assistantOptions.optionsOkButton.click();
    await expect(this.assistantOptions.optionsOkButton).toBeHidden({ timeout: 15000 });
  }

  async setChatProvider(provider: string): Promise<void> {
    await executeCommand(this.page, 'showOptions');
    await this.page.waitForSelector('#rstudio_preferences_confirm', { timeout: 15000 });

    await expect(this.assistantOptions.assistantTab).toBeVisible({ timeout: 15000 });
    await this.assistantOptions.assistantTab.click();
    await expect(this.assistantOptions.assistantPanel).toBeVisible();
    await sleep(1000);

    console.log(`Setting chat provider: ${provider}`);
    await this.assistantOptions.chatProviderSelect.selectOption({ label: provider });
    await sleep(1000);
    await this.acceptUpdateDialog();

    // Update dialog may have closed Options; only click OK if still visible.
    // Snapshot isVisible() -- absence (Options already closed) is the common
    // case and we shouldn't burn extra time waiting for an element that's gone.
    if (await this.assistantOptions.optionsOkButton.isVisible()) {
      await this.assistantOptions.optionsOkButton.click();
      await expect(this.assistantOptions.optionsOkButton).toBeHidden({ timeout: 15000 });
    }

    // Toggle sidebar twice to refresh a potentially stale chat pane
    await executeCommand(this.page, 'toggleSidebar');
    await sleep(1000);
    await executeCommand(this.page, 'toggleSidebar');
    await sleep(1000);
    await this.consolePaneActions.clearConsole();
  }

  async getChatProviderValue(): Promise<string> {
    await executeCommand(this.page, 'showOptions');
    await this.page.waitForSelector('#rstudio_preferences_confirm', { timeout: 15000 });

    await expect(this.assistantOptions.assistantTab).toBeVisible({ timeout: 15000 });
    await this.assistantOptions.assistantTab.click();
    await expect(this.assistantOptions.assistantPanel).toBeVisible();
    await sleep(1000);
    await this.acceptUpdateDialog();

    const value = await this.assistantOptions.chatProviderSelect.inputValue();

    await this.assistantOptions.optionsOkButton.click();
    await expect(this.assistantOptions.optionsOkButton).toBeHidden({ timeout: 15000 });

    return value;
  }
}
