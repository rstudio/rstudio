import type { Page } from '@playwright/test';
import { waitForConsoleIdle } from '../pages/console_pane.page';
import type { Ace } from './ace';

// `window.rstudio` is registered when rsession runs with --automation-agent
// (the Desktop fixture forwards that flag). The bridge lets tests trigger and
// inspect AppCommands, read/write preferences, and dispatch a few document
// actions without going through the R console -- avoiding focus-shift and
// pane-collapse pitfalls that the console path runs into.
//
// See ApplicationAutomation.java for the Java side. The shape is enumerated
// up front: every command and every preference is registered at agent init,
// so a missing-by-name lookup is a real "doesn't exist" (not just "not yet
// touched by GWT code").

type PrefValue = boolean | number | string;

type CommandEntry = {
  (): void;
  isChecked(): boolean;
  isEnabled(): boolean;
  isVisible(): boolean;
};

type PrefEntry = {
  get(): PrefValue | null;
  // set / clear return a Promise that resolves once the underlying
  // setUserPrefs RPC has completed server-side. Tests should `await` them
  // before triggering follow-up actions that depend on the pref being
  // observable in R / the session-side cache.
  set(value: PrefValue): Promise<void>;
  clear(): Promise<void>;
};

type ProjectInfo = {
  /** Active project file path (absolute), or null if no project is open. */
  path(): string | null;
  /** Active project display name, or null if no project is open. */
  name(): string | null;
  isActive(): boolean;
  /**
   * Cached project-root air.toml (or .air.toml) path, or null. Maintained by
   * the GWT Projects singleton from FileChangeEvents, so it tracks the file
   * monitor: poll it after creating or deleting an air.toml on disk to know
   * when formatter paths that consult the cache will see the change.
   */
  airTomlPath(): string | null;
  /**
   * Fire SwitchToProjectEvent on the GWT side, switching the session to the
   * project at `path`. Resets `window.rstudio.ready` to false synchronously
   * so a caller can poll for `ready === true` to wait for the new session's
   * DeferredInitCompletedEvent. Prefer the `openProject` helper over calling
   * this directly -- it bundles the post-call readiness wait.
   */
  open(path: string): void;
};

type VersionInfo = {
  /** RStudio long-version string, e.g. "2026.05.999". */
  rstudio: string;
  /** R version number, e.g. "4.5.3". */
  r: string;
};

type ActiveDocument = {
  id: string;
  /** Absolute file path, or null for an untitled document. */
  path: string | null;
  /** True when the document has unsaved changes in the editor model. */
  dirty: boolean;
};

type DocumentOpenOptions = {
  /** 1-indexed line to navigate to after opening. Omit (or <0) to skip navigation. */
  line?: number;
  /** 1-indexed column. Defaults to 1 when `line` is set. */
  col?: number;
  /** Whether to move the cursor to the position (true) or just scroll there (false). Defaults to true. */
  moveCursor?: boolean;
};

type Documents = {
  closeAllNoSave(): void;
  resetToUntitled(): void;
  /** Info on the focused source document, or null when no editor is active. */
  active(): ActiveDocument | null;
  /**
   * Native Ace editor instance backing the active source document, or null
   * when no editor is active (or the active editor isn't Ace-backed -- data
   * viewer, object explorer, etc.). Call Ace API methods (getValue, setValue,
   * getSession, ...) on the returned object directly. Prefer this over
   * iterating `.ace_editor` DOM nodes: the DOM scan can land on the console
   * scroll panel, dialog editors (IgnoreDialog, ViewFileDialog, ...), or
   * stale source editors left in the DOM after a tab close.
   */
  activeEditor(): Ace.Editor | null;
  /**
   * Open the file at `path` in the source pane. Fires OpenSourceFileEvent
   * directly; the caller must ensure the file exists on disk (the bridge
   * skips the ensureFileExists RPC that .rs.api.documentOpen does).
   */
  open(path: string, opts?: DocumentOpenOptions): void;
};

