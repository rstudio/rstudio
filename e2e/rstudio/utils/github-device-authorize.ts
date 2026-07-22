import type { Page } from '@playwright/test';
import crypto from 'crypto';
import { authStepsEnabled, launchAuthBrowser } from './auth-debug';

/**
 * The browser half of the GitHub Copilot device-flow sign-in: given a
 * userCode/verificationUri pair (obtained by the copilot-language-server via
 * signInInitiate -- see utils/copilot-agent.ts), sign in to GitHub, enter the
 * code, and click Authorize with a genuine trusted interaction.
 *
 * Every step here was proven live against github.com (see
 * prototypes/copilot-authorize.spec.ts for the original standalone flow).
 * The key finding: GitHub's anti-clickjacking gate renders the Authorize
 * button disabled and arms it only after a trusted scroll/wheel event;
 * page.mouse.wheel() (a real CDP event) satisfies it, including in headless
 * bundled Chromium, so no attribute hacks and no display are needed.
 *
 * This deliberately excludes the device-code fetch and the token poll: the
 * agent owns both ends of that exchange. Our job is only the human part in
 * the middle.
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

// Per-step narration, off unless PW_DEBUG_AUTH_STEPS is set (see auth-debug.ts).
// A normal run reports only the [auth-setup] outcome milestones; these lines
// are the opt-in detail for troubleshooting the GitHub flow. Failures don't
// rely on this -- they throw (GitHubLoginError and friends) and surface
// regardless.
function log(msg: string): void {
  if (authStepsEnabled()) console.log(`[gh-authorize] ${msg}`);
}

// A deterministic GitHub-side login rejection (wrong username/password). The
// caller (auth.setup.ts) treats this as fatal, matching how a wrong
// POSIT_PASSWORD fails the Posit AI sign-in flow: credentials were set
// deliberately, so a rejection should turn the run red, not quietly skip.
export class GitHubLoginError extends Error {
  constructor(message: string) {
    super(message);
    this.name = 'GitHubLoginError';
  }
}

// Locator.isVisible({ timeout }) does not actually wait -- Playwright's own
// types document the option as ignored, so it checks the DOM at that instant
// and returns immediately. Every optional-step check in this flow needs to
// genuinely wait (a step-conditional page can still be mid-navigation or
// mid-render when we ask), so route them all through waitFor instead: true
// if the element shows up within timeoutMs, false (not thrown) otherwise.
async function appears(locator: import('@playwright/test').Locator, timeoutMs: number): Promise<boolean> {
  return locator
    .waitFor({ state: 'visible', timeout: timeoutMs })
    .then(() => true)
    .catch(() => false);
}

// ---------------------------------------------------------------------------
// TOTP (RFC 6238), used only if the account has 2FA enrolled. Dependency-free
// so the helper adds nothing to package.json: base32-decode the secret, HMAC
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
// Browser steps.

async function signIn(page: Page, user: string, password: string, totpSecret?: string): Promise<void> {
  const loginField = page.locator('#login_field');
  if (await appears(loginField, 10_000)) {
    log('login page shown; entering username and password');
    await loginField.fill(user);
    await page.locator('#password').fill(password);
    await page.getByRole('button', { name: 'Sign in', exact: true }).click();

    // GitHub re-renders the login page with this banner on a rejected
    // password; a short wait is enough since it's part of the page GitHub
    // sends back, not something that appears after a delay.
    const loginError = page.getByText(/incorrect username or password/i);
    if (await appears(loginError, 3_000)) {
      throw new GitHubLoginError('GitHub rejected the login: incorrect username or password.');
    }
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
  // GitHub splits the code across per-character inputs (#user-code-0..8, the
  // 5th being the dash).
  const first = page.locator('#user-code-0');

  // After signing in, GitHub can show a device-activation intro with a Continue
  // button before the code-entry boxes appear. Only click it when the code
  // field is not already present, so we never submit an empty code on flows
  // that skip the intro.
  if (!(await appears(first, 5_000))) {
    const intro = page.getByRole('button', { name: /continue/i });
    if (await appears(intro, 3_000)) {
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

  // Read every user-code box back to confirm the entry landed correctly
  // before submitting -- log only whether it matches, never the code itself
  // (it's a live device-authorization code, not something to print).
  const boxes = page.locator('[id^="user-code-"]');
  const n = await boxes.count();
  const vals: string[] = [];
  for (let i = 0; i < n; i++) vals.push(await boxes.nth(i).inputValue().catch(() => '?'));
  log(`user-code boxes (${n}) filled; entry matches expected: ${vals.join('').replace(/-/g, '') === code}`);
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
    // The final page capture on close (see auth-debug.ts) records what showed
    // instead; the caller's checkStatus poll decides whether it mattered.
    log('.js-oauth-authorize-btn not found; continuing (the app may already be authorized)');
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
  // The shared launcher owns the browser config and the PW_DEBUG_AUTH_CAPTURE page
  // captures; session.close() takes the final capture (the failure-state
  // record when a step throws) and never itself throws.
  const session = await launchAuthBrowser('copilot');
  try {
    const page = session.page;
    await page.goto(opts.verificationUri, { waitUntil: 'domcontentloaded' });
    await signIn(page, opts.user, opts.password, opts.totpSecret);
    await enterUserCode(page, opts.userCode);
    await authorize(page);
  } finally {
    await session.close();
  }
}
