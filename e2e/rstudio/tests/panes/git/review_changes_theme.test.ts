// Regression test for issue #9026: the Review Changes (git commit) window is a
// satellite, and satellites opt out of theming by default -- so the window
// always rendered light, however dark the editor theme was. VCSApplicationWindow
// now opts in (supportsThemes), which puts the global theme classes on the
// satellite's container and injects the editor theme CSS there; the changelist
// and diff panes carry ace_editor_theme and follow the editor theme, as they do
// in the Git pane in the main window.
import { test, expect } from '@fixtures/rstudio.fixture';
import type { Page } from '@playwright/test';
import { CONSOLE_OUTPUT, executeInConsole } from '@pages/console_pane.page';
import { executeCommand, getPref, openProject } from '@utils/commands';
import { closeProjectIfOpen } from '@utils/project';
import { useSuiteSandbox } from '@utils/sandbox';
import { rPathLiteral } from '@utils/r';
import {
  DARK_THEME,
  DARK_THEME_HREF,
  LIGHT_THEME,
  LIGHT_THEME_HREF,
  THEME_CONTAINER,
  expectThemeStylesheet,
  getThemeStylesheetHref,
  luminance,
} from '@utils/theme';

// The changelist, the diff pane, and (in the History view) the commit list and
// commit detail all follow the editor theme. None has a stable element id, but
// each carries this (external, so un-obfuscated) class precisely because it is
// editor-themed.
const EDITOR_THEMED_PANE = '.ace_editor_theme';

// The diff's own CSS lives in a bundle whose class names GWT obfuscates, so the
// chunk action pills are reached the same way that CSS reaches them: by their
// data-action attribute. All three actions are always rendered; the patch mode
// decides which are display:none, so this needs the visible one. The window
// opens on Unstaged, where Stage is shown.
const ACTION_PILL = "div[data-action='Stage']";

// Both the changelist and the diff render a cell table inside an editor-themed
// pane. The base stylesheet paints those tables white unconditionally, and this
// PR's `.ace_editor_theme .cellTableWidget { background-color: inherit }` rules
// are what let the pane behind them show through.
const THEMED_TABLE = `${EDITOR_THEMED_PANE} table`;

const PROJECT_NAME = 'GitReviewTheme';

/**
 * Luminance of each editor-themed pane the window is currently showing.
 *
 * The window hosts both the Changes and the History view and keeps the
 * inactive one in the DOM but hidden (VCSPopup uses LayoutPanel.setWidgetVisible,
 * i.e. display:none). getComputedStyle still reports a background color for a
 * display:none subtree -- the theme's, here -- so those panes have to be
 * filtered out by geometry rather than by color, or every probe below sees
 * twice the elements it expects.
 *
 * Returns [NaN] rather than [] when nothing matches, so that a caller reducing
 * with Math.min/Math.max cannot be handed the vacuously passing Infinity.
 */
async function paneLuminances(satellite: Page): Promise<number[]> {
  const colors = await visibleStyles(satellite, EDITOR_THEMED_PANE, 'backgroundColor');
  return colors.length === 0 ? [Number.NaN] : colors.map(luminance);
}

/**
 * executeInConsole only confirms that R reached a new prompt, which an R error
 * does just as readily as a success -- so a failed setup step would otherwise
 * sail through and resurface later as an unrelated-looking timeout. Scrape for
 * an error header the way createSandbox does, but only over the output this
 * command produced: the console buffer is shared with everything else that ran
 * in this worker, and matching against all of it would let one earlier error
 * fail every later call.
 */
async function executeInConsoleChecked(
  page: Page,
  command: string,
  opts: { timeout?: number } = {},
): Promise<void> {
  const before = await page.locator(CONSOLE_OUTPUT).innerText();
  await executeInConsole(page, command, { wait: true, timeout: opts.timeout });
  const after = await page.locator(CONSOLE_OUTPUT).innerText();

  // R formats errors as `Error in <call> :\n  <message>`, so take the indented
  // continuation lines along with the header.
  const emitted = after.startsWith(before) ? after.slice(before.length) : after;
  const errMatch = emitted.match(/(?:Error in|Error:)[^\n]*(?:\n[ \t]+[^\n]*)*/);
  if (errMatch) {
    throw new Error(
      `R error running "${command.slice(0, 120)}": ${errMatch[0].replace(/\n+/g, ' | ').trim()}`,
    );
  }
}

/** One computed style property, for each element matching `selector` on screen. */
async function visibleStyles(
  satellite: Page,
  selector: string,
  property: 'backgroundColor' | 'backgroundImage',
): Promise<string[]> {
  return satellite.locator(selector).evaluateAll(
    (els, prop) =>
      els
        .filter((el) => el.getClientRects().length > 0)
        .map((el) => getComputedStyle(el)[prop as 'backgroundColor']),
    property,
  );
}

