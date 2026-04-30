import type { Page, Locator } from 'playwright';
import { PageObject } from './page_object_base_classes';
import { sleep } from '../utils/constants';
import { CONFIRM_BTN } from './modals.page';

export const ENV_PANEL = '#rstudio_workbench_panel_environment';
export const ENV_TAB = '#rstudio_workbench_tab_environment';
export const CLEAR_WORKSPACE_BTN = '#rstudio_tb_clearworkspace';

// The Environment pane's internal CSS classes (.objectGrid, .nameCol,
// .valueCol, .callFrame, .activeFrame, .tracebackHeader, etc.) come from
// GWT CssResource interfaces and are obfuscated in production builds —
// CssResource.style="pretty" is commented out in RStudio.gwt.xml. So
// queries scope by the stable workbench panel id and identify rows by
// structure (table rows) and content by visible text.
export class EnvironmentPane extends PageObject {
  public panel: Locator;
  public tab: Locator;
  public clearWorkspaceBtn: Locator;
  public objectGridRows: Locator;

  constructor(page: Page) {
    super(page);
    this.panel = page.locator(ENV_PANEL);
    this.tab = page.locator(ENV_TAB);
    this.clearWorkspaceBtn = page.locator(CLEAR_WORKSPACE_BTN);
    this.objectGridRows = this.panel.locator('table tr');
  }

  async getPanelText(): Promise<string> {
    return (await this.panel.innerText()).trim();
  }

  /** Returns true when the panel text contains both `name` and `value`. */
  async hasVariable(name: string, value: string): Promise<boolean> {
    const text = await this.getPanelText();
    return text.includes(name) && text.includes(value);
  }

  /** Locator for a call frame whose visible text matches `frameLabel`. */
  callFrameByText(frameLabel: string): Locator {
    return this.panel.getByText(frameLabel, { exact: false });
  }
}

// Backward-compatible action used by rmarkdown.test.ts.
export async function clearWorkspace(page: Page): Promise<void> {
  await page.locator(CLEAR_WORKSPACE_BTN).click();
  await sleep(500);
  await page.locator(CONFIRM_BTN).click();
  await sleep(1000);
}
