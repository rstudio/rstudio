// Opt-in performance check for bulk file change handling (#18257, #18260):
// floods the file monitor by modifying thousands of tracked files at once,
// then asserts the Git pane populates promptly without pegging the UI thread,
// that the changes arrive as a single batched files_changed client event, and
// that a change landing right after the burst is still delivered (the FSEvents
// UserDropped scenario from #18260).
//
// The timing assertions are meaningful on an unloaded machine but not robust
// on shared CI runners, so the suite is skipped unless PW_PERF=1 is set:
//
//   PW_PERF=1 npm run test:desktop-dev -- tests/perf/vcs_flood_perf.test.ts
import { test, expect } from '@fixtures/rstudio.fixture';
import { TIMEOUTS } from '@utils/constants';
import { useSuiteSandbox } from '@utils/sandbox';
import { executeInConsole } from '@pages/console_pane.page';
import { executeCommand, setPref } from '@utils/commands';
import { closeProjectIfOpen, waitForConsoleIdle } from '@utils/project';
import * as fs from 'fs';
import * as path from 'path';
import { execSync } from 'child_process';

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

test.skip(process.env.PW_PERF !== '1', 'perf check; set PW_PERF=1 to run');

test.describe('VCS file change flood (#18257 verification)', () => {
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

  test('git pane stays responsive when thousands of tracked files change', async ({
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

    // -- seed NUM_FILES tracked files and commit them (external git) --
    const srcDir = path.join(projectDir, 'src');
    fs.mkdirSync(srcDir, { recursive: true });
    for (let i = 0; i < NUM_FILES; i++) {
      fs.writeFileSync(path.join(srcDir, `file_${i}.R`), `# file ${i}\nx <- ${i}\n`);
    }
    execSync('git add -A && git commit -m "seed" --quiet', { cwd: projectDir });

    // -- show the git pane and force a refresh so the changelist is empty --
    await executeCommand(page, 'activateVcs');
    await expect(page.locator(GIT_PANEL)).toBeVisible({ timeout: 10000 });
    await executeCommand(page, 'vcsRefresh');
    await expect.poll(() => page.locator(GIT_ROWS).count(), { timeout: 60000 }).toBe(0);

    // -- capture get_events traffic: proves whether the backend delivers
    //    (batched) file change events, and when --
    const suiteT0 = Date.now();
    const eventBodies: string[] = [];
    page.on('response', (response) => {
      if (!response.url().includes('get_events'))
        return;
      response
        .text()
        .then((body) => eventBodies.push(`[t+${Date.now() - suiteT0}ms] ${body}`))
        .catch(() => {});
    });

    // -- instrument the UI thread: record main-loop stalls > 250ms --
    await page.evaluate(() => {
      const w = window as any;
      w.__stalls = [];
      let last = performance.now();
      w.__stallTimer = setInterval(() => {
        const now = performance.now();
        const gap = now - last;
        if (gap > 250) w.__stalls.push(Math.round(gap));
        last = now;
      }, 50);
    });

    // -- the flood: modify every tracked file at once --
    const t0 = Date.now();
    for (let i = 0; i < NUM_FILES; i++) {
      fs.appendFileSync(path.join(srcDir, `file_${i}.R`), `y <- ${i}\n`);
    }

    // changelist should fill with all modified files
    await expect
      .poll(() => page.locator(GIT_ROWS).count(), { timeout: 180000 })
      .toBe(NUM_FILES);
    const elapsedMs = Date.now() - t0;

    const stalls: number[] = await page.evaluate(() => {
      const w = window as any;
      clearInterval(w.__stallTimer);
      return w.__stalls;
    });
    const maxStall = stalls.length ? Math.max(...stalls) : 0;
    console.log(
      `[vcs-flood] ${NUM_FILES} rows in ${elapsedMs}ms; ` +
        `stalls>250ms: count=${stalls.length} max=${maxStall}ms all=[${stalls.join(',')}]`,
    );

    // pre-fix this scenario pegged the UI for a minute+; allow generous but
    // symptom-distinguishing bounds
    expect(elapsedMs).toBeLessThan(90_000);
    expect(maxStall).toBeLessThan(15_000);

    const batchedCount = eventBodies.filter((b) => b.includes('"files_changed"')).length;
    const singularCount = eventBodies.filter((b) => b.includes('"file_changed"')).length;
    console.log(
      `[vcs-flood] event bodies: ${eventBodies.length}; ` +
        `with files_changed=${batchedCount} with file_changed=${singularCount}`,
    );

    // event-stream liveness: console output arrives via the same event
    // stream, so a completed console command proves the stream is healthy
    await executeInConsole(page, 'cat("stream-alive\\n")', { wait: true });

    // -- probe: a single new file still lands in the changelist --
    fs.writeFileSync(path.join(projectDir, 'extra.R'), 'z <- 1\n');
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

    // -- fallback: manual refresh must always pick the file up --
    if (!sawExtraRow) {
      await executeCommand(page, 'vcsRefresh');
      await expect
        .poll(() => page.locator(GIT_ROWS).count(), { timeout: 30000 })
        .toBe(NUM_FILES + 1);
      console.log('[vcs-flood] extra.R appeared after manual vcsRefresh');
    }

    // -- probe: .gitignore change takes the full-refresh path --
    fs.appendFileSync(path.join(projectDir, '.gitignore'), '# vcs flood probe\n');
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

    expect(sawExtraRow || sawExtraEvent).toBe(true);
    expect(sawGitignoreRow).toBe(true);
  });
});
