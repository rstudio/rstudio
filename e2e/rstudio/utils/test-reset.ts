import type { Page } from 'playwright';
import { resetSourcePaneState } from './commands';

// Locators kept local to this helper -- inlined so resetForNextTest has no
// transitive dependency on the debugger / source page objects (which import
// other heavy modules and would slow down the per-test reset).
const DONT_SAVE_BTN = "button:has-text('Don\\'t Save'), button:has-text('Do not Save'), #rstudio_dlg_no";
const DEBUG_TOOLBAR = '[role="toolbar"][aria-label="Console Tab Debug"]';
// Stop is the rightmost debug-toolbar button; title prefix is locale-stable.
const DEBUG_STOP_BTN = `${DEBUG_TOOLBAR} [title^="Stop"]`;

/**
 * Reset RStudio to a clean per-test starting state. Called from a shared
 * `beforeEach` in `fixtures/rstudio.fixture.ts` so every test starts from
 * the same minimal state regardless of what the previous test left behind.
 *
 * Each step short-circuits cheaply when its trigger isn't present, so on a
 * clean session this adds only the cost of two `isVisible()` snapshots plus
 * a bridge call to resetSourcePaneState (typically tens of ms total).
 *
 * What we reset:
 *
 *   - **Save-changes dialog.** A leaked `<Save / Don't Save / Cancel>` dialog
 *     blocks every subsequent keystroke; click Don't Save when present.
 *   - **R debug mode.** A previous test that didn't drain its
 *     continueDebug / waitForDebugExit (e.g. an exception bubbling out of
 *     waitForDebugAdvance) leaves R at `Browse[N]>`. The first
 *     `waitForDebugMode` of the next test would then return instantly
 *     against a stale state and getActiveDebugLineRow throws because the
 *     debug marker is in a hidden editor tab. Click Stop on the debug
 *     toolbar when it's visible.
 *   - **Source pane buffers.** Collapse to a single Untitled tab via the
 *     `resetToUntitled` bridge (kept-or-created untitled + close everything
 *     else). resetSourcePaneState specifically -- NOT closeAllSourceDocs --
 *     so the source pane never transitions through the zero-tab HIDE state
 *     that races a following file.edit / View() (#17738).
 *
 * What we deliberately don't reset:
 *
 *   - **Prefs.** Test files manage their own pref state in `afterAll`;
 *     touching prefs here would clobber that and silently desync test
 *     expectations from session state.
 *   - **Open projects.** Some tests want a project context (e.g.
 *     reformat-on-save needs an active project per
 *     TextEditingTarget.maybeFormatOnUserInitiatedSave). Files that open
 *     a project must close it in their own afterAll -- enforced by
 *     convention, not here.
 *   - **Working directory.** `useSuiteSandbox` already scopes cwd; tests
 *     that need a specific cwd set it themselves.
 */
export async function resetForNextTest(page: Page): Promise<void> {
  // 1. Dismiss save dialog if present.
  const saveDialog = page.locator(DONT_SAVE_BTN);
  if (await saveDialog.isVisible().catch(() => false)) {
    await saveDialog.click();
    await saveDialog.waitFor({ state: 'hidden', timeout: 5000 }).catch(() => {});
  }

  // 2. Exit R debug mode if active.
  const debugToolbar = page.locator(DEBUG_TOOLBAR);
  if (await debugToolbar.isVisible().catch(() => false)) {
    const stopBtn = page.locator(DEBUG_STOP_BTN);
    await stopBtn.click().catch(() => {});
    await debugToolbar.waitFor({ state: 'hidden', timeout: 5000 }).catch(() => {});
  }

  // 3. Collapse source pane to a single Untitled tab.
  await resetSourcePaneState(page);
}
