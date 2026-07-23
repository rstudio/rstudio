import { test as setup, type Page } from '@playwright/test';
import * as fs from 'fs';
import * as os from 'os';
import * as path from 'path';
import {
  AUTH_STORAGE_KEY,
  PositAiOAuthEntry,
  copilotConfigDir,
  findAuthenticatedStore,
  isCopilotStoreAuthenticated,
  isStoreFileAuthenticated,
  noSeedCredentials,
  storeFileCandidates,
  strictAiAuth,
  writeAuthStatus,
} from '../utils/auth';
import {
  CopilotAgent,
  STATUS_ALREADY_SIGNED_IN,
  STATUS_NOT_AUTHORIZED,
  STATUS_OK,
  STATUS_PROMPT_DEVICE_FLOW,
} from '../utils/copilot-agent';
import { authorizeDeviceCode, GitHubLoginError } from '../utils/github-device-authorize';
import { launchAuthBrowser } from '../utils/auth-debug';

/**
 * Authenticate the AI providers (Posit AI and GitHub Copilot) for the
 * per-invocation sandbox.
 *
 * This is a Playwright setup project (see playwright.config.ts): it runs once
 * after globalSetup and before the desktop/server test projects, which depend
 * on it. Its job is to leave each provider's credential store in the sandbox
 * user-home so the AI tests start signed in. It is the sole authority for AI
 * credentials in the sandbox -- globalSetup (sandbox-setup.ts) copies nothing.
 * Every exit path that lets the run continue (success or skip) also records
 * what happened in <sandbox>/<provider>-auth-status.json (see AiAuthStatus in
 * utils/auth.ts), which requireAiCredentials() reads so skipped tests report
 * the actual cause. The fail-loud throw paths write nothing -- they fail the
 * setup, so dependent tests don't run at all.
 *
 * Each provider's credential source is auto-detected, not selected by an
 * environment variable:
 *   1. The provider's credentials are set (POSIT_EMAIL/POSIT_PASSWORD, or
 *      COPILOT_USER/COPILOT_PASSWORD) -> run its live sign-in flow.
 *      Setting the credentials is deliberate, so it wins even on a machine
 *      already signed in locally -- it's how you exercise the sign-in flow.
 *   2. else a local credential store exists (~/.posit/ai/auth/data.json;
 *      ~/.config/github-copilot or %LOCALAPPDATA%\github-copilot) and the
 *      PW_SANDBOX_NO_SEED_CREDENTIALS seed kill-switch is not set -> copy it
 *      into the sandbox.
 *   3. else -> provision nothing; that provider's tests skip with a reason
 *      drawn from its status file.
 *
 * The two providers detect their sources independently: Posit AI falling back
 * to copy never makes Copilot copy, and vice versa. Their only shared inputs
 * are the seed kill-switch and strict mode.
 */

const AUTH_HOST = 'login.posit.cloud';
const CLIENT_ID = 'rstudio-ide';
const SCOPE = 'prism';

// Whether this run targets RStudio Server. In server mode the rsession reads the
// logged-in user's real home directory (from the passwd db), not the sandbox this
// setup writes to, so the completion messages below warn that the sandbox
// credentials won't reach the session.
const IS_SERVER = (process.env.PW_RSTUDIO_MODE ?? 'desktop').toLowerCase() === 'server';

// Copy a single file, first creating the destination's parent directory.
function copyFile(src: string, dest: string): void {
  fs.mkdirSync(path.dirname(dest), { recursive: true });
  fs.copyFileSync(src, dest);
}

// Copy the local token store into the sandbox.
function copyStoreToSandbox(localStore: string, sandboxUserHome: string): void {
  for (const dest of storeFileCandidates(sandboxUserHome)) {
    copyFile(localStore, dest);
  }
}

