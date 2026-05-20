import { chromium } from 'playwright';
import type { Browser, Page } from 'playwright';
import { CONSOLE_INPUT, typeInConsole } from '../pages/console_pane.page';
import { sleep } from '../utils/constants';

export interface ServerSession {
  page: Page;
  browser: Browser;
}

/**
 * Connect to RStudio Server, log in, and return a ready session.
 */
export async function launchServer(): Promise<ServerSession> {
  const baseUrl = process.env.PW_RSTUDIO_SERVER_URL || 'http://localhost:8787';
  const url = new URL(baseUrl);
  if (process.env.PW_RSTUDIO_SERVER_PORT) {
    url.port = process.env.PW_RSTUDIO_SERVER_PORT;
  }
  const serverUrl = url.toString().replace(/\/$/, '');
  const username = process.env.PW_RSTUDIO_SERVER_USER || '';
  const password = process.env.PW_RSTUDIO_SERVER_PASSWORD || '';

  console.log(`Connecting to RStudio Server at ${serverUrl}...`);

  const browser = await chromium.launch({
    headless: false,
    args: ['--window-size=960,540', '--window-position=100,100'],
  });
  const context = await browser.newContext({ viewport: null });
  const page = await context.newPage();

  // Navigate to RStudio Server
  await page.goto(serverUrl, { waitUntil: 'domcontentloaded' });

  // Log in if a login form is presented. Servers running with --auth-none
  // (e.g. local rserver-dev) skip straight to the IDE and have no form,
  // so credentials are only required when the form appears.
  const usernameField = page.locator('#username');
  if (await usernameField.isVisible({ timeout: 5_000 }).catch(() => false)) {
    if (!username || !password) {
      throw new Error(
        'Server presented a login form but PW_RSTUDIO_SERVER_USER / PW_RSTUDIO_SERVER_PASSWORD are not set',
      );
    }
    await usernameField.fill(username);
    await page.locator('#password').fill(password);
    await page.locator('#signinbutton').click();
    console.log(`Logged in as ${username}`);
  } else {
    console.log('No login form detected (auth-none mode)');
  }

  // Wait for console to be ready
  const loginTimeout = Number(process.env.PW_RSTUDIO_SERVER_LOGIN_TIMEOUT) || 60_000;
  await page.waitForSelector(CONSOLE_INPUT, { state: 'visible', timeout: loginTimeout });
  console.log('RStudio console is ready');

  // Dismiss any leftover save dialog from a previous session
  try {
    const dontSaveBtn = page.locator("button:has-text('Don\\'t Save'), button:has-text('Do not Save'), #rstudio_dlg_no");
    await dontSaveBtn.click({ timeout: 2000 });
    console.log('Dismissed save dialog from previous session');
    await sleep(500);
  } catch {
    // No dialog present
  }

  // Wait for full UI to load — click Files tab then wait for its toolbar
  await page.getByRole('tab', { name: 'Files' }).click({ timeout: 120_000 });
  await page.waitForSelector('#rstudio_mb_files_touch_file', { state: 'visible', timeout: 120_000 });
  console.log('Files pane toolbar is ready');

  // Prevent "Save workspace image?" dialog on quit
  await typeInConsole(page, '.rs.api.writeRStudioPreference("save_workspace", "never")');
  await sleep(1000);

  // Clear console of any leftover output from previous session
  await page.locator(CONSOLE_INPUT).click({ force: true });
  await sleep(500);
  await page.keyboard.press('Control+l');
  await sleep(500);
  console.log('Console cleared');

  return { page, browser };
}

/**
 * Close the server session: close buffers, sign out, close browser.
 */
export async function shutdownServer(session: ServerSession): Promise<void> {
  const { page, browser } = session;

  try {
    await typeInConsole(page, '.rs.api.closeAllSourceBuffersWithoutSaving()');
    await sleep(1000);
    // Quit the R session so next login gets a fresh one
    await typeInConsole(page, 'q("no")');
    await sleep(2000);
  } catch {
    // Page may already be closed
  }

  await browser.close();
}
