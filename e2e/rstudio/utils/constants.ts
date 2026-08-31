export const TIMEOUTS = {
  processCleanup: 1000,
  rstudioStartup: 30000,
  consoleReady: 15000,
  sessionRestart: 30000,
  // The Environment pane requeries memory stats every memory_query_interval_seconds
  // (default 10s), so a memory-pie change can take a full interval to surface.
  memoryUsageUpdate: 30000,
  settleDelay: 1000,
  pollInterval: 500,
  fileOpen: 20000,
  fileEditSettle: 5000,
  ghostText: 30000,
  nesApply: 30000,
  displayOutput: 2000,
  layoutSettle: 300,
  // Per-character delay (ms) for typeSlowly. Long enough that GWT widgets
  // with typeahead/incremental-search handlers can finish reacting to one
  // keystroke before the next arrives.
  slowKeystroke: 200,
  packageInstall: 120000,
  // The suite-wide actionTimeout: playwright.config.ts reads it from here, and
  // typingTimeout() uses it as its floor, so the two cannot drift apart.
  action: 10000,
  // Per-character allowance for a pressSequentially budget: one CDP key-event
  // round trip plus whatever the widget does with it (a ProseMirror
  // transaction, a GWT typeahead query) on a loaded CI runner.
  keystrokeBudget: 50,
};

export async function sleep(ms: number): Promise<void> {
  return new Promise(resolve => setTimeout(resolve, ms));
}

/**
 * Timeout (ms) for a `pressSequentially` of `text`, with an optional
 * per-keystroke `delayMs`.
 *
 * Playwright applies the single configured `actionTimeout` to the whole call
 * however long the string is, so the budget per character shrinks as the text
 * grows: the assistant's ~940-character Shiny prompt gets ~10ms per character
 * to cover a CDP round trip plus a ProseMirror transaction, which a loaded CI
 * runner does not reliably beat. That surfaces as a "pressSequentially:
 * Timeout 10000ms exceeded" mid-message, which reads like a stuck widget
 * rather than the budget being too small for the input.
 *
 * Scale with the text instead, keeping the configured timeout as the floor so
 * short strings behave exactly as before.
 */
export function typingTimeout(text: string, delayMs: number = 0): number {
  return Math.max(TIMEOUTS.action, text.length * (TIMEOUTS.keystrokeBudget + delayMs));
}

/**
 * Send keystrokes one at a time with a delay between each. Use when typing
 * into a widget whose handler reacts on every keystroke (e.g. GWT type-ahead
 * lists in the Open File dialog), where the default `keyboard.type` speed
 * outraces the UI and characters get dropped or coalesced.
 */
export async function typeSlowly(
  page: import('@playwright/test').Page,
  text: string,
  delayMs: number = TIMEOUTS.slowKeystroke,
): Promise<void> {
  await page.keyboard.type(text, { delay: delayMs });
}

export const RSTUDIO_EXTRA_ARGS: string[] = process.env.PW_RSTUDIO_EXTRA_ARGS
  ? process.env.PW_RSTUDIO_EXTRA_ARGS.split(' ').filter(Boolean)
  : [];

export const CODE_SUGGESTION_PROVIDERS: Record<string, string> = {
  'copilot': 'GitHub Copilot',
  'posit-assistant': 'Posit AI Pass',
};

export const CHAT_PROVIDERS: Record<string, string> = {
  'posit-assistant': 'Posit Assistant',
};