// Verify the write succeeded: a partial or failed write must not pass.
function verifyStoreWritten(sandboxUserHome: string): void {
  for (const dest of storeFileCandidates(sandboxUserHome)) {
    if (!isStoreFileAuthenticated(dest)) {
      throw new Error(
        `[auth-setup] post-write verification failed for ${dest}: store is missing, unreadable, malformed, lacks required fields, or its token is already expired (the unreadable/malformed cases emit a preceding [auth] WARNING naming the cause; the missing-file, missing-fields, and expiry cases fail without one)`,
      );
    }
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
  // REQUIRED per RFC 8628 §3.2, but unused here (the poll runs under its own
  // timeout), so fetchDeviceCode doesn't validate it -- optional keeps the
  // type honest about what's actually guaranteed.
  expires_in?: number;
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
    // This fetch runs before (and outside) the withDeadline race in the setup
    // body, so it needs its own bound: unbounded, a hung request would run
    // into the harness timeout and bypass the transient path entirely.
    signal: AbortSignal.timeout(30_000),
  });
  if (!resp.ok) {
    const text = await resp.text();
    // A 4xx carrying an RFC 6749 error body is the server rejecting the
    // request as such (invalid_client, invalid_scope, ...) -- a config fault
    // that a retry can't heal, so it's terminal, matching how pollForToken
    // classifies the same codes. 5xx and proxy noise stay plain (transient).
    if (resp.status < 500) {
      let oauthError: unknown;
      try {
        oauthError = (JSON.parse(text) as Partial<TokenErrorResponse>).error;
      } catch {
        // Not JSON; fall through to the plain error below.
      }
      if (typeof oauthError === 'string' && oauthError !== '') {
        throw new OAuthTerminalError(
          `device-authorize request rejected by server (${oauthError}): HTTP ${resp.status}`,
        );
      }
    }
    throw new Error(`OAuth sign-in request failed: ${resp.status} ${text.slice(0, 200)}`);
  }
  let data: Partial<DeviceCodeResponse>;
  try {
    data = (await resp.json()) as Partial<DeviceCodeResponse>;
  } catch (err) {
    // A 200 that isn't JSON is a contract change, exactly like a 200 missing
    // fields below -- terminal, not flake.
    throw new OAuthTerminalError(
      `device-authorize response is not valid JSON: ${(err as Error).message}`,
    );
  }
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
      let token: Partial<TokenSuccessResponse>;
      try {
        token = (await resp.json()) as Partial<TokenSuccessResponse>;
      } catch (err) {
        // A 200 that isn't JSON is a contract change on the token endpoint,
        // not flake -- terminal, matching fetchDeviceCode. (Contrast with the
        // non-JSON *error* responses below, which are proxy noise and are
        // retried through.)
        throw new OAuthTerminalError(
          `token response is not valid JSON: ${(err as Error).message}`,
        );
      }
      // Same promotion discipline as fetchDeviceCode: name the missing field
      // here, where the culprit is identifiable as the token endpoint, rather
      // than let it surface later as a store-verification failure.
      for (const field of ['access_token', 'token_type'] as const) {
        if (typeof token[field] !== 'string' || token[field] === '') {
          throw new OAuthTerminalError(
            `token response is missing or has a non-string "${field}"; the endpoint contract may have changed`,
          );
        }
      }
      return token as TokenSuccessResponse;
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
    // page.url() is the one piece of non-sensitive context always available
    // with artifacts disabled for this project (see playwright.config.ts);
    // guard it in case the page is already closed. PW_DEBUG_AUTH_CAPTURE=1 opts into
    // page captures via the shared auth browser (utils/auth-debug.ts).
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

// Set by the authorize step when its clicks never visibly registered. The
// console warning it emits is only visible in the setup's own output; this
// variable carries the same diagnosis into the status file, so the eventual
// poll-timeout skip reason names the likely culprit. (Written at most once
// per process: the setup project runs one test with retries: 0.)
let authorizeStepWarning: string | undefined;