/**
 * Chat-pane lifecycle state, published by ChatPresenter at each transition.
 * `blocked` is true when the iframe is currently showing a blocking page that
 * prevents normal interaction; `state` identifies which (or, when not blocked,
 * which transitional / ready state the pane is in).
 */
type ChatBridge = {
  state:
    | 'ready'
    | 'starting'
    | 'restarting'
    | 'manifest-unavailable'
    | 'unsupported-protocol'
    | 'incompatible-version'
    | 'version-update-required'
    | 'version-no-update'
    | 'not-installed'
    | 'update-available'
    | 'assistant-not-selected'
    | 'error'
    | 'crashed';
  blocked: boolean;
  /**
   * Ordered list of state transitions since the chat presenter initialized.
   * Bounded to the last 50 entries by ChatPresenter to keep long sessions
   * from growing unbounded. Useful in test diagnostics to see what actually
   * happened before an unexpected current state.
   */
  history: Array<{ state: ChatBridge['state']; blocked: boolean; at: number }>;
  /**
   * Install (object) or clear (null) an automation-only override for the
   * next chat_check_for_updates response. Tests use this to drive each
   * blocking branch deterministically; the resolved Promise means rsession
   * has acknowledged and the next retry-manifest will return the override.
   */
  setUpdateCheckOverride(override: Record<string, unknown> | null): Promise<void>;
};

type DialogsBridge = {
  /** Count of modal dialogs currently in the GWT modal stack. */
  numShowing(): number;
  /**
   * Hide every modal dialog currently in the stack. Useful in test teardown
   * so a forgotten Tools > Global Options / Import Dataset dialog does not
   * block the Electron close path -- the renderer's quit confirmation
   * prompts queue behind an existing modal and the window can end up in a
   * half-shut-down state.
   */
  dismissAll(): void;
};

type LayoutBridge = {
  /**
   * End any active pane/window or column zoom, restoring the default layout;
   * a no-op when nothing is zoomed. The returned Promise resolves once the
   * relayout has settled (the restore flushes on a later animation frame even
   * under reduced_motion).
   */
  reset(): Promise<void>;
};

/** One uncaught client exception recorded by the automation agent. */
export type ClientException = {
  message: string;
  /**
   * Cause chain + frames. Java names in draft / super-dev builds; best-effort
   * (obfuscated) in optimized builds, where the message still identifies the
   * failure.
   */
  stack: string;
  /** Date.now() at record time. */
  time: number;
};

type ErrorsBridge = {
  /** Uncaught client exceptions recorded since the last clear(). */
  list(): ClientException[];
  clear(): void;
  /** Raise a real uncaught exception from a scheduled context (self-test only). */
  simulate(message: string): void;
};

type ConsoleBridge = {
  /**
   * Monotonic count of completed console commands (advances on each top-level
   * prompt). Absent on binaries built before the counter was added. See
   * ApplicationAutomation.registerConsole for the authoritative rationale, and
   * waitForConsoleCommandComplete for how callers consume it.
   */
  promptCount: number;
};

type ShinyBridge = {
  /**
   * Stop the running foreground shiny app via shiny::stopApp() on rsession.
   * Resolves once the RPC has landed and R has exited runApp. Tests use this
   * instead of driving the interrupt button: interrupt depends on a signal
   * landing inside runApp's event loop, which is unreliable on Windows where
   * the OS-level signal mechanism differs from Unix.
   */
  stopForegroundApp(): Promise<void>;
};

type CompletionsBridge = {
  /**
   * Automation-only override: when true, a completion request that returns
   * exactly one result shows the popup instead of auto-accepting the match
   * (RCompletionManager / CompletionManagerBase skip their auto-accept
   * branch). Lets popup-reading helpers enumerate items deterministically
   * regardless of how many results a token happens to have. Set it around a
   * completion request and clear it afterwards; leave it off to exercise the
   * real unique-match auto-accept behavior.
   */
  setAlwaysShowPopup(force: boolean): void;
};

