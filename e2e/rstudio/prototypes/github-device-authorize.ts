import { chromium, type Page } from '@playwright/test';
import crypto from 'crypto';
import * as fs from 'fs';
import * as path from 'path';

/**
 * Prototype: the browser half of the Copilot device-flow sign-in, extracted
 * from copilot-authorize.spec.ts (where every step here was proven live) and
 * parameterized so a caller that already holds a userCode/verificationUri --
 * i.e. the copilot-language-server after signInInitiate -- can complete the
 * GitHub authorization.
 *
 * What this deliberately does NOT contain, compared to the original spec:
 * the device-code fetch and the token poll. The agent owns both ends of that
 * exchange; our job is only the human part in the middle (sign in, enter the
 * code, click Authorize with a trusted interaction).
 */

export interface AuthorizeDeviceCodeOptions {
  /** GitHub's verification URI from signInInitiate (https://github.com/login/device). */
  verificationUri: string;
  /** The XXXX-XXXX user code from signInInitiate. */
  userCode: string;
  /** GitHub login (username, not email). */
  user: string;
  /** That account's password. */
  password: string;
  /** Optional base32 2FA secret, only if the account has an authenticator app enrolled. */
  totpSecret?: string;
}

function log(msg: string): void {
  console.log(`[gh-authorize] ${msg}`);
}

// This browser is launched directly, so it sits outside Playwright's
// trace/screenshot capture. Dump the live page state to files so a stuck step
// is diagnosable from the actual DOM rather than guesswork.
async function dumpPage(page: Page, label: string): Promise<void> {
  try {
    log(`[${label}] page ${page.url()} (title: ${await page.title().catch(() => '?')})`);
    await page.screenshot({ path: path.join(__dirname, `last-${label}.png`), fullPage: true });
    fs.writeFileSync(path.join(__dirname, `last-${label}.html`), await page.content());
    log(`[${label}] saved prototypes/last-${label}.png and last-${label}.html`);
  } catch (dumpErr) {
    log(`[${label}] could not capture page: ${(dumpErr as Error).message}`);
  }
}

// ---------------------------------------------------------------------------
// TOTP (RFC 6238), used only if the account has 2FA enrolled. Dependency-free
// so the prototype adds nothing to package.json: base32-decode the secret, HMAC
// it against the current 30s time window, truncate to 6 digits. That is the
// same computation an authenticator app runs, so it yields the code GitHub
// expects.

function base32Decode(input: string): Buffer {
  const alphabet = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ234567';
  const clean = input.replace(/=+$/, '').replace(/\s/g, '').toUpperCase();
  let bits = '';
  for (const ch of clean) {
    const idx = alphabet.indexOf(ch);
    if (idx === -1) continue;
    bits += idx.toString(2).padStart(5, '0');
  }
  const bytes: number[] = [];
  for (let i = 0; i + 8 <= bits.length; i += 8) {
    bytes.push(parseInt(bits.slice(i, i + 8), 2));
  }
  return Buffer.from(bytes);
}

function totp(secret: string): string {
  const key = base32Decode(secret);
  const counter = Math.floor(Date.now() / 1000 / 30);
  const buf = Buffer.alloc(8);
  buf.writeBigInt64BE(BigInt(counter));
  const hmac = crypto.createHmac('sha1', key).update(buf).digest();
  const offset = hmac[hmac.length - 1] & 0xf;
  const bin =
    ((hmac[offset] & 0x7f) << 24) |
    ((hmac[offset + 1] & 0xff) << 16) |
    ((hmac[offset + 2] & 0xff) << 8) |
    (hmac[offset + 3] & 0xff);
  return (bin % 1_000_000).toString().padStart(6, '0');
}

// ---------------------------------------------------------------------------
// Browser steps (each proven in the copilot-authorize.spec.ts runs).

async function signIn(page: Page, user: string, password: string, totpSecret?: string): Promise<void> {
  const loginField = page.locator('#login_field');
  if (await loginField.isVisible({ timeout: 10_000 }).catch(() => false)) {
    log('login page shown; entering username and password');
    await loginField.fill(user);
    await page.locator('#password').fill(password);
    await page.getByRole('button', { name: 'Sign in', exact: true }).click();
  } else {
    log('no login form (session may already be authenticated)');
  }

  // 2FA (authenticator app). GitHub renders the app-code field as #app_totp.
  const totpField = page.locator('#app_totp');
  if (await totpField.isVisible({ timeout: 5_000 }).catch(() => false)) {
    if (!totpSecret) {
      throw new Error(
        'GitHub is asking for a 2FA authenticator code, but GH_COPILOT_TOTP_SECRET is not set. '
        + 'Add the account\'s base32 2FA secret to .env.local, or remove 2FA from the account.',
      );
    }
    log('2FA prompt shown; entering generated TOTP code');
    await totpField.fill(totp(totpSecret));
  }
}

