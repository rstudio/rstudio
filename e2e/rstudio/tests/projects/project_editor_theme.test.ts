import { test, expect } from '@fixtures/rstudio.fixture';
import { executeInConsole, waitForConsoleIdle } from '@pages/console_pane.page';
import { getPref, setPref, clearPref, openProject } from '@utils/commands';
import { closeProjectIfOpen, restartSessionWithSentinel } from '@utils/project';
import { useSuiteSandbox } from '@utils/sandbox';
import { rPathLiteral } from '@utils/r';

// The 'Cobalt' theme ships with RStudio and is a dark theme. Its URL is
// "theme/default/cobalt.rstheme", which always appears in the stylesheet href
// when it is active.
const THEME_WITH_OVERRIDE = 'Cobalt';

// The default editor theme out of the box. Used to verify a project without
// EditorTheme: in .Rproj does NOT apply an override (global pref is kept).
const THEME_DEFAULT = 'Textmate (default)';

// ID on the <link> element AceThemes.java injects to apply the theme CSS.
const ACE_THEME_LINK = '#rstudio-acethemes-linkelement';

/**
 * Read the href of the Ace theme stylesheet link from the DOM.
 * Returns '' when the element is absent (no theme applied yet).
 */
async function getActiveThemeHref(
  page: import('@playwright/test').Page,
): Promise<string> {
  return page.evaluate(
    (id) => document.querySelector(id)?.getAttribute('href') ?? '',
    ACE_THEME_LINK,
  );
}

// -- Test suite ---------------------------------------------------------------

