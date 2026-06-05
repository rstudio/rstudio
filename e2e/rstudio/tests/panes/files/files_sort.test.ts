// Files pane sort-order regression test (#17872).
//
// When the Files pane is sorted by a column and a file is added externally
// (created on disk and picked up by the file monitor), the new file should
// land in its sorted position. The bug appended it to the bottom of the list
// instead, because the incremental FileChange.ADD path in FilesList did not
// re-apply the active sort the way the full-directory load (displayFiles) does.

import * as fs from 'fs';
import * as path from 'path';

import type { Locator } from 'playwright';

import { test, expect } from '@fixtures/rstudio.fixture';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { executeCommand } from '@utils/commands';

// Vertical position of a name cell (LinkColumn renders <div title="name">),
// or null if it isn't currently rendered.
async function rowTop(filesPanel: Locator, name: string): Promise<number | null> {
  const box = await filesPanel.locator(`div[title="${name}"]`).boundingBox();
  return box ? box.y : null;
}

// True if mid sits strictly between lo and hi, regardless of sort direction.
function between(lo: number | null, mid: number | null, hi: number | null): boolean | null {
  if (lo === null || mid === null || hi === null)
    return null;
  return mid > Math.min(lo, hi) && mid < Math.max(lo, hi);
}

// The desktop and server fixtures redirect HOME to ${PW_SANDBOX}/user-home, so
// a path built here on the runner side resolves to the same directory R sees.
function sandboxedHome(): string {
  const sandbox = process.env.PW_SANDBOX;
  if (!sandbox) {
    throw new Error('PW_SANDBOX is not set; sandbox-setup.ts should populate it');
  }
  return path.join(sandbox, 'user-home');
}

// @desktop_only: the test creates files on the runner's filesystem and relies
// on the rsession's file monitor seeing them. Against a remote rsession the
// files would live on the runner, not the server, so the ADD event would never
// fire. @serial: it navigates the shared Files pane and mutates external state.
test.describe.serial('Files pane keeps sort order when a file is added (#17872)', { tag: ['@desktop_only', '@serial'] }, () => {
  let consoleActions: ConsolePaneActions;

  const dir = path.join(sandboxedHome(), `pw-files-sort-${Date.now()}`);
  // Seeded with distinct sizes whose order matches alphabetical order, so the
  // same "lands between its neighbors" positional check works for both the Name
  // comparator and the Size comparator (which uses a different code path:
  // FoldersOnBottomComparator plus the default-descending logic in FilesList).
  // Sizes in bytes: aaa=10, ccc=30, eee=50.
  const seeded: Record<string, number> = {
    'aaa.txt': 10,
    'ccc.txt': 30,
    'eee.txt': 50,
  };
  // "bbb.txt" (20B) is added under a Name sort and must land between aaa and ccc.
  // "ddd.txt" (40B) is added under a Size sort and must land between ccc and eee.
  const addedByName = 'bbb.txt';
  const addedBySize = 'ddd.txt';

  const sized = (bytes: number): string => 'x'.repeat(bytes);

  test.beforeAll(async ({ rstudioPage: page }) => {
    consoleActions = new ConsolePaneActions(page);

    fs.mkdirSync(dir, { recursive: true });
    for (const [name, bytes] of Object.entries(seeded))
      fs.writeFileSync(path.join(dir, name), sized(bytes));

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

  test('a file added under a Name sort lands in its sorted position', async ({ rstudioPage: page }) => {
    await executeCommand(page, 'activateFiles');

    const filesPanel = page.locator('#rstudio_workbench_panel_files');
    // The Name column header renders with role=button and accessible name
    // "Name"; clicking it toggles the Name sort. (Sort direction is conveyed
    // via CSS only, not the accessible name, so we verify sorting below by
    // comparing row positions rather than reading the header text.)
    const nameHeader = filesPanel.getByRole('button', { name: /Name/ });

    // Wait for the seeded files to render, then sort by Name (the repro step).
    for (const name of Object.keys(seeded))
      await expect(filesPanel.locator(`div[title="${name}"]`)).toBeVisible({ timeout: 15000 });
    await nameHeader.click();

    // Confirm a Name sort is active: the seeded files are monotonic by name.
    // The direction (ascending or descending) doesn't matter for this test.
    await expect.poll(async () => {
      return between(await rowTop(filesPanel, 'aaa.txt'),
                     await rowTop(filesPanel, 'ccc.txt'),
                     await rowTop(filesPanel, 'eee.txt'));
    }, { timeout: 15000 }).toBe(true);

    // Add the new file from outside the IDE.
    fs.writeFileSync(path.join(dir, addedByName), sized(20));
    await expect(filesPanel.locator(`div[title="${addedByName}"]`)).toBeVisible({ timeout: 15000 });

    // It must sit between aaa.txt and ccc.txt. With the bug it was appended to
    // the end of the list (below eee.txt) regardless of the sort direction.
    await expect.poll(async () => {
      return between(await rowTop(filesPanel, 'aaa.txt'),
                     await rowTop(filesPanel, addedByName),
                     await rowTop(filesPanel, 'ccc.txt'));
    }, { timeout: 15000 }).toBe(true);
  });

  test('a file added under a Size sort lands in its sorted position', async ({ rstudioPage: page }) => {
    await executeCommand(page, 'activateFiles');

    const filesPanel = page.locator('#rstudio_workbench_panel_files');
    const sizeHeader = filesPanel.getByRole('button', { name: /Size/ });

    // The previous test left aaa(10), bbb(20), ccc(30), eee(50) in place. Sort
    // by Size, which exercises a different comparator than Name.
    for (const name of ['aaa.txt', 'ccc.txt', 'eee.txt'])
      await expect(filesPanel.locator(`div[title="${name}"]`)).toBeVisible({ timeout: 15000 });
    await sizeHeader.click();

    // Confirm a Size sort is active. Because the seeded sizes increase with the
    // alphabetical order, aaa/ccc/eee remain monotonic by row position here too.
    await expect.poll(async () => {
      return between(await rowTop(filesPanel, 'aaa.txt'),
                     await rowTop(filesPanel, 'ccc.txt'),
                     await rowTop(filesPanel, 'eee.txt'));
    }, { timeout: 15000 }).toBe(true);

    // Add a 40-byte file from outside the IDE; by size it must land between
    // ccc.txt (30B) and eee.txt (50B), not at the bottom.
    fs.writeFileSync(path.join(dir, addedBySize), sized(40));
    await expect(filesPanel.locator(`div[title="${addedBySize}"]`)).toBeVisible({ timeout: 15000 });

    await expect.poll(async () => {
      return between(await rowTop(filesPanel, 'ccc.txt'),
                     await rowTop(filesPanel, addedBySize),
                     await rowTop(filesPanel, 'eee.txt'));
    }, { timeout: 15000 }).toBe(true);
  });
});
