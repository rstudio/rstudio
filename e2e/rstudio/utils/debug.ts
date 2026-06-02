import type { Page } from '@playwright/test';
import { executeInConsole } from '../pages/console_pane.page';

const CONSOLE_INPUT_ID = 'rstudio_console_input';
const WAITING_FOR_INPUT_CLASS = 'rstudio-console-waiting-for-input';

/**
 * Whether the interactive debug harness is enabled (PW_DEBUG=1). When set,
 * the desktop fixture opens Chromium DevTools on startup and the per-test
 * beforeEach parks the run via waitForUserConsoleInput. Off by default.
 */
export function isDebugMode(): boolean {
  const flag = process.env.PW_DEBUG?.toLowerCase();
  return flag === '1' || flag === 'true';
}

/**
 * Debug-only pause that hands control back to a human mid-test, prompting in
 * the RStudio Console pane itself.
 *
 * Motivating case: investigating a renderer freeze (e.g. the data viewer
 * stalling the IDE when the summary panel renders many columns). Automated
 * assertions can see *that* something is slow, but not *why* -- for that you
 * want Chromium DevTools' Performance profiler armed before the slow action
 * runs. This helper parks the test so you can do exactly that.
 *
 * It issues readline() into the R console, which blocks R until you press
 * Enter in the Console pane. R reports a readline prompt by clearing the busy
 * state, so we can't watch the busy class; instead we watch the dedicated
 * rstudio-console-waiting-for-input class (added on a non-history prompt) --
 * it turns on while R waits and off the moment you submit input.
 *
 * No-op unless PW_DEBUG is set, so it is safe to leave wired into a beforeEach
 * hook; normal and CI runs skip it entirely. The desktop fixture also opens
 * DevTools under PW_DEBUG so the profiler is ready when you resume. A long
 * fallback timeout (default 1h, override with PW_DEBUG_TIMEOUT_MS) keeps a
 * forgotten pause from wedging forever.
 */
export async function waitForUserConsoleInput(page: Page, action: string = 'continue'): Promise<void> {
  if (!isDebugMode()) return;

  const timeout = Number(process.env.PW_DEBUG_TIMEOUT_MS) || 60 * 60 * 1000;
  const prompt = `[DEBUG] Press <Enter> in the Console to ${action} `;

  // Submit readline() fire-and-forget: it blocks R awaiting console input, so
  // executeInConsole's own idle wait would never return (and the console reads
  // as idle, not busy, while readline is pending). JSON.stringify yields a
  // valid R double-quoted string literal for the ASCII prompt text.
  await executeInConsole(page, `invisible(readline(${JSON.stringify(prompt)}))`, { wait: false });

  console.log('');
  console.log('========================================================================');
  console.log(`[waitForUserConsoleInput] Paused. ${prompt.trim()}`);
  console.log('[waitForUserConsoleInput] (DevTools is open under PW_DEBUG -- arm the');
  console.log('[waitForUserConsoleInput]  Performance profiler now, then press Enter');
  console.log('[waitForUserConsoleInput]  in the RStudio Console pane to proceed.)');
  console.log(`[waitForUserConsoleInput] (auto-resumes after ${Math.round(timeout / 1000)}s)`);
  console.log('========================================================================');
  console.log('');

  // Wait until R is actually blocked in readline...
  await page.waitForFunction(
    ([id, cls]) => {
      const el = document.getElementById(id);
      return !!el && el.classList.contains(cls);
    },
    [CONSOLE_INPUT_ID, WAITING_FOR_INPUT_CLASS] as const,
    { timeout: 10000, polling: 100 },
  );

  // ...then until the user answers the prompt (the class clears on submit).
  await page.waitForFunction(
    ([id, cls]) => {
      const el = document.getElementById(id);
      return !!el && !el.classList.contains(cls);
    },
    [CONSOLE_INPUT_ID, WAITING_FOR_INPUT_CLASS] as const,
    { timeout, polling: 200 },
  );

  console.log('[waitForUserConsoleInput] Resumed.');
}
