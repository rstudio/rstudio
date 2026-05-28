import type { Page } from 'playwright';
import {
  dismissAllModals,
  executeCommand,
  numModalsShowing,
  resetSourcePaneState,
} from './commands';

// Locators kept local to this helper -- inlined so resetForNextTest has no
// transitive dependency on the debugger / source page objects (which import
// other heavy modules and would slow down the per-test reset).
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
 *   - **Leaked modal dialogs.** Any modal a previous test left up throws a
 *     glass panel over the whole UI that intercepts the first action of the
 *     next test (typically executeInConsole's console-tab click). These come
 *     in several shapes -- a `<Save / Don't Save / Cancel>` prompt, an OK-only
 *     "Save File" error ("system error 2"), a "File Deleted" Yes/No prompt --
 *     and they stack, so a single targeted button click can't clear them
 *     (the topmost dialog intercepts the click meant for the one beneath).
 *     dismissAll hides the entire GWT modal stack in one shot.
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
 *
 * Error handling: the debug-exit step (2) is incidental cleanup and is fully
 * guarded -- it never throws. Steps 1, 3, and 4 (modal dismissal, source-pane
 * reset, console focus) are structural and call the automation bridge; they
 * will throw if `window.rstudio` is gone. That is intentional -- a missing
 * bridge means the session is unusable and every following test would fail
 * anyway, so failing here (named clearly) beats swallowing the error and
 * letting the next test fail on a mystery first action. Guarding the
 * source-pane reset in particular would let a previous test's buffers leak
 * forward silently, defeating the point of the hook.
 */
export async function resetForNextTest(page: Page): Promise<void> {
  // 1. Clear any leaked modal dialogs up front, before the steps below try to
  //    interact with elements a glass panel would block. dismissAll hides the
  //    whole GWT modal stack (handles stacked + OK-only dialogs a single
  //    button click can't); it hides rather than answers, so a dirty doc's
  //    changes are discarded by resetSourcePaneState's revert in step 3.
  await dismissAllModals(page);
  // Sentinel -1 (not 0) on a failed read: 0 would masquerade as "no modals
  // left" and suppress the very warning this block exists to emit.
  const leaked = await numModalsShowing(page).catch(() => -1);
  if (leaked < 0) {
    console.warn(
      '[test-reset] Could not read the modal count after dismissAll ' +
      '(the automation bridge may be unavailable); the next test could start ' +
      'behind a leaked dialog.',
    );
  } else if (leaked > 0) {
    console.warn(
      `[test-reset] ${leaked} modal dialog(s) still showing after dismissAll. ` +
      'A non-GWT (e.g. native desktop) dialog may be up; the next test could ' +
      'start behind a glass panel.',
    );
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

  // 3. Collapse source pane to a single Untitled tab. resetSourcePaneState
  //    waits for the async close chain to drain before returning, so lingering
  //    tabs (and their hidden Ace editors) can't bleed state into the next
  //    test -- stale `.ace_active_debug_line` markers, gutter breakpoints, etc.
  await resetSourcePaneState(page);

  // 4. Restore focus to the console so tests start from a deterministic
  //    focus state. resetToUntitled's handler moves focus to the kept /
  //    newly-created Untitled tab; without this, the first test action
  //    that needs console focus (e.g. clearConsole, executeInConsole) has
  //    to race the in-flight focus shift.
  await executeCommand(page, 'activateConsole');
}