async function automateLogin(
  verificationUriComplete: string,
  userCode: string,
  email: string,
  password: string,
): Promise<void> {
  const verificationUri = verificationUriComplete.split('?')[0];
  let session: Awaited<ReturnType<typeof launchAuthBrowser>>;
  try {
    session = await launchAuthBrowser('positai');
  } catch (err) {
    // A launch failure is an environment gap, not flake: desktop runs connect
    // to RStudio's Electron over CDP and never otherwise need Playwright's
    // own chromium, so a runner can pass the whole suite while lacking the
    // binary this flow launches. Fail the run with the remedy rather than
    // skipping the Posit AI tests forever.
    throw new LoginAutomationError(
      `could not launch the sign-in browser: ${(err as Error).message} -- if the Playwright browser is not installed, run "npx playwright install" from e2e/rstudio`,
    );
  }
  try {
    const page = session.page;
    // All selectors below track the login.posit.cloud markup; when that page
    // changes, these are the first thing to break (surfacing as a fatal error
    // on the credential steps, or as a skip whose login-failed reason names
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
        try {
          // Short explicit timeout: a click that starts just as the SPA's
          // post-authorize transition detaches the button would otherwise
          // retry actionability for the raw-page 30s default and surface as
          // a fatal "authorize failed" -- on a sign-in that succeeded.
          await authorizeBtn.click({ timeout: 2000 });
        } catch {
          // Button gone mid-click means a previous click registered and the
          // page advanced -- that's success. Still attached: keep trying.
          if (!(await authorizeBtn.isVisible())) break;
        }
        await page.waitForTimeout(1000);
      }
      if (await authorizeBtn.isVisible()) {
        authorizeStepWarning =
          'authorize button still present after 5 click attempts; authorization may not have registered';
        console.warn(`[auth-setup] WARNING: ${authorizeStepWarning} (expect a poll timeout)`);
      }
    }, true);
  } finally {
    // session.close() takes the final page capture and never throws, so it
    // can't replace an in-flight LoginAutomationError (see auth-debug.ts).
    await session.close();
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
  // Write every candidate location (currently one; the array exists so a
  // future store migration only needs to change POSITAI_STORE_CANDIDATES).
  for (const dest of storeFileCandidates(sandboxUserHome)) {
    fs.mkdirSync(path.dirname(dest), { recursive: true });
    fs.writeFileSync(dest, JSON.stringify(storeData, null, 2), { mode: 0o600 });
  }
}

