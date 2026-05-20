export const TIMEOUTS = {
  processCleanup: 1000,
  rstudioStartup: 10000,
  consoleReady: 15000,
  sessionRestart: 30000,
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
};

export async function sleep(ms: number): Promise<void> {
  return new Promise(resolve => setTimeout(resolve, ms));
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
  'posit-assistant': 'Posit AI',
};

export const CHAT_PROVIDERS: Record<string, string> = {
  'posit-assistant': 'Posit Assistant',
};
