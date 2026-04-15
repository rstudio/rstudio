// Regression test for https://github.com/rstudio/rstudio/issues/17353
// When the Environment pane inspects S4 objects whose defining package
// namespace isn't loaded, operations like length(), is(), and str() can
// trigger S4 method dispatch that loads a broken DLL, crashing the session.
// PR #17360 added .rs.isUnloadedS4() and early-exit guards in
// .rs.describeObject() / .rs.getObjectContents() to return a safe minimal
// description instead.
//
// Test plan (from the PR):
//   1. Create an S4 object from a package (DBI)
//   2. Save workspace to .RData
//   3. Remove the package
//   4. Restart R / RStudio
//   5. Session should not crash; the object should appear in the
//      Environment pane with a "not loaded" label

import { test as base, expect } from '@playwright/test';
import { launchRStudio, shutdownRStudio, type DesktopSession } from '@fixtures/desktop.fixture';
import { sleep } from '@utils/constants';
import { typeInConsole, clearConsole, CONSOLE_INPUT, CONSOLE_OUTPUT } from '@pages/console_pane.page';
import { YES_BTN } from '@pages/modals.page';
import type { Page } from 'playwright';

const OBJ_NAME = 's4_test_object';

/** Run an R expression and capture the text between unique markers. */
async function captureResult(page: Page, rExpr: string): Promise<string> {
  const marker = `__S4_${Date.now()}__`;
  await clearConsole(page);
  await typeInConsole(page, `cat("${marker}", ${rExpr}, "${marker}")`);
  await sleep(2000);

  const output = await page.locator(CONSOLE_OUTPUT).innerText();
  const match = output.match(new RegExp(`${marker}\\s+([\\s\\S]*?)\\s+${marker}`));
  expect(match, `marker not found in console output for: ${rExpr}`).toBeTruthy();
  return match![1].trim();
}

/**
 * Install DBI, create an S4 object, save workspace, then remove DBI.
 * Also creates plain objects as controls for the Environment pane.
 */
async function setupWorkspace(page: Page): Promise<void> {
  // Install DBI (binary, fast -- only dependency is methods which is always loaded)
  await typeInConsole(page, 'install.packages("DBI", repos = "https://cran.r-project.org")');
  await sleep(15000);

  // Create an S4 object from DBI
  await typeInConsole(page, 'library(DBI)');
  await sleep(1000);
  await typeInConsole(page, `${OBJ_NAME} <- DBI::SQL("SELECT 1")`);
  await sleep(500);

  // Plain objects as controls
  await typeInConsole(page, 'x <- 1');
  await sleep(500);
  await typeInConsole(page, 'y <- 22');
  await sleep(500);
  await typeInConsole(page, `z <- "I'll so offend, to make offense a skill"`);
  await sleep(500);

  // Enable workspace persistence
  await typeInConsole(page, '.rs.api.writeRStudioPreference("save_workspace", "always")');
  await sleep(500);
  await typeInConsole(page, '.rs.api.writeRStudioPreference("load_workspace", TRUE)');
  await sleep(500);

  // Save workspace
  await typeInConsole(page, 'save.image()');
  await sleep(1000);

  // Remove DBI so it won't be loadable after restart.
  // This may trigger a "loaded packages" dialog -- click Yes if it appears.
  await typeInConsole(page, 'remove.packages("DBI")');
  await sleep(1000);
  try {
    await page.locator(YES_BTN).click({ timeout: 3000 });
    console.log('Clicked Yes on loaded-packages dialog');
  } catch {
    // No dialog appeared
  }
  await sleep(3000);

  // Verify removal succeeded by checking the disk (not requireNamespace, which
  // returns TRUE for already-loaded namespaces even after the files are deleted)
  const dbiRemoved = await captureResult(page, 'file.exists(file.path(.libPaths()[1], "DBI"))');
  expect(dbiRemoved, 'DBI should be uninstalled after remove.packages').toBe('FALSE');
}

/** Remove test objects, delete .RData, restore preferences, reinstall DBI. */
async function cleanup(page: Page): Promise<void> {
  await typeInConsole(page, `suppressWarnings(rm("${OBJ_NAME}", "x", "y", "z", envir = .GlobalEnv))`);
  await sleep(500);
  await typeInConsole(page, 'unlink(".RData")');
  await sleep(500);
  await typeInConsole(page, '.rs.api.writeRStudioPreference("save_workspace", "never")');
  await sleep(500);
  // Reinstall DBI so we leave things as we found them
  await typeInConsole(page, 'install.packages("DBI", repos = "https://cran.r-project.org")');
  await sleep(15000);
}

