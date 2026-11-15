import { test, expect } from '@fixtures/a11y.fixture';
import { GLOBAL_OPTIONS_DIALOG } from '@pages/global_options.page';
import { scanOptionsDialog } from '@utils/a11y-options-dialog';
import { executeCommand } from '@utils/commands';

/**
 * The initial state with Tools > Global Options open, one state per pane, and
 * per tab of a pane with a tab control (e.g. global-options-code-display).
 * The dialog is cancelled before the assertions, so no option changes.
 */
test('global options dialog has no accessibility violations', { tag: ['@a11y', '@server_only'] },
  async ({ a11yPage: page }, testInfo) => {
    const violations = await scanOptionsDialog(page, testInfo, GLOBAL_OPTIONS_DIALOG,
      () => executeCommand(page, 'showOptions'));
    for (const [state, summary] of violations)
      expect.soft(summary, state).toEqual([]);
  });
