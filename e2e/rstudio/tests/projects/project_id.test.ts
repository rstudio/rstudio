import { test, expect } from '@fixtures/rstudio.fixture';
import { executeInConsole, CONSOLE_OUTPUT } from '@pages/console_pane.page';
import { createAndOpenProject, restartSessionWithSentinel, waitForSessionRestart } from '@utils/project';
import { useSuiteSandbox } from '@utils/sandbox';
import { rPathLiteral, rStringLiteral } from '@utils/r';
import { setPref } from '@utils/commands';
import type { Page } from 'playwright';

const PROJECT_MENU = '#rstudio_project_menubutton_toolbar';
const CLOSE_PROJECT_MENU_ITEM = '#rstudio_label_close_project_command';

async function captureResult(page: Page, rExpression: string): Promise<string> {
  const marker = `__PI_${Date.now()}__`;
  // Gate on R reporting idle (busy class cleared) before reading the console.
  // Post-restart the busy class can toggle off momentarily before R has
  // actually drained the new command, so use expect.poll on the console
  // text rather than a single read -- the poll keeps re-reading until the
  // markers appear (typically within tens of ms once R is truly idle).
  await executeInConsole(
    page,
    `cat(${rStringLiteral(marker)}, ${rExpression}, ${rStringLiteral(marker)})`,
    { wait: true },
  );

  const pattern = new RegExp(`${marker}\\s+(.*?)\\s+${marker}`, 's');
  let match: RegExpMatchArray | null = null;
  await expect
    .poll(
      async () => {
        const output = await page.locator(CONSOLE_OUTPUT).innerText();
        match = output.match(pattern);
        return match !== null;
      },
      { timeout: 15000 },
    )
    .toBe(true);
  return match![1].trim();
}

async function closeProject(page: Page): Promise<void> {
  const menu = page.locator(PROJECT_MENU);
  if (!(await menu.isVisible({ timeout: 5000 }).catch(() => false))) return;
  await menu.click();
  const close = page.locator(CLOSE_PROJECT_MENU_ITEM);
  if (await close.isVisible({ timeout: 3000 }).catch(() => false)) {
    await close.click();
    await waitForSessionRestart(page);
  }
}

// Regression: ProjectId should only be added to .Rproj when a project
// requires per-project user data (project_user_data_directory pref set),
// and once generated, must persist even after the pref is cleared.
test.describe.serial('ProjectId in .Rproj', () => {
  const sandbox = useSuiteSandbox();
  let projectDir = '';
  let rprojLit = '';

  test.beforeAll(async ({ rstudioPage: page }) => {
    projectDir = await createAndOpenProject(page, sandbox.dir, 'ProjectId');
    rprojLit = rPathLiteral(`${projectDir}/ProjectId.Rproj`);
  });

  test.afterAll(async ({ rstudioPage: page }) => {
    try {
      await setPref(page, 'project_user_data_directory', '');
      await closeProject(page);
    } catch (err) {
      console.warn('project_id afterAll cleanup failed:', err);
    }
  });

  test('ProjectId generated only when user-data directory is set, then persists', async ({
    rstudioPage: page,
  }) => {
    const dataDirLit = rPathLiteral(`${projectDir}/Data`);

    // Fresh project: .Rproj should not contain a ProjectId line yet.
    let hasProjectId = await captureResult(
      page,
      `any(grepl("^ProjectId", readLines(${rprojLit})))`,
    );
    expect(hasProjectId, 'fresh .Rproj should not contain ProjectId').toBe('FALSE');

    // Set the project user-data directory pref, then restart.
    await executeInConsole(page, `dir.create(${dataDirLit})`, { wait: true });
    await executeInConsole(
      page,
      `.rs.api.writeRStudioPreference("project_user_data_directory", normalizePath(${dataDirLit}))`,
      { wait: true },
    );
    await restartSessionWithSentinel(page);

    // ProjectId should now be present in .Rproj.
    hasProjectId = await captureResult(
      page,
      `any(grepl("^ProjectId", readLines(${rprojLit})))`,
    );
    expect(hasProjectId, '.Rproj should contain ProjectId after user-data dir set').toBe('TRUE');

    const projectIdLine = await captureResult(
      page,
      `grep("^ProjectId", readLines(${rprojLit}), value = TRUE)`,
    );
    expect(projectIdLine.length, 'ProjectId line should be non-empty').toBeGreaterThan(0);

    // Clear the pref and restart again. The ProjectId must persist.
    await setPref(page, 'project_user_data_directory', '');
    await restartSessionWithSentinel(page);

    const projectIdLineAfter = await captureResult(
      page,
      `grep("^ProjectId", readLines(${rprojLit}), value = TRUE)`,
    );
    expect(projectIdLineAfter, 'ProjectId should be preserved across restart').toBe(projectIdLine);
  });
});
