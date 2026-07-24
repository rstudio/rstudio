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

  /** Accept the Posit Assistant install/update prompt if it is showing, then dismiss "Installation Complete" */
  private async acceptUpdateDialog(): Promise<void> {
    // Snapshot: acts only if the prompt is already rendered. Callers that need
    // to wait for it to appear use settleInstallPrompt().
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

  /**
   * Wait for the Posit Assistant install/update prompt to appear and complete
   * the install. Selecting a Posit provider kicks off an async check that pops
   * this prompt; the check can take several seconds (a network manifest fetch).
   * If the addon is already installed and current, no prompt appears and the
   * wait expires -- that's expected, so we just return. Settling the install
   * here keeps the prompt's modal from intercepting later configuration clicks.
   */
  private async settleInstallPrompt(): Promise<void> {
    const updateBtn = this.page.locator('#rstudio_dlg_yes');
    try {
      await updateBtn.waitFor({ state: 'visible', timeout: 30000 });
    } catch {
      return;
    }
    await this.acceptUpdateDialog();
  }

  /**
   * Confirm (OK) the Options dialog, retrying until it actually closes. While a
   * Posit Assistant install/update check is in flight, OK is blocked and pops a
   * "Checking for Updates" warning instead of closing (issue #18350); accept a
   * late-appearing install prompt and dismiss that warning, then retry.
   */
  private async confirmOptions(): Promise<void> {
    await expect(async () => {
      // A blocked OK leaves a "Checking for Updates" warning whose OK button is
      // #rstudio_dlg_ok. Dismiss it before accepting any install prompt, so it
      // cannot coexist with the install-complete dialog (same id).
      const warningOk = this.page.locator('#rstudio_dlg_ok');
      if (await warningOk.isVisible()) {
        await warningOk.click();
      }

      await this.acceptUpdateDialog();

      if (await this.assistantOptions.optionsOkButton.isVisible()) {
        await this.assistantOptions.optionsOkButton.click();
      }

      await expect(this.assistantOptions.optionsOkButton).toBeHidden({ timeout: 2000 });
    }).toPass({ timeout: 60000 });
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

    // Only Posit providers trigger the addon install/update check. Settle its
    // prompt before configuring the rest, so the modal can't block the controls
    // below (issue #18350).
    if (provider.startsWith('Posit')) {
      await this.settleInstallPrompt();
    }

    await this.assistantOptions.showCodeSuggestionsSelect.selectOption({ label: 'Automatically' });
    await sleep(1000);

    if (!(await this.assistantOptions.enableNesCheckbox.isChecked())) {
      await this.assistantOptions.enableNesCheckbox.click();
    }
    await sleep(1000);

    await this.confirmOptions();
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

    // A Posit chat provider triggers the addon install/update check; settle its
    // prompt before confirming so OK is not blocked (issue #18350).
    if (provider.startsWith('Posit')) {
      await this.settleInstallPrompt();
    }
    await this.confirmOptions();

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

    const value = await this.assistantOptions.chatProviderSelect.inputValue();

    await this.confirmOptions();

    return value;
  }
}
