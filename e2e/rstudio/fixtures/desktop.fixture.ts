import type { Page } from '@playwright/test';
import { chromium } from 'playwright';
import type { Browser, BrowserContext } from 'playwright';
import { spawn, execSync } from 'child_process';
import type { ChildProcess } from 'child_process';
import { TIMEOUTS, sleep } from '../utils/constants';
import { CONSOLE_INPUT, typeInConsole } from '../pages/console_pane.page';

// Constants
export const RSTUDIO_PATH = process.platform === 'win32'
  ? 'C:\\Program Files\\RStudio\\rstudio.exe'
  : process.platform === 'darwin'
    ? '/Applications/RStudio.app/Contents/MacOS/RStudio'
    : '/usr/bin/rstudio';
export const CDP_PORT = 9222;
export const CDP_URL = `http://localhost:${CDP_PORT}`;

export interface DesktopSession {
  page: Page;
  browser: Browser;
  rstudioProcess: ChildProcess;
}

/**
 * Launch RStudio with CDP, connect Playwright, and return the session.
 */
export async function launchRStudio(): Promise<DesktopSession> {
  // Clean up any existing RStudio processes
  console.log('Cleaning up any existing RStudio processes...');
  try {
    if (process.platform === 'win32') {
      execSync('taskkill /F /IM rstudio.exe', { stdio: 'ignore' });
    } else if (process.platform === 'darwin') {
      execSync('killall RStudio 2>/dev/null', { stdio: 'ignore' });
    } else {
      execSync('killall rstudio rsession 2>/dev/null', { stdio: 'ignore' });
    }
    await sleep(5000); // Give RStudio time to shut down gracefully
  } catch {
    // Process might not exist, that's fine
  }
  await sleep(TIMEOUTS.processCleanup);

  // Wait for port 9222 to be released (up to 15 seconds)
  const portDeadline = Date.now() + 15000;
  while (Date.now() < portDeadline) {
    try {
      if (process.platform === 'win32') {
        const result = execSync(`powershell.exe -NoProfile -Command "Get-NetTCPConnection -LocalPort ${CDP_PORT} -ErrorAction SilentlyContinue"`, { encoding: 'utf-8' });
        if (!result.trim()) break;
      } else {
        execSync(`lsof -i :${CDP_PORT} -t`, { encoding: 'utf-8' });
        // If lsof succeeds, port is still in use — keep waiting
      }
    } catch {
      break; // No connections on the port
    }
    await sleep(1000);
  }

  // Start RStudio with remote debugging enabled
  console.log(`Starting RStudio with CDP on port ${CDP_PORT}...`);
  const rstudioProcess = spawn(RSTUDIO_PATH, [`--remote-debugging-port=${CDP_PORT}`]);
  let launchError: Error | undefined;
  rstudioProcess.on('error', (err) => {
    launchError = new Error(`Failed to launch RStudio at ${RSTUDIO_PATH}: ${err.message}`);
  });
  console.log(`RStudio process started (PID: ${rstudioProcess.pid})`);

  // Wait for RStudio to start
  await sleep(TIMEOUTS.rstudioStartup);
  if (launchError) throw launchError;

  // Connect to CDP and set up the session.
  // If anything fails after spawn, kill the process to avoid orphaning RStudio on port 9222.
  let browser: Browser | undefined;
  try {
    browser = await chromium.connectOverCDP(CDP_URL);
    const contexts: BrowserContext[] = browser.contexts();
    if (contexts.length === 0) {
      throw new Error('CDP connected but no browser contexts available — RStudio window may not be ready');
    }
    const pages = contexts[0].pages();
    if (pages.length === 0) {
      throw new Error('CDP context has no pages — RStudio window may not be ready');
    }
    const page: Page = pages[0];

    // Dismiss any "save changes" modal from a previous interrupted run
    try {
      const dontSaveBtn = page.locator("button:has-text('Don\\'t Save'), button:has-text('Do not Save'), #rstudio_dlg_no");
      await dontSaveBtn.click({ timeout: 3000 });
      console.log('Dismissed save dialog from previous session');
      await sleep(1000);
    } catch {
      // No dialog, continue normally
    }

    // Dismiss any other modal overlay (e.g. update notification, options dialog)
    try {
      const overlay = page.locator('.gwt-PopupPanelGlass');
      if (await overlay.isVisible({ timeout: 1000 })) {
        await page.keyboard.press('Escape');
        console.log('Dismissed modal overlay during startup');
        await sleep(1000);
      }
    } catch {
      // No overlay
    }

    // Wait for RStudio's GWT app to fully initialize
    await page.waitForFunction('typeof window.desktopHooks?.invokeCommand === "function"', null, { timeout: 30000 });

    // Activate console (makes it visible without zooming)
    await page.evaluate("window.desktopHooks.invokeCommand('activateConsole')");
    await sleep(2000);

    // Wait for console to be ready
    await page.waitForSelector(CONSOLE_INPUT, { state: 'visible', timeout: TIMEOUTS.consoleReady });
    console.log('RStudio console is ready');

    // Prevent "Save workspace image?" dialog on quit
    await typeInConsole(page, '.rs.api.writeRStudioPreference("save_workspace", "never")');
    await sleep(1000);

    return { page, browser, rstudioProcess };
  } catch (err) {
    await browser?.close().catch(() => {});
    rstudioProcess.kill();
    throw err;
  }
}

/**
 * Graceful shutdown: q() in console, close browser, kill process.
 */
export async function shutdownRStudio(session: DesktopSession): Promise<void> {
  const { page, browser, rstudioProcess } = session;

  // Close all source files without prompting to save
  await typeInConsole(page, '.rs.api.closeAllSourceBuffersWithoutSaving()');
  await sleep(1000);

  try {
    await typeInConsole(page, 'q(save = "no")');
    await sleep(5000); // Give RStudio time to shut down and release port
    await browser.close();
  } catch {
    await browser.close().catch(() => {});
    // Only force kill if graceful shutdown failed
    rstudioProcess.kill();
  }
}

