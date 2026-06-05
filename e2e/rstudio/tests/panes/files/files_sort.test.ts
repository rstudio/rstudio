// Files pane sort-order regression test (#17872).
//
// When the Files pane is sorted by a column and a file is added externally
// (created on disk and picked up by the file monitor), the new file should
// land in its sorted position. The bug appended it to the bottom of the list
// instead, because the incremental FileChange.ADD path in FilesList did not
// re-apply the active sort the way the full-directory load (displayFiles) does.

import * as fs from 'fs';
import * as path from 'path';

import { test, expect } from '@fixtures/rstudio.fixture';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { executeCommand } from '@utils/commands';

// The desktop and server fixtures redirect HOME to ${PW_SANDBOX}/user-home, so
// a path built here on the runner side resolves to the same directory R sees.
function sandboxedHome(): string {
  const sandbox = process.env.PW_SANDBOX;
  if (!sandbox) {
    throw new Error('PW_SANDBOX is not set; sandbox-setup.ts should populate it');
  }
  return path.join(sandbox, 'user-home');
}

test.describe('Files pane keeps sort order when a file is added (#17872)', () => {
  let consoleActions: ConsolePaneActions;

  const dir = path.join(sandboxedHome(), `pw-files-sort-${Date.now()}`);
  // Seeded so the listing has a clear alphabetical span. "bbb.txt" is added
  // last (out of insertion order) and must sort between "aaa.txt" and "ccc.txt".
  const seeded = ['aaa.txt', 'ccc.txt', 'eee.txt'];
  const added = 'bbb.txt';

  test.beforeAll(async ({ rstudioPage: page }) => {
    consoleActions = new ConsolePaneActions(page);

    fs.mkdirSync(dir, { recursive: true });
    for (const name of seeded)
      fs.writeFileSync(path.join(dir, name), 'seed\n');

    // Point the Files pane at the test directory. This issues a list_files RPC
    // with monitor=true, arming the non-recursive watcher that turns the later
    // external file creation into a FileChange.ADD client event.
    await consoleActions.executeInConsole(
      `.rs.api.filesPaneNavigate(${JSON.stringify(dir)})`,
      { wait: true },
    );
  });

  test.afterAll(async ({ rstudioPage: page }) => {
    await consoleActions.executeInConsole(
      '.rs.api.filesPaneNavigate(path.expand("~"))',
      { wait: true },
    );
    fs.rmSync(dir, { recursive: true, force: true });
  });

  test('a file created externally lands in its sorted position', async ({ rstudioPage: page }) => {
    await executeCommand(page, 'activateFiles');

    const filesPanel = page.locator('#rstudio_workbench_panel_files');
    // The Name column header is a button whose accessible name carries the
    // current sort state (e.g. "Ascending sort Name" / "Descending sort Name").
    const nameHeader = filesPanel.getByRole('button', { name: /Name/ });

    // Vertical position of a name cell (LinkColumn renders <div title="name">),
    // or null if it isn't currently rendered.
    const rowTop = async (name: string): Promise<number | null> => {
      const box = await filesPanel.locator(`div[title="${name}"]`).boundingBox();
      return box ? box.y : null;
    };

    // Wait for the seeded files to render, then sort by Name (the repro step).
    for (const name of seeded)
      await expect(filesPanel.locator(`div[title="${name}"]`)).toBeVisible({ timeout: 15000 });
    await nameHeader.click();

    // Confirm a Name sort is active: the seeded files are monotonic by name.
    // The direction (ascending or descending) doesn't matter for this test.
    await expect.poll(async () => {
      const a = await rowTop('aaa.txt');
      const c = await rowTop('ccc.txt');
      const e = await rowTop('eee.txt');
      if (a === null || c === null || e === null)
        return null;
      return (a < c && c < e) || (a > c && c > e);
    }, { timeout: 15000 }).toBe(true);

    // Add the new file from outside the IDE.
    fs.writeFileSync(path.join(dir, added), 'added\n');
    await expect(filesPanel.locator(`div[title="${added}"]`)).toBeVisible({ timeout: 15000 });

    // It must sit between aaa.txt and ccc.txt. With the bug it was appended to
    // the end of the list (below eee.txt) regardless of the sort direction.
    await expect.poll(async () => {
      const a = await rowTop('aaa.txt');
      const b = await rowTop(added);
      const c = await rowTop('ccc.txt');
      if (a === null || b === null || c === null)
        return null;
      return b > Math.min(a, c) && b < Math.max(a, c);
    }, { timeout: 15000 }).toBe(true);
  });
});
