// Bulk file change handling in the Git pane (#18257, #18260): floods the file
// monitor by modifying thousands of tracked files at once, then asserts that
// the changelist populates, that the changes arrive as batched files_changed
// client events (rather than one file_changed event per file), and that
// changes landing after the burst -- including a .gitignore change, which
// takes the full-refresh path -- are still delivered.
//
// Timing (elapsed to populate) is logged for diagnostics but not asserted,
// since wall-clock bounds are not robust on shared CI runners.
import { test, expect } from '@fixtures/rstudio.fixture';
import { TIMEOUTS } from '@utils/constants';
import { useSuiteSandbox } from '@utils/sandbox';
import { executeInConsole } from '@pages/console_pane.page';
import { executeCommand, setPref } from '@utils/commands';
import { closeProjectIfOpen, waitForConsoleIdle } from '@utils/project';
import { rPathLiteral } from '@utils/r';

const PALETTE_LIST = '#rstudio_command_palette_list';
const WIZARD_DIALOG = '.gwt-DialogBox[aria-label="New Project Wizard"]';
const NEW_DIRECTORY_OPTION = '#rstudio_label_new_directory_wizard_page';
const NEW_PROJECT_OPTION = '#rstudio_label_new_project_wizard_page';
const PROJECT_NAME_INPUT = '#rstudio_new_project_directory_name';
const GIT_CHECKBOX = '#rstudio_new_project_git_repo input';
const CREATE_PROJECT_BTN = '#rstudio_label_create_project_wizard_confirm';
const PROJECT_MENU = '#rstudio_project_menubutton_toolbar';

const GIT_PANEL = '#rstudio_workbench_panel_git';
// GWT CellTable marks each data row with a __gwt_row attribute (headers are
// not marked); the staged "checkbox" is an <img>, not an <input>
const GIT_ROWS = `${GIT_PANEL} tr[__gwt_row]`;

const NUM_FILES = 3000;
const PROJECT_NAME = 'VcsFloodVerify';

