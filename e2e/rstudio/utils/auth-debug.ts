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
 * Set PW_DEBUG_AUTH=1 to also write a numbered full-page screenshot and the
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

export function authDebugEnabled(): boolean {
  return ['1', 'true'].includes((process.env.PW_DEBUG_AUTH ?? '').toLowerCase());
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

/**
 * Launch the headless browser a sign-in flow drives, with logging and the
 * PW_DEBUG_AUTH capture wired up. Callers must close via session.close() (in
 * a finally), which takes the final capture and shuts the browser down.
 */
export async function launchAuthBrowser(flowName: string): Promise<AuthBrowserSession> {
  const log = (msg: string) => console.log(`[auth-browser] [${flowName}] ${msg}`);

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

  page.on('pageerror', (err) => log(`[pageerror] ${err.message}`));
  page.on('console', (msg) => {
    if (msg.type() === 'error') log(`[console.error] ${msg.text()}`);
  });

  // Numbered captures tell the story in order; the counter also keys the
  // final capture, so a run's files sort chronologically.
  let seq = 0;
  const capture = async (label: string): Promise<void> => {
    try {
      seq += 1;
      const name = `auth-debug-${flowName}-${String(seq).padStart(2, '0')}-${label}`;
      log(`[${label}] page ${page.url()} (title: ${await page.title().catch(() => '?')})`);
      if (!authDebugEnabled()) return;
      const outDir = path.join(__dirname, '..', 'test-results');
      fs.mkdirSync(outDir, { recursive: true });
      await page.screenshot({ path: path.join(outDir, `${name}.png`), fullPage: true });
      fs.writeFileSync(path.join(outDir, `${name}.html`), await page.content());
      log(`[${label}] saved test-results/${name}.png and .html`);
    } catch (dumpErr) {
      // A failed capture must never replace the flow failure it documents.
      log(`[${label}] could not capture page: ${(dumpErr as Error).message}`);
    }
  };

  if (authDebugEnabled()) {
    log('PW_DEBUG_AUTH is set; capturing every page load into test-results/');
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
        log(`failed to close the sign-in browser: ${(err as Error).message}`);
      }
    },
  };
}
