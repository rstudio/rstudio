// Files pane file-monitor integration test (#17669).
//
// The Files pane installs a non-recursive watch on the directory it is
// showing (FilesListingMonitor in SessionFiles.cpp). A file created or
// removed outside the IDE should propagate through file_monitor and
// surface in the pane without an explicit refresh.
//
// Per-platform backends exercised: macOS uses FSEvents with
// kFSEventStreamCreateFlagFileEvents (the path this PR introduces);
// Linux uses inotify; Windows uses ReadDirectoryChangesW.

import { test, expect } from '@fixtures/rstudio.fixture';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { executeCommand } from '@utils/commands';
import { rStringLiteral } from '@utils/r';

function uniqueName(prefix: string): string {
  return `${prefix}-${Date.now()}-${Math.random().toString(36).slice(2, 8)}.txt`;
}

test.describe('Files pane reflects file events in ~', () => {
  let consoleActions: ConsolePaneActions;

  test.beforeAll(async ({ rstudioPage: page }) => {
    consoleActions = new ConsolePaneActions(page);
    // Navigate the Files pane to ~ once per worker. This triggers a
    // list_files RPC with monitor=true, which arms a non-recursive watcher
    // rooted there -- the watcher whose macOS path this PR changes.
    await consoleActions.executeInConsole(
      '.rs.api.filesPaneNavigate(path.expand("~"))',
      { wait: true },
    );
  });

  test.beforeEach(async ({ rstudioPage: page }) => {
    // The shared per-test reset (resetForNextTest) ends by activating the
    // console; bring the Files pane back to front so the virtualized grid
    // actually renders rows we then assert against.
    await executeCommand(page, 'activateFiles');
  });

  test('files created in and removed from ~ are reflected in the pane', async ({ rstudioPage: page }) => {
    const fileName = uniqueName('pw-file-monitor');
    // The name column renders each entry as <div title="filename">filename</div>
    // (LinkColumn.java). Waiting on this locator covers the whole roundtrip:
    // file_monitor -> FilesListingMonitor -> client event -> FilesPresenter
    // -> data provider update -> grid render.
    const row = page.locator(`div[title="${fileName}"]`);
    const fileNameLit = rStringLiteral(fileName);

    // Create / remove the file via R rather than Node fs. R's path.expand("~")
    // matches the directory the Files pane is monitoring on every mode --
    // Desktop, locally-spawned Server (HOME redirected to ${PW_SANDBOX}/user-home
    // by the fixtures), AND externally-hosted Server (HOME = /home/$user, as
    // in CI where rsession runs under PW_RSTUDIO_SERVER_USER). Using Node fs
    // would write to the runner's sandbox path instead of rsession's HOME on
    // the last case, and the file would never show up in the pane.
    await consoleActions.executeInConsole(
      `writeLines("created", file.path(path.expand("~"), ${fileNameLit}))`,
      { wait: true },
    );
    try {
      await expect(row).toBeVisible({ timeout: 15000 });
    } catch (err) {
      await consoleActions.executeInConsole(
        `unlink(file.path(path.expand("~"), ${fileNameLit}))`,
        { wait: true },
      );
      throw err;
    }

    await consoleActions.executeInConsole(
      `unlink(file.path(path.expand("~"), ${fileNameLit}))`,
      { wait: true },
    );
    await expect(row).toHaveCount(0, { timeout: 15000 });
  });
});
