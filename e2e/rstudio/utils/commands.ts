import type { Page } from '@playwright/test';

// `window.rstudioCallbacks` is registered when rsession runs with
// `--automation-agent` (the Desktop fixture forwards that flag). The bridge
// lets tests trigger and inspect AppCommands without typing through the
// console -- avoiding the focus-shift and pane-collapse pitfalls that the
// console path runs into.

type PrefValue = boolean | number | string;

type RStudioCallbacks = {
  commandExecute(id: string): void;
  commandIsChecked(id: string): boolean;
  commandIsEnabled(id: string): boolean;
  commandList(): string[];
  documentCloseAllNoSave(): void;
  prefGet(name: string): PrefValue | null;
  prefSet(name: string, value: PrefValue): void;
  prefClear(name: string): void;
};

declare global {
  interface Window {
    rstudioCallbacks?: RStudioCallbacks;
  }
}

/** Run an AppCommand by id (no console roundtrip). */
export async function executeCommand(page: Page, commandId: string): Promise<void> {
  await page.evaluate((id) => {
    const cb = window.rstudioCallbacks;
    if (!cb) throw new Error('window.rstudioCallbacks is not defined; launch RStudio with --automation-agent');
    cb.commandExecute(id);
  }, commandId);
}

/** True when the named AppCommand reports `isChecked` (e.g. an active layout zoom). */
export async function isCommandChecked(page: Page, commandId: string): Promise<boolean> {
  return page.evaluate((id) => {
    const cb = window.rstudioCallbacks;
    if (!cb) throw new Error('window.rstudioCallbacks is not defined; launch RStudio with --automation-agent');
    return cb.commandIsChecked(id);
  }, commandId);
}

/** True when the named AppCommand is currently enabled. */
export async function isCommandEnabled(page: Page, commandId: string): Promise<boolean> {
  return page.evaluate((id) => {
    const cb = window.rstudioCallbacks;
    if (!cb) throw new Error('window.rstudioCallbacks is not defined; launch RStudio with --automation-agent');
    return cb.commandIsEnabled(id);
  }, commandId);
}

/**
 * Close every open source document, discarding unsaved changes. Equivalent
 * to `.rs.api.closeAllSourceBuffersWithoutSaving()` but skips the R-side
 * round trip (and therefore the "session is busy" dialog risk).
 */
export async function documentCloseAllNoSave(page: Page): Promise<void> {
  await page.evaluate(() => {
    const cb = window.rstudioCallbacks;
    if (!cb) throw new Error('window.rstudioCallbacks is not defined; launch RStudio with --automation-agent');
    cb.documentCloseAllNoSave();
  });
}

/**
 * Read a user preference by name. Returns null when the preference name is
 * unknown.
 */
export async function getPref(page: Page, name: string): Promise<PrefValue | null> {
  return page.evaluate((prefName) => {
    const cb = window.rstudioCallbacks;
    if (!cb) throw new Error('window.rstudioCallbacks is not defined; launch RStudio with --automation-agent');
    return cb.prefGet(prefName);
  }, name);
}

/**
 * Set a user preference and persist it. Equivalent to
 * `.rs.api.writeRStudioPreference(name, value)` / `.rs.uiPrefs$<name>$set(value)`.
 * The bridge dispatches by the preference's declared type, so the JS value
 * must match: pass a boolean for boolean prefs, a number for integer/double
 * prefs, a string for string/enum prefs.
 */
export async function setPref(page: Page, name: string, value: PrefValue): Promise<void> {
  await page.evaluate(({ prefName, prefValue }) => {
    const cb = window.rstudioCallbacks;
    if (!cb) throw new Error('window.rstudioCallbacks is not defined; launch RStudio with --automation-agent');
    cb.prefSet(prefName, prefValue);
  }, { prefName: name, prefValue: value });
}

/**
 * Remove a user-layer preference value (the default takes over). Equivalent
 * to `.rs.uiPrefs$<name>$clear()`.
 */
export async function clearPref(page: Page, name: string): Promise<void> {
  await page.evaluate((prefName) => {
    const cb = window.rstudioCallbacks;
    if (!cb) throw new Error('window.rstudioCallbacks is not defined; launch RStudio with --automation-agent');
    cb.prefClear(prefName);
  }, name);
}
