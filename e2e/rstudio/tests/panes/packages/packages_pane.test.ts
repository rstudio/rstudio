// Packages pane behaviors ported from
// src/cpp/tests/automation/testthat/test-automation-packages-pane.R.
//
// Regression coverage for https://github.com/rstudio/rstudio/issues/16842
// (packages pane checkbox state diverges from search() after attach/detach).

import { test, expect } from '@fixtures/rstudio.fixture';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { TIMEOUTS } from '@utils/constants';
import { executeCommand } from '@utils/commands';
import { rStringLiteral } from '@utils/r';
import type { Page } from 'playwright';

const PACKAGES_TAB = '#rstudio_workbench_tab_packages';
const PACKAGES_PANEL = '#rstudio_workbench_panel_packages';
const FILES_TAB = '#rstudio_workbench_tab_files';

function checkboxLocator(page: Page, packageName: string) {
  // A package can appear in more than one library section (e.g. recommended
  // packages live in the system lib AND a copy may be present in the user
  // lib). `.first()` keeps the locator deterministic regardless -- the global
  // attached-state is what we're verifying, and any one checkbox reflects it.
  return page
    .locator(`${PACKAGES_PANEL} input[type='checkbox'][aria-label='${packageName}']`)
    .first();
}

async function selectPackagesTab(page: Page): Promise<void> {
  await page.locator(PACKAGES_TAB).click();
  await expect(page.locator(PACKAGES_PANEL)).toBeVisible({ timeout: TIMEOUTS.consoleReady });
}

async function clickPackageCheckbox(page: Page, packageName: string): Promise<void> {
  const checkbox = checkboxLocator(page, packageName);
  await checkbox.scrollIntoViewIfNeeded();
  await checkbox.click();
}

function packageRow(page: Page, packageName: string) {
  return page
    .locator(`${PACKAGES_PANEL} tr`)
    .filter({ has: page.locator(`input[type='checkbox'][aria-label='${packageName}']`) });
}

// Installs minimal packages, keyed by name with extra DESCRIPTION lines, into a
// temporary library placed first on .libPaths(), then shows them in the pane.
// Undo with restoreLibPaths.
async function installLocalPackages(
  page: Page,
  consoleActions: ConsolePaneActions,
  packages: Record<string, string[]>,
): Promise<void> {
  const descriptions = Object.entries(packages).map(([name, extra]) => {
    const lines = [
      `Package: ${name}`,
      'Version: 0.0.1',
      'Title: Local Test Package',
      'Description: Installed from a local path.',
      'License: GPL-2',
      ...extra,
    ];
    return `c(${lines.map(rStringLiteral).join(', ')})`;
  });

  await consoleActions.executeInConsole(
    [
      'local({',
      'lib <- tempfile("pwlib"); dir.create(lib);',
      `for (desc in list(${descriptions.join(', ')})) {`,
      'src <- file.path(tempfile("pwsrc"), sub("^Package: ", "", desc[[1L]]));',
      'dir.create(src, recursive = TRUE);',
      'writeLines(desc, file.path(src, "DESCRIPTION"));',
      'file.create(file.path(src, "NAMESPACE"));',
      'install.packages(src, repos = NULL, type = "source", lib = lib, quiet = TRUE) };',
      'options(pw.savedLibPaths = .libPaths()); .libPaths(c(lib, .libPaths()))',
      '})',
    ].join(' '),
    { wait: true },
  );

  const names = `c(${Object.keys(packages).map(rStringLiteral).join(', ')})`;
  const ok = await consoleActions.evalRLogical(
    `all(${names} %in% rownames(installed.packages(.libPaths()[1])))`,
  );
  expect(ok, `setup failed to install ${names} into a temporary library`).toBe(true);

  await selectPackagesTab(page);
  await executeCommand(page, 'refreshPackages');
  for (const name of Object.keys(packages))
    await expect(packageRow(page, name)).toHaveCount(1, { timeout: TIMEOUTS.consoleReady });
}

