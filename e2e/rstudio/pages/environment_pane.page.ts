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

  /** Returns true when a single object-grid row contains both `name` and
   *  `value`. Row-scoped matching avoids false positives where `name`
   *  appears in one row (e.g. as another variable's value) and `value`
   *  appears in a different row. */
  async hasVariable(name: string, value: string): Promise<boolean> {
    const rowCount = await this.objectGridRows.count();
    for (let i = 0; i < rowCount; i++) {
      const rowText = await this.objectGridRows.nth(i).innerText();
      if (rowText.includes(name) && rowText.includes(value)) {
        return true;
      }
    }
    return false;
  }

  /** Locator for a call frame whose visible text matches `frameLabel`.
   *  Pass `exact = true` to require an exact text match; the default is
   *  substring matching. The locator is scoped to the Environment panel,
   *  but the panel renders both the call-frame list and the locals grid —
   *  pass `exact = true` (or use `.first()` on the result) when the label
   *  could also appear as a local variable name. The traceback region's
   *  CSS classes are GWT-obfuscated, which is why scoping happens at the
   *  whole-panel level rather than the call-frame region itself. */
  callFrameByText(frameLabel: string, exact = false): Locator {
    return this.panel.getByText(frameLabel, { exact });
  }
}

// Backward-compatible action used by rmarkdown.test.ts.
export async function clearWorkspace(page: Page): Promise<void> {
  await page.locator(CLEAR_WORKSPACE_BTN).click();
  await sleep(500);
  await page.locator(CONFIRM_BTN).click();
  await sleep(1000);
}