test.describe.serial('Review Changes window theming (#9026)', () => {
  const sandbox = useSuiteSandbox();
  let satellitePage: Page | undefined;

  // Captured runner-side: an R global would not survive the session restart
  // openProject() performs below.
  let originalTheme: string | null = null;

  test.beforeAll(async ({ rstudioPage: page }) => {
    await closeProjectIfOpen(page);
    originalTheme = (await getPref(page, 'editor_theme')) as string | null;
  });

  test.afterAll(async ({ rstudioPage: page }) => {
    await satellitePage?.close().catch(() => undefined);

    // Each step is caught separately: they are independent, and sharing one try
    // meant a setPref timeout (waitForPrefEntry waits up to 10s for the bridge)
    // silently skipped the project close, leaking an open version-controlled
    // project into every later spec in this worker.

    // applyTheme, not setPref: nothing re-themes a live session off the
    // editor_theme pref (AceThemes binds UserState.theme(), and the backend
    // only reconciles the two at session start), so setPref alone would leave
    // the worker rendering Cobalt until something happened to restart R.
    await executeInConsole(
      page,
      `.rs.api.applyTheme(${JSON.stringify(originalTheme ?? LIGHT_THEME)})`,
      { wait: true },
    ).catch((err) => {
      console.warn(`[review_changes_theme] theme restore failed: ${(err as Error).message}`);
    });

    await closeProjectIfOpen(page).catch((err) => {
      console.warn(`[review_changes_theme] project close failed: ${(err as Error).message}`);
    });
  });

  test('opens themed when a dark editor theme is active', async ({ rstudioPage: page }) => {
    test.setTimeout(180_000);

    const projectDir = `${sandbox.dir}/${PROJECT_NAME}`.replace(/\\/g, '/');
    const rprojPath = `${projectDir}/${PROJECT_NAME}.Rproj`;
    const sourcePath = `${projectDir}/script.R`;

    // -- build a git repo with one committed file, then open it as a project.
    // The repo has to exist before the project opens: RStudio decides whether
    // the project is version controlled at session start. All file and git
    // work goes through the R console so this also works in Server mode, where
    // the rsession may not share a filesystem with the test runner. --
    await executeInConsoleChecked(
      page,
      `{ stopifnot(dir.create(${rPathLiteral(projectDir)}, recursive = TRUE)); ` +
        `writeLines("Version: 1.0", ${rPathLiteral(rprojPath)}); ` +
        `writeLines("x <- 1", ${rPathLiteral(sourcePath)}) }`,
    );

    // CI runners have no global git config, hence the inline -c identity
    const gitC = `"-C", shQuote(${rPathLiteral(projectDir)})`;
    await executeInConsoleChecked(
      page,
      `{ s <- c(system2("git", c(${gitC}, "init", "--quiet")), ` +
        `system2("git", c(${gitC}, "add", "-A")), ` +
        `system2("git", c(${gitC}, "-c", "user.name=rstudio-e2e", ` +
        `"-c", "user.email=rstudio-e2e@posit.co", ` +
        `"commit", "-m", "seed", "--quiet"))); ` +
        `if (any(s != 0)) stop("git seed failed (exit status: ", ` +
        `paste(s, collapse = "/"), ")") }`,
      { timeout: 60000 },
    );

    // leave an uncommitted change so the changelist and the diff are populated
    await executeInConsoleChecked(
      page,
      `cat("y <- 2\\n", file = ${rPathLiteral(sourcePath)}, append = TRUE)`,
    );

    await openProject(page, rprojPath);

    // -- apply a dark editor theme before opening the window, so this covers
    //    the window's initial paint rather than only a live re-theme --
    await executeInConsoleChecked(page, `.rs.api.applyTheme(${JSON.stringify(DARK_THEME)})`);
    await expectThemeStylesheet(page, DARK_THEME_HREF);

    const satellitePromise = page.context().waitForEvent('page', { timeout: 60000 });
    await executeCommand(page, 'vcsCommit');
    satellitePage = await satellitePromise;
    await satellitePage.waitForLoadState('domcontentloaded');
    expect(satellitePage.url()).toContain('view=review_changes');

    // the global theme classes reach the satellite's container...
    await expect(satellitePage.locator(`${THEME_CONTAINER}.rstudio-themes-dark`)).toBeAttached({
      timeout: 60000,
    });

    // ...the editor theme CSS is injected into the satellite document...
    await expect
      .poll(() => getThemeStylesheetHref(satellitePage!), { timeout: 30000 })
      .toContain(DARK_THEME_HREF);

    // ...and every editor-themed pane on screen actually paints dark
    await expect
      .poll(async () => (await paneLuminances(satellitePage!)).length, { timeout: 30000 })
      .toBeGreaterThanOrEqual(2);
    await expect
      .poll(async () => Math.max(...(await paneLuminances(satellitePage!))), { timeout: 30000 })
      .toBeLessThan(128);

    // The pane backgrounds above come from the editor theme's own stylesheet, so
    // they would still pass if every rule this PR adds were deleted.
    //
    // The table check below is only a partial guard against that: under a dark
    // theme RStudioCellTableStyle.css already carries an identical
    // `.editor_dark .cellTableWidget { background-color: inherit }`, so these
    // tables go transparent whether or not this PR's rules match. The light
    // half of this suite re-runs the same probe, where there is no such base
    // rule and only the new CSS can produce it. The action-pill assertions at
    // the end of this test are dark-specific with no base equivalent.
    await expect
      .poll(async () => visibleStyles(satellitePage!, THEMED_TABLE, 'backgroundColor'), {
        timeout: 30000,
      })
      .toEqual(['rgba(0, 0, 0, 0)', 'rgba(0, 0, 0, 0)']);

    // Chunk action pills are suppressed for untracked and unmerged files
    // (GitReviewPresenter gates setShowActions on the status being neither "??"
    // nor "UU"), and the changelist's default selection here is the untracked
    // .gitignore -- which sorts ahead of script.R, and which the test never
    // creates: opening a git-backed project makes the session write one
    // (SessionGit's augmentGitIgnore), i.e. after the `git add -A` above.
    await satellitePage.getByText('script.R', { exact: true }).click();
    const actionPill = satellitePage.locator(ACTION_PILL).filter({ visible: true }).first();
    await expect(actionPill).toBeVisible({ timeout: 30000 });

    // the base draws each pill as a three-slice sprite over no background of its
    // own; the dark rules flatten it to a translucent fill with no image. Check
    // every slice, not just the first -- the rule targets all of them, so one
    // that stopped matching would otherwise go unnoticed.
    const pill = await actionPill.evaluate((el) => ({
      background: getComputedStyle(el).backgroundColor,
      sliceImages: Array.from(el.children).map((c) => getComputedStyle(c).backgroundImage),
    }));
    expect(pill.background).toMatch(/^rgba\([\d\s,]+0?\.\d+\)$/);
    expect(pill.sliceImages.length).toBeGreaterThan(0);
    expect(pill.sliceImages).toEqual(pill.sliceImages.map(() => 'none'));
  });

  test('re-themes an open window when the editor theme changes', async ({ rstudioPage: page }) => {
    // Not test.skip(): describe.serial already skips this test if the one above
    // failed, so the only way to arrive here without a window is a bug in the
    // test itself -- which should fail rather than report a green skip.
    expect(satellitePage, 'Review Changes window was never opened').toBeDefined();

    await executeInConsoleChecked(page, `.rs.api.applyTheme(${JSON.stringify(LIGHT_THEME)})`);
    await expectThemeStylesheet(page, LIGHT_THEME_HREF);

    // the already-open window drops the dark class and repaints light. Assert
    // the container is still there as well, so this cannot pass by the window
    // having gone away.
    await expect(satellitePage!.locator(THEME_CONTAINER)).toBeAttached({ timeout: 30000 });
    await expect(
      satellitePage!.locator(`${THEME_CONTAINER}.rstudio-themes-dark`),
    ).toHaveCount(0, { timeout: 30000 });

    await expect
      .poll(async () => (await paneLuminances(satellitePage!)).length, { timeout: 30000 })
      .toBeGreaterThanOrEqual(2);
    await expect
      .poll(async () => Math.min(...(await paneLuminances(satellitePage!))), { timeout: 30000 })
      .toBeGreaterThan(128);

    // The pane colors above come from textmate.rstheme and would hold with every
    // rule in this PR dead. The tables are the light-theme check that cannot:
    // the base paints them white unconditionally and scopes its only override to
    // .editor_dark, so under a light theme nothing but this PR's
    // `.ace_editor_theme .cellTableWidget` rules can make them transparent.
    // That also covers the unconditional rules shipped alongside them (header
    // borders and padding, the dropped .whitebg and commitTableScrollPanel
    // backgrounds), which change light rendering too.

    // AceThemes swaps editor_dark for editor_light on the satellite's own body,
    // and that lags the pane repaint above by a frame or two. Wait for it: while
    // editor_dark is still on, RStudioCellTableStyle.css's
    // `.editor_dark .cellTableWidget` keeps the tables transparent by itself,
    // and the probe below would pass without exercising anything this PR adds.
    await expect
      .poll(() => satellitePage!.evaluate(() => document.body.className), { timeout: 30000 })
      .toContain('editor_light');

    await expect
      .poll(async () => visibleStyles(satellitePage!, THEMED_TABLE, 'backgroundColor'), {
        timeout: 30000,
      })
      .toEqual(['rgba(0, 0, 0, 0)', 'rgba(0, 0, 0, 0)']);
  });
});