test.describe('VCS file change flood (#18257)', () => {
  const sandbox = useSuiteSandbox();

  test.beforeAll(async ({ rstudioPage: page }) => {
    await closeProjectIfOpen(page);
    await setPref(page, 'default_project_location', sandbox.dir);
  });

  test.afterAll(async ({ rstudioPage: page }) => {
    try {
      await closeProjectIfOpen(page);
      await setPref(page, 'default_project_location', '');
    } catch (err) {
      console.warn('vcs flood afterAll cleanup failed:', err);
    }
  });

  test('git pane batches changes when thousands of tracked files change', async ({
    rstudioPage: page,
  }) => {
    test.setTimeout(420_000);

    const projectDir = `${sandbox.dir}/${PROJECT_NAME}`.replace(/\\/g, '/');

    // -- create a git-enabled project via the New Project wizard --
    await page.keyboard.press('ControlOrMeta+Shift+p');
    await expect(page.locator(PALETTE_LIST)).toBeVisible({ timeout: 5000 });
    await page.keyboard.type('Create a New Project');
    const paletteItem = page.locator(`${PALETTE_LIST} >> text=Create a New Project...`);
    await expect(paletteItem).toBeVisible({ timeout: 5000 });
    await paletteItem.click();
    await expect(page.locator(WIZARD_DIALOG)).toBeVisible({ timeout: 15000 });

    await page.locator(NEW_DIRECTORY_OPTION).click();
    await page.locator(NEW_PROJECT_OPTION).click();

    const nameInput = page.locator(PROJECT_NAME_INPUT);
    await nameInput.click();
    await nameInput.pressSequentially(PROJECT_NAME);
    await expect(nameInput).toHaveValue(PROJECT_NAME, { timeout: 2000 });
    await page.locator(GIT_CHECKBOX).check();
    await page.locator(CREATE_PROJECT_BTN).click();
    await expect(page.locator(WIZARD_DIALOG)).not.toBeVisible({ timeout: 15000 });
    await expect(page.locator(PROJECT_MENU)).toContainText(PROJECT_NAME, {
      timeout: TIMEOUTS.sessionRestart,
    });
    await waitForConsoleIdle(page);

    // -- seed NUM_FILES tracked files and commit them. All file and git
    //    operations in this test run through the R console: in Server mode
    //    the rsession may live on a different host (or run as a different
    //    user), so the Playwright process cannot write into the project
    //    directory itself. The commit carries inline -c identity because CI
    //    runners have no global git config. --
    const srcDir = `${projectDir}/src`;
    await executeInConsole(
      page,
      `{ dir.create(${rPathLiteral(srcDir)}, recursive = TRUE); ` +
        `for (i in 0:${NUM_FILES - 1}) ` +
        `writeLines(c(paste0("# file ", i), paste0("x <- ", i)), ` +
        `paste0(${rPathLiteral(srcDir)}, "/file_", i, ".R")) }`,
      { timeout: 120000 },
    );
    const gitC = `"-C", shQuote(${rPathLiteral(projectDir)})`;
    await executeInConsole(
      page,
      `{ s <- c(system2("git", c(${gitC}, "add", "-A")), ` +
        `system2("git", c(${gitC}, "-c", "user.name=rstudio-e2e", ` +
        `"-c", "user.email=rstudio-e2e@posit.co", ` +
        `"commit", "-m", "seed", "--quiet"))); ` +
        `if (any(s != 0)) stop("git seed commit failed (add/commit exit status: ", ` +
        `paste(s, collapse = "/"), ")") }`,
      { timeout: 120000 },
    );

    // -- show the git pane and force a refresh so the changelist is empty --
    await executeCommand(page, 'activateVcs');
    await expect(page.locator(GIT_PANEL)).toBeVisible({ timeout: 10000 });
    await executeCommand(page, 'vcsRefresh');
    await expect.poll(() => page.locator(GIT_ROWS).count(), { timeout: 60000 }).toBe(0);

    // -- capture get_events traffic: proves whether the backend delivers the
    //    changes batched (files_changed) or per-file (file_changed) --
    const eventBodies: string[] = [];
    const onResponse = (response: import('@playwright/test').Response) => {
      if (!response.url().includes('get_events'))
        return;
      response
        .text()
        .then((body) => eventBodies.push(body))
        .catch(() => {});
    };
    page.on('response', onResponse);

    try {
      // -- the flood: modify every tracked file at once --
      const t0 = Date.now();
      await executeInConsole(
        page,
        `for (i in 0:${NUM_FILES - 1}) cat("y <- ", i, "\\n", sep = "", ` +
          `file = paste0(${rPathLiteral(srcDir)}, "/file_", i, ".R"), append = TRUE)`,
        { timeout: 120000 },
      );

      // changelist should fill with all modified files
      await expect
        .poll(() => page.locator(GIT_ROWS).count(), { timeout: 180000 })
        .toBe(NUM_FILES);
      console.log(`[vcs-flood] ${NUM_FILES} rows in ${Date.now() - t0}ms`);

      // the burst must have arrived as batched files_changed events; the
      // response bodies are captured asynchronously, so poll for the first
      // batched event rather than sampling once
      await expect
        .poll(() => eventBodies.filter((b) => b.includes('"files_changed"')).length, {
          timeout: 10000,
        })
        .toBeGreaterThan(0);

      // a regression to per-file events would deliver the flood as thousands
      // of singular file_changed events; nothing in this test emits them
      // legitimately (note '"file_changed"' does not substring-match
      // '"files_changed"')
      const floodBodies = eventBodies.slice();
      const batchedCount = floodBodies.filter((b) => b.includes('"files_changed"')).length;
      const singularCount = floodBodies.filter((b) => b.includes('"file_changed"')).length;
      console.log(
        `[vcs-flood] event bodies: ${floodBodies.length}; ` +
          `with files_changed=${batchedCount} with file_changed=${singularCount}`,
      );
      expect(singularCount).toBe(0);

      // event-stream liveness: console output arrives via the same event
      // stream, so a completed console command proves the stream is healthy
      await executeInConsole(page, 'cat("stream-alive\\n")', { wait: true });

      // -- probe: a change landing after the burst is still delivered. This
      // exercises the normal post-burst delivery path; it cannot
      // deterministically force the fseventsd UserDropped overflow from
      // #18260, whose recovery lives in the monitor's rescan path. --
      // the filename is assembled with paste0 so the command echo (which
      // travels through the same get_events stream) cannot substring-match
      // the 'extra.R' diagnostic probe below
      await executeInConsole(
        page,
        `writeLines("z <- 1", paste0(${rPathLiteral(projectDir)}, "/extra", ".R"))`,
      );
      let sawExtraEvent = false;
      let sawExtraRow = false;
      const probeStart = Date.now();
      while (Date.now() - probeStart < 30000) {
        sawExtraEvent = eventBodies.some((b) => b.includes('extra.R'));
        sawExtraRow = (await page.locator(GIT_ROWS).count()) === NUM_FILES + 1;
        if (sawExtraRow)
          break;
        await new Promise((r) => setTimeout(r, 500));
      }
      console.log(
        `[vcs-flood] extra.R probe: eventDelivered=${sawExtraEvent} rowShown=${sawExtraRow}`,
      );

      // diagnostic when delivery failed: a manual refresh always picks the
      // file up, which distinguishes "monitor missed it" from "git pane broke"
      if (!sawExtraRow) {
        await executeCommand(page, 'vcsRefresh');
        await expect
          .poll(() => page.locator(GIT_ROWS).count(), { timeout: 30000 })
          .toBe(NUM_FILES + 1);
        console.log('[vcs-flood] extra.R appeared after manual vcsRefresh');
      }
      expect(sawExtraRow).toBe(true);

      // -- probe: .gitignore change takes the full-refresh path (filename
      //    split with paste0 for the same echo-vs-event reason as above) --
      await executeInConsole(
        page,
        `cat("# vcs flood probe\\n", ` +
          `file = paste0(${rPathLiteral(projectDir)}, "/.git", "ignore"), append = TRUE)`,
      );
      let sawGitignoreRow = false;
      const giStart = Date.now();
      while (Date.now() - giStart < 30000) {
        sawGitignoreRow = (await page.locator(GIT_ROWS).count()) === NUM_FILES + 2;
        if (sawGitignoreRow)
          break;
        await new Promise((r) => setTimeout(r, 500));
      }
      const sawGitignoreEvent = eventBodies.some((b) => b.includes('.gitignore'));
      console.log(
        `[vcs-flood] .gitignore probe: eventDelivered=${sawGitignoreEvent} rowShown=${sawGitignoreRow}`,
      );
      expect(sawGitignoreRow).toBe(true);
    } finally {
      page.off('response', onResponse);
    }
  });
});
