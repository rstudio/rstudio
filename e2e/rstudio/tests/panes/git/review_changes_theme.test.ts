// Regression test for issue #9026: the Review Changes (git commit) window is a
// satellite, and satellites opt out of theming by default -- so the window
// always rendered light, however dark the editor theme was. VCSApplicationWindow
// now opts in (supportsThemes), which puts the flat-theme classes on the
// satellite's container and injects the editor theme CSS there; the changelist
// and diff panes carry ace_editor_theme and follow the editor theme, as they do
// in the Git pane in the main window.
import { test, expect } from '@fixtures/rstudio.fixture';
import type { Page } from '@playwright/test';
import { executeInConsole } from '@pages/console_pane.page';
import { executeCommand, openProject } from '@utils/commands';
import { closeProjectIfOpen } from '@utils/project';
import { useSuiteSandbox } from '@utils/sandbox';
import { rPathLiteral } from '@utils/r';

// 'Cobalt' ships with RStudio and is dark; its stylesheet href always contains
// "cobalt" when active. 'Textmate (default)' is the light default.
const DARK_THEME = 'Cobalt';
const LIGHT_THEME = 'Textmate (default)';

// id on the <link> element AceThemes.java injects to apply the theme CSS; the
// satellite gets its own copy once the window opts in to theming
const ACE_THEME_LINK = '#rstudio-acethemes-linkelement';

// RStudioThemes.initializeThemes() assigns this id to the themed container and
// adds rstudio-themes-dark to it for dark themes
const THEME_CONTAINER = '#rstudio_container';

// The changelist and the diff pane are the two panes that follow the editor
// theme. Neither has a stable element id, but both carry this (external, so
// un-obfuscated) class precisely because they are editor-themed.
const EDITOR_THEMED_PANE = '.ace_editor_theme';

const PROJECT_NAME = 'GitReviewTheme';

/** Relative luminance (0 = black, 255 = white) of a CSS rgb()/rgba() color. */
function luminance(cssColor: string): number {
  const match = cssColor.match(/rgba?\((\d+),\s*(\d+),\s*(\d+)(?:,\s*([\d.]+))?\)/);
  if (!match)
    throw new Error(`unexpected color format: ${cssColor}`);

  // a fully transparent background means nothing was actually painted; return
  // NaN so both the light and the dark comparison fail rather than letting
  // rgba(0, 0, 0, 0) read as black
  if (match[4] !== undefined && Number(match[4]) === 0)
    return Number.NaN;

  const [r, g, b] = [Number(match[1]), Number(match[2]), Number(match[3])];
  return 0.2126 * r + 0.7152 * g + 0.0722 * b;
}

/**
 * Luminance of each editor-themed pane the window is currently showing.
 *
 * The window hosts both the Changes and the History view and keeps the
 * inactive one in the DOM but not rendered; a non-rendered element reports no
 * background color at all, so restrict this to the panes actually on screen.
 */
async function paneLuminances(satellite: Page): Promise<number[]> {
  const colors = await satellite.locator(EDITOR_THEMED_PANE).evaluateAll((els) =>
    els
      .filter((el) => el.getClientRects().length > 0)
      .map((el) => getComputedStyle(el).backgroundColor),
  );
  return colors.map(luminance);
}

test.describe.serial('Review Changes window theming (#9026)', () => {
  const sandbox = useSuiteSandbox();
  let satellitePage: Page | undefined;

  test.beforeAll(async ({ rstudioPage: page }) => {
    await closeProjectIfOpen(page);
    await executeInConsole(page, '.rstudio.e2e.vcsThemeOrig <- .rs.api.getThemeInfo()$editor', {
      wait: true,
    });
  });

  test.afterAll(async ({ rstudioPage: page }) => {
    await satellitePage?.close().catch(() => undefined);

    try {
      await executeInConsole(
        page,
        `if (exists(".rstudio.e2e.vcsThemeOrig")) .rs.api.applyTheme(.rstudio.e2e.vcsThemeOrig) else .rs.api.applyTheme(${JSON.stringify(LIGHT_THEME)})`,
        { wait: true },
      );
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
    await expect
      .poll(async () =>
        page.evaluate((id) => document.querySelector(id)?.getAttribute('href') ?? '', ACE_THEME_LINK),
      )
      .toContain('cobalt');

    const satellitePromise = page.context().waitForEvent('page', { timeout: 60000 });
    await executeCommand(page, 'vcsCommit');
    satellitePage = await satellitePromise;
    await satellitePage.waitForLoadState('domcontentloaded');
    expect(satellitePage.url()).toContain('view=review_changes');

    // the flat theme classes reach the satellite's container...
    await expect(satellitePage.locator(`${THEME_CONTAINER}.rstudio-themes-dark`)).toBeAttached({
      timeout: 60000,
    });

    // ...the editor theme CSS is injected into the satellite document...
    await expect
      .poll(
        () =>
          satellitePage!.evaluate(
            (id) => document.querySelector(id)?.getAttribute('href') ?? '',
            ACE_THEME_LINK,
          ),
        { timeout: 30000 },
      )
      .toContain('cobalt');

    // ...and both editor-themed panes (changelist, diff) actually paint dark
    await expect
      .poll(async () => (await paneLuminances(satellitePage!)).length, { timeout: 30000 })
      .toBeGreaterThanOrEqual(2);
    await expect
      .poll(async () => Math.max(...(await paneLuminances(satellitePage!))), { timeout: 30000 })
      .toBeLessThan(128);
  });

  test('re-themes an open window when the editor theme changes', async ({ rstudioPage: page }) => {
    test.skip(satellitePage === undefined, 'Review Changes window not opened');

    await executeInConsole(page, `.rs.api.applyTheme(${JSON.stringify(LIGHT_THEME)})`, {
      wait: true,
    });
    await expect
      .poll(async () =>
        page.evaluate((id) => document.querySelector(id)?.getAttribute('href') ?? '', ACE_THEME_LINK),
      )
      .toContain('textmate');

    // the already-open window drops the dark class and repaints light
    await expect(satellitePage!.locator(`${THEME_CONTAINER}.rstudio-themes-dark`)).toHaveCount(0, {
      timeout: 30000,
    });
    await expect
      .poll(async () => Math.min(...(await paneLuminances(satellitePage!))), { timeout: 30000 })
      .toBeGreaterThan(128);
  });
});
