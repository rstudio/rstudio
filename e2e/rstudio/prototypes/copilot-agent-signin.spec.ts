import { test, expect } from '@playwright/test';
import { execFileSync } from 'child_process';
import * as fs from 'fs';
import * as os from 'os';
import * as path from 'path';
import { authorizeDeviceCode } from '../utils/github-device-authorize';
import {
  CopilotAgent,
  STATUS_ALREADY_SIGNED_IN,
  STATUS_NOT_AUTHORIZED,
  STATUS_OK,
  STATUS_PROMPT_DEVICE_FLOW,
} from '../utils/copilot-agent';

/**
 * Prototype: prove the full agent-driven Copilot sign-in, end to end, with no
 * RStudio involved. This is the capability that gives Copilot parity with
 * Posit AI's auth.setup.ts: a machine that has never signed in to Copilot,
 * holding only a GitHub username/password, ends up with a valid credential
 * store (auth.db) written by the copilot-language-server itself.
 *
 * Flow: spawn the agent with HOME pointed at a throwaway temp directory ->
 * initialize/initialized -> signInInitiate (agent fetches the device code) ->
 * complete the GitHub authorization in a browser (github-device-authorize) ->
 * poll checkStatus until OK (the agent exchanges the device code for a token
 * and persists it) -> shut the agent down -> assert auth.db holds a token row.
 *
 * The temp HOME is deleted in finally, pass or fail, so no token ever
 * outlives the run. The real ~/.config/github-copilot is never touched.
 *
 * Credentials (e2e/rstudio/.env.local, gitignored; use a throwaway account):
 *   COPILOT_USER          GitHub login (username, not email)
 *   COPILOT_PASSWORD      that account's password
 *   COPILOT_TOTP_SECRET   optional; base32 2FA secret if the account has one
 */

function log(msg: string): void {
  console.log(`[copilot-signin] ${msg}`);
}

test('copilot-language-server signs in via device flow and writes auth.db', async () => {
  const user = process.env.COPILOT_USER;
  const password = process.env.COPILOT_PASSWORD;
  const totpSecret = process.env.COPILOT_TOTP_SECRET;

  test.skip(
    !user || !password,
    'Set COPILOT_USER and COPILOT_PASSWORD in e2e/rstudio/.env.local (throwaway account).',
  );

  const homeDir = fs.mkdtempSync(path.join(os.tmpdir(), 'copilot-agent-home-'));
  log(`temp HOME: ${homeDir}`);

  let agent: CopilotAgent | undefined;
  try {
    agent = new CopilotAgent(homeDir);
    await agent.initialize();

    const initiate = await agent.signInInitiate();
    log(`signInInitiate: ${JSON.stringify({ ...initiate, userCode: initiate.userCode ? '(set)' : undefined })}`);

    if (initiate.status === STATUS_ALREADY_SIGNED_IN) {
      // Should be impossible with a fresh temp HOME; if it happens, the HOME
      // redirect is broken and the agent found the real user's credentials.
      throw new Error('agent reports AlreadySignedIn under a fresh temp HOME; the HOME redirect is not isolating');
    }

    expect(initiate.status, 'signInInitiate should ask for the device flow').toBe(STATUS_PROMPT_DEVICE_FLOW);
    expect(initiate.userCode, 'signInInitiate should return a user code').toBeTruthy();
    expect(initiate.verificationUri, 'signInInitiate should return a verification URI').toBeTruthy();

    // The browser half: sign in to GitHub and authorize the agent's device code.
    await authorizeDeviceCode({
      verificationUri: initiate.verificationUri!,
      userCode: initiate.userCode!,
      user: user!,
      password: password!,
      totpSecret,
    });

    // The agent polls GitHub itself after signInInitiate; once the grant
    // registers it exchanges the code, persists the token, and checkStatus
    // flips to OK.
    const finalStatus = await agent.waitForSignIn();

    if (finalStatus.status === STATUS_NOT_AUTHORIZED) {
      // The mechanics worked (GitHub authorized the device) but the account
      // has no Copilot access. This is an entitlement problem, not a code
      // problem: enable Copilot Free for the account at
      // https://github.com/settings/copilot and re-run.
      throw new Error(
        'checkStatus returned NotAuthorized: the GitHub account has no Copilot access. '
        + 'Enable Copilot Free at https://github.com/settings/copilot and re-run.',
      );
    }

    expect(finalStatus.status, 'sign-in should complete').toBe(STATUS_OK);
    log(`signed in as ${finalStatus.user ?? '(unknown user)'}`);

    // Orderly shutdown before reading the store, so the WAL is checkpointed
    // into auth.db and the row is visible to a fresh reader.
    await agent.shutdown();
    agent = undefined;

    const authDb = path.join(homeDir, '.config', 'github-copilot', 'auth.db');
    expect(fs.existsSync(authDb), `expected ${authDb} to exist`).toBe(true);

    const rowCount = execFileSync('sqlite3', [authDb, 'SELECT COUNT(*) FROM oauth_tokens;'], {
      encoding: 'utf8',
    }).trim();
    log(`oauth_tokens rows in ${authDb}: ${rowCount}`);
    expect(Number(rowCount), 'auth.db should hold at least one oauth token').toBeGreaterThan(0);

    log('SUCCESS: agent-driven sign-in produced a usable credential store from scratch');
  } finally {
    if (agent) await agent.shutdown();
    fs.rmSync(homeDir, { recursive: true, force: true });
    log(`removed temp HOME ${homeDir}`);
  }
});
