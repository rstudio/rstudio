import { test, expect, chromium, type Page, type Locator } from '@playwright/test';
import crypto from 'crypto';
import * as fs from 'fs';

/**
 * Prototype: get past GitHub's Authorize button in the device flow WITHOUT the
 * removeAttribute('disabled') hack that the Workbench test used.
 *
 * The one thing this proves is the hard, unproven part of Copilot login
 * parity: that a genuine (trusted) Playwright interaction satisfies GitHub's
 * clickjacking guard on the device-authorization "Authorize" button, so a
 * normal click registers and the device_code becomes exchangeable. Everything
 * else here (device-code fetch, browser login, token poll) is scaffolding that
 * mirrors tests/auth.setup.ts, present only so the authorize step runs in a
 * real end-to-end context. The token poll is the arbiter: if authorize did not
 * truly register, the poll times out.
 *
 * This is a throwaway. It does not write any Copilot credential store, touch
 * the sandbox, or launch RStudio. It runs under its own config
 * (prototypes/playwright.prototype.config.ts).
 *
 * Credentials (e2e/rstudio/.env.local, gitignored; use a throwaway account):
 *   COPILOT_USER          GitHub login (username, not email)
 *   COPILOT_PASSWORD      that account's password
 *   COPILOT_TOTP_SECRET   optional; base32 2FA secret. Only needed if the
 *                         account has an authenticator app enrolled.
 */

// The public client id of the GitHub Copilot OAuth app; the same value the
// editor plugins use to start the device flow. Not a secret.
const CLIENT_ID = 'Iv1.b507a08c87ecfe98';
const SCOPE = 'read:user';

function log(msg: string): void {
  console.log(`[copilot-proto] ${msg}`);
}

// Locator.isVisible({ timeout }) does not actually wait -- Playwright's own
// types document the option as ignored, so it checks the DOM at that instant
// and returns immediately. Every optional-step check below needs to
// genuinely wait (a step-conditional page can still be mid-navigation or
// mid-render when we ask), so route them all through waitFor instead: true
// if the element shows up within timeoutMs, false (not thrown) otherwise.
async function appears(locator: Locator, timeoutMs: number): Promise<boolean> {
  return locator
    .waitFor({ state: 'visible', timeout: timeoutMs })
    .then(() => true)
    .catch(() => false);
}

// This browser is launched directly, so it sits outside Playwright's
// trace/screenshot capture. Dump the live page state to files so a stuck step
// is diagnosable from the actual DOM rather than guesswork.
async function dumpPage(page: Page, label: string): Promise<void> {
  try {
    log(`[${label}] page ${page.url()} (title: ${await page.title().catch(() => '?')})`);
    await page.screenshot({ path: `prototypes/last-${label}.png`, fullPage: true });
    fs.writeFileSync(`prototypes/last-${label}.html`, await page.content());
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
// Device flow endpoints (github.com). Accept: application/json so the responses
// come back as JSON rather than form-encoded.

interface DeviceCodeResponse {
  device_code: string;
  user_code: string;
  verification_uri: string;
  expires_in: number;
  interval: number;
}

async function fetchDeviceCode(): Promise<DeviceCodeResponse> {
  const resp = await fetch('https://github.com/login/device/code', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded',
      Accept: 'application/json',
    },
    body: new URLSearchParams({ client_id: CLIENT_ID, scope: SCOPE }).toString(),
    signal: AbortSignal.timeout(30_000),
  });
  if (!resp.ok) {
    throw new Error(`device/code request failed: ${resp.status} ${(await resp.text()).slice(0, 200)}`);
  }
  return (await resp.json()) as DeviceCodeResponse;
}