type RStudioBridge = {
  commands: { [id: string]: CommandEntry } & { list: string[] };
  prefs: { [name: string]: PrefEntry };
  documents: Documents;
  project: ProjectInfo;
  version: VersionInfo;
  dialogs: DialogsBridge;
  layout: LayoutBridge;
  errors: ErrorsBridge;
  /** Console completion-signal surface (promptCount). */
  console?: ConsoleBridge;
  /** Shiny-app automation surface. */
  shiny?: ShinyBridge;
  /** Completion-popup overrides. Absent on builds that predate the knob. */
  completions?: CompletionsBridge;
  /** Chat-pane state surface (populated lazily by ChatPresenter). */
  chat?: ChatBridge;
  /**
   * False during workbench init; flips to true once the
   * DeferredInitCompletedEvent fires. Wait for this in test fixtures and
   * project-open helpers before driving any R-to-GWT roundtrip (file.edit,
   * client-event-driven actions) -- the bridge can be installed and reading
   * window.rstudio still races with workbench init otherwise.
   */
  ready: boolean;
};

declare global {
  interface Window {
    rstudio?: RStudioBridge;
  }
}

// Convert snake_case to camelCase. Preference names are snake_case in the
// schema (matching rstudio-prefs.json), but the Java bridge registers them
// camelCased to read better as JS object keys. Keeping the TS wrapper APIs
// snake_case lets callers continue to use the canonical pref name.
function snakeToCamel(s: string): string {
  return s.replace(/_([a-z])/g, (_match, c: string) => c.toUpperCase());
}

/** Run an AppCommand by id (no console roundtrip). */
export async function executeCommand(page: Page, commandId: string): Promise<void> {
  // Wait for the command to exist AND be ready to dispatch:
  //  - existence covers the bridge being transiently absent during session
  //    restarts (project open/close, Restart R);
  //  - enabled-state covers the brief command-state lag after a focus or
  //    cursor change (e.g. navigating into a chunk before executeCurrentChunk).
  // Executing a disabled command trips a dev-build assertion in
  // AppCommand.execute() (AppCommand.java) -- the handler never runs, so the
  // failure surfaces as an opaque "AppCommand executed when it was not
  // enabled" client exception plus a downstream hang. Gating here both
  // absorbs the lag and, on timeout, names the offending command. When the
  // command is already up and enabled the condition is true on the first
  // poll, so this adds no measurable latency to the steady-state path.
  //
  // isEnabled() is `enabled_ && isVisible()` (AppCommand.isEnabled), but
  // doExecute() only requires `enabled_`. Invisible programmatic commands
  // (visible="false", e.g. restoreDefaultPaneAndTabLayoutNoPrompt) are
  // therefore always reported disabled yet dispatch fine, so the gate would
  // block them forever. Bypass the enabled-wait for invisible commands --
  // their enabled_ flag isn't observable from JS, and they are not the
  // focus/cursor-lag case this gate exists for.
  const ENABLE_TIMEOUT = 10000;
  try {
    await page.waitForFunction(
      (id) => {
        const cmd = window.rstudio?.commands?.[id];
        return typeof cmd === 'function' && (cmd.isEnabled() || !cmd.isVisible());
      },
      commandId,
      { timeout: ENABLE_TIMEOUT, polling: 50 },
    );
  } catch {
    // Distinguish "never appeared" from "present but stayed disabled" so the
    // failure names the command and its actual blocking condition, rather
    // than a generic waitForFunction timeout.
    const exists = await page.evaluate(
      (id) => typeof window.rstudio?.commands?.[id] === 'function',
      commandId,
    );
    throw new Error(
      exists
        ? `Command "${commandId}" did not become enabled within ${ENABLE_TIMEOUT}ms`
        : `Command "${commandId}" never became available within ${ENABLE_TIMEOUT}ms`,
    );
  }
  await page.evaluate((id) => {
    const r = window.rstudio!;
    const cmd = r.commands[id];
    cmd();
  }, commandId);
}

