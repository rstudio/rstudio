import type { Page } from 'playwright';
import { executeCommand, resetSourcePaneState } from './commands';

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

  // 2. Exit R debug mode if active. Use a generous timeout so a slow Stop
  //    round-trip doesn't leave the next test running against a stale debug
  //    toolbar -- waitForDebugMode would return instantly against it, and
  //    subsequent getActiveDebugLineRow calls look for a line that lives in
  //    a hidden tab from the previous session.
  const debugToolbar = page.locator(DEBUG_TOOLBAR);
  if (await debugToolbar.isVisible().catch(() => false)) {
    const stopBtn = page.locator(DEBUG_STOP_BTN);
    await stopBtn.click().catch(() => {});
    try {
      await debugToolbar.waitFor({ state: 'hidden', timeout: 15000 });
    } catch {
      console.warn(
        '[test-reset] Debug toolbar still visible 15s after clicking Stop. ' +
        'A prior test left R wedged in debug mode; the next test will see ' +
        'this stale toolbar and waitForDebugMode will return instantly.',
      );
    }
  }

  // 3. Collapse source pane to a single Untitled tab, then wait for the
  //    reset to actually land before returning. resetToUntitled dispatches
  //    a GWT event whose handler does its work async: it reverts dirty
  //    targets, then closes every tab except a kept Untitled in a CPS chain
  //    of closeTab calls. page.evaluate returns the moment the event
  //    dispatch enqueues, NOT when the chain finishes.
  //
  //    The wait below polls for two conditions together:
  //      - active doc is the Untitled (path === null), AND
  //      - exactly one source tab remains in the DOM.
  //    The previous version waited only on the first condition, which is
  //    satisfied as soon as focus switches to the Untitled -- often well
  //    before the close-all chain has actually closed the other tabs. Those
  //    lingering tabs (and their hidden Ace editors) bleed state into the
  //    next test: stale `.ace_active_debug_line` markers in hidden editors,
  //    stale gutter breakpoints, etc.
  await resetSourcePaneState(page);
  await page.waitForFunction(
    () => {
      const doc = window.rstudio?.documents.active() ?? null;
      if (doc === null || doc.path !== null) return false;
      // Count source tabs across all source columns. The DocTabLayoutPanel
      // wrapper tags its root with class `rstudio_source_panel`, and each
      // open document renders one `[role="tab"]` child of the panel's
      // tablist.
      const tabs = document.querySelectorAll(
        "[class*='rstudio_source_panel'] [role='tab']",
      );
      return tabs.length === 1;
    },
    null,
    { timeout: 10000, polling: 50 },
  ).catch(() => {
    // Best-effort: don't fail the test if the reset didn't fully drain.
    // The activateConsole below moves focus to the console either way, and
    // any latent source-pane staleness will show up as a more precise
    // assertion failure in the test itself.
    console.warn(
      '[test-reset] resetToUntitled did not settle within 10s ' +
      '(either active doc never became Untitled, or extra tabs remain). ' +
      'The next test starts with leftover source-pane state.',
    );
  });

  // 4. Restore focus to the console so tests start from a deterministic
  //    focus state. resetToUntitled's handler moves focus to the kept /
  //    newly-created Untitled tab; without this, the first test action
  //    that needs console focus (e.g. clearConsole, executeInConsole) has
  //    to race the in-flight focus shift.
  await executeCommand(page, 'activateConsole');
}
