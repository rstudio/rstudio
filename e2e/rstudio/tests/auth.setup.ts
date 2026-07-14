import { test as setup, chromium, type Page } from '@playwright/test';
import * as fs from 'fs';
import * as os from 'os';
import * as path from 'path';
import {
  AUTH_STORAGE_KEY,
  POSITAI_DIR_RELATIVE,
  PositAiOAuthEntry,
  isStoreFileAuthenticated,
  noSeedCredentials,
  storeFile,
  writeAuthStatus,
} from '../utils/auth';

/**
 * Authenticate Posit AI for the per-invocation sandbox.
 *
 * This is a Playwright setup project (see playwright.config.ts): it runs once
 * after globalSetup and before the desktop/server test projects, which depend
 * on it. Its job is to leave a valid Posit AI token store at
 * <sandbox>/user-home/.posit/assistant/store/data.json so the Posit AI tests
 * start signed in. It is the sole authority for Posit AI credentials in the
 * sandbox -- globalSetup (sandbox-setup.ts) no longer copies Posit AI creds.
 * Every exit path also records what happened in
 * <sandbox>/positai-auth-status.json (see PositAiAuthStatus in utils/auth.ts),
 * which requireAiCredentials() reads so skipped tests report the actual cause.
 *
 * Modes (PW_SANDBOX_POSITAI_AUTH), default "flow":
 *   flow   Run the OAuth sign-in using POSIT_EMAIL/POSIT_PASSWORD. If
 *          either is unset, log and let the Posit AI tests skip. Never falls
 *          back to "seed". This is the default -- unset behaves as "flow".
 *   seed   Copy the host user's whole ~/.posit/assistant directory (tokens
 *          under store/data.json, plus skills, workspaces, and any other
 *          state) into the sandbox. Skips (Posit AI tests) if the host is
 *          not signed in. Explicit only; never used as a fallback. Useful
 *          for fast local iteration when the developer is already signed in
 *          on the host. Suppressed by the global host-copy kill-switch
 *          PW_SANDBOX_NO_SEED_CREDENTIALS, in which case the tests skip.
 *   off    Provision nothing; the Posit AI tests skip.
 */

const AUTH_HOST = 'login.posit.cloud';
const CLIENT_ID = 'rstudio-ide';
const SCOPE = 'prism';

// Valid PW_SANDBOX_POSITAI_AUTH values. The array is the single source of
// truth -- the type is derived from it -- so the runtime check, its error
// message, and the type can't drift apart. Unset behaves as "flow".
const AUTH_MODES = ['off', 'seed', 'flow'] as const;
type PositAiAuthMode = (typeof AUTH_MODES)[number];

function copyTree(src: string, dest: string): void {
  fs.mkdirSync(path.dirname(dest), { recursive: true });
  fs.cpSync(src, dest, { recursive: true });
}

function verifyStoreWritten(sandboxUserHome: string): void {
  const dest = storeFile(sandboxUserHome);
  if (!isStoreFileAuthenticated(dest)) {
    throw new Error(
      `[auth-setup] post-write verification failed for ${dest}: store is missing, unreadable, malformed, lacks required fields, or its token is already expired (see any preceding [auth] WARNING for the specific cause)`,
    );
  }
}

interface DeviceCodeResponse {
  user_code: string;
  verification_uri: string;
  // OPTIONAL per RFC 8628 §3.2, but login.posit.cloud always returns it and
  // automateLogin depends on it; fetchDeviceCode validates its presence.
  verification_uri_complete: string;
  device_code: string;
  // Optional per RFC 8628 §3.2 (default 5s); defaulted at the call site.
  interval?: number;
  expires_in: number;
}

// Per RFC 6749 §5.1 / RFC 8628 §3.5: access_token and token_type are required;
// expires_in is RECOMMENDED; refresh_token and scope are optional.
interface TokenSuccessResponse {
  access_token: string;
  token_type: string;
  expires_in?: number;
  refresh_token?: string;
  scope?: string;
}

// Per RFC 6749 §5.2: error is required; error_description and error_uri are
// optional. Device-flow error codes (RFC 8628 §3.5) include
// authorization_pending and slow_down (transient) plus access_denied and
// expired_token.
interface TokenErrorResponse {
  error: string;
  error_description?: string;
  error_uri?: string;
}

