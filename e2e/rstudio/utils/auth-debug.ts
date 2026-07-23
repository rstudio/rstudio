import { chromium, type Page } from '@playwright/test';
import * as fs from 'fs';
import * as path from 'path';

/**
 * Shared browser launcher for the live sign-in flows (Posit AI, GitHub
 * Copilot, and any future provider login), with opt-in page capture.
 *
 * Every flow gets its page from launchAuthBrowser(), which owns the launch
 * configuration (headless, automation flags hidden, common user agent) and
 * the diagnostics -- so a new provider flow gets both by construction,
 * without any provider-specific debug code.
 *
 * The auth setup project runs with Playwright artifacts off (trace, video,
 * screenshot -- see setupProject in playwright.config.ts) because real
 * credentials are typed into the pages it drives. By default this module
 * honors that policy: it logs page URLs, titles, and console errors (the
 * non-sensitive context the flows have always reported) and writes nothing
 * to disk. Playwright tracing is deliberately not used even for debugging:
 * a trace records fill() argument values, i.e. the password itself.
 *
 * Set PW_DEBUG_AUTH_CAPTURE=1 to also write a numbered full-page screenshot and the
 * page HTML into test-results/ on every page load, plus a final capture just
 * before the browser closes (which is the failure-state capture when a flow
 * throws). That is what you want when a provider's login page changes markup
 * and the flow needs re-mapping -- the realistic failure mode these flows
 * have. Only enable it on machines where capturing those pages is
 * acceptable: the dumps can include the typed username (passwords stay safe
 * -- masked in screenshots, and fill() does not serialize values into the
 * HTML). Limitation: captures trigger on page loads, so a page that morphs
 * in place (SPA-style, e.g. Posit's inline password step) is only seen in
 * the next load or the final capture.
 */

// PW_DEBUG_AUTH_CAPTURE: write page screenshots + HTML to disk.
export function authCaptureEnabled(): boolean {
  return ['1', 'true'].includes((process.env.PW_DEBUG_AUTH_CAPTURE ?? '').toLowerCase());
}

// PW_DEBUG_AUTH_STEPS: emit the sign-in flows' step-level console output --
// the browser narration ([gh-authorize]: which page, which field, button
// state) and the Copilot agent's raw diagnostics ([copilot-agent]: LSP
// notifications, spawn/exit, checkStatus polling). Off by default, so a normal
// run shows only the [auth-setup] outcome milestones; this detail is opt-in
// for troubleshooting a flow. Independent of authCaptureEnabled -- this output
// is authored text kept free of account identifiers, whereas the captures
// include unredacted page content, so the lower-risk detail has its own switch.
export function authStepsEnabled(): boolean {
  return ['1', 'true'].includes((process.env.PW_DEBUG_AUTH_STEPS ?? '').toLowerCase());
}

// One shared user agent: a plain desktop Chrome, so the login pages serve
// their standard markup rather than an automation-flavored variant.
const USER_AGENT =
  'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36';

export interface AuthBrowserSession {
  page: Page;
  /** Final page capture, then browser shutdown. Never throws. */
  close(): Promise<void>;
}

// A filename-safe label for the page's current host, e.g. "github.com".
function hostLabel(page: Page): string {
  try {
    return new URL(page.url()).hostname.replace(/[^a-zA-Z0-9.-]/g, '_') || 'page';
  } catch {
    return 'page';
  }
}

// Origin + path only, dropping the query string. Device-flow URLs carry the
// user code in a query param (e.g. .../login?redirect=...user_code=XXXX-XXXX),
// so logging the raw URL would print the code. The path alone is enough to
// tell which page we're on.
function safeUrl(page: Page): string {
  try {
    const u = new URL(page.url());
    return `${u.origin}${u.pathname}`;
  } catch {
    return '(unknown url)';
  }
}

/**
 * Launch the headless browser a sign-in flow drives, with logging and the
 * PW_DEBUG_AUTH_CAPTURE capture wired up. Callers must close via session.close() (in
 * a finally), which takes the final capture and shuts the browser down.
 */
export async function launchAuthBrowser(flowName: string): Promise<AuthBrowserSession> {
  // Two gates, so quiet mode (neither flag) stays purely [auth-setup]:
  //   stepLog    -- browser diagnostics (final page, page errors); shown with
  //                 PW_DEBUG_AUTH_STEPS, same switch as the flows' step logs.
  //   captureLog -- capture-mechanism messages (what was written); shown with
  //                 PW_DEBUG_AUTH_CAPTURE, and only reached when it's set.
  const stepLog = (msg: string) => {
    if (authStepsEnabled()) console.log(`[auth-browser] [${flowName}] ${msg}`);
  };
  const captureLog = (msg: string) => {
    if (authCaptureEnabled()) console.log(`[auth-browser] [${flowName}] ${msg}`);
  };

  const browser = await chromium.launch({
    headless: true,
    args: ['--disable-blink-features=AutomationControlled'],
  });
  const context = await browser.newContext({ userAgent: USER_AGENT });
  // Hide the automation flag before any page script runs: some login pages
  // gate behavior on navigator.webdriver, and there is no reason to advertise
  // automation to them.
  await context.addInitScript(() => {
    Object.defineProperty(navigator, 'webdriver', { get: () => undefined });
  });
  const page = await context.newPage();

  page.on('pageerror', (err) => stepLog(`[pageerror] ${err.message}`));
  page.on('console', (msg) => {
    if (msg.type() === 'error') stepLog(`[console.error] ${msg.text()}`);
  });

  // Numbered captures tell the story in order; the counter also keys the
  // final capture, so a run's files sort chronologically.
  let seq = 0;
  const capture = async (label: string): Promise<void> => {
    try {
      seq += 1;
      const name = `auth-debug-${flowName}-${String(seq).padStart(2, '0')}-${label}`;
      stepLog(`[${label}] page ${safeUrl(page)} (title: ${await page.title().catch(() => '?')})`);
      if (!authCaptureEnabled()) return;
      const outDir = path.join(__dirname, '..', 'test-results');
      fs.mkdirSync(outDir, { recursive: true });
      await page.screenshot({ path: path.join(outDir, `${name}.png`), fullPage: true });
      fs.writeFileSync(path.join(outDir, `${name}.html`), await page.content());
      captureLog(`[${label}] saved test-results/${name}.png and .html`);
    } catch (dumpErr) {
      // A failed capture must never replace the flow failure it documents.
      captureLog(`[${label}] could not capture page: ${(dumpErr as Error).message}`);
    }
  };

  if (authCaptureEnabled()) {
    captureLog('PW_DEBUG_AUTH_CAPTURE is set; capturing every page load into test-results/');
    page.on('load', () => {
      void capture(hostLabel(page));
    });
  }

  return {
    page,
    close: async () => {
      // The final capture doubles as the failure-state record: flows close
      // in a finally, so on a throw this is the page as it stood when the
      // flow gave up.
      await capture('final');
      try {
        await browser.close();
      } catch (err) {
        // A rejection escaping a caller's finally would replace the in-flight
        // flow error -- log and discard instead.
        stepLog(`failed to close the sign-in browser: ${(err as Error).message}`);
      }
    },
  };
}
