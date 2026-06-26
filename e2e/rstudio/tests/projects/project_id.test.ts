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

// Tracked by #18064 (re-enable on Server).
// Server-on-Linux: the second restartSessionWithSentinel (after clearing
// project_user_data_directory, line 125) times out waiting for
// window.rstudio.ready. Server logs show the session restarts cleanly (event
// stream up in ~2s, client_init in 1-3s), so this is the automation bridge's
// ready signal not re-arming across a Server restart -- not a slow or failed
// session, and distinct from the post-restart console-text issue that
// disables the other restart suites. Intermittent (the first restart in this
// test passes; Desktop passes). Skip on Server until restartSessionWithSentinel
// handles the Server restart lifecycle; the whole serial describe is tagged
// because its beforeAll/afterAll also restart.
//
// Regression: ProjectId should only be added to .Rproj when a project
// requires per-project user data (project_user_data_directory pref set),
// and once generated, must persist even after the pref is cleared.
test.describe.serial('ProjectId in .Rproj', { tag: ['@desktop_only'] }, () => {
  const sandbox = useSuiteSandbox();
  let projectDir = '';
  let rprojLit = '';

  test.beforeAll(async ({ rstudioPage: page }) => {
    // Make the precondition for the "fresh .Rproj has no ProjectId" check
    // deterministic. openProject() runs the IDE's ProjectContext::startup(),
    // which calls readProjectFile() with buildDefaults(); when our minimal
    // hand-written .Rproj is missing fields (everything beyond the four
    // lines we write below), `providedDefaults` becomes true and the IDE
    // re-writes the file with the full defaults filled in -- including a
    // freshly-generated `ProjectId: <uuid>` line whenever
    // getCustomUserDataDir() is non-empty.
    // That helper reads `project_user_data_directory` (user pref) first,
    // falling back to `session-project-user-data-dir` (admin option). A
    // leftover non-empty value from a prior interaction in the same worker
    // (or a non-default value baked into a hosted rstudio-server install)
    // therefore writes ProjectId into the freshly-opened .Rproj and breaks
    // the baseline. Setting the user pref empty here pins the pref side of
    // the lookup; the admin side defaults to empty in stock builds.
    await setPref(page, 'project_user_data_directory', '');
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
