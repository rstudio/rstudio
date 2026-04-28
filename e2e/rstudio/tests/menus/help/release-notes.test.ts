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
import { sleep } from '@utils/constants';

const BASE_URL = 'https://www.rstudio.org/links/release_notes';
const isDesktop = (process.env.PW_RSTUDIO_MODE || 'desktop').toLowerCase() === 'desktop';

test.describe('Help > Release Notes - #17330', { tag: ['@parallel_safe'] }, () => {

  let consoleActions: ConsolePaneActions;

  test.beforeAll(async ({ rstudioPage: page }) => {
    consoleActions = new ConsolePaneActions(page);
  });

  if (isDesktop) {
    test('command executes without error', async ({ rstudioPage: page }) => {
      // Desktop opens the URL via shell.openExternal() — Playwright cannot
      // intercept it, so we just verify the command doesn't throw.
      await consoleActions.clearConsole();
      await consoleActions.typeInConsole(".rs.api.executeCommand('showReleaseNotes')");
      await sleep(2000);

      // Verify no error appeared in the console
      const output = await page.locator('#rstudio_console_output').innerText();
      expect(output).not.toContain('Error');
    });
  } else {
    test('opens correct URL', async ({ rstudioPage: page }) => {
      const context = page.context();
      const newPagePromise = context.waitForEvent('page', { timeout: 15000 });

      await consoleActions.typeInConsole(".rs.api.executeCommand('showReleaseNotes')");

      const newPage = await newPagePromise;
      const initialUrl = newPage.url();
      expect(initialUrl).toContain(BASE_URL);

      // Verify the redirect lands on the Posit docs release notes page
      await newPage.waitForLoadState('load', { timeout: 15000 });
      const finalUrl = newPage.url();
      expect(finalUrl).toContain('docs.posit.co/ide/news/');

      // If the initial URL had a version fragment, verify it survived the redirect
      const fragment = new URL(initialUrl).hash;
      if (fragment) {
        expect(finalUrl).toContain(fragment);
      }

      await newPage.close();
    });
  }
});