// PW_AI_AUTH_STRICT: fail the run instead of returning normally when the
// setup ends without credentials. Called after the status file is written, so
// the record of what happened survives either way. This is the guard against
// perpetual green-with-skips: on runs that expect credentials to be present
// (e.g. CI once secrets are wired in), a broken credential source must turn
// the run red, however the failure was classified.
function failIfStrict(providerLabel: string, reason: string): void {
  if (strictAiAuth()) {
    throw new Error(
      `[auth-setup] PW_AI_AUTH_STRICT is set but ${providerLabel} was not provisioned: ${reason}`,
    );
  }
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
  const email = process.env.POSIT_EMAIL;
  const password = process.env.POSIT_PASSWORD;

  // A half-set pair is a config mistake, not a source: dropping silently into
  // copy/skip would bypass the sign-in flow the user clearly intended, and the
  // skip reason below would wrongly report both as unset. Fail loud so the typo
  // (or the forgotten second variable) surfaces immediately.
  if (!!email !== !!password) {
    throw new Error(
      '[auth-setup] exactly one of POSIT_EMAIL / POSIT_PASSWORD is set; set both to exercise the sign-in flow, or neither to copy the local token store',
    );
  }

  // Source 2/3: no credentials set -> copy the local token store, else skip.
  // (Source 1, the sign-in flow, is below and runs when credentials are set.)
  if (!email || !password) {
    // The global seed kill-switch blocks copying the local token store.
    if (noSeedCredentials()) {
      writeAuthStatus(sandbox, 'positai', {
        source: 'none',
        outcome: 'unavailable',
        reason: 'Not provisioning Posit AI: POSIT_EMAIL/POSIT_PASSWORD are unset (so no sign-in) and PW_SANDBOX_NO_SEED_CREDENTIALS blocked copying the local token store. Set the credentials for the sign-in flow, or unset the seed kill-switch while signed in to Posit AI locally.',
      });
      console.log('[auth-setup] no credentials set and PW_SANDBOX_NO_SEED_CREDENTIALS set; Posit AI tests will skip');
      failIfStrict('Posit AI', 'POSIT_EMAIL/POSIT_PASSWORD are unset and PW_SANDBOX_NO_SEED_CREDENTIALS blocked the local copy');
      return;
    }
    const localStore = findAuthenticatedStore(os.homedir());
    if (localStore === null) {
      writeAuthStatus(sandbox, 'positai', {
        source: 'none',
        outcome: 'unavailable',
        reason: 'Not provisioning Posit AI: not signed in to Posit AI locally (no valid token store at ~/.posit/ai/auth/data.json) and POSIT_EMAIL/POSIT_PASSWORD are unset. Sign in to Posit AI locally, or set the credentials for the sign-in flow.',
      });
      console.log('[auth-setup] not signed in to Posit AI locally and no credentials set; Posit AI tests will skip');
      failIfStrict('Posit AI', 'not signed in to Posit AI locally and POSIT_EMAIL/POSIT_PASSWORD are unset');
      return;
    }
    copyStoreToSandbox(localStore, sandboxUserHome);
    verifyStoreWritten(sandboxUserHome);
    writeAuthStatus(sandbox, 'positai', {
      source: 'copy',
      outcome: 'success',
      reason: `copied the local token store from ${localStore}`,
    });
    console.log(`[auth-setup] copied Posit AI token store from ${localStore}`);
    console.log(
      "[auth-setup] real Posit AI tokens now live in the sandbox; teardown scrubs them if the run is preserved, and warns loudly if it can't.",
    );
    return;
  }

  // Source 1: credentials set -> OAuth device-flow sign-in. Chosen whenever
  // POSIT_EMAIL/POSIT_PASSWORD are set, even on a machine already signed in
  // locally: setting the credentials is the deliberate way to exercise the
  // sign-in flow. The seed kill-switch never affects it (it copies nothing from
  // the host); note it in the log so a run configured with the seed kill-switch set
  // doesn't look silently ignored.
  if (noSeedCredentials()) {
    console.log('[auth-setup] PW_SANDBOX_NO_SEED_CREDENTIALS set; Posit AI sign-in flow is unaffected.');
  }

  console.log('[auth-setup] starting Posit AI sign-in...');
  let tokenData: TokenSuccessResponse;
  try {
    const { verification_uri, verification_uri_complete, user_code, device_code, interval } =
      await fetchDeviceCode();
    console.log(`[auth-setup] verification_uri: ${verification_uri}`);
    // Drive the browser and poll for the token concurrently. Promise.all
    // attaches a handler to both, so a failure in automateLogin can't leave the
    // poll as an orphaned unhandled rejection. interval defaults to 5s per
    // RFC 8628 §3.2.
    const flow = Promise.all([
      automateLogin(verification_uri_complete, user_code, email, password),
      pollForToken(device_code, interval ?? 5, 90000),
    ]);
    // Keep a handler attached so a rejection landing after the deadline race
    // below has already settled can't become an unhandled rejection. Once the
    // race HAS settled, though, a flow rejection is otherwise-lost diagnostic
    // (the real cause behind a deadline expiry), so log it then. This handler
    // is attached before the race, so when the flow's own rejection is what
    // settles the race, it runs first -- outerSettled still false -- and
    // stays quiet: no double-logging on the normal failure path.
    let outerSettled = false;
    flow.catch((err) => {
      if (outerSettled) {
        console.warn(
          '[auth-setup] late sign-in rejection (after the overall deadline already fired):',
          err instanceof Error ? err.message : String(err),
        );
      }
    });
    // Bound the whole flow well under this test's 240s timeout (set above) so
    // a pile-up of slow steps fails through the transient path here instead
    // of hitting the harness timeout, which would skip the catch below and
    // fail the entire run. (fetchDeviceCode above runs outside this race and
    // carries its own 30s bound.)
    let resolvedPair: [void, TokenSuccessResponse];
    try {
      resolvedPair = await withDeadline(flow, 180_000, 'overall sign-in deadline exceeded');
    } finally {
      outerSettled = true;
    }
    tokenData = resolvedPair[1];
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
    // A swallowed authorize click surfaces here as a generic poll timeout;
    // append the authorize step's own diagnosis so the skip reason names the
    // likely culprit rather than only the symptom.
    const authorizeNote = authorizeStepWarning ? ` [${authorizeStepWarning}]` : '';
    writeAuthStatus(sandbox, 'positai', {
      source: 'login',
      outcome: 'login-failed',
      reason: `sign-in flow was attempted but failed: ${msg}${authorizeNote}`,
    });
    console.warn(
      '[auth-setup] WARNING: Posit AI authentication flow failed; Posit AI tests will be skipped:',
      msg,
    );
    failIfStrict('Posit AI', `the sign-in flow was attempted but failed: ${msg}${authorizeNote}`);
    return;
  }

  // Past this point sign-in demonstrably succeeded, so a malformed token
  // response or a failed store write is a contract violation, not flake -- let
  // these throw and fail the run rather than masquerade as a skip.
  writeTokensToSandbox(tokenData, sandboxUserHome);
  verifyStoreWritten(sandboxUserHome);
  writeAuthStatus(sandbox, 'positai', {
    source: 'login',
    outcome: 'success',
    reason: 'sign-in flow completed',
  });
  console.log(
    IS_SERVER
      ? '[auth-setup] WARNING: Posit AI sign-in complete, but these sandbox credentials will not reach an RStudio Server rsession, which reads the logged-in user\'s real home directory instead'
      : '[auth-setup] Posit AI sign-in complete; credentials are in place for RStudio Desktop',
  );
});

