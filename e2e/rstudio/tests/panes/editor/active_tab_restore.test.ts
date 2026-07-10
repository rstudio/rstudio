// Regression test for https://github.com/rstudio/rstudio/issues/17944.
//
// The active source tab is persisted across reloads via client state. It used
// to be stored as a physical tab index, but documents are restored in
// persisted (relativeOrder / creation-time) order, which can differ from the
// in-session physical order -- e.g. a document opened mid-session is inserted
// immediately after the active tab, but restored at the end (its
// relativeOrder is 0 and it has the latest creation time). The fix persists
// the active document's ID instead (client state key "activeTabDocId") and
// resolves it to a tab after the documents are restored.

import { Page } from '@playwright/test';
import { test, expect } from '@fixtures/rstudio.fixture';
import { useSuiteSandbox } from '@utils/sandbox';
import { writeAndOpenFile, closeAndDeleteSandboxFiles } from '@utils/files';
import { documentOpen, executeCommand } from '@utils/commands';
import { TIMEOUTS } from '@utils/constants';
import * as path from 'path';

// Filename (basename) of the active source document, or null if none.
async function getActiveFilename(page: Page): Promise<string | null> {
  const active = await page.evaluate(() => window.rstudio?.documents.active()?.path ?? null);
  if (active === null) return null;
  const normalized = active.replace(/\\/g, '/');
  return normalized.slice(normalized.lastIndexOf('/') + 1);
}

// Client state is pushed on a passive ~5s timer; wait until a push carrying
// the persisted active document id (under the "activeTabDocId" client state
// key -- doc ids also appear in other client state structures, so matching
// the id alone is not enough) has gone out before reloading, otherwise the
// reload restores a stale value. Best-effort: if the push is never observed
// (e.g. the persisted value regressed to something other than the doc id),
// proceed to the reload anyway so the failure surfaces in the behavioral
// assertions that follow.
async function waitForActiveDocIdPush(
  page: Page,
  clientStatePushes: string[],
  docId: string,
): Promise<void> {
  // satellite source windows persist under "activeTabDocIdSourceWindow<n>"
  const pattern = new RegExp(`"activeTabDocId[^"]*"\\s*:\\s*"${docId}"`);

  const deadline = Date.now() + 20000;
  let pushed = false;
  while (!pushed && Date.now() < deadline) {
    pushed = clientStatePushes.some((body) => pattern.test(body));
    if (!pushed)
      await page.waitForTimeout(250);
  }
  if (!pushed) {
    console.warn(
      '[active_tab_restore] no set_client_state push carrying the active ' +
      'doc id was observed within 20s; reloading anyway',
    );
  }
}

async function reloadAndWaitForReady(page: Page): Promise<void> {
  await page.reload();
  await page.waitForFunction(() => window.rstudio?.ready === true, null, {
    timeout: TIMEOUTS.sessionRestart,
    polling: 50,
  });
}

test.describe('Active source tab restore across reload', () => {
  const sandbox = useSuiteSandbox();

  const stamp = Date.now();
  const fileA = `restore_a_${stamp}.R`;
  const fileB = `restore_b_${stamp}.R`;
  const fileC = `restore_c_${stamp}.R`;
  const fileD = `restore_d_${stamp}.R`;
  const fileE = `restore_e_${stamp}.R`;
  const fileF = `restore_f_${stamp}.R`;
  const fileG = `restore_g_${stamp}.R`;

  test.afterAll(async ({ rstudioPage: page }) => {
    await closeAndDeleteSandboxFiles(page, sandbox.dir, [
      fileA, fileB, fileC, fileD, fileE, fileF, fileG,
    ]);
  });

  test('restores the active tab by document id, not by tab position (#17944)', async ({
    rstudioPage: page,
  }) => {
    // Collect set_client_state RPC bodies so we can tell when the saved
    // active-tab value has actually reached the server. Registered up front
    // so a push that fires immediately after the last open can't be missed.
    const clientStatePushes: string[] = [];
    page.on('request', (request) => {
      if (request.url().includes('set_client_state'))
        clientStatePushes.push(request.postData() ?? '');
    });

    // Open A, B, C in order. None of them are drag-reordered, so they all
    // keep relativeOrder == 0 and a reload restores them in creation order.
    for (const file of [fileA, fileB, fileC])
      await writeAndOpenFile(page, sandbox.dir, file, `# ${file}`);

    // Re-select A, then open D. New tabs are inserted immediately after the
    // current tab, so the physical order is now [.., A, D, B, C] with D
    // active -- while the restored order will be [.., A, B, C, D].
    await documentOpen(page, path.join(sandbox.dir, fileA));
    await writeAndOpenFile(page, sandbox.dir, fileD, `# ${fileD}`);

    const docId = await page.evaluate(() => window.rstudio?.documents.active()?.id ?? null);
    expect(docId).not.toBeNull();

    await waitForActiveDocIdPush(page, clientStatePushes, docId as string);

    await reloadAndWaitForReady(page);

    // Before the fix the restored selection was positional: the saved index
    // pointed at D's pre-reload physical position, which lands on B in the
    // restored order. With the fix the active tab is resolved by document id.
    await expect
      .poll(() => getActiveFilename(page), { timeout: TIMEOUTS.fileOpen })
      .toBe(fileD);
  });

  test('falls back to a sane active tab when the persisted document was closed (#17944)', async ({
    rstudioPage: page,
  }) => {
    const clientStatePushes: string[] = [];
    page.on('request', (request) => {
      if (request.url().includes('set_client_state'))
        clientStatePushes.push(request.postData() ?? '');
    });

    // Open E, F, G; G is active.
    for (const file of [fileE, fileF, fileG])
      await writeAndOpenFile(page, sandbox.dir, file, `# ${file}`);

    const docId = await page.evaluate(() => window.rstudio?.documents.active()?.id ?? null);
    expect(docId).not.toBeNull();

    // Make sure G's id has been persisted, then close G so the persisted
    // active document no longer exists at restore time. Wait for the
    // close_document RPC to complete so the document is gone server-side
    // before the reload. (A later timer push may legitimately re-persist the
    // new active document before the reload; both outcomes are accepted
    // below.)
    await waitForActiveDocIdPush(page, clientStatePushes, docId as string);

    const closed = page.waitForResponse((response) => response.url().includes('close_document'));
    await executeCommand(page, 'closeSourceDoc');
    await closed;

    await reloadAndWaitForReady(page);

    // The persisted active document may not resolve to a tab; the source
    // pane must still land on some open document (and not crash, and not
    // somehow resurrect the closed one).
    await expect
      .poll(() => getActiveFilename(page), { timeout: TIMEOUTS.fileOpen })
      .not.toBeNull();
    expect(await getActiveFilename(page)).not.toBe(fileG);
  });
});