/** True when the named AppCommand reports `isChecked` (e.g. an active layout zoom). */
export async function isCommandChecked(page: Page, commandId: string): Promise<boolean> {
  return page.evaluate((id) => {
    const r = window.rstudio;
    if (!r)
      throw new Error('window.rstudio is not defined; launch RStudio with --automation-agent');
    const cmd = r.commands[id];
    if (!cmd)
      throw new Error(`Unknown command: ${id}`);
    return cmd.isChecked();
  }, commandId);
}

/** True when the named AppCommand is currently enabled. */
export async function isCommandEnabled(page: Page, commandId: string): Promise<boolean> {
  return page.evaluate((id) => {
    const r = window.rstudio;
    if (!r)
      throw new Error('window.rstudio is not defined; launch RStudio with --automation-agent');
    const cmd = r.commands[id];
    if (!cmd)
      throw new Error(`Unknown command: ${id}`);
    return cmd.isEnabled();
  }, commandId);
}

/**
 * End any active pane/column zoom or pane maximize, restoring the default
 * layout.
 *
 * The automation session is worker-scoped, so a test that zooms a pane (e.g.
 * layoutZoomEnvironment) and fails before toggling it back off leaves the
 * layout maximized -- which squeezes every other pane to near-zero and makes
 * the next test's targets unclickable / invisible. The same applies to a
 * WindowFrame-level maximize (the pane header min/max buttons, or an R
 * Notebook preview maximizing the Viewer), which additionally persists via
 * client state. This breaks the cascade by ending either state.
 *
 * Delegates to the GWT bridge (window.rstudio.layout.reset), which decides
 * based on the live PaneManager state: it restores only when something is
 * actually zoomed or maximized (so a normal layout keeps its column widths)
 * and covers pane/window zoom, column zoom, and WindowFrame maximize. The
 * bridge returns a Promise that resolves once the relayout has settled (the
 * restore flushes on a later animation frame even under reduced_motion), so
 * awaiting this leaves the layout stable before the caller measures or clicks
 * panes. A no-op when the bridge is absent.
 */
export async function resetLayoutZoom(page: Page): Promise<void> {
  await page.evaluate(() => window.rstudio?.layout?.reset());
}

/**
 * Read and clear the uncaught-client-exception record kept by the automation
 * agent (window.rstudio.errors). Returns [] when the bridge is absent (e.g.
 * mid-restart) -- callers treat that as "nothing recorded" because a dead
 * session fails loudly elsewhere.
 *
 * The per-test fixture drains this after every test and fails the test that
 * produced exceptions; a leftover drained before the next test is logged but
 * not attributed (it may belong to teardown or the gap between tests).
 */
export async function drainClientExceptions(page: Page): Promise<ClientException[]> {
  return page.evaluate(() => {
    const errors = window.rstudio?.errors;
    if (!errors)
      return [];
    const items = errors.list();
    errors.clear();
    return items;
  }).catch(() => []);
}

/**
 * Save the active source document and wait for it to be marked clean.
 *
 * Polls `window.rstudio.documents.active().dirty` -- the pure dirty bit
 * from the editing target's model, distinct from `saveSourceDoc.isEnabled()`
 * which stays true for source-on-save / reformat-on-save / untitled docs
 * regardless of dirty state. Pre-save transforms (trim-trailing-whitespace,
 * styler, Air) update the model before the dirty bit clears, so reading the
 * editor's value after this returns reflects the final on-disk content.
 */
export async function saveDocument(page: Page, timeout = 5000): Promise<void> {
  // Only issue Save when the document is actually dirty. A clean document
  // (e.g. one opened from a file that createAndOpenFile already wrote to
  // disk) is already saved, and saveSourceDoc is disabled for it -- invoking
  // it anyway would trip the disabled-command guard. The wait below then
  // confirms the clean state regardless of which branch we took.
  const dirty = await page.evaluate(() => {
    const doc = window.rstudio?.documents.active() ?? null;
    return doc !== null && doc.dirty;
  });
  if (dirty)
    await executeCommand(page, 'saveSourceDoc');

  await page.waitForFunction(
    () => {
      const doc = window.rstudio?.documents.active() ?? null;
      return doc !== null && !doc.dirty;
    },
    null,
    { timeout, polling: 100 },
  );
}

