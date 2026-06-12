import type { Page } from 'playwright';
import {
  dismissAllModals,
  executeCommand,
  numModalsShowing,
  resetLayoutZoom,
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
 * clean session this adds only the cost of an already-satisfied readiness
 * check, two `isVisible()` snapshots, a layout-zoom reset (a no-op bridge call
 * when nothing is zoomed), and a bridge call to resetSourcePaneState
 * (typically tens of ms total).
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
 *   - **Pane zoom / maximize.** A previous test that zoomed a pane
 *     (layoutZoom*) or left one maximized at the WindowFrame level (the pane
 *     header min/max buttons, or a notebook preview maximizing the Viewer)
 *     squeezes other panes to near-zero or hides their tab strips entirely,
 *     so the next test can't click its targets. End either state when active
 *     (see resetLayoutZoom).
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
 * Error handling: the debug-exit (2) and zoom-reset (3) steps are incidental
 * cleanup and are fully guarded -- they never throw (resetLayoutZoom treats a
 * missing bridge as "nothing zoomed"). Steps 1, 4, and 5 (modal dismissal,
 * source-pane reset, console focus) are structural and call the automation
 * bridge; they will throw if `window.rstudio` is gone. That is intentional --
 * a missing
 * bridge means the session is unusable and every following test would fail
 * anyway, so failing here (named clearly) beats swallowing the error and
 * letting the next test fail on a mystery first action. Guarding the
 * source-pane reset in particular would let a previous test's buffers leak
 * forward silently, defeating the point of the hook.
 */
export async function resetForNextTest(page: Page): Promise<void> {
  // 0. Wait until workbench init has completed. The session is worker-scoped,
  //    so a spec can start while the previous one's page.reload() (or a session
  //    relaunch) is still settling: window.rstudio can be installed while the
  //    workbench is still wiring up. Driving it then races init -- a command
  //    bridge call returns "markers not found", or an offsetWidth read returns
  //    0 before layout (both observed as intermittent failures). window.rstudio
  //    .ready flips true on DeferredInitCompletedEvent (the same signal
  //    openProject waits on), so it's the canonical "safe to drive" gate. It is
  //    independent of any leaked modal -- a glass panel doesn't reset ready, and
  //    the dismissAll in step 1 clears the modal via the bridge regardless. A
  //    genuinely dead session still fails loud here with a precise timeout.
  await page.waitForFunction(
    () => window.rstudio?.ready === true,
    null,
    { timeout: 30000, polling: 100 },
  );

  // 1. Clear any leaked modal dialogs up front, before the steps below try to
  //    interact with elements a glass panel would block. dismissAll hides the
  //    whole GWT modal stack (handles stacked + OK-only dialogs a single
  //    button click can't); it hides rather than answers, so a dirty doc's
  //    changes are discarded by resetSourcePaneState's revert in step 3.
  //
  //    Capture each dialog's contents BEFORE hiding it: a leaked modal is
  //    often the only visible artifact of a real product error (e.g. the
  //    uncaught-exception "Error" dialog GWT raises), and dismissing it
  //    silently would bury the evidence -- the test that *caused* it can
  //    pass, and the dialog only blocks some later test. The warning puts
  //    the dialog text in the test output where CI failures can be triaged.
  const leakedDialogs = await page.evaluate(() => {
    const visible = (el: Element) => {
      const r = el.getBoundingClientRect();
      return r.width > 0 && r.height > 0;
    };
    return Array.from(document.querySelectorAll('.gwt-DialogBox'))
      .filter(visible)
      .map((el) => {
        const label = el.getAttribute('aria-label') ?? 'dialog';
        const text = ((el as HTMLElement).innerText ?? '').replace(/\s+/g, ' ').trim();
        return `[${label}] ${text.slice(0, 400)}`;
      });
  }).catch(() => [] as string[]);
  for (const dialog of leakedDialogs) {
    console.warn(`[test-reset] dismissing leaked modal dialog -- ${dialog}`);
  }
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

  // 3. End any active pane zoom or pane maximize. The session is worker-scoped,
  //    so a prior test that zoomed a pane (e.g. layoutZoomEnvironment) or left
  //    one maximized at the WindowFrame level (e.g. an R Notebook preview
  //    maximizes the Viewer, minimizing TabSet1 and hiding its tab strip)
  //    leaves the next test's locator clicks landing on a zero-size or hidden
  //    element, timing out as "not visible". resetLayoutZoom reads the live
  //    layout state and is a no-op (preserving any custom column widths) when
  //    nothing is zoomed or maximized.
  await resetLayoutZoom(page);

  // 4. Collapse source pane to a single Untitled tab. resetSourcePaneState
  //    waits for the async close chain to drain before returning, so lingering
  //    tabs (and their hidden Ace editors) can't bleed state into the next
  //    test -- stale `.ace_active_debug_line` markers, gutter breakpoints, etc.
  await resetSourcePaneState(page);

  // 5. Restore focus to the console so tests start from a deterministic
  //    focus state. resetToUntitled's handler moves focus to the kept /
  //    newly-created Untitled tab; without this, the first test action
  //    that needs console focus (e.g. clearConsole, executeInConsole) has
  //    to race the in-flight focus shift.
  await executeCommand(page, 'activateConsole');
}