// A deterministic OAuth-side failure: the server terminated the sign-in
// (access_denied, expired_token, a config fault) or returned a malformed
// response. Treated as fatal so it fails the run rather than masquerading as
// a skip.
class OAuthTerminalError extends Error {
  constructor(message: string) {
    super(message);
    this.name = 'OAuthTerminalError';
  }
}

// A browser-side login failure at a step where wrong credentials or a denied
// authorization is the likely cause (see automateLogin). Treated as fatal so a
// bad POSIT_EMAIL/POSIT_PASSWORD fails the run rather than masquerading as a
// skip.
class LoginAutomationError extends Error {
  constructor(message: string) {
    super(message);
    this.name = 'LoginAutomationError';
  }
}

async function fetchDeviceCode(): Promise<DeviceCodeResponse> {
  const resp = await fetch(`https://${AUTH_HOST}/oauth/device/authorize`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body: new URLSearchParams({ scope: SCOPE, client_id: CLIENT_ID }).toString(),
  });
  if (!resp.ok) {
    throw new Error(`OAuth sign-in request failed: ${resp.status} ${await resp.text()}`);
  }
  const data = (await resp.json()) as Partial<DeviceCodeResponse>;
  // A 200 with a shape we don't understand is a contract change on the auth
  // server, not flake -- fail the run naming the field rather than letting a
  // downstream TypeError read as "no credentials" (a perpetual dark skip).
  for (const field of ['device_code', 'user_code', 'verification_uri', 'verification_uri_complete'] as const) {
    if (typeof data[field] !== 'string' || data[field] === '') {
      throw new OAuthTerminalError(
        `device-authorize response is missing or has a non-string "${field}"; the endpoint contract may have changed`,
      );
    }
  }
  if (data.interval !== undefined && (typeof data.interval !== 'number' || data.interval <= 0)) {
    throw new OAuthTerminalError(
      `device-authorize response has invalid "interval" (${JSON.stringify(data.interval)}); expected a positive number`,
    );
  }
  return data as DeviceCodeResponse;
}

async function pollForToken(
  deviceCode: string,
  intervalSeconds: number,
  timeoutMs: number,
): Promise<TokenSuccessResponse> {
  const deadline = Date.now() + timeoutMs;
  let waitMs = intervalSeconds * 1000;
  // Most recent response that fit neither the success nor the RFC error shape
  // (e.g. a proxy 502 with an HTML body); surfaced in the timeout message so
  // a retried-through hiccup is still diagnosable.
  let lastAnomaly: string | undefined;
  while (Date.now() < deadline) {
    await new Promise(resolve => setTimeout(resolve, waitMs));
    const resp = await fetch(`https://${AUTH_HOST}/oauth/token`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body: new URLSearchParams({
        scope: SCOPE,
        client_id: CLIENT_ID,
        grant_type: 'urn:ietf:params:oauth:grant-type:device_code',
        device_code: deviceCode,
      }).toString(),
    });
    if (resp.ok) {
      return resp.json() as Promise<TokenSuccessResponse>;
    }
    const text = await resp.text();
    let body: Partial<TokenErrorResponse> = {};
    try {
      body = JSON.parse(text) as TokenErrorResponse;
    } catch {
      // Not JSON; handled as an anomaly below.
    }
    // A 5xx, or any response without the RFC-required "error" string, is a
    // server-side hiccup or a proxy interjection, not an OAuth verdict. Keep
    // polling until the deadline rather than misreading it as terminal (a
    // "(undefined)" run failure) or letting a JSON parse error escape as a
    // transient-looking skip when the next poll might have succeeded.
    if (resp.status >= 500 || typeof body.error !== 'string' || body.error === '') {
      lastAnomaly = `HTTP ${resp.status}: ${text.slice(0, 200)}`;
      continue;
    }
    if (body.error === 'authorization_pending') continue;
    if (body.error === 'slow_down') {
      waitMs += 5000;
      continue;
    }
    // Only authorization_pending / slow_down are non-terminal (RFC 8628 §3.5).
    // Every other error is a deterministic problem -- access_denied,
    // expired_token, invalid_grant, or a config fault (invalid_client,
    // invalid_scope, ...) -- so fail loud rather than let it look like a skip.
    // (expired_token can also mean a slow runner missed the window; failing
    // loud there is the accepted trade-off over silently skipping.)
    const suffix = body.error_description ? `: ${body.error_description}` : '';
    throw new OAuthTerminalError(
      `OAuth sign-in terminated by server (${body.error})${suffix}`,
    );
  }
  throw new Error(
    'timed out waiting for authorization'
      + (lastAnomaly ? ` (last unexpected token response: ${lastAnomaly})` : ''),
  );
}