async function pollForToken(
  deviceCode: string,
  intervalSeconds: number,
  timeoutMs: number,
): Promise<string> {
  const deadline = Date.now() + timeoutMs;
  let waitMs = intervalSeconds * 1000;
  while (Date.now() < deadline) {
    await new Promise((r) => setTimeout(r, waitMs));
    const resp = await fetch('https://github.com/login/oauth/access_token', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/x-www-form-urlencoded',
        Accept: 'application/json',
      },
      body: new URLSearchParams({
        client_id: CLIENT_ID,
        device_code: deviceCode,
        grant_type: 'urn:ietf:params:oauth:grant-type:device_code',
      }).toString(),
    });
    const body = (await resp.json()) as {
      access_token?: string;
      error?: string;
      error_description?: string;
    };
    if (body.access_token) return body.access_token;
    if (body.error === 'authorization_pending') continue;
    if (body.error === 'slow_down') {
      waitMs += 5000;
      continue;
    }
    throw new Error(
      `token endpoint returned "${body.error}"${body.error_description ? `: ${body.error_description}` : ''}`,
    );
  }
  throw new Error('timed out waiting for authorization (the Authorize click never registered)');
}

// ---------------------------------------------------------------------------
// Browser steps.

async function signIn(page: Page, user: string, password: string, totpSecret?: string): Promise<void> {
  const loginField = page.locator('#login_field');
  if (await appears(loginField, 10_000)) {
    log('login page shown; entering username and password');
    await loginField.fill(user);
    await page.locator('#password').fill(password);
    await page.getByRole('button', { name: 'Sign in', exact: true }).click();
  } else {
    log('no login form (session may already be authenticated)');
  }

  // 2FA (authenticator app). GitHub renders the app-code field as #app_totp.
  const totpField = page.locator('#app_totp');
  if (await appears(totpField, 5_000)) {
    if (!totpSecret) {
      throw new Error(
        'GitHub is asking for a 2FA authenticator code, but COPILOT_TOTP_SECRET is not set. '
        + 'Add the account\'s base32 2FA secret to .env.local, or remove 2FA from the account.',
      );
    }
    log('2FA prompt shown; entering generated TOTP code');
    await totpField.fill(totp(totpSecret));
  }
}

async function enterUserCode(page: Page, userCode: string): Promise<void> {
  // Navigating to the bare verification_uri gives an empty code form. GitHub
  // splits the code across per-character inputs (#user-code-0..N).
  const first = page.locator('#user-code-0');

  // After signing in, GitHub can show a device-activation intro with a Continue
  // button before the code-entry boxes appear (the Workbench flow hits this
  // too). Only click it when the code field is not already present, so we never
  // submit an empty code on flows that skip the intro.
  if (!(await appears(first, 5_000))) {
    const intro = page.getByRole('button', { name: /continue/i });
    if (await appears(intro, 3_000)) {
      log('device-activation intro shown; clicking Continue to reach the code form');
      await intro.click();
    }
  }

  await first.waitFor({ state: 'visible', timeout: 20_000 });
  log('device activation page shown; entering user code');

  // Capture the form once so the real input structure is known instead of
  // guessed (one combined field vs. eight per-character boxes).
  await dumpPage(page, 'code-form');

  // Real keystrokes into the first box with a per-key delay: GitHub auto-advances
  // focus after each character, and the delay lets that settle so none are
  // dropped (the race that produced device/failure?reason=not_found). This is
  // the approach that was accepted in an earlier run. fill() would be synthetic
  // and is what the reactive form ignores.
  const code = userCode.replace(/-/g, '');
  await first.click();
  await first.pressSequentially(code, { delay: 150 });

  // Read every user-code box back: the count reveals the structure, and the
  // joined value confirms the code landed correctly before we submit.
  const boxes = page.locator('[id^="user-code-"]');
  const n = await boxes.count();
  const vals: string[] = [];
  for (let i = 0; i < n; i++) vals.push(await boxes.nth(i).inputValue().catch(() => '?'));
  log(`user-code boxes (${n}) hold "${vals.join('')}" (expected "${code}")`);
  await page.getByRole('button', { name: /continue/i }).click();
}