// ---------------------------------------------------------------------------
// GitHub Copilot. Same auto-detect shape as Posit AI above, different
// mechanics: the sign-in flow spawns the copilot-language-server itself
// (utils/copilot-agent.ts) with HOME pointed at the sandbox user-home, lets it
// run GitHub's OAuth device-flow sign-in, and completes the browser half
// (utils/github-device-authorize.ts). The agent writes its own credential
// store (auth.db), so the sandbox ends up with exactly what a real sign-in
// through the IDE would produce. The copy mode replicates the host's
// github-copilot config dir, as sandbox-setup.ts used to do.

// The host-side Copilot config dir (the copy-mode source). Windows honors
// %LOCALAPPDATA%, which need not equal <home>\AppData\Local -- that's why this
// is computed here and not with copilotConfigDir, which owns the sandbox-side
// (destination) layout.
function hostCopilotConfigDir(): string {
  return process.platform === 'win32'
    ? path.join(process.env.LOCALAPPDATA ?? path.join(os.homedir(), 'AppData', 'Local'), 'github-copilot')
    : path.join(os.homedir(), '.config', 'github-copilot');
}

// A deterministic Copilot sign-in failure: the flow worked but the account
// can't use Copilot, or the config is unusable. Fails the run rather than
// masquerading as a skip, mirroring OAuthTerminalError / LoginAutomationError
// for Posit AI.
class CopilotTerminalError extends Error {
  constructor(message: string) {
    super(message);
    this.name = 'CopilotTerminalError';
  }
}