test.describe.serial('Project-level EditorTheme in .Rproj', () => {
  const sandbox = useSuiteSandbox();

  // Track original global editor_theme so afterAll can restore it.
  let originalTheme: string | null = null;

  test.beforeAll(async ({ rstudioPage: page }) => {
    // Capture the current global editor_theme before any test touches it.
    originalTheme = (await getPref(page, 'editor_theme')) as string | null;
  });

  test.afterAll(async ({ rstudioPage: page }) => {
    // Restore the original global editor_theme pref first.
    try {
      if (originalTheme !== null) {
        await setPref(page, 'editor_theme', originalTheme);
      } else {
        await clearPref(page, 'editor_theme');
      }
    } catch (err) {
      console.warn('[project_editor_theme] afterAll theme restore failed:', err);
    }

    // Then force a session rebuild so the restored pref is re-applied to the
    // live userState.theme -- setPref alone does not re-theme mid-session; only
    // syncThemePrefs (run on session init) does. The tests close their own
    // projects, so normally no project is open here: restart the session
    // explicitly. If a project is somehow still open, closing it rebuilds the
    // session instead (avoiding a redundant second rebuild). Without this, the
    // next suite in this worker could inherit a stale active theme.
    try {
      const projectActive = await page
        .evaluate(() => window.rstudio?.project?.isActive() === true)
        .catch(() => false);
      if (projectActive) {
        await closeProjectIfOpen(page);
      } else {
        await restartSessionWithSentinel(page);
      }
    } catch (err) {
      console.warn('[project_editor_theme] afterAll session rebuild failed:', err);
    }
  });

  // ---------------------------------------------------------------------------
  // Test 1: Opening a project with EditorTheme: Cobalt applies Cobalt.
  // ---------------------------------------------------------------------------
  test('project with EditorTheme: Cobalt applies Cobalt on open', async ({
    rstudioPage: page,
  }) => {
    const parentDir = sandbox.dir.replace(/\\/g, '/');

    // Pin the global editor theme to a non-Cobalt theme. This is the value a
    // no-override project falls back to, and the contrast that makes the
    // post-override "cobalt" assertion meaningful (the override changed
    // something rather than coincidentally matching an already-Cobalt global).
    //
    // Note: setPref alone does NOT change the live rendered theme mid-session.
    // The active theme <link> is driven by userState.theme(), which is only
    // mapped from the editor_theme pref by the backend syncThemePrefs(), and
    // that runs on session/project open -- not on a pref change. So the
    // baseline below must be established by a real project open, not by reading
    // the active theme right after setPref.
    await setPref(page, 'editor_theme', THEME_DEFAULT);

    // Baseline: open a NO-override project. Opening it re-runs syncThemePrefs(),
    // which applies the global (Textmate) to userState.theme(). This makes the
    // "before" state deterministic -- Textmate, NOT Cobalt -- regardless of
    // whatever userState.theme() happened to be before.
    const baselineName = 'theme_baseline_project';
    const baselineDir = `${parentDir}/${baselineName}`;
    const baselineRproj = `${baselineDir}/${baselineName}.Rproj`;
    await executeInConsole(
      page,
      `{ dir.create(${rPathLiteral(baselineDir)}); ` +
        `writeLines(c(` +
        `"Version: 1.0", "", ` +
        `"RestoreWorkspace: Default", ` +
        `"SaveWorkspace: Default"` +
        `), ${rPathLiteral(baselineRproj)}) }`,
    );
    await waitForConsoleIdle(page);
    await openProject(page, baselineRproj);

    await expect
      .poll(
        () => getActiveThemeHref(page),
        {
          message: `Expected baseline ace theme link href to contain "textmate" (global theme: ${THEME_DEFAULT})`,
          timeout: 15000,
          intervals: [200, 500, 1000],
        },
      )
      .toMatch(/textmate/i);
    expect(await getActiveThemeHref(page)).not.toMatch(/cobalt/i);

    // Now open the Cobalt-override project. Its .Rproj sets EditorTheme: Cobalt,
    // and the standard DCF fields surrounding it follow the format RStudio uses.
    const name = 'theme_override_project';
    const projectDir = `${parentDir}/${name}`;
    const rprojPath = `${projectDir}/${name}.Rproj`;
    const rprojLit = rPathLiteral(rprojPath);

    await executeInConsole(
      page,
      `{ dir.create(${rPathLiteral(projectDir)}); ` +
        `writeLines(c(` +
        `"Version: 1.0", "", ` +
        `"RestoreWorkspace: Default", ` +
        `"SaveWorkspace: Default", ` +
        `"AlwaysSaveHistory: Default", "", ` +
        `"EnableCodeIndexing: Yes", ` +
        `"UseSpacesForTab: Yes", ` +
        `"NumSpacesForTab: 2", ` +
        `"Encoding: UTF-8", "", ` +
        `"RnwWeave: Sweave", ` +
        `"LaTeX: pdfLaTeX", "", ` +
        `"EditorTheme: ${THEME_WITH_OVERRIDE}"` +
        `), ${rprojLit}) }`,
    );
    await waitForConsoleIdle(page);

    // Open the project. openProject resets window.rstudio.ready, fires
    // SwitchToProjectEvent, and polls until ready===true and the active
    // project path matches.
    await openProject(page, rprojPath);

    // The theme CSS link href must now include "cobalt" (case-insensitive):
    // syncThemePrefs() applied the project-level EditorTheme on session init,
    // overriding the Textmate global the baseline project resolved to.
    await expect
      .poll(
        () => getActiveThemeHref(page),
        {
          message: `Expected ace theme link href to contain "cobalt" (theme: ${THEME_WITH_OVERRIDE})`,
          timeout: 15000,
          intervals: [200, 500, 1000],
        },
      )
      .toMatch(/cobalt/i);

    // Clean up: close the project before the second test.
    await closeProjectIfOpen(page);

    // Remove the project directories via the R session (cross-platform, works
    // in both Desktop and Server mode where the test runner may not share a FS).
    await executeInConsole(
      page,
      `unlink(c(${rPathLiteral(projectDir)}, ${rPathLiteral(baselineDir)}), recursive = TRUE)`,
    );
    await waitForConsoleIdle(page);
  });

  // ---------------------------------------------------------------------------
  // Test 2: Opening a project WITHOUT EditorTheme uses the global pref.
  // ---------------------------------------------------------------------------
  test('project without EditorTheme keeps global theme', async ({
    rstudioPage: page,
  }) => {
    // Explicitly set the global theme to the default so the assertion is
    // deterministic regardless of what any earlier test may have left behind.
    await setPref(page, 'editor_theme', THEME_DEFAULT);

    const parentDir = sandbox.dir.replace(/\\/g, '/');
    const name = 'no_theme_project';
    const projectDir = `${parentDir}/${name}`;
    const rprojPath = `${projectDir}/${name}.Rproj`;
    const rprojLit = rPathLiteral(rprojPath);

    // Write a minimal .Rproj with no EditorTheme line.
    await executeInConsole(
      page,
      `{ dir.create(${rPathLiteral(projectDir)}); ` +
        `writeLines(c(` +
        `"Version: 1.0", "", ` +
        `"RestoreWorkspace: Default", ` +
        `"SaveWorkspace: Default"` +
        `), ${rprojLit}) }`,
    );
    await waitForConsoleIdle(page);

    await openProject(page, rprojPath);

    // With no project-level override the global pref ("Textmate (default)")
    // remains active. The href for Textmate contains "textmate".
    await expect
      .poll(
        () => getActiveThemeHref(page),
        {
          message: `Expected ace theme link href to contain "textmate" (global theme: ${THEME_DEFAULT})`,
          timeout: 15000,
          intervals: [200, 500, 1000],
        },
      )
      .toMatch(/textmate/i);

    // Clean up.
    await closeProjectIfOpen(page);
    await executeInConsole(
      page,
      `unlink(${rPathLiteral(projectDir)}, recursive = TRUE)`,
    );
    await waitForConsoleIdle(page);
  });
});
