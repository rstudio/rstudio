// Regression test for issue #9026: the Review Changes (git commit) window is a
// satellite, and satellites opt out of theming by default -- so the window
// always rendered light, however dark the editor theme was. VCSApplicationWindow
// now opts in (supportsThemes), which puts the global theme classes on the
// satellite's container and injects the editor theme CSS there; the changelist
// and diff panes carry ace_editor_theme and follow the editor theme, as they do
// in the Git pane in the main window.
import { test, expect } from '@fixtures/rstudio.fixture';
import type { Page } from '@playwright/test';
import { executeInConsole } from '@pages/console_pane.page';
import { executeCommand, getPref, openProject, setPref } from '@utils/commands';
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
// pane; the base stylesheet paints those tables an opaque white.
const THEMED_TABLE = `${EDITOR_THEMED_PANE} table`;

const PROJECT_NAME = 'GitReviewTheme';

/**
 * Luminance of each editor-themed pane the window is currently showing.
 *
 * The window hosts both the Changes and the History view and keeps the
 * inactive one in the DOM but not rendered; a non-rendered element reports no
 * background color at all, so restrict this to the panes actually on screen.
 *
 * Returns [NaN] rather than [] when nothing matches, so that a caller reducing
 * with Math.min/Math.max cannot be handed the vacuously passing Infinity.
 */
async function paneLuminances(satellite: Page): Promise<number[]> {
  const colors = await visibleStyles(satellite, EDITOR_THEMED_PANE, 'backgroundColor');
  return colors.length === 0 ? [Number.NaN] : colors.map(luminance);
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

    try {
      await setPref(page, 'editor_theme', originalTheme ?? LIGHT_THEME);
      await closeProjectIfOpen(page);
    } catch (err) {
      console.warn(`[review_changes_theme] cleanup failed: ${(err as Error).message}`);
    }
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
    await executeInConsole(
      page,
      `{ dir.create(${rPathLiteral(projectDir)}, recursive = TRUE); ` +
        `writeLines("Version: 1.0", ${rPathLiteral(rprojPath)}); ` +
        `writeLines("x <- 1", ${rPathLiteral(sourcePath)}) }`,
      { wait: true },
    );

    // CI runners have no global git config, hence the inline -c identity
    const gitC = `"-C", shQuote(${rPathLiteral(projectDir)})`;
    await executeInConsole(
      page,
      `{ s <- c(system2("git", c(${gitC}, "init", "--quiet")), ` +
        `system2("git", c(${gitC}, "add", "-A")), ` +
        `system2("git", c(${gitC}, "-c", "user.name=rstudio-e2e", ` +
        `"-c", "user.email=rstudio-e2e@posit.co", ` +
        `"commit", "-m", "seed", "--quiet"))); ` +
        `if (any(s != 0)) stop("git seed failed (exit status: ", ` +
        `paste(s, collapse = "/"), ")") }`,
      { wait: true, timeout: 60000 },
    );

    // leave an uncommitted change so the changelist and the diff are populated
    await executeInConsole(page, `cat("y <- 2\\n", file = ${rPathLiteral(sourcePath)}, append = TRUE)`, {
      wait: true,
    });

    await openProject(page, rprojPath);

    // -- apply a dark editor theme before opening the window, so this covers
    //    the window's initial paint rather than only a live re-theme --
    await executeInConsole(page, `.rs.api.applyTheme(${JSON.stringify(DARK_THEME)})`, {
      wait: true,
    });
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
    // they would still pass if every rule this PR adds stopped matching (an
    // @external declaration dropped from one of these CssResources is enough --
    // GWT would obfuscate the class and the rule would silently go dead). The
    // rest of this test asserts things only the new CSS produces.

    // The tables inside those panes are painted an opaque white by the base
    // stylesheet, and have to go transparent for the theme behind them to show.
    await expect
      .poll(async () => visibleStyles(satellitePage!, THEMED_TABLE, 'backgroundColor'), {
        timeout: 30000,
      })
      .toEqual(['rgba(0, 0, 0, 0)', 'rgba(0, 0, 0, 0)']);

    // Chunk action pills are drawn for a modified file's diff, but not for the
    // whole-file diff of an untracked one -- which is what the changelist selects
    // by default here, .gitignore sorting ahead of the R file.
    await satellitePage.getByText('script.R', { exact: true }).click();
    const actionPill = satellitePage.locator(ACTION_PILL).filter({ visible: true }).first();
    await expect(actionPill).toBeVisible({ timeout: 30000 });

    // the base draws each pill as a three-slice sprite over no background of its
    // own; the dark rules flatten it to a translucent fill with no image
    const pill = await actionPill.evaluate((el) => ({
      background: getComputedStyle(el).backgroundColor,
      sliceImage: getComputedStyle(el.firstElementChild!).backgroundImage,
    }));
    expect(pill.background).toMatch(/^rgba\([\d\s,]+0?\.\d+\)$/);
    expect(pill.sliceImage).toBe('none');
  });

  test('re-themes an open window when the editor theme changes', async ({ rstudioPage: page }) => {
    test.skip(satellitePage === undefined, 'Review Changes window not opened');

    await executeInConsole(page, `.rs.api.applyTheme(${JSON.stringify(LIGHT_THEME)})`, {
      wait: true,
    });
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
  });
});
