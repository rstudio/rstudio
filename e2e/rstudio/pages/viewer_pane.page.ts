import type { Page, FrameLocator } from 'playwright';

// Selectors
export const VIEWER_TAB = '#rstudio_workbench_tab_viewer';
export const VIEWER_FRAME = "iframe[title='Viewer Pane']";
export const PUBLISH_BTN_IN_PANEL = '#rstudio_publish_item_viewer';
export const CONTAINER = "[class*='container']";
export const CONTAINER_IMG = '.main-container img';
export const MAIN_CONTAINER = '.main-container';
export const QUARTO_CONTENT = '#quarto-content';

// Actions
export function switchToViewerFrame(page: Page): FrameLocator {
  return page.frameLocator(VIEWER_FRAME);
}