async function authorize(page: Page): Promise<void> {
  const authorizeBtn = page.locator('.js-oauth-authorize-btn');

  // On a re-run the OAuth app is already authorized, so GitHub skips straight
  // to a success page and no button appears. That is not a failure; the token
  // poll will confirm the grant.
  const appeared = await authorizeBtn
    .waitFor({ state: 'visible', timeout: 20_000 })
    .then(() => true)
    .catch(() => false);
  if (!appeared) {
    // Do not assume the app is already authorized: capture the page so the real
    // state (a renamed button, another confirmation step, an error) is visible.
    // The token poll remains the arbiter of whether authorization happened.
    log('.js-oauth-authorize-btn not found; capturing page to diagnose');
    await dumpPage(page, 'no-authorize-button');
    return;
  }

  const initiallyDisabled = await authorizeBtn.isDisabled();
  log(`Authorize button visible; initially ${initiallyDisabled ? 'DISABLED' : 'enabled'}`);

  // The point of the prototype. Instead of removeAttribute('disabled'), give
  // GitHub a genuine interaction: scrollIntoView + hover both go through CDP
  // and land as isTrusted=true events. If GitHub's guard is a plain timer, the
  // toBeEnabled wait below rides it out; if it needs a trusted interaction,
  // this supplies one. A JS-dispatched event would be isTrusted=false and
  // ignored, which is why the attribute hack "clicks" a button the server may
  // still reject.
  await authorizeBtn.scrollIntoViewIfNeeded();
  await authorizeBtn.hover();
  // A genuine trusted wheel event is what arms GitHub's anti-clickjacking gate:
  // a manual trackpad scroll enabled the button by hand, so replicate it with a
  // real wheel event. page.mouse.wheel dispatches through CDP (isTrusted=true);
  // the scrollIntoViewIfNeeded above is a JS scroll and does not count.
  await page.mouse.wheel(0, 300);

  // Diagnostic: is this a headless focus/visibility gate, or a plain timer?
  const env = await page.evaluate(() => ({
    webdriver: navigator.webdriver,
    hasFocus: document.hasFocus(),
    visibility: document.visibilityState,
  }));
  log(`page env at authorize: navigator.webdriver=${env.webdriver} hasFocus=${env.hasFocus} visibility=${env.visibility}`);

  // Wait the button out (up to 120s) and record exactly when it flips. The
  // Workbench note blamed a variable delay that is "longer for unknown IPs";
  // 30s was not enough, so observe a longer window before drawing conclusions.
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

test('GitHub device-flow Authorize registers with a trusted click (no attribute hack)', async () => {
  const user = process.env.COPILOT_USER;
  const password = process.env.COPILOT_PASSWORD;
  const totpSecret = process.env.COPILOT_TOTP_SECRET;

  test.skip(
    !user || !password,
    'Set COPILOT_USER and COPILOT_PASSWORD in e2e/rstudio/.env.local (throwaway account).',
  );

  const device = await fetchDeviceCode();
  log(`user_code: ${device.user_code}`);
  log(`verification_uri: ${device.verification_uri}`);

  const browser = await chromium.launch({
    // Bundled headless Chromium. Now that a real wheel event (page.mouse.wheel)
    // is what arms the Authorize button, check whether that works here too; if
    // so, CI needs no display and no Chrome install.
    headless: true,
    args: ['--disable-blink-features=AutomationControlled'],
  });
  try {
    const context = await browser.newContext({
      userAgent:
        'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36',
    });
    // Hide the automation flag: Playwright sets navigator.webdriver = true, and
    // GitHub may gate the authorize-button enable script on its absence. Runs
    // before any page script, so the override is in place at first paint.
    await context.addInitScript(() => {
      Object.defineProperty(navigator, 'webdriver', { get: () => undefined });
    });
    const page = await context.newPage();

    // Surface client-side errors on the authorize page: if GitHub's
    // enable-script throws under this browser, that is the theory-2 smoking gun.
    page.on('pageerror', (err) => log(`[pageerror] ${err.message}`));
    page.on('console', (msg) => {
      if (msg.type() === 'error') log(`[console.error] ${msg.text()}`);
    });

    try {
      await page.goto(device.verification_uri, { waitUntil: 'domcontentloaded' });
      await signIn(page, user!, password!, totpSecret);
      await enterUserCode(page, device.user_code);
      await authorize(page);
    } catch (err) {
      await dumpPage(page, 'failure');
      throw err;
    }
  } finally {
    await browser.close();
  }

  // The real proof: after a genuine Authorize, the device_code is exchangeable.
  const token = await pollForToken(device.device_code, device.interval ?? 5, 60_000);
  expect(token, 'expected a non-empty access token after Authorize').toBeTruthy();
  log(`SUCCESS: received an access token (len=${token.length}). Authorize registered without the attribute hack.`);
});