/**
 * Close every open source document, discarding unsaved changes. Equivalent
 * to `.rs.api.closeAllSourceBuffersWithoutSaving()` but skips the R-side
 * round trip (and therefore the "session is busy" dialog risk).
 */
export async function documentCloseAllNoSave(page: Page): Promise<void> {
  await page.evaluate(() => {
    const r = window.rstudio;
    if (!r)
      throw new Error('window.rstudio is not defined; launch RStudio with --automation-agent');
    r.documents.closeAllNoSave();
  });
}

/**
 * Open the file at `path` in the source pane via the automation bridge, and
 * wait until it is the active document.
 *
 * Mirrors `.rs.api.documentOpen(path, line, col, moveCursor)` but skips the
 * R round-trip and the ensureFileExists RPC; the caller is responsible for
 * ensuring the file exists on disk.
 *
 * The event dispatch is synchronous; the actual open is async (file load,
 * editor create), so this also polls `documents.active()` until the path
 * matches -- equivalent to following the open with `waitForActiveDocument`.
 *
 * `line` is 1-indexed; omit (or pass <0) to open without navigating.
 */
export async function documentOpen(
  page: Page,
  path: string,
  opts: DocumentOpenOptions = {},
  timeout = 20000,
): Promise<void> {
  await page.evaluate(({ docPath, options }) => {
    const r = window.rstudio;
    if (!r)
      throw new Error('window.rstudio is not defined; launch RStudio with --automation-agent');
    r.documents.open(docPath, options);
  }, { docPath: path, options: opts });
  await waitForActiveDocument(page, path, timeout);
}

/**
 * Reset the source pane to a single untitled document, discarding any unsaved
 * changes in other tabs. If an untitled tab is already open it is kept;
 * otherwise a fresh one is created before the others close, so the pane
 * never transitions through the zero-tab HIDE state -- the gap that races a
 * subsequent file.edit (#17738). Prefer this over documentCloseAllNoSave for
 * cross-test resets where a following file.edit / newDoc is imminent.
 *
 * Waits for the reset to actually land before returning (see
 * waitForSourcePaneReset). resetToUntitled's close chain is async, so
 * returning on dispatch would let callers race the still-open tabs -- most
 * damagingly, deleting their files while editors hold them open, which makes
 * RStudio raise "File Deleted" / "Save File" (system error 2) modals whose
 * glass panels block the next test. Best-effort: a reset that never settles
 * warns rather than throwing, matching the cross-test reset's
 * don't-fail-the-hook contract; residual staleness surfaces as a more precise
 * failure in the test body.
 */
export async function resetSourcePaneState(page: Page): Promise<void> {
  await page.evaluate(() => {
    const r = window.rstudio;
    if (!r)
      throw new Error('window.rstudio is not defined; launch RStudio with --automation-agent');
    r.documents.resetToUntitled();
  });
  await waitForSourcePaneReset(page).catch(() => {
    console.warn(
      '[commands] resetSourcePaneState did not settle within 10s ' +
      '(active doc never became Untitled, or extra tabs remain). ' +
      'Callers proceed against possibly-stale source-pane state.',
    );
  });
}