async function enterUserCode(page: Page, userCode: string): Promise<void> {
  // GitHub splits the code across per-character inputs (#user-code-0..8, the
  // 5th being the dash).
  const first = page.locator('#user-code-0');

  // After signing in, GitHub can show a device-activation intro with a Continue
  // button before the code-entry boxes appear. Only click it when the code
  // field is not already present, so we never submit an empty code on flows
  // that skip the intro.
  if (!(await first.isVisible({ timeout: 5_000 }).catch(() => false))) {
    const intro = page.getByRole('button', { name: /continue/i });
    if (await intro.isVisible({ timeout: 3_000 }).catch(() => false)) {
      log('device-activation intro shown; clicking Continue to reach the code form');
      await intro.click();
    }
  }

  await first.waitFor({ state: 'visible', timeout: 20_000 });
  log('device activation page shown; entering user code');

  // Real keystrokes into the first box with a per-key delay: GitHub auto-advances
  // focus after each character, and the delay lets that settle so none are
  // dropped (the race that produced device/failure?reason=not_found). fill()
  // would be synthetic and is what the reactive form ignores.
  const code = userCode.replace(/-/g, '');
  await first.click();
  await first.pressSequentially(code, { delay: 150 });

  // Read every user-code box back so a partial entry is visible in the log
  // before we submit.
  const boxes = page.locator('[id^="user-code-"]');
  const n = await boxes.count();
  const vals: string[] = [];
  for (let i = 0; i < n; i++) vals.push(await boxes.nth(i).inputValue().catch(() => '?'));
  log(`user-code boxes (${n}) hold "${vals.join('')}" (expected "${code}")`);
  await page.getByRole('button', { name: /continue/i }).click();
}

async function authorize(page: Page): Promise<void> {
  const authorizeBtn = page.locator('.js-oauth-authorize-btn');

  // On a re-run the OAuth app may already be authorized, so GitHub can skip
  // straight to a success page and no button appears. That is not necessarily
  // a failure; the caller's checkStatus poll is the arbiter of whether
  // authorization actually happened.
  const appeared = await authorizeBtn
    .waitFor({ state: 'visible', timeout: 20_000 })
    .then(() => true)
    .catch(() => false);
  if (!appeared) {
    log('.js-oauth-authorize-btn not found; capturing page to diagnose');
    await dumpPage(page, 'no-authorize-button');
    return;
  }

  const initiallyDisabled = await authorizeBtn.isDisabled();
  log(`Authorize button visible; initially ${initiallyDisabled ? 'DISABLED' : 'enabled'}`);

  // The proven core: GitHub's anti-clickjacking gate arms the button only
  // after a genuine trusted interaction, specifically a wheel/scroll event.
  // page.mouse.wheel dispatches through CDP (isTrusted=true) and enables the
  // button within a few seconds; scrollIntoViewIfNeeded is a JS scroll and
  // does not count on its own (it is here just to bring the button on-screen).
  await authorizeBtn.scrollIntoViewIfNeeded();
  await authorizeBtn.hover();
  await page.mouse.wheel(0, 300);

  const t0 = Date.now();
  const deadline = t0 + 120_000;
  while (Date.now() < deadline) {
    if (await authorizeBtn.isEnabled()) break;
    await page.waitForTimeout(3000);
  }
  const enabled = await authorizeBtn.isEnabled();
  log(`after ${Math.round((Date.now() - t0) / 1000)}s the Authorize button is ${enabled ? 'ENABLED' : 'still DISABLED'}`);
  if (!enabled) {
    await dumpPage(page, 'still-disabled');
    throw new Error('Authorize button never enabled within 120s');
  }

  await authorizeBtn.click();
  log('clicked Authorize (trusted click, no attribute hack)');
}

// ---------------------------------------------------------------------------

/**
 * Complete GitHub's device-flow authorization in a browser: sign in, enter the
 * user code, and click Authorize via a trusted interaction. Launches (and
 * closes) its own headless Chromium; proven to work without a display.
 *
 * This function returning is necessary but not sufficient: the caller must
 * confirm the grant registered (for the Copilot agent, by polling checkStatus
 * until it reports OK).
 */
export async function authorizeDeviceCode(opts: AuthorizeDeviceCodeOptions): Promise<void> {
  const browser = await chromium.launch({
    headless: true,
    args: ['--disable-blink-features=AutomationControlled'],
  });
  try {
    const context = await browser.newContext({
      userAgent:
        'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36',
    });
    // Hide the automation flag before any page script runs. The wheel event is
    // what actually arms the Authorize button, but there is no reason to
    // advertise automation to the rest of GitHub's scripts either.
    await context.addInitScript(() => {
      Object.defineProperty(navigator, 'webdriver', { get: () => undefined });
    });
    const page = await context.newPage();

    page.on('pageerror', (err) => log(`[pageerror] ${err.message}`));
    page.on('console', (msg) => {
      if (msg.type() === 'error') log(`[console.error] ${msg.text()}`);
    });

    try {
      await page.goto(opts.verificationUri, { waitUntil: 'domcontentloaded' });
      await signIn(page, opts.user, opts.password, opts.totpSecret);
      await enterUserCode(page, opts.userCode);
      await authorize(page);
    } catch (err) {
      await dumpPage(page, 'failure');
      throw err;
    }
  } finally {
    await browser.close();
  }
}
