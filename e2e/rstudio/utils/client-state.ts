// Helpers for observing client state persistence.
//
// Client state (see ClientState.java) is pushed to the server on a passive
// ~5s timer via the set_client_state RPC. Values persisted there -- notably
// the active source tab ("activeTabDocId", see #17944) -- are only as fresh
// as the last push, so a test that reloads the page right after changing
// such state races the timer and can restore a stale value.

import type { Page } from '@playwright/test';

// Start collecting set_client_state RPC bodies on the page. Register this
// before the state change of interest so a push that fires immediately
// afterwards can't be missed.
export function collectClientStatePushes(page: Page): string[] {
  const pushes: string[] = [];
  page.on('request', (request) => {
    if (request.url().includes('set_client_state'))
      pushes.push(request.postData() ?? '');
  });
  return pushes;
}

// Wait until a push carrying the persisted active document id (under the
// "activeTabDocId" client state key -- doc ids also appear in other client
// state structures, so matching the id alone is not enough) has gone out,
// so a subsequent reload restores it. Best-effort: if the push is never
// observed (e.g. the persisted value regressed to something other than the
// doc id), proceed anyway so the failure surfaces in the behavioral
// assertions that follow.
export async function waitForActiveDocIdPush(
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
      '[client-state] no set_client_state push carrying the active ' +
      'doc id was observed within 20s; proceeding anyway',
    );
  }
}