async function restoreLibPaths(page: Page, consoleActions: ConsolePaneActions): Promise<void> {
  await consoleActions.executeInConsole(
    'if (!is.null(getOption("pw.savedLibPaths"))) ' +
      '{ .libPaths(getOption("pw.savedLibPaths")); options(pw.savedLibPaths = NULL) }',
    { wait: true },
  );
  await executeCommand(page, 'refreshPackages');
}

test.describe('Packages pane', () => {
  let consoleActions: ConsolePaneActions;

  test.beforeAll(async ({ rstudioPage: page }) => {
    consoleActions = new ConsolePaneActions(page);
    await consoleActions.resetSourcePane();
  });

  // Restore the Files tab as the active tabset member when we're done so a
  // later spec doesn't inherit the Packages tab focus.
  test.afterAll(async ({ rstudioPage: page }) => {
    await page.locator(FILES_TAB).click().catch(() => {});
  });

  test('checkbox reflects loaded state when attaching and detaching DBI', async ({ rstudioPage: page }) => {
    // Use DBI rather than a recommended package: recommended packages can be
    // present in both the system lib AND the user lib (when pre-installed by
    // r-libs-setup.ts), producing duplicate checkboxes that the locator would
    // need to disambiguate. A non-recommended package guarantees a single row.
    const installed = await consoleActions.ensurePackage('DBI');
    test.skip(!installed, 'DBI is not installed');

    // Ensure DBI is not currently attached -- defensive against earlier specs.
    await consoleActions.executeInConsole(
      'if ("package:DBI" %in% search()) detach("package:DBI", character.only = TRUE)',
      { wait: true },
    );

    await selectPackagesTab(page);

    const checkbox = checkboxLocator(page, 'DBI');
    await expect(checkbox).toBeVisible({ timeout: TIMEOUTS.consoleReady });
    await expect(checkbox).not.toBeChecked();

    // Attach via checkbox.
    await clickPackageCheckbox(page, 'DBI');
    await expect(checkbox).toBeChecked({ timeout: TIMEOUTS.consoleReady });

    // Verify package is actually attached via search().
    const attachedMarker = `__ATTACH_${Date.now()}__`;
    await consoleActions.executeInConsole(
      `cat("${attachedMarker}", "package:DBI" %in% search(), "${attachedMarker}")`,
    );
    await expect(consoleActions.consolePane.consoleOutput).toContainText(
      `${attachedMarker} TRUE ${attachedMarker}`,
      { timeout: TIMEOUTS.consoleReady },
    );

    // Detach via checkbox.
    await clickPackageCheckbox(page, 'DBI');
    await expect(checkbox).not.toBeChecked({ timeout: TIMEOUTS.consoleReady });

    const detachedMarker = `__DETACH_${Date.now()}__`;
    await consoleActions.executeInConsole(
      `cat("${detachedMarker}", "package:DBI" %in% search(), "${detachedMarker}")`,
    );
    await expect(consoleActions.consolePane.consoleOutput).toContainText(
      `${detachedMarker} FALSE ${detachedMarker}`,
      { timeout: TIMEOUTS.consoleReady },
    );
  });

  // https://github.com/rstudio/rstudio/issues/19471: the Help button's
  // accessible name was the package's website URL.
  test('names the Help button by its action, not its URL', async ({ rstudioPage: page }) => {
    // DBI's DESCRIPTION has a URL field, so its row shows a Help button.
    const installed = await consoleActions.ensurePackage('DBI');
    test.skip(!installed, 'DBI is not installed');

    await selectPackagesTab(page);

    // DBI can have a row in more than one library; any one row will do.
    const helpButton = page
      .locator(PACKAGES_PANEL)
      .getByRole('img', { name: 'Open DBI website', exact: true })
      .first();
    await expect(helpButton).toBeAttached({ timeout: TIMEOUTS.consoleReady });

    // The URL stays available to sighted users as the tooltip.
    await expect(helpButton.locator('xpath=..')).toHaveAttribute('title', /^https?:\/\//);
  });

  // https://github.com/rstudio/rstudio/issues/19470: the Remove button didn't
  // name its package, and the Browse button's name included its URL.
  test('names the Browse and Remove buttons by action and package', async ({ rstudioPage: page }) => {
    const installed = await consoleActions.ensurePackage('DBI');
    test.skip(!installed, 'DBI is not installed');

    await selectPackagesTab(page);
    const panel = page.locator(PACKAGES_PANEL);

    // DBI can have a row in more than one library; any one row will do.
    const browseButton = panel.getByRole('img', { name: /^Browse DBI on / }).first();
    await expect(browseButton).toBeAttached({ timeout: TIMEOUTS.consoleReady });
    await expect(browseButton).not.toHaveAttribute('alt', /https?:/);
    await expect(browseButton.locator('xpath=..')).toHaveAttribute('title', /^https?:\/\//);

    await expect(
      panel.getByRole('img', { name: 'Remove DBI', exact: true }).first(),
    ).toBeAttached();
  });

  // https://github.com/rstudio/rstudio/issues/19470: a package installed from a
  // local path has no browse URL, but its row showed an unlabeled Browse button.
  test('shows no Browse button for a package installed from a local path', async ({ rstudioPage: page }) => {
    const pkg = 'pwLocalPkg19470';
    try {
      await installLocalPackages(page, consoleActions, { [pkg]: [] });

      // The package has no URL field, so Remove is the row's only button. The
      // broken build also rendered the Browse image, with an empty alt.
      const row = packageRow(page, pkg);
      await expect(row.locator('img')).toHaveCount(1);
      await expect(row.locator('img')).toHaveAttribute('alt', `Remove ${pkg}`);
    } finally {
      await restoreLibPaths(page, consoleActions);
    }
  });

  // https://github.com/rstudio/rstudio/issues/19470: the Browse button's name
  // includes the package's source only when that's the site Browse opens.
  test('names the Browse button by where it goes', async ({ rstudioPage: page }) => {
    const githubPkg = 'pwGitHubPkg19470';
    const localPkg = 'pwLocalBiocPkg19470';
    const unknownPkg = 'pwUnknownBiocPkg19470';
    try {
      // biocViews gives each Bioconductor package a bioconductor.org browse URL.
      await installLocalPackages(page, consoleActions, {
        [githubPkg]: [
          'RemoteType: github',
          'RemoteHost: api.github.com',
          'RemoteUsername: example',
          `RemoteRepo: ${githubPkg}`,
        ],
        // RStudio's install hook records this one's source as "Local [<path>]".
        [localPkg]: ['biocViews: Software'],
        // Having a Remote field makes the hook skip this one, as if installed
        // outside RStudio; with no Repository its source is "[Unknown]".
        [unknownPkg]: ['biocViews: Software', 'RemoteType: standard'],
      });

      const githubBrowse = packageRow(page, githubPkg).getByRole('img', {
        name: `Browse ${githubPkg} on GitHub [example/${githubPkg}]`,
        exact: true,
      });
      await expect(githubBrowse).toBeAttached();
      await expect(githubBrowse.locator('xpath=..')).toHaveAttribute(
        'title',
        `https://github.com/example/${githubPkg}`,
      );

      // Confirm each row's source is the one the fallback is meant for.
      await expect(packageRow(page, localPkg)).toContainText('Local [');
      await expect(packageRow(page, unknownPkg)).toContainText('[Unknown]');

      for (const pkg of [localPkg, unknownPkg]) {
        const browse = packageRow(page, pkg).getByRole('img', {
          name: `Browse ${pkg}`,
          exact: true,
        });
        await expect(browse).toBeAttached();
        await expect(browse.locator('xpath=..')).toHaveAttribute('title', /bioconductor/);
      }
    } finally {
      await restoreLibPaths(page, consoleActions);
    }
  });

  test('shows correct initial checkbox state for an always-attached base package', async ({ rstudioPage: page }) => {
    // `stats` is always on the search path -- if its checkbox isn't checked,
    // the initial-state plumbing is broken.
    await selectPackagesTab(page);

    const checkbox = checkboxLocator(page, 'stats');
    await expect(checkbox).toBeVisible({ timeout: TIMEOUTS.consoleReady });
    await expect(checkbox).toBeChecked();
  });
});
