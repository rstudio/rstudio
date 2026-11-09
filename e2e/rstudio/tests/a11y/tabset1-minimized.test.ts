import { test, expect } from '@fixtures/a11y.fixture';
import type { Page, Request } from '@playwright/test';
import type { AxeResults } from 'axe-core';
import { scanA11y, violationSummary } from '@utils/a11y';
import { TIMEOUTS } from '@utils/constants';

// Minimizing a pane only schedules a passive save, 5 s out (ClientStateUpdater).
const LAYOUT_SAVE_TIMEOUT = 15_000;

/**
 * Whether a request saves TabSet1's minimized/normal state, `state`. The
 * window layout is project-persistent client state (the third
 * set_client_state parameter) that outlives the session -- see
 * DualWindowLayoutPanel.WindowLayoutStateValue.
 */
function isLayoutSave(r: Request, state: 'MINIMIZE' | 'NORMAL'): boolean {
  if (!r.url().includes('set_client_state'))
    return false;
  const layout = r.postDataJSON()?.params?.[2]?.windowlayoutstate;
  return layout !== undefined && JSON.stringify(layout).includes(`"topwindowstate":"${state}"`);
}

async function setTabSet1State(page: Page, button: string, state: 'MINIMIZE' | 'NORMAL'): Promise<void> {
  const save = page.waitForRequest((r) => isLayoutSave(r, state), { timeout: LAYOUT_SAVE_TIMEOUT });
  await page.getByRole('button', { name: button, exact: true }).click({ timeout: TIMEOUTS.action });
  const response = await (await save).response();
  // A rejected RPC still answers HTTP 200, with an error member.
  expect(await response?.text(), `saving TabSet1 as ${state}`).not.toContain('"error"');
}

/**
 * The initial state with the Environment pane (TabSet1) minimized to its
 * strip of tabs. The minimized state outlives the session, so the pane is
 * restored, and the save waited for, before the assertion.
 */
test('minimized TabSet1 has no accessibility violations', { tag: ['@a11y', '@server_only'] },
  async ({ a11yPage: page }, testInfo) => {
    const restoreButton = page.getByRole('button', { name: 'Restore TabSet1', exact: true });
    let results: AxeResults;
    try {
      await setTabSet1State(page, 'Minimize TabSet1', 'MINIMIZE');
      const tablist = page.getByRole('region', { name: 'TabSet1 minimized', exact: true })
        .getByRole('tablist', { name: 'TabSet1', exact: true });
      await expect(tablist.getByRole('tab', { name: 'Environment', exact: true }))
        .toBeVisible({ timeout: TIMEOUTS.consoleReady });
      results = await scanA11y(page, testInfo);
    } catch (err) {
      // Only logged, so a restore problem can't hide the original error.
      if (await restoreButton.isVisible())
        await setTabSet1State(page, 'Restore TabSet1', 'NORMAL')
          .catch((restoreErr: Error) => console.warn(`TabSet1 not restored: ${restoreErr.message}`));
      throw err;
    }
    await setTabSet1State(page, 'Restore TabSet1', 'NORMAL');
    expect(violationSummary(results)).toEqual([]);
  });
