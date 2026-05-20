import type { Page } from '@playwright/test';

// `window.rstudioCallbacks` is registered when rsession runs with
// `--automation-agent` (the Desktop fixture forwards that flag). The bridge
// lets tests trigger and inspect AppCommands without typing through the
// console -- avoiding the focus-shift and pane-collapse pitfalls that the
// console path runs into.

type RStudioCallbacks = {
  commandExecute(id: string): void;
  commandIsChecked(id: string): boolean;
  commandIsEnabled(id: string): boolean;
  commandList(): string[];
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
