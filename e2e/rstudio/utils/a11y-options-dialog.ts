import { expect, test, type Locator, type Page, type TestInfo } from '@playwright/test';
import { APPEARANCE_PREVIEW, OPTIONS_CANCEL } from '../pages/global_options.page';
import { declareStates, scanA11y, violationSummary } from './a11y';
import { TIMEOUTS } from './constants';
import { trackRpcs, type RpcTracker } from './rpc';

// The options dialogs' pane chooser (SectionChooser) is their only vertical tablist.
const SECTION_TABS = '[role="tablist"][aria-orientation="vertical"] [role="tab"]';
// For the data every pane fetches when the dialog opens.
const DATA_TIMEOUT = 60_000;
// Opening the dialog, and selecting each pane once to list the states.
const LISTING_BUDGET = 60_000;
// Selecting, settling, the screenshot and the axe run, for one state.
const STATE_BUDGET = 30_000;

/** One pane of an options dialog, or one tab of a pane with its own tab control. */
interface OptionsState {
  name: string;
  sectionId: string;
  tabId?: string;
}

/** "Git/SVN" is "git-svn", "C / C++" is "c-cpp". */
export function stateSlug(label: string): string {
  return label.toLowerCase().replace(/\+/g, 'p').replace(/[^a-z0-9]+/g, '-').replace(/^-|-$/g, '');
}

/** Select tab `id` -- a pane in the chooser, or a tab inside a pane -- and return its panel. */
async function selectTab(dialog: Locator, id: string): Promise<Locator> {
  const tab = dialog.locator(`[id="${id}"]`);
  await tab.click({ timeout: TIMEOUTS.action });
  await expect(tab).toHaveAttribute('aria-selected', 'true', { timeout: TIMEOUTS.action });
  const panelId = await tab.getAttribute('aria-controls');
  if (!panelId)
    throw new Error(`Options tab ${id} has no aria-controls naming its panel`);
  const panel = dialog.locator(`[id="${panelId}"]`);
  await expect(panel).toBeVisible({ timeout: TIMEOUTS.action });
  return panel;
}

/**
 * Every pane the dialog shows, and every tab of a pane with a tab control.
 * Read from the dialog, since which panes it shows depends on the server's
 * options. A pane with a single tab hides its tab bar, so its tab is not visible.
 */
async function listStates(dialog: Locator): Promise<OptionsState[]> {
  const states: OptionsState[] = [];
  for (const section of await dialog.locator(SECTION_TABS).all()) {
    if (!await section.isVisible())
      continue;
    const sectionId = await section.getAttribute('id');
    if (!sectionId)
      throw new Error(`Options pane "${await section.innerText()}" has no id`);
    const sectionName = stateSlug(await section.innerText());
    const panel = await selectTab(dialog, sectionId);
    const tabs: OptionsState[] = [];
    for (const tab of await panel.locator('[role="tab"]').all()) {
      if (!await tab.isVisible())
        continue;
      const tabId = await tab.getAttribute('id');
      if (!tabId)
        throw new Error(`Tab "${await tab.innerText()}" of options pane ${sectionId} has no id`);
      tabs.push({ name: `${sectionName}-${stateSlug(await tab.innerText())}`, sectionId, tabId });
    }
    states.push(...(tabs.length > 0 ? tabs : [{ name: sectionName, sectionId }]));
  }
  if (states.length === 0)
    throw new Error('The options dialog shows no panes');
  return states;
}

/** Wait for the dialog's frames to finish loading. */
async function waitForFrames(dialog: Locator): Promise<void> {
  await expect.poll(() => dialog.evaluate((el) =>
    [...el.querySelectorAll('iframe')].every((f) => f.contentDocument?.readyState === 'complete')),
  { timeout: TIMEOUTS.fileOpen }).toBe(true);
}

/**
 * Wait for a shown editor theme preview (Appearance) to render. Its frame
 * loads Ace only once it is complete, then adds the theme as a stylesheet.
 */
async function waitForEditorPreview(dialog: Locator): Promise<void> {
  const preview = dialog.locator(`${APPEARANCE_PREVIEW}:visible`);
  if (await preview.count() === 0)
    return;
  const frame = preview.contentFrame();
  await expect(frame.locator('.ace_editor .ace_line').first())
    .toBeVisible({ timeout: TIMEOUTS.fileOpen });
  await expect.poll(() => frame.locator('body').evaluate((body) =>
    [...body.ownerDocument.querySelectorAll<HTMLLinkElement>('link[rel="stylesheet"]')]
      .every((l) => l.sheet !== null)),
  { timeout: TIMEOUTS.fileOpen }).toBe(true);
}

async function scanStates(
  page: Page,
  testInfo: TestInfo,
  dialog: Locator,
  rpcs: RpcTracker,
): Promise<Map<string, string[]>> {
  await expect(dialog).toBeVisible({ timeout: TIMEOUTS.consoleReady });
  await rpcs.idle(DATA_TIMEOUT);
  const states = await listStates(dialog);
  declareStates(testInfo, states.map((s) => s.name));
  test.setTimeout(testInfo.timeout + states.length * STATE_BUDGET);
  const violations = new Map<string, string[]>();
  for (const state of states) {
    await test.step(state.name, async () => {
      await selectTab(dialog, state.sectionId);
      if (state.tabId)
        await selectTab(dialog, state.tabId);
      await rpcs.idle(DATA_TIMEOUT);
      await waitForFrames(dialog);
      await waitForEditorPreview(dialog);
      violations.set(state.name, violationSummary(await scanA11y(page, testInfo, state.name)));
    });
    // The reporter prints nothing between tests, and CI's heartbeat wrapper
    // ends a run that is silent for 5 minutes, which a slow dialog can outlast.
    console.log(`[a11y] scanned ${state.name} (${violations.size}/${states.length})`);
  }
  return violations;
}

async function cancelDialog(dialog: Locator): Promise<void> {
  await dialog.locator(OPTIONS_CANCEL).click({ timeout: TIMEOUTS.action });
  await expect(dialog).toHaveCount(0, { timeout: TIMEOUTS.action });
}

/**
 * Open an options dialog with `open` and scan each of its panes, and each tab
 * of a pane with a tab control, as its own state (see declareStates). The
 * dialog is cancelled afterwards, so no option changes, including when the
 * test fails first. Returns each state's violation summary.
 */
export async function scanOptionsDialog(
  page: Page,
  testInfo: TestInfo,
  dialogSelector: string,
  open: () => Promise<void>,
): Promise<Map<string, string[]>> {
  const dialog = page.locator(dialogSelector);
  test.setTimeout(testInfo.timeout + DATA_TIMEOUT + LISTING_BUDGET);
  // Started before the dialog opens, which is when its panes fetch their data.
  const rpcs = trackRpcs(page);
  let violations: Map<string, string[]>;
  try {
    await open();
    violations = await scanStates(page, testInfo, dialog, rpcs);
  } catch (err) {
    // Logged only, so a close problem can't hide the original error.
    if (await dialog.count() > 0) {
      await cancelDialog(dialog)
        .catch((closeErr: Error) =>
          console.warn(`Options dialog not closed: ${closeErr.message}`));
    }
    throw err;
  } finally {
    rpcs.stop();
  }
  await cancelDialog(dialog);
  return violations;
}