setup('authenticate GitHub Copilot', async () => {
  // Same reasoning as the Posit AI block: agent startup, five browser steps,
  // the authorize-button delay (up to 120s), and the agent's own token poll
  // can legitimately exceed the global 120s test timeout; the withDeadline
  // race below settles well before this outer limit.
  setup.setTimeout(300_000);

  const sandbox = process.env.PW_SANDBOX;
  if (!sandbox) throw new Error('PW_SANDBOX is not set; sandbox-setup must run first');

  const sandboxUserHome = path.join(sandbox, 'user-home');
  const user = process.env.COPILOT_USER;
  const password = process.env.COPILOT_PASSWORD;
  const totpSecret = process.env.COPILOT_TOTP_SECRET;

  // Mirror the POSIT_EMAIL/POSIT_PASSWORD rule: a half-set pair is a config
  // mistake, not a source.
  if (!!user !== !!password) {
    throw new Error(
      '[auth-setup] exactly one of COPILOT_USER / COPILOT_PASSWORD is set; set both to exercise the Copilot sign-in flow, or neither to copy the local credential store',
    );
  }

  // Source 2/3: no credentials set -> copy the host's Copilot config dir,
  // else skip.
  if (!user || !password) {
    if (noSeedCredentials()) {
      writeAuthStatus(sandbox, 'copilot', {
        source: 'none',
        outcome: 'unavailable',
        reason: 'Not provisioning GitHub Copilot: COPILOT_USER/COPILOT_PASSWORD are unset (so no sign-in) and PW_SANDBOX_NO_SEED_CREDENTIALS blocked copying the local credential store. Set the credentials for the sign-in flow, or unset the seed kill-switch while signed in to Copilot locally.',
      });
      console.log('[auth-setup] no GitHub credentials set and PW_SANDBOX_NO_SEED_CREDENTIALS set; Copilot tests will skip');
      failIfStrict('GitHub Copilot', 'COPILOT_USER/COPILOT_PASSWORD are unset and PW_SANDBOX_NO_SEED_CREDENTIALS blocked the local copy');
      return;
    }
    const hostDir = hostCopilotConfigDir();
    if (!fs.existsSync(hostDir)) {
      writeAuthStatus(sandbox, 'copilot', {
        source: 'none',
        outcome: 'unavailable',
        reason: `Not provisioning GitHub Copilot: no local credential store (${hostDir} does not exist) and COPILOT_USER/COPILOT_PASSWORD are unset. Sign in to Copilot locally, or set the credentials for the sign-in flow.`,
      });
      console.log(`[auth-setup] no ${hostDir} on host and no GitHub credentials set; Copilot tests will skip`);
      failIfStrict('GitHub Copilot', 'not signed in to Copilot locally and COPILOT_USER/COPILOT_PASSWORD are unset');
      return;
    }
    fs.cpSync(hostDir, copilotConfigDir(sandboxUserHome), { recursive: true });
    // A config dir can exist without a signed-in token (the agent creates one
    // on first launch); a copy of that must not read as provisioned.
    if (!isCopilotStoreAuthenticated(sandboxUserHome)) {
      writeAuthStatus(sandbox, 'copilot', {
        source: 'copy',
        outcome: 'unavailable',
        reason: `Copied ${hostDir}, but its credential store holds no signed-in token. Sign in to Copilot locally (e.g. through RStudio), or set COPILOT_USER/COPILOT_PASSWORD for the sign-in flow.`,
      });
      console.log('[auth-setup] host Copilot config dir copied but holds no signed-in token; Copilot tests will skip');
      failIfStrict('GitHub Copilot', 'the local Copilot store holds no signed-in token');
      return;
    }
    writeAuthStatus(sandbox, 'copilot', {
      source: 'copy',
      outcome: 'success',
      reason: `copied the local credential store from ${hostDir}`,
    });
    console.log(`[auth-setup] copied GitHub Copilot credential store from ${hostDir}`);
    console.log(
      "[auth-setup] real GitHub Copilot tokens now live in the sandbox; teardown scrubs them if the run is preserved, and warns loudly if it can't.",
    );
    return;
  }

  // Source 1: credentials set -> agent-driven sign-in. Same precedence rule as
  // Posit AI: setting the credentials is deliberate, so this wins even on a
  // machine signed in locally, and the seed kill-switch never affects it.
  if (noSeedCredentials()) {
    console.log('[auth-setup] PW_SANDBOX_NO_SEED_CREDENTIALS set; Copilot sign-in flow is unaffected.');
  }

  console.log('[auth-setup] starting GitHub Copilot sign-in...');
  const agent = new CopilotAgent(sandboxUserHome);
  try {
    const flow = (async () => {
      await agent.initialize();
      const initiate = await agent.signInInitiate();

      // With credentials the sandbox user-home may already hold a token from a
      // prior source; the agent then just confirms it, which is success.
      if (initiate.status === STATUS_ALREADY_SIGNED_IN || initiate.status === STATUS_OK) {
        console.log('[auth-setup] Copilot agent reports already signed in');
        return;
      }

      if (initiate.status !== STATUS_PROMPT_DEVICE_FLOW || !initiate.userCode || !initiate.verificationUri) {
        // An unrecognized signInInitiate shape is a protocol change on the
        // agent, not flake.
        throw new CopilotTerminalError(
          `signInInitiate returned an unexpected result (status "${initiate.status}"); the agent protocol may have changed`,
        );
      }
      console.log(`[auth-setup] Copilot device flow: ${initiate.verificationUri}`);

      // The browser half: sign in to GitHub and authorize the agent's device
      // code. The agent polls GitHub itself afterwards, exchanges the code,
      // and persists the token.
      await authorizeDeviceCode({
        verificationUri: initiate.verificationUri,
        userCode: initiate.userCode,
        user,
        password,
        totpSecret,
      });

      const finalStatus = await agent.waitForSignIn();
      if (finalStatus.status === STATUS_NOT_AUTHORIZED) {
        // The mechanics worked (GitHub authorized the device) but the account
        // has no Copilot access -- deterministic, so fail the run with the
        // remedy rather than skipping forever.
        throw new CopilotTerminalError(
          'Copilot sign-in completed but the account has no Copilot access (NotAuthorized). Enable Copilot Free at https://github.com/settings/copilot and re-run.',
        );
      }
      if (finalStatus.status !== STATUS_OK && finalStatus.status !== STATUS_ALREADY_SIGNED_IN) {
        throw new Error(`Copilot sign-in ended in unexpected status "${finalStatus.status}"`);
      }
      console.log('[auth-setup] Copilot signed in');
    })();

    // Bound the whole flow under this test's 300s timeout (set above), for the
    // same reason as the Posit AI block: fail through the transient path here
    // rather than hit the harness timeout and take the whole run down.
    await withDeadline(flow, 270_000, 'overall Copilot sign-in deadline exceeded');
  } catch (err) {
    if (err instanceof CopilotTerminalError || err instanceof GitHubLoginError) throw err;
    const msg = err instanceof Error ? err.message : String(err);
    writeAuthStatus(sandbox, 'copilot', {
      source: 'login',
      outcome: 'login-failed',
      reason: `Copilot sign-in flow was attempted but failed: ${msg}`,
    });
    console.warn(
      '[auth-setup] WARNING: GitHub Copilot sign-in flow failed; Copilot tests will be skipped:',
      msg,
    );
    failIfStrict('GitHub Copilot', `the sign-in flow was attempted but failed: ${msg}`);
    return;
  } finally {
    // Orderly shutdown before reading the store, so the SQLite WAL is
    // checkpointed into auth.db and the row is visible to fresh readers (the
    // gate here, and the IDE processes launched later).
    await agent.shutdown();
  }

  // Past this point sign-in demonstrably succeeded, so a store the gate can't
  // read back is a contract violation, not flake.
  if (!isCopilotStoreAuthenticated(sandboxUserHome)) {
    throw new Error(
      '[auth-setup] Copilot sign-in reported success, but the sandbox credential store holds no token row afterwards',
    );
  }
  writeAuthStatus(sandbox, 'copilot', {
    source: 'login',
    outcome: 'success',
    reason: 'agent-driven sign-in flow completed',
  });
  console.log(
    IS_SERVER
      ? '[auth-setup] WARNING: Copilot sign-in complete, but these sandbox credentials will not reach an RStudio Server rsession, which reads the logged-in user\'s real home directory instead'
      : '[auth-setup] Copilot sign-in complete; credentials are in place for RStudio Desktop',
  );
});
