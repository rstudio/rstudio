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
import {
  executeInConsole,
  clearConsole,
  ensurePackageInstalled,
  waitForConsoleIdle,
  CONSOLE_INPUT,
  CONSOLE_OUTPUT,
} from '@pages/console_pane.page';
import { YES_BTN } from '@pages/modals.page';
import { executeCommand, setPref } from '@utils/commands';
import type { Page } from 'playwright';

const OBJ_NAME = 's4_test_object';

/** Run an R expression and capture the text between unique markers. */
async function captureResult(page: Page, rExpr: string): Promise<string> {
  const marker = `__S4_${Date.now()}__`;
  await clearConsole(page);
  await executeInConsole(page, `cat("${marker}", ${rExpr}, "${marker}")`, { wait: true });

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
  // Install DBI if not already present (binary, fast -- only dep is methods,
  // which is always loaded). ensurePackageInstalled skips the download when
  // DBI was left behind by a prior run.
  await ensurePackageInstalled(page, 'DBI');

  // Create an S4 object from DBI
  await executeInConsole(page, 'library(DBI)', { wait: true });
  await executeInConsole(page, `${OBJ_NAME} <- DBI::SQL("SELECT 1")`, { wait: true });

  // Plain objects as controls
  await executeInConsole(page, 'x <- 1', { wait: true });
  await executeInConsole(page, 'y <- 22', { wait: true });
  await executeInConsole(page, `z <- "I'll so offend, to make offense a skill"`, { wait: true });

  // Enable workspace persistence (setPref is a JS-side bridge call -- no R round-trip)
  await setPref(page, 'save_workspace', 'always');
  await setPref(page, 'load_workspace', true);

  // Save workspace
  await executeInConsole(page, 'save.image()', { wait: true });

  // Remove DBI so it won't be loadable after restart. This may trigger a
  // "loaded packages" dialog -- click Yes if it appears. Can't pass
  // { wait: true } here: if the dialog blocks R, the busy class would never
  // clear. Fire-and-forget, click the dialog if present, then wait for idle.
  await executeInConsole(page, 'remove.packages("DBI")');
  try {
    await page.locator(YES_BTN).click({ timeout: 3000 });
    console.log('Clicked Yes on loaded-packages dialog');
  } catch {
    // No dialog appeared
  }
  await waitForConsoleIdle(page);

  // Verify removal succeeded by checking the disk (not requireNamespace, which
  // returns TRUE for already-loaded namespaces even after the files are deleted)
  const dbiRemoved = await captureResult(page, 'file.exists(file.path(.libPaths()[1], "DBI"))');
  expect(dbiRemoved, 'DBI should be uninstalled after remove.packages').toBe('FALSE');
}

/** Remove test objects, delete .RData, restore preferences, reinstall DBI. */
async function cleanup(page: Page): Promise<void> {
  await executeInConsole(
    page,
    `suppressWarnings(rm("${OBJ_NAME}", "x", "y", "z", envir = .GlobalEnv))`,
    { wait: true },
  );
  await executeInConsole(page, 'unlink(".RData")', { wait: true });
  await setPref(page, 'save_workspace', 'never');
  // Reinstall DBI so we leave things as we found them. The test just removed
  // it, so ensurePackageInstalled will see the gap and do a real install.
  await ensurePackageInstalled(page, 'DBI');
}

/** Verify the session survived and the S4 object is handled safely. */
async function verifySessionSurvived(page: Page): Promise<void> {
  // Session is alive
  const aliveMarker = `__ALIVE_${Date.now()}__`;
  await clearConsole(page);
  await executeInConsole(page, `cat("${aliveMarker}")`, { wait: true });
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
  await executeCommand(page, 'activateEnvironment');
  await executeCommand(page, 'refreshEnvironment');

  const envPanel = page.locator('#rstudio_workbench_panel_environment');

  // Control objects should be visible (confirms the pane is populated).
  // The toContainText polls so we don't need to sleep after refreshEnvironment.
  await expect(envPanel).toContainText('22', { timeout: 10000 });

  // S4 object should show the "not loaded" description
  await expect(envPanel).toContainText('not loaded', { timeout: 5000 });
}

// ==========================================================================
// Test 1: R session restart
// ==========================================================================
base.describe.serial('S4 unloaded package -- R session restart (#17353)', { tag: ['@serial', '@desktop_only'] }, () => {
  let session: DesktopSession;
  let page: Page;

  base('session survives R restart with unloaded S4 in workspace', async () => {
    session = await launchRStudio();
    page = session.page;

    await setupWorkspace(page);

    // Restart R session. The 5s lead-in is a guard against restartR being
    // dispatched before the prior commands have fully drained -- waitForConsoleIdle
    // can otherwise see a momentarily-idle busy class. Replace later once the
    // restartR command has a confirmable post-condition.
    await executeCommand(page, 'restartR');
    await sleep(5000);
    await page.waitForSelector(CONSOLE_INPUT, { state: 'visible', timeout: 30000 });
    await waitForConsoleIdle(page);

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
base.describe.serial('S4 unloaded package -- RStudio restart (#17353)', { tag: ['@serial', '@desktop_only'] }, () => {
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
    await waitForConsoleIdle(page);

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
