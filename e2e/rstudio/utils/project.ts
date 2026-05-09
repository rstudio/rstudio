import type { Page } from 'playwright';
import { typeInConsole, CONSOLE_INPUT, CONSOLE_OUTPUT } from '../pages/console_pane.page';
import { sleep } from './constants';

/**
 * Wait for the R session to settle after a project switch. The IDE reloads
 * and the console becomes briefly unavailable; this best-effort sequence
 * waits for the page to load, the console input to reappear, and R to
 * confirm idle by echoing a unique marker.
 *
 * Logs a warning and returns if R does not confirm idle within three attempts;
 * the caller decides how to proceed.
 */
export async function waitForSessionRestart(page: Page): Promise<void> {
  await page.waitForLoadState('load', { timeout: 30000 }).catch(() => {});
  await sleep(3000);
  await page.waitForSelector(CONSOLE_INPUT, { state: 'visible', timeout: 60000 });
  await sleep(2000);

  await page.waitForFunction(
    'typeof window.rstudioapi !== "undefined" || typeof window.$RStudio !== "undefined"',
    null,
    { timeout: 15000 }
  ).catch(() => {});
  await sleep(1000);

  for (let attempt = 0; attempt < 3; attempt++) {
    try {
      const marker = `__READY_${Date.now()}__`;
      await page.locator(CONSOLE_INPUT).click({ force: true });
      await page.keyboard.pressSequentially(`cat("${marker}")`);
      await sleep(200);
      await page.keyboard.press('Enter');
      await sleep(1500);
      const output = await page.locator(CONSOLE_OUTPUT).innerText();
      if (output.includes(marker)) return;
    } catch { /* console not ready yet */ }
    await sleep(2000);
  }
  console.warn('waitForSessionRestart: R session did not confirm idle after 3 attempts');
}

/**
 * Create a fresh project directory inside `parentDir`, write a minimal
 * `.Rproj` file, and open the project in RStudio. Triggers a session
 * restart and waits for it to complete. Returns the absolute project
 * directory path.
 *
 * Callers must reconstruct any page-action wrappers held over this call;
 * the session restart invalidates them.
 */
export async function createAndOpenProject(
  page: Page,
  parentDir: string,
  name: string,
): Promise<string> {
  const parentDirR = parentDir.replace(/\\/g, '/');
  const projectDir = `${parentDirR}/${name}`;

  await typeInConsole(page, `dir.create("${projectDir}")`);
  await sleep(500);
  await typeInConsole(
    page,
    `writeLines(c("Version: 1.0", "", "RestoreWorkspace: Default", "SaveWorkspace: Default"), "${projectDir}/${name}.Rproj")`
  );
  await sleep(500);

  await typeInConsole(page, `.rs.api.openProject("${projectDir}/${name}.Rproj")`);
  await waitForSessionRestart(page);

  return projectDir;
}