/**
 * Wait until a resetSourcePaneState dispatch has actually settled: the active
 * document is the kept Untitled (path === null) AND exactly one source tab
 * remains across all columns.
 *
 * resetToUntitled dispatches a GWT event whose handler reverts dirty targets,
 * then closes every tab except a kept Untitled in an async CPS chain of
 * closeTab calls. The page.evaluate inside resetSourcePaneState returns the
 * moment that event is enqueued, NOT when the chain finishes -- so callers
 * that go on to touch what the still-open tabs hold (deleting their files on
 * disk, opening a colliding file, asserting on tab count) race the close.
 * The most damaging case: deleting a file while its editor is still open makes
 * RStudio raise a "File Deleted" prompt and a "Save File" (system error 2)
 * error, whose glass panels then block the next test.
 *
 * resetSourcePaneState already awaits this (best-effort) after dispatching, so
 * call it directly only when you need to wait on the settle without
 * re-dispatching the reset. Throws on timeout; best-effort callers `.catch()`.
 */
export async function waitForSourcePaneReset(page: Page, timeout = 10000): Promise<void> {
  await page.waitForFunction(
    () => {
      const doc = window.rstudio?.documents.active() ?? null;
      if (doc === null || doc.path !== null) return false;
      // Count source tabs across all source columns. The DocTabLayoutPanel
      // wrapper tags its root with class `rstudio_source_panel`, and each
      // open document renders one `[role="tab"]` child of the panel's tablist.
      const tabs = document.querySelectorAll(
        "[class*='rstudio_source_panel'] [role='tab']",
      );
      return tabs.length === 1;
    },
    null,
    { timeout, polling: 50 },
  );
}

/**
 * Wait until the focused source document's path equals `expectedPath`. Polls
 * `window.rstudio.documents.active()` -- the bridge value updates the moment
 * the active editor changes, so this is a deterministic post-condition for
 * `.rs.api.documentOpen(...)`, `file.edit(...)`, and other open-by-path flows.
 *
 * On case-insensitive filesystems (macOS HFS+, NTFS) the comparison ignores
 * case. On Windows, backslashes and forward slashes are treated as equivalent
 * so callers can pass Node.js path.join results regardless of whether RStudio
 * normalizes separators. RStudio home-aliases doc paths under the rsession's
 * home directory ("~/sub/file.R"); those match when `expectedPath` ends with
 * the aliased path minus the "~" -- still a full home-relative comparison, so
 * same-basename files in different directories can't be confused.
 */
export async function waitForActiveDocument(
  page: Page,
  expectedPath: string,
  timeout = 10000,
): Promise<void> {
  await page.waitForFunction(
    (target) => {
      const doc = window.rstudio?.documents.active() ?? null;
      if (doc === null || doc.path === null) return false;
      const dp = doc.path.replace(/\\/g, '/').toLowerCase();
      const expected = target.replace(/\\/g, '/').toLowerCase();
      return dp === expected
        || (dp.startsWith('~/') && expected.endsWith(dp.slice(1)));
    },
    expectedPath,
    { timeout, polling: 100 },
  );
}

/**
 * Read a user preference by name. Returns null when the preference name is
 * unknown.
 */
export async function getPref(page: Page, name: string): Promise<PrefValue | null> {
  const camel = snakeToCamel(name);
  return page.evaluate((prefName) => {
    const r = window.rstudio;
    if (!r)
      throw new Error('window.rstudio is not defined; launch RStudio with --automation-agent');
    const entry = r.prefs[prefName];
    return entry ? entry.get() : null;
  }, camel);
}

// The bridge (and its prefs map) is transiently absent during session
// restarts -- project open/close, Restart R -- and a preceding spec can hand
// off the worker with such a reload still in flight. Mirror executeCommand's
// guard: wait for the specific pref entry to land before touching it, so the
// steady-state path adds no latency while the restart path stops racing.
async function waitForPrefEntry(page: Page, camelName: string): Promise<void> {
  await page.waitForFunction(
    (prefName) => window.rstudio?.prefs?.[prefName] !== undefined,
    camelName,
    { timeout: 10000, polling: 50 },
  );
}

