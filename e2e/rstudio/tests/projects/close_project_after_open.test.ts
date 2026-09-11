import { test, expect } from '@fixtures/rstudio.fixture';
import { createAndOpenProject, closeProjectIfOpen } from '@utils/project';
import { useSuiteSandbox } from '@utils/sandbox';
import { getPref } from '@utils/commands';

// Closing a project immediately after opening one must not prompt to save the
// workspace image when save_workspace is "never" (rstudio/rstudio#18784).
//
// The session reports its save action two ways: in the client_init SessionInfo
// payload, and asynchronously through save_action_changed. The client used to
// have only the event, defaulting to "ask" until one arrived, and the session's
// first event for a new session was the same "ask" placeholder. So a close
// issued before the first event batch was delivered prompted "Save workspace
// image to .../.RData?" -- with nothing to answer the modal, the close never
// completed and project.isActive() stayed true.
//
// This test leaves no gap between the open and the close: no console command
// runs in the new session, so nothing forces the R-side detect-changes pass
// that produced the corrected event. A regression fails inside
// closeProjectIfOpen, which blocks on project.isActive() === false.
test.describe('Close project immediately after open', { tag: ['@projects'] }, () => {
  const sandbox = useSuiteSandbox();

  test('closes without prompting to save the workspace', async ({ rstudioPage: page }) => {
    // Two session restarts, each of which can be slow on a loaded CI worker.
    test.setTimeout(180000);

    // The prompt this guards against is only wrong because the preference says
    // never (set in fixtures/base-prefs.jsonc). Assert it rather than assume
    // it: under "ask" the dialog is correct behaviour and the test would be
    // checking nothing.
    expect(
      await getPref(page, 'save_workspace'),
      'harness default save_workspace should be never',
    ).toBe('never');

    await createAndOpenProject(page, sandbox.dir, 'close_after_open');
    await closeProjectIfOpen(page);

    expect(
      await page.evaluate(() => window.rstudio?.project?.isActive()),
      'project should be closed',
    ).toBe(false);
  });
});
