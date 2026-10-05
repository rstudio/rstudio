import type { Page, Locator } from 'playwright';
import { PageObject } from './page_object_base_classes';

export const PLOTS_TAB = '#rstudio_workbench_tab_plots';
export const PLOT_IMAGE_FRAME = '#rstudio_plot_image_frame';

export class PlotsPane extends PageObject {
  public tab: Locator;
  public plotImage: Locator;

  // Toolbar buttons
  public zoomPlotBtn: Locator;
  public removePlotBtn: Locator;
  public clearPlotsBtn: Locator;
  public refreshPlotBtn: Locator;
  public publishBtn: Locator;
  public nextPlotBtn: Locator;
  public previousPlotBtn: Locator;

  // Export menu button and its items
  public exportMenu: Locator;
  public saveAsImageItem: Locator;
  public saveAsPdfItem: Locator;
  public copyToClipboardItem: Locator;

  // Export dialogs (identified by stable aria-labels from the GWT ModalDialog ctors)
  public saveAsImageDialog: Locator;
  public saveAsPdfDialog: Locator;
  public copyToClipboardDialog: Locator;

  // Plot size menu button, its items, and the Fixed Plot Size dialog
  public sizeMenu: Locator;
  public fitToPaneItem: Locator;
  public fixedSizeItem: Locator;
  public fixedSizeDialog: Locator;
  public fixedSizeWidth: Locator;
  public fixedSizeHeight: Locator;
  public fixedSizeUnits: Locator;

  constructor(page: Page) {
    super(page);
    this.tab = page.locator(PLOTS_TAB);
    this.plotImage = page.locator(PLOT_IMAGE_FRAME);

    this.zoomPlotBtn = page.locator('#rstudio_tb_zoomplot');
    this.removePlotBtn = page.locator('#rstudio_tb_removeplot');
    this.clearPlotsBtn = page.locator('#rstudio_tb_clearplots');
    this.refreshPlotBtn = page.locator('#rstudio_tb_refreshplot');
    this.publishBtn = page.locator('#rstudio_publish_item_plots_pane');
    this.nextPlotBtn = page.locator('#rstudio_tb_nextplot');
    this.previousPlotBtn = page.locator('#rstudio_tb_previousplot');

    this.exportMenu = page.locator('#rstudio_mb_plots_export');
    this.saveAsImageItem = page.locator('#rstudio_label_save_as_image_command');
    this.saveAsPdfItem = page.locator('#rstudio_label_save_as_pdf_command');
    this.copyToClipboardItem = page.locator('#rstudio_label_copy_to_clipboard_command');

    // getByRole('dialog') excludes aria-hidden ghost dialogs that GWT keeps in
    // the DOM after closing, avoiding strict-mode violations from two matches.
    this.saveAsImageDialog = page.getByRole('dialog', { name: 'Save Plot as Image' });
    this.saveAsPdfDialog = page.getByRole('dialog', { name: 'Save Plot as PDF' });
    this.copyToClipboardDialog = page.getByRole('dialog', { name: 'Copy Plot to Clipboard' });

    this.sizeMenu = page.locator('#rstudio_mb_plots_size');
    this.fitToPaneItem = page.locator('#rstudio_label_fit_to_pane_command');
    this.fixedSizeItem = page.locator('#rstudio_label_fixed_size_command');
    this.fixedSizeDialog = page.getByRole('dialog', { name: 'Fixed Plot Size' });
    this.fixedSizeWidth = page.locator('#rstudio_fixed_plot_size_width');
    this.fixedSizeHeight = page.locator('#rstudio_fixed_plot_size_height');
    this.fixedSizeUnits = page.locator('#rstudio_fixed_plot_size_units');
  }
}
