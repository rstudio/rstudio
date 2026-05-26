import type { Page } from '@playwright/test';
import { waitForConsoleIdle } from '../pages/console_pane.page';
import { withBridgeLog, withBridgeLogResult } from './log';
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

type RStudioBridge = {
  commands: { [id: string]: CommandEntry } & { list: string[] };
  prefs: { [name: string]: PrefEntry };
  documents: Documents;
  project: ProjectInfo;
  version: VersionInfo;
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
  await withBridgeLog('executeCommand', commandId, async () => {
    // The bridge is transiently absent during session restarts (project open
    // /close, Restart R). Wait for the specific command to land before
    // dispatching so callers don't have to manage that themselves. When the
    // bridge is already up, the condition is true on the first poll, so this
    // adds no measurable latency to the steady-state path.
    await page.waitForFunction(
      (id) => typeof (window.rstudio?.commands as Record<string, unknown> | undefined)?.[id] === 'function',
      commandId,
      { timeout: 10000, polling: 50 },
    );
    await page.evaluate((id) => {
      const r = window.rstudio!;
      const cmd = r.commands[id];
      cmd();
    }, commandId);
  });
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
  await withBridgeLog('saveDocument', '', async () => {
    await executeCommand(page, 'saveSourceDoc');
    await page.waitForFunction(
      () => {
        const doc = window.rstudio?.documents.active() ?? null;
        return doc !== null && !doc.dirty;
      },
      null,
      { timeout, polling: 100 },
    );
  });
}

/**
 * Close every open source document, discarding unsaved changes. Equivalent
 * to `.rs.api.closeAllSourceBuffersWithoutSaving()` but skips the R-side
 * round trip (and therefore the "session is busy" dialog risk).
 */
export async function documentCloseAllNoSave(page: Page): Promise<void> {
  await withBridgeLog('documentCloseAllNoSave', '', async () => {
    await page.evaluate(() => {
      const r = window.rstudio;
      if (!r)
        throw new Error('window.rstudio is not defined; launch RStudio with --automation-agent');
      r.documents.closeAllNoSave();
    });
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
  await withBridgeLog('documentOpen', path, async () => {
    await page.evaluate(({ docPath, options }) => {
      const r = window.rstudio;
      if (!r)
        throw new Error('window.rstudio is not defined; launch RStudio with --automation-agent');
      r.documents.open(docPath, options);
    }, { docPath: path, options: opts });
    await waitForActiveDocument(page, path, timeout);
  });
}

/**
 * Reset the source pane to a single untitled document, discarding any unsaved
 * changes in other tabs. If an untitled tab is already open it is kept;
 * otherwise a fresh one is created before the others close, so the pane
 * never transitions through the zero-tab HIDE state -- the gap that races a
 * subsequent file.edit (#17738). Prefer this over documentCloseAllNoSave for
 * cross-test resets where a following file.edit / newDoc is imminent.
 */
export async function resetSourcePaneState(page: Page): Promise<void> {
  await withBridgeLog('resetSourcePaneState', '', async () => {
    await page.evaluate(() => {
      const r = window.rstudio;
      if (!r)
        throw new Error('window.rstudio is not defined; launch RStudio with --automation-agent');
      r.documents.resetToUntitled();
    });
  });
}

/**
 * Wait until the focused source document's path equals `expectedPath`. Polls
 * `window.rstudio.documents.active()` -- the bridge value updates the moment
 * the active editor changes, so this is a deterministic post-condition for
 * `.rs.api.documentOpen(...)`, `file.edit(...)`, and other open-by-path flows.
 *
 * On case-insensitive filesystems (macOS HFS+, NTFS) the comparison ignores
 * case so callers can pass the same string they handed to R without worrying
 * about case-folding round-trips.
 */
export async function waitForActiveDocument(
  page: Page,
  expectedPath: string,
  timeout = 10000,
): Promise<void> {
  await page.waitForFunction(
    (target) => {
      const doc = window.rstudio?.documents.active() ?? null;
      return doc !== null && doc.path !== null
        && doc.path.toLowerCase() === target.toLowerCase();
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
  return withBridgeLogResult(
    'getPref',
    (result) => `${name}=${JSON.stringify(result)}`,
    async () => {
      const camel = snakeToCamel(name);
      return page.evaluate((prefName) => {
        const r = window.rstudio;
        if (!r)
          throw new Error('window.rstudio is not defined; launch RStudio with --automation-agent');
        const entry = r.prefs[prefName];
        return entry ? entry.get() : null;
      }, camel);
    },
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
  await withBridgeLog('setPref', `${name}=${JSON.stringify(value)}`, async () => {
    const camel = snakeToCamel(name);
    await page.evaluate(async ({ prefName, prefValue }) => {
      const r = window.rstudio;
      if (!r)
        throw new Error('window.rstudio is not defined; launch RStudio with --automation-agent');
      const entry = r.prefs[prefName];
      if (!entry)
        throw new Error(`Unknown user preference: ${prefName}`);
      await entry.set(prefValue);
    }, { prefName: camel, prefValue: value });
  });
}

/**
 * Remove a user-layer preference value (the default takes over). Equivalent
 * to `.rs.uiPrefs$<name>$clear()`. Awaits the setUserPrefs RPC completion
 * server-side before returning.
 */
export async function clearPref(page: Page, name: string): Promise<void> {
  await withBridgeLog('clearPref', name, async () => {
    const camel = snakeToCamel(name);
    await page.evaluate(async (prefName) => {
      const r = window.rstudio;
      if (!r)
        throw new Error('window.rstudio is not defined; launch RStudio with --automation-agent');
      const entry = r.prefs[prefName];
      if (!entry)
        throw new Error(`Unknown user preference: ${prefName}`);
      await entry.clear();
    }, camel);
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
  await withBridgeLog(
    'setChatUpdateCheckOverride',
    override === null ? 'null' : JSON.stringify(override),
    async () => {
      await page.evaluate(async (o) => {
        if (!window.rstudio?.chat?.setUpdateCheckOverride) {
          throw new Error(
            'window.rstudio.chat.setUpdateCheckOverride missing; launch RStudio with --automation-agent',
          );
        }
        await window.rstudio.chat.setUpdateCheckOverride(o);
      }, override);
    },
  );
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
  await withBridgeLog('openProject', projectFilePath, async () => {
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
    await page.waitForFunction(
      (target) => {
        const path = window.rstudio?.project.path() ?? null;
        return path !== null && path.toLowerCase() === target.toLowerCase();
      },
      projectFilePath,
      { timeout, polling: 100 },
    );

    // The console-busy class can still be set briefly while the post-switch
    // prompt transition completes (same gap restartSessionWithSentinel
    // guards against in project.ts). Without this wait callers can issue a
    // console action into a still-busy session.
    await waitForConsoleIdle(page);
  });
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
