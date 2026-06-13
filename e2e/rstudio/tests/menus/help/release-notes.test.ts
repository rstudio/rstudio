/**
 * Help > Release Notes (#17330)
 *
 * Server: triggers the command and catches the new browser tab to verify
 *         the URL contains the expected release notes link.
 * Desktop: the URL is opened via Electron's shell.openExternal(), which
 *          Playwright cannot intercept. We verify the command executes
 *          without error.
 */

import { expect } from '@playwright/test';
import { test } from '@fixtures/rstudio.fixture';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { executeCommand } from '@utils/commands';

const BASE_URL = 'https://www.rstudio.org/links/release_notes';

test.describe('Help > Release Notes - #17330', { tag: ['@parallel_safe'] }, () => {

  let consoleActions: ConsolePaneActions;

  test.beforeAll(async ({ rstudioPage: page }) => {
    consoleActions = new ConsolePaneActions(page);
  });

  test('command executes without error', { tag: ['@desktop_only'] }, async ({ rstudioPage: page }) => {
    // Desktop opens the URL via shell.openExternal() -- Playwright cannot
    // intercept it, so we just verify the command doesn't throw. The handler
    // is synchronous (Application.onShowReleaseNotes), so any error would
    // already be in console output by the time executeCommand returns.
    await consoleActions.clearConsole();
    await executeCommand(page, 'showReleaseNotes');

    const output = await page.locator('#rstudio_console_output').innerText();
    expect(output).not.toContain('Error');
  });

  test('opens correct URL', { tag: ['@server_only'] }, async ({ rstudioPage: page }) => {
    const context = page.context();
    const newPagePromise = context.waitForEvent('page', { timeout: 15000 });

    await executeCommand(page, 'showReleaseNotes');

    const newPage = await newPagePromise;
    const initialUrl = newPage.url();
    // Two valid paths to the release notes page:
    //   1) IDE opens the rstudio.org short link, which 302's to docs.posit.co
    //   2) IDE links straight to docs.posit.co (current PR-built rserver
    //      behaviour -- the short-link hop was removed)
    // Either is correct user-facing behaviour, so accept either.
    const FINAL_URL_HOST = 'docs.posit.co/ide/news/';
    expect(
      initialUrl.includes(BASE_URL) || initialUrl.includes(FINAL_URL_HOST),
      `expected initialUrl to be either BASE_URL or ${FINAL_URL_HOST}; got ${initialUrl}`,
    ).toBe(true);

    // Verify the page (after any redirect) lands on the Posit docs release notes page
    await newPage.waitForLoadState('load', { timeout: 15000 });
    const finalUrl = newPage.url();
    expect(finalUrl).toContain(FINAL_URL_HOST);

    // If the initial URL had a version fragment, verify it survived the redirect
    const fragment = new URL(initialUrl).hash;
    if (fragment) {
      expect(finalUrl).toContain(fragment);
    }

    await newPage.close();
  });
});