async function step<T>(page: Page, name: string, fn: () => Promise<T>, fatal = false): Promise<T> {
  try {
    return await fn();
  } catch (err) {
    const msg = err instanceof Error ? err.message : String(err);
    // page.url() is the one piece of non-sensitive context available with
    // artifacts disabled for this project (see playwright.config.ts); guard
    // it in case the page is already closed.
    let url = 'unknown';
    try {
      url = page.url();
    } catch {
      // keep 'unknown'
    }
    const wrapped = `[automate-login: ${name}] ${msg} (page: ${url})`;
    // Failures at credential/authorization steps are fatal (LoginAutomationError);
    // non-credential steps stay transient so a flaky page load lets the Posit AI
    // tests skip rather than failing the whole run. Note this classifies by
    // step, not by error kind -- see the catch in the setup body.
    throw fatal ? new LoginAutomationError(wrapped) : new Error(wrapped);
  }
}

// Race a promise against a deadline. On expiry, rejects with a plain Error so
// the caller's transient path handles it; the underlying work is not
// cancelled (Playwright kills any leftover browser at worker exit), but the
// setup test itself settles promptly instead of running into the harness
// timeout, which would bypass the transient path entirely.
function withDeadline<T>(promise: Promise<T>, ms: number, label: string): Promise<T> {
  let timer: NodeJS.Timeout | undefined;
  const expiry = new Promise<never>((_, reject) => {
    timer = setTimeout(() => reject(new Error(`${label} after ${ms / 1000}s`)), ms);
  });
  return Promise.race([promise, expiry]).finally(() => clearTimeout(timer));
}

async function automateLogin(
  verificationUriComplete: string,
  userCode: string,
  email: string,
  password: string,
): Promise<void> {
  const verificationUri = verificationUriComplete.split('?')[0];
  const browser = await chromium.launch({
    headless: true,
    args: ['--disable-blink-features=AutomationControlled'],
  });
  try {
    const context = await browser.newContext({
      userAgent:
        'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36',
    });
    const page = await context.newPage();
    // All selectors below track the login.posit.cloud markup; when that page
    // changes, these are the first thing to break (surfacing as a fatal error
    // on the credential steps, or as a skip whose flow-failed reason names
    // the step for the others).
    await step(page, 'open activation URL', () => page.goto(verificationUriComplete));
    await step(page, 'submit email', async () => {
      await page
        .locator('input[type="email"], input[name="username"], input[name="email"]')
        .fill(email);
      await page.getByRole('button', { name: /continue/i }).click();
    });
    await step(page, 'submit password', async () => {
      // Password field appears inline after the email step submits via XHR;
      // no navigation fires, so waitForLoadState would return immediately
      // (the page is already past "load") and waitForURL would hang. Wait
      // for the field itself instead.
      const passwordInput = page.locator('input[type="password"]');
      await passwordInput.waitFor({ state: 'visible' });
      await passwordInput.fill(password);
      await page.getByRole('button', { name: /log.?in|sign.?in|continue/i }).click();
      await page.waitForURL(/\/oauth\/device/, { timeout: 30000 });
    }, true);
    await step(page, 'enter user code', async () => {
      // _complete URL prefills the userCode form; navigate to the bare URI
      // for an empty form to type into.
      await page.goto(verificationUri);
      await page.locator('input[name="userCode0"]').waitFor({ state: 'visible' });
      // pressSequentially fires React's onChange per character; fill()
      // bypasses it and leaves the form empty.
      const codeChars = userCode.replace(/-/g, '').split('');
      for (let i = 0; i < codeChars.length; i++) {
        await page.locator(`input[name="userCode${i}"]`).pressSequentially(codeChars[i]);
      }
      await page.getByRole('button', { name: /continue/i }).click();
    });
    await step(page, 'authorize', async () => {
      const authorizeBtn = page.getByRole('button', { name: 'Authorize' });
      await authorizeBtn.waitFor({ state: 'visible', timeout: 15000 });
      // The button can pass Playwright's actionability checks a beat before the
      // SPA binds its click handler, so a single too-fast click is silently
      // swallowed: the device is never authorized and the only symptom is a
      // later poll timeout, not a click error. Re-click while the button is
      // still present so a swallowed click is retried once the handler is
      // bound; once a click registers the page advances and the button
      // detaches, ending the loop. pollForToken remains the arbiter of whether
      // authorization actually succeeded -- if it never takes, its timeout
      // drives the transient -> skip path.
      for (let attempt = 0; attempt < 5; attempt++) {
        if (!(await authorizeBtn.isVisible())) break;
        await authorizeBtn.click();
        await page.waitForTimeout(1000);
      }
    }, true);
  } finally {
    await browser.close();
  }
}