/**
 * Set a user preference and persist it. Equivalent to
 * `.rs.api.writeRStudioPreference(name, value)` / `.rs.uiPrefs$<name>$set(value)`.
 * The bridge dispatches by the preference's declared type, so the JS value
 * must match: pass a boolean for boolean prefs, a number for integer/double
 * prefs, a string for string/enum prefs.
 *
 * Awaits the setUserPrefs RPC completion server-side before returning, so the
 * pref change is observable in R / the session-side cache when this resolves.
 */
export async function setPref(page: Page, name: string, value: PrefValue): Promise<void> {
  const camel = snakeToCamel(name);
  await waitForPrefEntry(page, camel);
  await page.evaluate(async ({ prefName, prefValue }) => {
    const r = window.rstudio;
    if (!r)
      throw new Error('window.rstudio is not defined; launch RStudio with --automation-agent');
    const entry = r.prefs[prefName];
    if (!entry)
      throw new Error(`Unknown user preference: ${prefName}`);
    await entry.set(prefValue);
  }, { prefName: camel, prefValue: value });
}

/**
 * Remove a user-layer preference value (the default takes over). Equivalent
 * to `.rs.uiPrefs$<name>$clear()`. Awaits the setUserPrefs RPC completion
 * server-side before returning.
 */
export async function clearPref(page: Page, name: string): Promise<void> {
  const camel = snakeToCamel(name);
  await waitForPrefEntry(page, camel);
  await page.evaluate(async (prefName) => {
    const r = window.rstudio;
    if (!r)
      throw new Error('window.rstudio is not defined; launch RStudio with --automation-agent');
    const entry = r.prefs[prefName];
    if (!entry)
      throw new Error(`Unknown user preference: ${prefName}`);
    await entry.clear();
  }, camel);
}

/**
 * Count of modal dialogs currently in the GWT modal stack.
 */
export async function numModalsShowing(page: Page): Promise<number> {
  return page.evaluate(() => {
    const r = window.rstudio;
    if (!r)
      throw new Error('window.rstudio is not defined; launch RStudio with --automation-agent');
    return r.dialogs.numShowing();
  });
}

/**
 * Hide every modal dialog currently in the GWT modal stack. Use in test
 * teardown to keep a leftover dialog (Global Options, Import Dataset, etc.)
 * from blocking the Electron close path.
 */
export async function dismissAllModals(page: Page): Promise<void> {
  await page.evaluate(() => {
    const r = window.rstudio;
    if (!r)
      throw new Error('window.rstudio is not defined; launch RStudio with --automation-agent');
    r.dialogs.dismissAll();
  });
}

/**
 * Stop the running foreground shiny app via shiny::stopApp() on the rsession
 * side. Cleaner than driving the interrupt button: interrupt relies on R's
 * R_interrupts_pending flag being checked inside runApp's event loop, which
 * is unreliable on Windows where the OS uses CTRL_BREAK_EVENT rather than
 * SIGINT. The RPC goes through shiny's own shutdown path on every platform.
 * Resolves once R has returned from runApp.
 */
export async function stopForegroundShinyApp(page: Page): Promise<void> {
  await page.evaluate(() => {
    const stop = window.rstudio?.shiny?.stopForegroundApp;
    if (!stop)
      throw new Error(
        'window.rstudio.shiny.stopForegroundApp missing; launch RStudio with --automation-agent',
      );
    return stop();
  });
}

/**
 * Read window.rstudio.chat.state. Returns null when the chat presenter
 * hasn't yet published its first state (i.e., the chat pane was never
 * activated this session).
 */
export async function getChatState(page: Page): Promise<ChatBridge['state'] | null> {
  return await page.evaluate(() => window.rstudio?.chat?.state ?? null);
}

/**
 * Read window.rstudio.chat.blocked. Returns null when the chat presenter
 * hasn't published its first state yet.
 */
export async function isChatBlocked(page: Page): Promise<boolean | null> {
  return await page.evaluate(() => window.rstudio?.chat?.blocked ?? null);
}

/**
 * Install (or clear) an automation-only override for the next
 * chat_check_for_updates response. Resolves once rsession has acknowledged
 * the override.
 */