/** Verify the session survived and the S4 object is handled safely. */
async function verifySessionSurvived(page: Page): Promise<void> {
  // Session is alive
  const aliveMarker = `__ALIVE_${Date.now()}__`;
  await clearConsole(page);
  await typeInConsole(page, `cat("${aliveMarker}")`);
  await sleep(2000);
  const output = await page.locator(CONSOLE_OUTPUT).innerText();
  expect(output).toContain(aliveMarker);

  // Control objects were loaded from .RData
  const xExists = await captureResult(page, 'exists("x", envir = .GlobalEnv)');
  expect(xExists).toBe('TRUE');
  const yExists = await captureResult(page, 'exists("y", envir = .GlobalEnv)');
  expect(yExists).toBe('TRUE');
  const zExists = await captureResult(page, 'exists("z", envir = .GlobalEnv)');
  expect(zExists).toBe('TRUE');

  // S4 object was loaded from .RData
  const exists = await captureResult(page, `exists("${OBJ_NAME}", envir = .GlobalEnv)`);
  expect(exists).toBe('TRUE');

  // DBI namespace should NOT be loaded (package was removed)
  const nsLoaded = await captureResult(page, 'isNamespaceLoaded("DBI")');
  expect(nsLoaded).toBe('FALSE');

  // Detected as an unloaded S4
  const isUnloaded = await captureResult(page, `.rs.isUnloadedS4(${OBJ_NAME})`);
  expect(isUnloaded).toBe('TRUE');
}

/** Check that the Environment pane renders the "not loaded" label. */
async function verifyEnvironmentPane(page: Page): Promise<void> {
  await typeInConsole(page, ".rs.api.executeCommand('activateEnvironment')");
  await sleep(1000);
  await typeInConsole(page, ".rs.api.executeCommand('refreshEnvironment')");
  await sleep(3000);

  const envPanel = page.locator('#rstudio_workbench_panel_environment');

  // Control objects should be visible (confirms the pane is populated)
  await expect(envPanel).toContainText('22', { timeout: 10000 });

  // S4 object should show the "not loaded" description
  await expect(envPanel).toContainText('not loaded', { timeout: 5000 });
}

// ==========================================================================
// Test 1: R session restart
// ==========================================================================
base.describe.serial('S4 unloaded package -- R session restart (#17353)', { tag: ['@serial'] }, () => {
  base.skip(process.env.RSTUDIO_EDITION === 'server', 'Uses Desktop launch -- Server not supported');

  let session: DesktopSession;
  let page: Page;

  base('session survives R restart with unloaded S4 in workspace', async () => {
    session = await launchRStudio();
    page = session.page;

    await setupWorkspace(page);

    // Restart R session
    await typeInConsole(page, ".rs.api.executeCommand('restartR')");
    await sleep(5000);
    await page.waitForSelector(CONSOLE_INPUT, { state: 'visible', timeout: 30000 });
    await sleep(2000);

    await verifySessionSurvived(page);
  });

  base('Environment pane shows "not loaded" label after R restart', async () => {
    await verifyEnvironmentPane(page);
  });

  base.afterAll(async () => {
    try { await cleanup(page); } catch (err) { console.log(`cleanup failed: ${err}`); }
    await shutdownRStudio(session);
  });
});

// ==========================================================================
// Test 2: Full RStudio Desktop restart
// ==========================================================================
base.describe.serial('S4 unloaded package -- RStudio restart (#17353)', { tag: ['@serial'] }, () => {
  base.skip(process.env.RSTUDIO_EDITION === 'server', 'Uses Desktop launch -- Server not supported');

  let session: DesktopSession;
  let page: Page;

  base('session survives RStudio restart with unloaded S4 in workspace', async () => {
    session = await launchRStudio();
    await setupWorkspace(session.page);

    console.log('Shutting down RStudio...');
    await shutdownRStudio(session);

    console.log('Relaunching RStudio...');
    session = await launchRStudio();
    page = session.page;
    await sleep(2000);

    await verifySessionSurvived(page);
  });

  base('Environment pane shows "not loaded" label after RStudio restart', async () => {
    await verifyEnvironmentPane(page);
  });

  base.afterAll(async () => {
    try { await cleanup(page); } catch (err) { console.log(`cleanup failed: ${err}`); }
    await shutdownRStudio(session);
  });
});