function writeTokensToSandbox(tokenData: TokenSuccessResponse, sandboxUserHome: string): void {
  if (tokenData.expires_in === undefined) {
    throw new Error('OAuth success response lacks expires_in; cannot compute expiresAt');
  }
  const expiresAt = new Date(Date.now() + tokenData.expires_in * 1000).toISOString();
  const entry: PositAiOAuthEntry = {
    authenticated: true,
    oauthAuth: {
      tokenData: {
        accessToken: tokenData.access_token,
        ...(tokenData.refresh_token !== undefined && { refreshToken: tokenData.refresh_token }),
        expiresAt,
        tokenType: tokenData.token_type,
        ...(tokenData.scope !== undefined && { scope: tokenData.scope }),
      },
      expiresAt,
      ...(tokenData.scope !== undefined && { scope: tokenData.scope }),
    },
  };
  const storeData = { [AUTH_STORAGE_KEY]: entry };
  const dest = storeFile(sandboxUserHome);
  fs.mkdirSync(path.dirname(dest), { recursive: true });
  fs.writeFileSync(dest, JSON.stringify(storeData, null, 2), { mode: 0o600 });
}

setup('authenticate Posit AI', async () => {
  // Explicit headroom above the flow's own budget: browser launch, five login
  // steps (30s library default each on the raw chromium context -- the
  // config's actionTimeout doesn't apply there), and the 90s token poll can
  // legitimately exceed the global 120s test timeout. If the harness timeout
  // fired here, the catch below would never run and every dependent test
  // would be marked "did not run"; the withDeadline race below fails through
  // the transient path well before this outer limit can be reached.
  setup.setTimeout(240_000);

  const sandbox = process.env.PW_SANDBOX;
  if (!sandbox) throw new Error('PW_SANDBOX is not set; sandbox-setup must run first');

  const sandboxUserHome = path.join(sandbox, 'user-home');
  const modeRaw = process.env.PW_SANDBOX_POSITAI_AUTH ?? 'flow';
  if (!AUTH_MODES.includes(modeRaw as PositAiAuthMode)) {
    throw new Error(`PW_SANDBOX_POSITAI_AUTH="${modeRaw}" -- expected one of: ${AUTH_MODES.join(', ')}, or unset`);
  }
  const mode: PositAiAuthMode = modeRaw as PositAiAuthMode;

  if (mode === 'off') {
    writeAuthStatus(sandbox, {
      mode,
      outcome: 'off',
      reason: 'PW_SANDBOX_POSITAI_AUTH=off: Posit AI credentials deliberately not provisioned.',
    });
    console.log('[auth-setup] PW_SANDBOX_POSITAI_AUTH=off; Posit AI tests will skip');
    return;
  }

  if (mode === 'seed') {
    // The global host-copy kill-switch suppresses seed (but not the sign-in flow).
    if (noSeedCredentials()) {
      writeAuthStatus(sandbox, {
        mode,
        outcome: 'seed-suppressed',
        reason: 'PW_SANDBOX_POSITAI_AUTH=seed, but the PW_SANDBOX_NO_SEED_CREDENTIALS kill-switch suppressed the host copy.',
      });
      console.log('[auth-setup] PW_SANDBOX_NO_SEED_CREDENTIALS set; skipping seed, Posit AI tests will skip');
      return;
    }
    if (!isStoreFileAuthenticated(storeFile(os.homedir()))) {
      writeAuthStatus(sandbox, {
        mode,
        outcome: 'host-not-signed-in',
        reason: 'PW_SANDBOX_POSITAI_AUTH=seed, but the host is not signed in to Posit AI (or its token has expired).',
      });
      console.log('[auth-setup] PW_SANDBOX_POSITAI_AUTH=seed: host is not signed in to Posit AI (or its token has expired); Posit AI tests will be skipped');
      return;
    }
    copyTree(
      path.join(os.homedir(), POSITAI_DIR_RELATIVE),
      path.join(sandboxUserHome, POSITAI_DIR_RELATIVE),
    );
    verifyStoreWritten(sandboxUserHome);
    writeAuthStatus(sandbox, {
      mode,
      outcome: 'success',
      reason: 'seeded from the host ~/.posit/assistant',
    });
    console.log('[auth-setup] seeded Posit AI state from ~/.posit/assistant');
    console.log(
      '[auth-setup] real Posit AI tokens now live in the sandbox and persist if the run is preserved or teardown fails.',
    );
    return;
  }

  // mode === 'flow' (default): OAuth sign-in. Never falls back to seed.
  const email = process.env.POSIT_EMAIL;
  const password = process.env.POSIT_PASSWORD;

  if (!email || !password) {
    writeAuthStatus(sandbox, {
      mode,
      outcome: 'credentials-unset',
      reason: 'POSIT_EMAIL/POSIT_PASSWORD not set. Set them for the sign-in flow (default), or run with PW_SANDBOX_POSITAI_AUTH=seed while signed in to Posit AI on the host.',
    });
    console.log('[auth-setup] POSIT_EMAIL/POSIT_PASSWORD not set; Posit AI tests will be skipped');
    return;
  }

  if (noSeedCredentials()) {
    console.log('[auth-setup] PW_SANDBOX_NO_SEED_CREDENTIALS set; Posit AI sign-in flow is unaffected.');
  }

  console.log('[auth-setup] starting Posit AI sign-in...');
  let tokenData: TokenSuccessResponse;
  try {
    const { verification_uri, verification_uri_complete, user_code, device_code, interval } =
      await fetchDeviceCode();
    console.log(`[auth-setup] verification_uri: ${verification_uri}`);
    console.log(`[auth-setup] user_code: ${user_code}`);
    // Drive the browser and poll for the token concurrently. Promise.all
    // attaches a handler to both, so a failure in automateLogin can't leave the
    // poll as an orphaned unhandled rejection. interval defaults to 5s per
    // RFC 8628 §3.2.
    const flow = Promise.all([
      automateLogin(verification_uri_complete, user_code, email, password),
      pollForToken(device_code, interval ?? 5, 90000),
    ]);
    // Keep a handler attached so a rejection landing after the deadline race
    // below has already settled can't become an unhandled rejection.
    flow.catch(() => {});
    // Bound the whole flow well under this test's 240s timeout (set above) so
    // a pile-up of slow steps fails through the transient path here instead
    // of hitting the harness timeout, which would skip the catch below and
    // fail the entire run.
    const [, resolved] = await withDeadline(flow, 180_000, 'overall sign-in deadline exceeded');
    tokenData = resolved;
  } catch (err) {
    // Fail loud on deterministic problems: a terminal OAuth error (denied
    // authorization, config fault, malformed device-authorize response) or a
    // browser-side failure at the credential/authorization steps (e.g. a
    // wrong POSIT_PASSWORD). Everything else falls through to a skip. That
    // bucket is usually transient (page load, network, poll timeout, overall
    // deadline), but the split is by step/error kind, not by root cause --
    // deterministic login-page changes at the non-credential steps land here
    // too. The status file keeps that visible: every skipped Posit AI test
    // reports this failure as its skip reason.
    if (err instanceof OAuthTerminalError || err instanceof LoginAutomationError) {
      throw err;
    }
    const msg = err instanceof Error ? err.message : String(err);
    writeAuthStatus(sandbox, {
      mode,
      outcome: 'flow-failed',
      reason: `sign-in flow was attempted but failed: ${msg}`,
    });
    console.warn(
      '[auth-setup] WARNING: Posit AI authentication flow failed; Posit AI tests will be skipped:',
      msg,
    );
    return;
  }

  // Past this point sign-in demonstrably succeeded, so a malformed token
  // response or a failed store write is a contract violation, not flake -- let
  // these throw and fail the run rather than masquerade as a skip.
  writeTokensToSandbox(tokenData, sandboxUserHome);
  verifyStoreWritten(sandboxUserHome);
  writeAuthStatus(sandbox, {
    mode,
    outcome: 'success',
    reason: 'sign-in flow completed',
  });
  console.log('[auth-setup] Posit AI sign-in complete; tokens written to sandbox');
});
