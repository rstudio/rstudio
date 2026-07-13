import { test as setup, chromium } from '@playwright/test';
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
} from '../utils/auth';

/**
 * Authenticate Posit AI for the per-invocation sandbox.
 *
 * This is a Playwright setup project (see playwright.config.ts): it runs once
 * after globalSetup and before the desktop/server test projects, which depend
 * on it. Its job is to leave a valid Posit AI token store at
 * <sandbox>/user-home/.posit/assistant/store/data.json so the @ai tests start
 * signed in. It is the sole authority for Posit AI credentials in the sandbox
 * -- globalSetup (sandbox-setup.ts) no longer copies Posit AI creds.
 *
 * Modes (PW_SANDBOX_POSITAI_AUTH), default "flow":
 *   flow   Run the OAuth sign-in using POSIT_EMAIL/POSIT_PASSWORD. If
 *          either is unset, log and let the @ai tests skip. Never falls back
 *          to "seed". This is the default -- unset behaves as "flow".
 *   seed   Copy the host user's whole ~/.posit/assistant directory (tokens
 *          under store/data.json, plus skills, settings, workspaces) into the
 *          sandbox. Skips (Posit AI tests) if the host is not signed in.
 *          Explicit only; never used as a fallback. Useful for fast local
 *          iteration when the developer is already signed in on the host.
 *          Suppressed by the global host-copy kill-switch
 *          PW_SANDBOX_NO_SEED_CREDENTIALS, in which case the tests skip.
 *   off    Provision nothing; the @ai tests skip.
 */

const AUTH_HOST = 'login.posit.cloud';
const CLIENT_ID = 'rstudio-ide';
const SCOPE = 'prism';

// Valid PW_SANDBOX_POSITAI_AUTH values, in one place so the runtime check and
// its error message stay in sync. Unset behaves as "flow".
type PositAiAuthMode = 'off' | 'seed' | 'flow';
const AUTH_MODES: readonly PositAiAuthMode[] = ['off', 'seed', 'flow'];

function copyTree(src: string, dest: string): void {
  fs.mkdirSync(path.dirname(dest), { recursive: true });
  fs.cpSync(src, dest, { recursive: true });
}

function verifyStoreWritten(sandboxUserHome: string): void {
  const dest = storeFile(sandboxUserHome);
  if (!isStoreFileAuthenticated(dest)) {
    throw new Error(
      `[auth-setup] post-write verification failed for ${dest}: store is missing required fields or token is expired`,
    );
  }
}

interface DeviceCodeResponse {
  user_code: string;
  verification_uri: string;
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

class OAuthTerminalError extends Error {
  constructor(public readonly oauthError: string, message: string) {
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
  return resp.json() as Promise<DeviceCodeResponse>;
}

async function pollForToken(
  deviceCode: string,
  intervalSeconds: number,
  timeoutMs: number,
): Promise<TokenSuccessResponse> {
  const deadline = Date.now() + timeoutMs;
  let waitMs = intervalSeconds * 1000;
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
    const body = (await resp.json()) as TokenErrorResponse;
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
      body.error,
      `OAuth sign-in terminated by server (${body.error})${suffix}`,
    );
  }
  throw new Error('timed out waiting for authorization');
}

async function step<T>(name: string, fn: () => Promise<T>, fatal = false): Promise<T> {
  try {
    return await fn();
  } catch (err) {
    const msg = err instanceof Error ? err.message : String(err);
    const wrapped = `[automate-login: ${name}] ${msg}`;
    // Failures at credential/authorization steps are fatal (LoginAutomationError);
    // earlier steps stay transient so a flaky page load lets the @ai tests skip
    // rather than failing the whole run.
    throw fatal ? new LoginAutomationError(wrapped) : new Error(wrapped);
  }
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
    await step('open activation URL', () => page.goto(verificationUriComplete));
    await step('submit email', async () => {
      await page
        .locator('input[type="email"], input[name="username"], input[name="email"]')
        .fill(email);
      await page.getByRole('button', { name: /continue/i }).click();
    });
    await step('submit password', async () => {
      // Password field appears inline after the email step submits via XHR;
      // no navigation fires, so waitForLoadState would hang.
      const passwordInput = page.locator('input[type="password"]');
      await passwordInput.waitFor({ state: 'visible' });
      await passwordInput.fill(password);
      await page.getByRole('button', { name: /log.?in|sign.?in|continue/i }).click();
      await page.waitForURL(/\/oauth\/device/, { timeout: 30000 });
    }, true);
    await step('enter user code', async () => {
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
    await step('authorize', async () => {
      const authorizeBtn = page.getByRole('button', { name: 'Authorize' });
      await authorizeBtn.waitFor({ state: 'visible', timeout: 15000 });
      await authorizeBtn.click();
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
  const sandbox = process.env.PW_SANDBOX;
  if (!sandbox) throw new Error('PW_SANDBOX is not set; sandbox-setup must run first');

  const sandboxUserHome = path.join(sandbox, 'user-home');
  const modeRaw = process.env.PW_SANDBOX_POSITAI_AUTH ?? 'flow';
  if (!AUTH_MODES.includes(modeRaw as PositAiAuthMode)) {
    throw new Error(`PW_SANDBOX_POSITAI_AUTH="${modeRaw}" -- expected one of: ${AUTH_MODES.join(', ')}, or unset`);
  }
  const mode: PositAiAuthMode = modeRaw as PositAiAuthMode;

  if (mode === 'off') {
    console.log('[auth-setup] PW_SANDBOX_POSITAI_AUTH=off; Posit AI tests will skip');
    return;
  }

  if (mode === 'seed') {
    // The global host-copy kill-switch suppresses seed (but not the sign-in flow).
    if (noSeedCredentials()) {
      console.log('[auth-setup] PW_SANDBOX_NO_SEED_CREDENTIALS set; skipping seed, Posit AI tests will skip');
      return;
    }
    if (!isStoreFileAuthenticated(storeFile(os.homedir()))) {
      console.log('[auth-setup] PW_SANDBOX_POSITAI_AUTH=seed: host is not signed in to Posit AI; Posit AI tests will be skipped');
      return;
    }
    copyTree(
      path.join(os.homedir(), POSITAI_DIR_RELATIVE),
      path.join(sandboxUserHome, POSITAI_DIR_RELATIVE),
    );
    verifyStoreWritten(sandboxUserHome);
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
    const [, resolved] = await Promise.all([
      automateLogin(verification_uri_complete, user_code, email, password),
      pollForToken(device_code, interval ?? 5, 90000),
    ]);
    tokenData = resolved;
  } catch (err) {
    // Fail loud on deterministic problems: a terminal OAuth error (denied or
    // bad authorization, config fault) or a browser-side failure at the
    // credential/authorization steps (e.g. a wrong POSIT_PASSWORD). Only
    // genuinely transient failures (page load, network, poll timeout) fall
    // through to a skip, so the @ai tests report "no credentials" rather than
    // masquerading a real failure as one.
    if (err instanceof OAuthTerminalError || err instanceof LoginAutomationError) {
      throw err;
    }
    console.warn(
      '[auth-setup] WARNING: Posit AI authentication flow failed; Posit AI tests will be skipped:',
      err instanceof Error ? err.message : err,
    );
    return;
  }

  // Past this point sign-in demonstrably succeeded, so a malformed token
  // response or a failed store write is a contract violation, not flake -- let
  // these throw and fail the run rather than masquerade as a skip.
  writeTokensToSandbox(tokenData, sandboxUserHome);
  verifyStoreWritten(sandboxUserHome);
  console.log('[auth-setup] Posit AI sign-in complete; tokens written to sandbox');
});
