// Regression test for https://github.com/rstudio/rstudio/issues/17608.
//
// Focusing a source document used to fire the assistant_doc_focused RPC: a
// synchronous handler that ran on the session main thread, read the full
// document contents from disk, and could attempt to launch the Copilot agent
// inline (busy-waiting up to ~10 seconds per focus while a launch was pending
// or failing). Under storage or CPU pressure that RPC could stall the
// session's serial RPC queue long enough to make the whole IDE feel hung.
//
// Document focus is now delivered via lsp_doc_focused: an async RPC that is
// acknowledged immediately (asyncHandle) and fans out to interested modules
// (e.g. the assistant) through the in-session LSP didFocus signal. This test
// pins down that contract: the RPC fires on tab focus and is registered
// async, so the client never waits on the handler's work.

import { test, expect } from '@fixtures/rstudio.fixture';
import { useSuiteSandbox } from '@utils/sandbox';
import { writeAndOpenFile, closeAndDeleteSandboxFiles } from '@utils/files';
import { documentOpen } from '@utils/commands';
import * as path from 'path';

test.describe('Document focus notification', () => {
  const sandbox = useSuiteSandbox();

  const stamp = Date.now();
  const fileA = `focus_a_${stamp}.R`;
  const fileB = `focus_b_${stamp}.R`;

  test.afterAll(async ({ rstudioPage: page }) => {
    await closeAndDeleteSandboxFiles(page, sandbox.dir, [fileA, fileB]);
  });

  test('focusing a tab sends lsp_doc_focused and gets an async ack (#17608)', async ({
    rstudioPage: page,
  }) => {
    await writeAndOpenFile(page, sandbox.dir, fileA, `# ${fileA}`);
    await writeAndOpenFile(page, sandbox.dir, fileB, `# ${fileB}`);

    // Re-focusing A fires DocFocusedEvent -> lsp_doc_focused.
    const focusResponse = page.waitForResponse((response) =>
      response.url().includes('/rpc/lsp_doc_focused'),
    );
    await documentOpen(page, path.join(sandbox.dir, fileA));

    const response = await focusResponse;
    expect(response.status()).toBe(200);

    // The handler is registered with registerAsyncRpcMethod: the session
    // acknowledges with an asyncHandle before doing any work, so a slow
    // focus notification never holds the HTTP connection (the result is
    // delivered later as a client event). If the method were missing or
    // registered synchronously, the response would carry an error or a
    // result instead of the handle.
    const body = await response.json();
    expect(body.asyncHandle).toBeTruthy();
    expect(body.error).toBeUndefined();
  });
});
