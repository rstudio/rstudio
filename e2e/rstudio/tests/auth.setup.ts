import { test as setup, chromium } from '@playwright/test';
import * as fs from 'fs';
import * as os from 'os';
import * as path from 'path';
import {
  AUTH_STORAGE_KEY,
  POSITAI_STORE_RELATIVE,
  PositAiOAuthEntry,
  isStoreFileAuthenticated,
} from '../utils/auth';

/**
 * Authenticate Posit AI for the per-invocation sandbox.
 *
 * This is a Playwright setup project (see playwright.config.ts): it runs once
 * after globalSetup and before the desktop/server test projects, which depend
 * on it. Its job is to leave a valid Posit AI token store at
 * <sandbox>/user-home/.positai/store/data.json so the @ai tests start signed
 * in. It is the sole authority for Posit AI credentials in the sandbox --
 * globalSetup (sandbox-setup.ts) no longer copies Posit AI creds.
 *
 * Modes (PW_SANDBOX_POSITAI_AUTH), default "flow":
 *   flow   Run the OAuth device flow using POSIT_EMAIL/POSIT_PASSWORD. If
 *          either is unset, log and let the @ai tests skip. Never falls back
 *          to "seed". This is the default -- unset behaves as "flow".
 *   seed   Copy the host user's ~/.positai/store/data.json into the sandbox.
 *          Fails loud if the host file is not authenticated. Explicit only;
 *          never used as a fallback. Useful for fast local iteration when the
 *          developer is already signed in on the host. Suppressed by the
 *          global host-copy kill-switch PW_SANDBOX_NO_SEED_CREDENTIALS, in
 *          which case the @ai tests skip.
 *   off    Provision nothing; the @ai tests skip.
 */

const AUTH_HOST = 'login.posit.cloud';
const CLIENT_ID = 'rstudio-ide';
const SCOPE = 'prism';

function storeFile(homeDir: string): string {
  return path.join(homeDir, POSITAI_STORE_RELATIVE);
}

// The global host-copy kill-switch (Scope A): when set, seed mode is
// suppressed (device flow is unaffected). Parsed the same way as in
// sandbox-setup.ts.
function noSeedCredentials(): boolean {
  return ['1', 'true'].includes(
    (process.env.PW_SANDBOX_NO_SEED_CREDENTIALS ?? '').toLowerCase(),
  );
}

function copyStoreFile(src: string, dest: string): void {
  fs.mkdirSync(path.dirname(dest), { recursive: true });
  fs.copyFileSync(src, dest);
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
  interval: number;
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
// optional. Device-flow error codes (RFC 8628 §3.5) include authorization_-
// pending and slow_down (transient) plus access_denied and expired_token.
interface TokenErrorResponse {
  error: string;
  error_description?: string;
  error_uri?: string;
}

class DeviceFlowTerminalError extends Error {
  constructor(public readonly oauthError: string, message: string) {
    super(message);
    this.name = 'DeviceFlowTerminalError';
  }
}

const TERMINAL_OAUTH_ERRORS = new Set(['access_denied', 'expired_token', 'invalid_grant']);

async function fetchDeviceCode(): Promise<DeviceCodeResponse> {
  const resp = await fetch(`https://${AUTH_HOST}/oauth/device/authorize`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body: new URLSearchParams({ scope: SCOPE, client_id: CLIENT_ID }).toString(),
  });
  if (!resp.ok) {
    throw new Error(`Device auth request failed: ${resp.status} ${await resp.text()}`);
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
    if (TERMINAL_OAUTH_ERRORS.has(body.error)) {
      const suffix = body.error_description ? `: ${body.error_description}` : '';
      throw new DeviceFlowTerminalError(
        body.error,
        `Device flow terminated by OAuth server (${body.error})${suffix}`,
      );
    }
    throw new Error(`Token polling failed: ${body.error}`);
  }
  throw new Error('timed out waiting for device code authorization');
}

async function step<T>(name: string, fn: () => Promise<T>): Promise<T> {
  try {
    return await fn();
  } catch (err) {
    const msg = err instanceof Error ? err.message : String(err);
    throw new Error(`[automate-login: ${name}] ${msg}`);
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
    });
    await step('enter device code', async () => {
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
    });
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
  const mode = process.env.PW_SANDBOX_POSITAI_AUTH ?? 'flow';

  if (mode !== 'off' && mode !== 'seed' && mode !== 'flow') {
    throw new Error(`PW_SANDBOX_POSITAI_AUTH="${mode}" -- expected "off", "seed", "flow", or unset`);
  }

  if (mode === 'off') {
    console.log('[auth-setup] PW_SANDBOX_POSITAI_AUTH=off; @ai tests will skip');
    return;
  }

  if (mode === 'seed') {
    // Scope A: the global host-copy kill-switch suppresses seed (but not flow).
    if (noSeedCredentials()) {
      console.log('[auth-setup] PW_SANDBOX_NO_SEED_CREDENTIALS set; skipping seed, @ai tests will skip');
      return;
    }
    const src = storeFile(os.homedir());
    if (!isStoreFileAuthenticated(src)) {
      throw new Error(
        'PW_SANDBOX_POSITAI_AUTH=seed: no valid tokens at ~/.positai/store/data.json -- sign in to Posit AI first',
      );
    }
    copyStoreFile(src, storeFile(sandboxUserHome));
    verifyStoreWritten(sandboxUserHome);
    console.log('[auth-setup] seeded PAI tokens from ~/.positai');
    return;
  }

  // mode === 'flow' (default): OAuth device flow. Never falls back to seed.
  const email = process.env.POSIT_EMAIL;
  const password = process.env.POSIT_PASSWORD;

  if (!email || !password) {
    console.log('[auth-setup] POSIT_EMAIL/POSIT_PASSWORD not set; @ai tests will be skipped');
    return;
  }

  console.log('[auth-setup] starting device flow...');
  try {
    const { verification_uri, verification_uri_complete, user_code, device_code, interval } =
      await fetchDeviceCode();
    console.log(`[auth-setup] verification_uri: ${verification_uri}`);
    console.log(`[auth-setup] user_code: ${user_code}`);
    const tokenPromise = pollForToken(device_code, interval, 90000);
    await automateLogin(verification_uri_complete, user_code, email, password);
    const tokenData = await tokenPromise;
    writeTokensToSandbox(tokenData, sandboxUserHome);
    verifyStoreWritten(sandboxUserHome);
    console.log('[auth-setup] device flow complete; PAI tokens written to sandbox');
  } catch (err) {
    // Terminal OAuth errors (access_denied, expired_token, invalid_grant) are
    // deterministic config/credential problems, not flake; surface them so a
    // bad POSIT_EMAIL/POSIT_PASSWORD fails the run instead of looking like a
    // skipped @ai test.
    if (err instanceof DeviceFlowTerminalError) {
      throw err;
    }
    console.warn(
      '[auth-setup] device flow failed; @ai tests will be skipped:',
      err instanceof Error ? err.message : err,
    );
  }
});
