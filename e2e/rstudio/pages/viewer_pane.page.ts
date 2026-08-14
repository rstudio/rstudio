import type { Page, FrameLocator } from 'playwright';

// Selectors
export const VIEWER_TAB = '#rstudio_workbench_tab_viewer';
export const VIEWER_FRAME = "iframe[title='Viewer Pane']";
export const PUBLISH_BTN_IN_PANEL = '#rstudio_publish_item_viewer';
export const CONTAINER = "[class*='container']";
export const CONTAINER_IMG = '.main-container img';
export const MAIN_CONTAINER = '.main-container';
export const QUARTO_CONTENT = '#quarto-content';

// Console command asking the Viewer to show a page at maximized height, the
// one live producer of EnsureHeightEvent.MAXIMIZED (what an R Notebook preview
// requests). Serving a file from the session tempdir is the documented viewer
// path that works on Desktop and Server alike; a fresh temp file per call keeps
// repeated requests from being suppressed as a re-show of the same URL.
export const VIEWER_MAXIMIZE_R =
  'f <- tempfile(fileext = ".html"); ' +
  'writeLines("<h1>hi</h1>", f); ' +
  '.rs.api.viewer(f, height = "maximize")';

// Actions
export function switchToViewerFrame(page: Page): FrameLocator {
  return page.frameLocator(VIEWER_FRAME);
}
