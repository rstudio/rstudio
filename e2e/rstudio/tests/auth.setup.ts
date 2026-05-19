import { test as setup, chromium } from '@playwright/test';
import * as fs from 'fs';
import * as os from 'os';
import * as path from 'path';
import {
  AUTH_STORAGE_KEY,
  POSITAI_STORE_RELATIVE,
  isStoreFileAuthenticated,
} from '../utils/auth';

const AUTH_HOST = 'login.posit.cloud';
const CLIENT_ID = 'rstudio-ide';
const SCOPE = 'prism';

function getCacheDir(): string {
  if (process.platform === 'win32') {
    const localAppData = process.env.LOCALAPPDATA ?? path.join(os.homedir(), 'AppData', 'Local');
    return path.join(localAppData, 'rstudio-playwright-auth', 'positai');
  }
  if (process.platform === 'darwin') {
    return path.join(os.homedir(), 'Library', 'Caches', 'rstudio-playwright-auth', 'positai');
  }
  const xdgCache = process.env.XDG_CACHE_HOME ?? path.join(os.homedir(), '.cache');
  return path.join(xdgCache, 'rstudio-playwright-auth', 'positai');
}

function storeFile(homeDir: string): string {
  return path.join(homeDir, POSITAI_STORE_RELATIVE);
}

function cachedStoreFile(): string {
  return path.join(getCacheDir(), 'data.json');
}

function copyToSandbox(src: string, sandboxUserHome: string): void {
  const dest = storeFile(sandboxUserHome);
  fs.mkdirSync(path.dirname(dest), { recursive: true });
  fs.copyFileSync(src, dest);
}

function saveToCache(sandboxUserHome: string): void {
  const src = storeFile(sandboxUserHome);
  const dest = cachedStoreFile();
  fs.mkdirSync(path.dirname(dest), { recursive: true });
  fs.copyFileSync(src, dest);
}

interface DeviceCodeResponse {
  user_code: string;
  verification_uri: string;
  verification_uri_complete: string;
  device_code: string;
  interval: number;
  expires_in: number;
}

interface TokenResponse {
  access_token: string;
  refresh_token: string;
  expires_in: number;
  token_type: string;
  scope: string;
}

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

async function pollForToken(deviceCode: string, intervalSeconds: number, timeoutMs: number): Promise<TokenResponse> {
  const deadline = Date.now() + timeoutMs;
  let waitMs = intervalSeconds * 1000;
  for (;;) {
    if (Date.now() > deadline) {
      throw new Error('timed out waiting for device code authorization');
    }
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
      return resp.json() as Promise<TokenResponse>;
    }
    const error = (await resp.json() as { error: string }).error;
    if (error === 'authorization_pending') continue;
    if (error === 'slow_down') { waitMs += 5000; continue; }
    throw new Error(`Token polling failed: ${error}`);
  }
}

async function automateLogin(verificationUriComplete: string, userCode: string, email: string, password: string): Promise<void> {
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
    // The device activation URL redirects to login when unauthenticated.
    await page.goto(verificationUriComplete);
    await page.locator('input[type="email"], input[name="username"], input[name="email"]').fill(email);
    await page.getByRole('button', { name: /continue/i }).click();
    // Password field appears inline -- wait for it rather than waitForLoadState.
    await page.locator('input[type="password"]').waitFor({ state: 'visible' });
    await page.locator('input[type="password"]').fill(password);
    await page.getByRole('button', { name: /log.?in|sign.?in|continue/i }).click();
    await page.waitForURL(/\/oauth\/device/, { timeout: 30000 });
    // Navigate to the bare verification URI for a fresh empty code form.
    await page.goto(verificationUri);
    await page.locator('input[name="userCode0"]').waitFor({ state: 'visible' });
    // Type each character into its named input -- pressSequentially triggers React onChange.
    const codeChars = userCode.replace(/-/g, '').split('');
    for (let i = 0; i < codeChars.length; i++) {
      await page.locator(`input[name="userCode${i}"]`).pressSequentially(codeChars[i]);
    }
    // Click Continue to submit the code, then Authorize on the confirmation screen.
    await page.getByRole('button', { name: /continue/i }).click();
    const authorizeBtn = page.getByRole('button', { name: 'Authorize' });
    await authorizeBtn.waitFor({ state: 'visible', timeout: 15000 });
    await authorizeBtn.click();
  } finally {
    await browser.close();
  }
}

function writeTokensToSandbox(tokenData: TokenResponse, sandboxUserHome: string): void {
  const expiresAt = new Date(Date.now() + tokenData.expires_in * 1000).toISOString();
  const storeData = {
    [AUTH_STORAGE_KEY]: {
      authenticated: true,
      oauthAuth: {
        tokenData: {
          accessToken: tokenData.access_token,
          refreshToken: tokenData.refresh_token,
          expiresAt,
          tokenType: tokenData.token_type,
          scope: tokenData.scope,
        },
        expiresAt,
        scope: tokenData.scope,
      },
    },
  };
  const dest = storeFile(sandboxUserHome);
  fs.mkdirSync(path.dirname(dest), { recursive: true });
  fs.writeFileSync(dest, JSON.stringify(storeData, null, 2), { mode: 0o600 });
}

setup('authenticate Posit AI', async () => {
  const sandbox = process.env.PW_SANDBOX;
  if (!sandbox) throw new Error('PW_SANDBOX is not set; sandbox-setup must run first');

  const sandboxUserHome = path.join(sandbox, 'user-home');
  const mode = process.env.PW_SANDBOX_POSITAI_AUTH;

  if (mode !== undefined && mode !== 'seed' && mode !== 'cache') {
    throw new Error(`PW_SANDBOX_POSITAI_AUTH="${mode}" -- expected "seed", "cache", or unset`);
  }

  if (mode === 'seed') {
    const src = storeFile(os.homedir());
    if (!isStoreFileAuthenticated(src)) {
      throw new Error(
        'PW_SANDBOX_POSITAI_AUTH=seed: no valid tokens at ~/.positai/store/data.json -- sign in to Posit AI first',
      );
    }
    copyToSandbox(src, sandboxUserHome);
    console.log('[auth-setup] seeded PAI tokens from ~/.positai');
    return;
  }

  const cached = cachedStoreFile();

  if (mode === 'cache' && isStoreFileAuthenticated(cached)) {
    copyToSandbox(cached, sandboxUserHome);
    console.log('[auth-setup] loaded PAI tokens from cache');
    return;
  }

  const email = process.env.POSIT_EMAIL;
  const password = process.env.POSIT_PASSWORD;

  if (!email || !password) {
    console.log('[auth-setup] POSIT_EMAIL/POSIT_PASSWORD not set; @auth tests will be skipped');
    return;
  }

  console.log('[auth-setup] starting device flow...');
  try {
    const { verification_uri, verification_uri_complete, user_code, device_code, interval } = await fetchDeviceCode();
    console.log(`[auth-setup] verification_uri: ${verification_uri}`);
    console.log(`[auth-setup] user_code: ${user_code}`);
    const tokenPromise = pollForToken(device_code, interval, 90000);
    await automateLogin(verification_uri_complete, user_code, email, password);
    const tokenData = await tokenPromise;
    writeTokensToSandbox(tokenData, sandboxUserHome);
    console.log('[auth-setup] device flow complete; PAI tokens written to sandbox');
    if (mode === 'cache') {
      saveToCache(sandboxUserHome);
      console.log('[auth-setup] PAI tokens saved to cache');
    }
  } catch (err) {
    console.warn('[auth-setup] device flow failed; @auth tests will be skipped:', err instanceof Error ? err.message : err);
  }
});
