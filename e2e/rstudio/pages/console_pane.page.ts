import type { Page, Locator } from 'playwright';
import { PageObject } from './page_object_base_classes';
import { sleep } from '../utils/constants';

// ---------------------------------------------------------------------------
// Class-based page object
// ---------------------------------------------------------------------------

export class ConsolePane extends PageObject {
  public consoleInput: Locator;
  public consoleTab: Locator;
  public consoleOutput: Locator;
  public interruptRBtn: Locator;
  public tracebackBtn: Locator;
  public stackTrace: Locator;
  public findBar: Locator;
  public findInput: Locator;
  public findNext: Locator;
  public findClose: Locator;
  public findBtn: Locator;
  public findCaseSensitive: Locator;

  constructor(page: Page) {
    super(page);
    this.consoleInput = page.locator('#rstudio_console_input .ace_text-input');
    this.consoleTab = page.locator('#rstudio_workbench_tab_console');
    this.consoleOutput = page.locator('#rstudio_workbench_panel_console');
    this.interruptRBtn = page.locator("[id^='rstudio_tb_interruptr']");
    this.tracebackBtn = page.locator("[class*='show_traceback_text']");
    this.stackTrace = page.locator("[class*='stack_trace']");

    // Find in Console: #rstudio_find_replace_bar is the inner panel;
    // the Close button is a sibling at the shelf level, so scope it at the console panel.
    const consolePanel = page.locator('#rstudio_workbench_panel_console');
    this.findBar = consolePanel.locator('#rstudio_find_replace_bar');
    this.findInput = this.findBar.locator('input[type="text"]');
    this.findNext = this.findBar.getByRole('button', { name: 'Next' });
    this.findClose = consolePanel.getByRole('button', { name: 'Close' }).first();
    this.findBtn = consolePanel.locator('button[aria-label="Find in Console"]').first();
    this.findCaseSensitive = this.findBar.getByRole('checkbox', { name: 'Case sensitive' });
  }

  async consoleInputValue(): Promise<string> {
    return this.page.evaluate(() => {
      const el = document.getElementById('rstudio_console_input') as
        | (HTMLElement & { env?: { editor?: { getValue(): string } } })
        | null;
      return el?.env?.editor?.getValue() ?? '';
    });
  }
}

// ---------------------------------------------------------------------------
// Types
// ---------------------------------------------------------------------------

export interface EnvironmentVersions {
  r: string;
  rstudio: string;
}

// ---------------------------------------------------------------------------
// Backward-compatible exports (used by other tests & desktop.fixture.ts)
// ---------------------------------------------------------------------------

export const CONSOLE_INPUT = '#rstudio_console_input .ace_text-input';
export const CONSOLE_TAB = '#rstudio_workbench_tab_console';
export const CONSOLE_OUTPUT = '#rstudio_workbench_panel_console';
export const INTERRUPT_R_BTN = "[id^='rstudio_tb_interruptr']";

export async function typeInConsole(page: Page, command: string): Promise<void> {
  await page.locator(CONSOLE_TAB).click();
  await page.locator(CONSOLE_INPUT).click({ force: true });
  await sleep(500);
  await page.locator(CONSOLE_INPUT).pressSequentially(command);
  await sleep(200);
  if (await page.locator('#rstudio_popup_completions').isVisible()) {
    await page.keyboard.press('Escape');
    await sleep(100);
  }
  await page.locator(CONSOLE_INPUT).press('Enter');
}

export async function clearConsole(page: Page): Promise<void> {
  await page.locator(CONSOLE_TAB).click();
  await page.locator(CONSOLE_INPUT).click({ force: true });
  await sleep(200);
  await page.keyboard.press('Control+l');
  await sleep(500);
}

export async function closeAllBuffersWithoutSaving(page: Page): Promise<void> {
  await typeInConsole(page, '.rs.api.closeAllSourceBuffersWithoutSaving()');
  await sleep(1000);
}

export async function getEnvironmentVersions(page: Page): Promise<EnvironmentVersions> {
  await typeInConsole(page, 'cat("R:", R.version.string, "\\nRStudio:", RStudio.Version()$long_version)');
  await sleep(2000);

  const output = await page.locator(CONSOLE_OUTPUT).innerText();
  const rMatch = output.match(/R:\s*(R version [\d.]+[^\n]*)/);
  const rstudioMatch = output.match(/RStudio:\s*([\d.+]+)/);

  return {
    r: rMatch?.[1] ?? 'unknown',
    rstudio: rstudioMatch?.[1] ?? 'unknown',
  };
}

export async function goToLine(page: Page, line: number): Promise<void> {
  await typeInConsole(page, `.rs.api.executeCommand('goToLine')`);
  await sleep(500);
  await page.keyboard.type(String(line));
  await page.keyboard.press('Enter');
  await sleep(500);
}