export async function setChatUpdateCheckOverride(
  page: Page,
  override: Record<string, unknown> | null,
): Promise<void> {
  await page.evaluate(async (o) => {
    if (!window.rstudio?.chat?.setUpdateCheckOverride) {
      throw new Error(
        'window.rstudio.chat.setUpdateCheckOverride missing; launch RStudio with --automation-agent',
      );
    }
    await window.rstudio.chat.setUpdateCheckOverride(o);
  }, override);
}

/**
 * Switch to the project at `projectFilePath` via the automation bridge.
 *
 * Resets `window.rstudio.ready` synchronously, fires SwitchToProjectEvent on
 * the GWT side (forceSaveAll=true to skip any save-changes prompt), then
 * waits for the new session's DeferredInitCompletedEvent. Replaces the
 * `.Rprofile`-sentinel marker dance for tests that don't need a custom
 * `.Rprofile` -- the readiness signal is the same `window.rstudio.ready`
 * flag the rest of the bridge relies on.
 *
 * Callers must reconstruct any page-action wrappers held over this call; the
 * session restart invalidates them.
 */
export async function openProject(
  page: Page,
  projectFilePath: string,
  timeout = 60000,
): Promise<void> {
  await page.evaluate((p) => {
    const r = window.rstudio;
    if (!r)
      throw new Error('window.rstudio is not defined; launch RStudio with --automation-agent');
    r.project.open(p);
  }, projectFilePath);

  // The Server mode page may navigate as part of the project switch; let it
  // settle before polling the bridge. On Desktop this is a no-op.
  await page.waitForLoadState('load', { timeout: 30000 }).catch(() => {});

  await page.waitForFunction(
    () => window.rstudio?.ready === true,
    null,
    { timeout, polling: 50 },
  );

  // ready=true tells us the workbench is wired up, but SessionInfo can
  // still report the previous project's path for a beat -- and the project
  // menu UI lags that. Poll project.path() against the requested file so
  // the helper's post-condition is "the bridge agrees this project is
  // active" rather than "ready flipped true." Case-insensitive to match
  // waitForActiveDocument's handling of HFS+ / NTFS.
  try {
    await page.waitForFunction(
      (target) => {
        const path = window.rstudio?.project.path() ?? null;
        return path !== null && path.replace(/\\/g, '/').toLowerCase() === target.replace(/\\/g, '/').toLowerCase();
      },
      projectFilePath,
      { timeout, polling: 100 },
    );
  } catch (err) {
    // ready flipped true but the active project never became the target.
    // OpenProjectErrorEvent also sets ready=true (see ApplicationAutomation
    // registerReadinessHandlers), so a silently-failed or lost open lands
    // here as an opaque timeout. Surface what the bridge actually reports so
    // the failure is "open failed / opened the wrong project" rather than a
    // bare waitForFunction timeout.
    if (err instanceof Error && err.name === 'TimeoutError') {
      const actual = await page
        .evaluate(() => window.rstudio?.project.path() ?? null)
        .catch(() => null);
      throw new Error(
        `openProject: session became ready but the active project did not ` +
        `become "${projectFilePath}" within ${timeout}ms (active project: ` +
        `${actual ?? 'none'}). This usually means the project open failed ` +
        `(OpenProjectErrorEvent) rather than that it was merely slow.`,
      );
    }
    throw err;
  }

  // The console-busy class can still be set briefly while the post-switch
  // prompt transition completes (same gap restartSessionWithSentinel
  // guards against in project.ts). Without this wait callers can issue a
  // console action into a still-busy session.
  await waitForConsoleIdle(page);
}

/** Read the RStudio + R version info installed on the automation bridge. */
export async function getVersion(page: Page): Promise<{ rstudio: string; r: string }> {
  return page.evaluate(() => {
    const r = window.rstudio;
    if (!r)
      throw new Error('window.rstudio is not defined; launch RStudio with --automation-agent');
    return { rstudio: r.version.rstudio, r: r.version.r };
  });
}
