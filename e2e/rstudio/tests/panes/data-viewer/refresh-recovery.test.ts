// Recovery from a failed in-place data viewer refresh -- e.g. a transient
// network failure or a busy/restarting session while the grid re-fetches its
// columns. Two invariants, both found in review:
//
// 1. A failed bootstrap must not strand the iframe's `bootstrapping` guard
//    flag: pre-fix it was only cleared at the end of a successful initGrid,
//    so after one failed refresh every column-navigation action was silently
//    swallowed (`if (bootstrapping) return;`) -- the go-to-column box
//    appeared dead until a full reload.
// 2. A bootstrap that succeeds after a failure must clear the error overlay:
//    pre-fix showError() was never undone, so the recovered grid rebuilt
//    underneath a stuck error mask.

import { test, expect } from '@fixtures/rstudio.fixture';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { SourcePane } from '@pages/source_pane.page';
import { DataViewerPane } from '@pages/data_viewer.page';
import { resetSourcePaneState } from '@utils/commands';
import { TIMEOUTS } from '@utils/constants';

const VIEWER_FRAME = '#rstudio_data_viewer_frame';

// The grid's data layer POSTs to .../grid_data with a urlencoded body; the
// column (metadata) fetch carries "show=cols" while row fetches carry
// "show=data". Match by regex per the Electron-CDP interception guidance
// (glob patterns can miss query strings).
const GRID_DATA = /\/grid_data/;

test.describe('Data Viewer refresh recovery', () => {
  let consoleActions: ConsolePaneActions;
  let sourcePane: SourcePane;
  let dataViewer: DataViewerPane;

  test.beforeAll(async ({ rstudioPage: page }) => {
    consoleActions = new ConsolePaneActions(page);
    sourcePane = new SourcePane(page);
    dataViewer = new DataViewerPane(page);
    await consoleActions.resetSourcePane();
  });

  test.afterEach(async ({ rstudioPage: page }) => {
    await resetSourcePaneState(page);
    await expect(sourcePane.selectedTab).toContainText('Untitled', { timeout: 5000 });
  });

  test('column pagination recovers after a transient refresh failure', async ({
    rstudioPage: page,
  }) => {
    // Wide enough to paginate (200 columns per page).
    await consoleActions.executeInConsole(
      '.rs.recovery_df <- as.data.frame(matrix(1:4000, nrow = 10, ncol = 400))',
      { wait: true },
    );
    await consoleActions.executeInConsole('View(.rs.recovery_df)');
    try {
      await expect(dataViewer.frame.locator('th[data-col-idx="1"]'))
        .toBeVisible({ timeout: TIMEOUTS.fileOpen });
      await expect(dataViewer.gotoColumnInput).toBeVisible();
      await expect(dataViewer.gridInfo)
        .toContainText('of 10 entries', { timeout: TIMEOUTS.fileOpen });

      // Fail the next column fetch; everything else passes through.
      // route.abort() (not fulfill) -- dev-mode Electron rewrites fulfilled
      // responses to status=0, but an abort is a plain network failure in
      // every mode, and any failure exercises the same showError path.
      let failNext = true;
      await page.route(GRID_DATA, async (route) => {
        if (failNext && (route.request().postData() ?? '').includes('show=cols')) {
          failNext = false;
          await route.abort('failed');
          return;
        }
        await route.continue();
      });

      try {
        // Trigger the in-place refresh; its column fetch fails and the
        // error overlay appears over the (destroyed) grid.
        await page.evaluate((sel: string) => {
          const f = document.querySelector(sel) as HTMLIFrameElement | null;
          const w = f?.contentWindow as unknown as { refreshData?: () => void } | undefined;
          if (!w?.refreshData) throw new Error('refreshData() not available on data viewer iframe');
          w.refreshData();
        }, VIEWER_FRAME);
        await expect(dataViewer.frame.locator('#errorWrapper'))
          .toBeVisible({ timeout: TIMEOUTS.fileOpen });
      } finally {
        await page.unroute(GRID_DATA);
      }

      // Column navigation must still respond. On a dead grid the box has no
      // matches to offer, so Enter falls back to a direct index jump, which
      // retries the bootstrap: the grid comes back and the error overlay
      // clears. Pre-fix the action was silently swallowed and the viewer
      // stayed an error mask until a full reload. (Type with raw key
      // presses and skip the suggestion wait -- there are none to wait for.)
      await dataViewer.gotoColumnInput.click();
      await dataViewer.gotoColumnInput.pressSequentially('201');
      await dataViewer.gotoColumnInput.press('Enter');
      await expect(dataViewer.frame.locator('th[data-col-idx="1"]'))
        .toBeVisible({ timeout: TIMEOUTS.fileOpen });
      await expect(dataViewer.frame.locator('#errorWrapper')).toBeHidden();
      await expect(dataViewer.gridInfo)
        .toContainText('of 10 entries', { timeout: TIMEOUTS.fileOpen });

      // With the grid recovered, the next jump lands normally.
      await dataViewer.goToColumn(201);
      await expect(dataViewer.columnHeader(201))
        .toBeVisible({ timeout: TIMEOUTS.fileOpen });
    } finally {
      await page.unroute(GRID_DATA).catch(() => { /* already removed */ });
      await consoleActions.executeInConsole('rm(".rs.recovery_df", envir = .GlobalEnv)');
    }
  });
});
