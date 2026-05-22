import type { Page } from '@playwright/test';

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
  set(value: PrefValue): void;
  clear(): void;
};

type ProjectInfo = {
  /** Active project file path (absolute), or null if no project is open. */
  path(): string | null;
  /** Active project display name, or null if no project is open. */
  name(): string | null;
  isActive(): boolean;
};

type VersionInfo = {
  /** RStudio long-version string, e.g. "2026.05.999". */
  rstudio: string;
  /** R version number, e.g. "4.5.3". */
  r: string;
};

type RStudioBridge = {
  commands: { [id: string]: CommandEntry } & { list: string[] };
  prefs: { [name: string]: PrefEntry };
  documents: { closeAllNoSave(): void };
  project: ProjectInfo;
  version: VersionInfo;
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

/**
 * Set a user preference and persist it. Equivalent to
 * `.rs.api.writeRStudioPreference(name, value)` / `.rs.uiPrefs$<name>$set(value)`.
 * The bridge dispatches by the preference's declared type, so the JS value
 * must match: pass a boolean for boolean prefs, a number for integer/double
 * prefs, a string for string/enum prefs.
 */
export async function setPref(page: Page, name: string, value: PrefValue): Promise<void> {
  const camel = snakeToCamel(name);
  await page.evaluate(({ prefName, prefValue }) => {
    const r = window.rstudio;
    if (!r)
      throw new Error('window.rstudio is not defined; launch RStudio with --automation-agent');
    const entry = r.prefs[prefName];
    if (!entry)
      throw new Error(`Unknown user preference: ${prefName}`);
    entry.set(prefValue);
  }, { prefName: camel, prefValue: value });
}

/**
 * Remove a user-layer preference value (the default takes over). Equivalent
 * to `.rs.uiPrefs$<name>$clear()`.
 */
export async function clearPref(page: Page, name: string): Promise<void> {
  const camel = snakeToCamel(name);
  await page.evaluate((prefName) => {
    const r = window.rstudio;
    if (!r)
      throw new Error('window.rstudio is not defined; launch RStudio with --automation-agent');
    const entry = r.prefs[prefName];
    if (!entry)
      throw new Error(`Unknown user preference: ${prefName}`);
    entry.clear();
  }, camel);
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
