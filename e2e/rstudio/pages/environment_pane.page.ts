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

  // Toolbar buttons.
  public searchBar: Locator;
  public refreshWorkspaceBtn: Locator;
  public loadWorkspaceBtn: Locator;
  public saveWorkspaceBtn: Locator;
  public memoryPieBtn: Locator;

  // Toolbar menu buttons (each opens a dropdown menu).
  public importDatasetMenu: Locator;
  public viewMenu: Locator;
  public envListMenu: Locator;

  // Import Dataset menu items.
  public datasetTextBase: Locator;
  public datasetTextReadr: Locator;
  public datasetExcel: Locator;
  public datasetSpss: Locator;
  public datasetSas: Locator;
  public datasetStata: Locator;

  // Object-view menu items.
  public listViewOption: Locator;
  public gridViewOption: Locator;

  // Environment-list menu items.
  public globalEnvOption: Locator;
  public packageStats: Locator;
  public packageGraphics: Locator;
  public packageGrDevices: Locator;
  public packageUtils: Locator;
  public packageMethods: Locator;
  public packageBase: Locator;

  constructor(page: Page) {
    super(page);
    this.panel = page.locator(ENV_PANEL);
    this.tab = page.locator(ENV_TAB);
    this.clearWorkspaceBtn = page.locator(CLEAR_WORKSPACE_BTN);
    this.objectGridRows = this.panel.locator('table tr');

    this.searchBar = page.locator('#rstudio_sw_environment');
    this.refreshWorkspaceBtn = page.locator('#rstudio_tb_refreshenvironment');
    this.loadWorkspaceBtn = page.locator('#rstudio_tb_loadworkspace');
    this.saveWorkspaceBtn = page.locator('#rstudio_tb_saveworkspace');
    this.memoryPieBtn = page.locator('#rstudio_memory_dropdown');

    this.importDatasetMenu = page.locator('#rstudio_mb_import_dataset');
    this.viewMenu = page.locator('#rstudio_mb_object_list_view');
    this.envListMenu = page.locator('#rstudio_mb_environment_list');

    this.datasetTextBase = page.locator('#rstudio_label_from_text_base_command');
    this.datasetTextReadr = page.locator('#rstudio_label_from_text_readr_command');
    this.datasetExcel = page.locator('#rstudio_label_from_excel_command');
    this.datasetSpss = page.locator('#rstudio_label_from_spss_command');
    this.datasetSas = page.locator('#rstudio_label_from_sas_command');
    this.datasetStata = page.locator('#rstudio_label_from_stata_command');

    this.listViewOption = page.locator('#rstudio_label_list_command');
    this.gridViewOption = page.locator('#rstudio_label_grid_command');

    this.globalEnvOption = page.locator('#rstudio_label_global_environment_command');
    this.packageStats = page.locator('#rstudio_label_package_stats_command');
    this.packageGraphics = page.locator('#rstudio_label_package_graphics_command');
    this.packageGrDevices = page.locator('#rstudio_label_package_grdevices_command');
    this.packageUtils = page.locator('#rstudio_label_package_utils_command');
    this.packageMethods = page.locator('#rstudio_label_package_methods_command');
    this.packageBase = page.locator('#rstudio_label_package_base_command');
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

  /** Read the memory figure on the Environment pane's memory-pie button and
   *  normalize it to MiB. MemUsageWidget.formatBigMemory renders the text as
   *  "<n> MiB" or "<x.yy> GiB" (and "Memory" before the first usage sample
   *  arrives), so this returns null when no numeric value is present yet --
   *  callers can poll. Normalizing the unit avoids the cross-environment bug
   *  where stripping non-digits compares "1.2 GiB" (-> 12) against
   *  "850 MiB" (-> 850). */
  async getMemoryMiB(): Promise<number | null> {
    const text = (await this.memoryPieBtn.innerText()).trim();
    const match = text.match(/([\d.,]+)\s*(KiB|MiB|GiB|TiB)/i);
    if (!match) return null;
    const value = parseFloat(match[1].replace(/,/g, ''));
    if (Number.isNaN(value)) return null;
    const factorToMiB: Record<string, number> = {
      kib: 1 / 1024,
      mib: 1,
      gib: 1024,
      tib: 1024 * 1024,
    };
    return value * factorToMiB[match[2].toLowerCase()];
  }
}

// Backward-compatible action used by rmarkdown.test.ts.
export async function clearWorkspace(page: Page): Promise<void> {
  await page.locator(CLEAR_WORKSPACE_BTN).click();
  await sleep(500);
  await page.locator(CONFIRM_BTN).click();
  await sleep(1000);
}
