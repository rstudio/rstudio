import { test, expect } from '@fixtures/rstudio.fixture';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { SourcePaneActions } from '@actions/source_pane.actions';
import { installDepIfPrompted, CONFIRM_BTN, NO_BTN } from '@pages/modals.page';
import { useSuiteSandbox } from '@utils/sandbox';
import { executeCommand } from '@utils/commands';
import type { Page } from 'playwright';

// Original: test_desktop_Create_File_Types.py::TestCreateAllFileTypes
//
// Each test creates a new document of a given type via its New-File command and
// verifies (a) a new Untitled tab opens (or a named tab for Shiny/Plumber), and
// (b) the editor status-bar footer reports the expected file type.
//
// The suite keeps one Untitled placeholder tab open (resetSourcePane never drops
// to zero tabs, to dodge the #17738 HIDE-animation race), and that placeholder is
// itself an R Script. Asserting on tab/footer text alone can therefore race a slow
// create (reading the placeholder's "R Script") or pass falsely for newSourceDoc.
// So each test waits for the source-tab count to reach 2 -- proof a new doc
// opened -- before reading the footer. The Selenium version looped all the simple
// types inside a single test; here they're one test each so one type's failure
// doesn't mask the rest.

// Source-document tabs; a successful create brings the count to 2 (see header).
function sourceTabs(page: Page) {
  return page.locator("[class*='rstudio_source_panel'] [role='tab']");
}

test.describe('Create file types', () => {
  // Sets cwd to a per-spec sandbox so any on-disk writes land there.
  useSuiteSandbox();
  let consoleActions: ConsolePaneActions;
  let sourceActions: SourcePaneActions;

  test.beforeAll(async ({ rstudioPage: page }) => {
    consoleActions = new ConsolePaneActions(page);
    sourceActions = new SourcePaneActions(page, consoleActions);
    await consoleActions.resetSourcePane();
  });

  // ---- Types that need neither an R package nor a creation dialog ----
  // Original: test_creating_new_file_without_required_packages_nor_modals
  const simpleTypes: ReadonlyArray<readonly [command: string, fileType: string]> = [
    ['newCDoc', 'C/C++'],
    ['newCppDoc', 'C/C++'],
    ['newHeaderDoc', 'C/C++'],
    ['newCssDoc', 'CSS'],
    ['newHtmlDoc', 'HTML'],
    ['newJavaScriptDoc', 'JavaScript'],
    ['newMarkdownDoc', 'Markdown'],
    ['newPythonDoc', 'Python'],
    ['newRHTMLDoc', 'R HTML'],
    ['newShellDoc', 'Shell'],
    // Selenium asserted just "R" here (a loose Selene substring match); the
    // status bar actually reads "R Script", so assert that.
    ['newSourceDoc', 'R Script'],
    // Commented out in the Selenium source (no stated reason); it needs no
    // package or dialog, so it belongs in this sweep.
    ['newSweaveDoc', 'R Sweave'],
    ['newTextDoc', 'Text file'],
  ];

  for (const [command, fileType] of simpleTypes) {
    test(`create ${fileType} document via ${command}`, async ({ rstudioPage: page }) => {
      await executeCommand(page, command);
      await expect(sourceTabs(page)).toHaveCount(2, { timeout: 20000 });
      await expect(sourceActions.sourcePane.footerTable).toContainText(fileType);
      await consoleActions.resetSourcePane();
    });
  }

  // SQL is its own test (not in the sweep above) because the New SQL handler
  // runs a prerequisites check on the template's preview-connection line, which
  // can pop a modal offering to install/update RSQLite. The doc is created
  // regardless; declining the prompt (if it appears) keeps the leftover modal
  // from blocking the next test's console focus (it otherwise broke newRNotebook).
  test('create SQL document via newSqlDoc', async ({ rstudioPage: page }) => {
    await executeCommand(page, 'newSqlDoc');
    await expect(sourceTabs(page)).toHaveCount(2, { timeout: 20000 });
    await expect(sourceActions.sourcePane.footerTable).toContainText('SQL');
    // Decline the RSQLite prompt if it appeared; it may not (e.g. RSQLite
    // already current), and the doc is created either way.
    await page.locator(NO_BTN).click({ timeout: 15000 }).catch(() => {});
    await consoleActions.resetSourcePane();
  });

  // ---- Types that need an R package and/or a creation dialog ----
  test.describe('with packages or dialogs', () => {
    let missingPackages: string[] = [];
    let rstanAvailable = false;

    test.beforeAll(async ({ rstudioPage: page }) => {
      // r2d3 -> newD3Doc; rmarkdown -> newRNotebook / Quarto / R Markdown;
      // shiny -> newRShinyApp; plumber -> newRPlumberDoc. 180s covers a cold
      // transitive install on a fresh CI runner; warm caches are near-instant.
      missingPackages = await consoleActions.ensurePackages(
        ['r2d3', 'rmarkdown', 'shiny', 'plumber'],
        180_000,
      );
      // rstan gates newStanDoc (dependencyManager_.withStan). It's a heavy
      // source compile where no binary is available, so we don't auto-install
      // it here -- just check presence. Pre-populate it via REQUIRED_PACKAGES
      // to enable the Stan test; otherwise it skips cleanly.
      rstanAvailable = (await consoleActions.evalRLogical('requireNamespace("rstan", quietly = TRUE)')) === true;
      await consoleActions.clearConsole();
    });

    // Types that create directly once their package is present (no dialog).
    // Original: test_creating_new_file_with_required_packages_but_no_modals.
    // newRNotebook creates a FileTypeRegistry.RMARKDOWN doc (from notebook.Rmd),
    // so the footer reads "R Markdown" -- not "R Notebook".
    const packageTypes: ReadonlyArray<readonly [command: string, fileType: string, pkg: string]> = [
      ['newD3Doc', 'JavaScript', 'r2d3'],
      ['newRNotebook', 'R Markdown', 'rmarkdown'],
    ];

    for (const [command, fileType, pkg] of packageTypes) {
      test(`create ${fileType} document via ${command}`, async ({ rstudioPage: page }) => {
        test.skip(missingPackages.includes(pkg), `required R package not available: ${pkg}`);
        await executeCommand(page, command);
        // Packages are pre-installed in beforeAll, so a dependency prompt
        // shouldn't appear; keep the check defensive but short.
        await installDepIfPrompted(page, 2500);
        await expect(sourceTabs(page)).toHaveCount(2, { timeout: 20000 });
        await expect(sourceActions.sourcePane.footerTable).toContainText(fileType);
        await consoleActions.resetSourcePane();
      });
    }

    // Stan creates directly (no dialog), but newStanDoc is gated on rstan via
    // dependencyManager_.withStan. We don't auto-install rstan (heavy source
    // compile); the test runs only when it's already present, else it skips.
    test('create Stan document via newStanDoc', async ({ rstudioPage: page }) => {
      test.skip(!rstanAvailable, 'rstan not installed');
      await executeCommand(page, 'newStanDoc');
      await installDepIfPrompted(page, 2500);
      await expect(sourceTabs(page)).toHaveCount(2, { timeout: 20000 });
      await expect(sourceActions.sourcePane.footerTable).toContainText('Stan');
      await consoleActions.resetSourcePane();
    });

    // Quarto document and presentation both open the New Quarto Document wizard
    // and create a "Quarto" doc; the only difference is the presentation flag.
    // Original: test_creating_new_file_quarto / test_creating_new_file_quarto_presentation.
    // The Selenium presentation test set command="newQuartoPres" but then ran a
    // helper that re-issued newQuartoDoc, so it never actually exercised the
    // presentation path -- fixed here by driving each command directly.
    for (const [command, label] of [
      ['newQuartoDoc', 'document'],
      ['newQuartoPres', 'presentation'],
    ] as const) {
      test(`create Quarto ${label} via ${command}`, async ({ rstudioPage: page }) => {
        test.skip(missingPackages.includes('rmarkdown'), 'required R package not available: rmarkdown');
        await executeCommand(page, command);
        await installDepIfPrompted(page, 2500);

        // Accept the wizard's defaults. The Create button is briefly disabled
        // while the dialog initializes, so wait for it to enable before clicking.
        const okBtn = page.locator(CONFIRM_BTN);
        await expect(okBtn).toBeEnabled({ timeout: 20000 });
        await okBtn.click();

        await expect(sourceTabs(page)).toHaveCount(2, { timeout: 20000 });
        await expect(sourceActions.sourcePane.footerTable).toContainText('Quarto');
        await expect(sourceActions.sourcePane.publishBtn).toBeVisible({ timeout: 10000 });
        await consoleActions.resetSourcePane();
      });
    }

    test('create R Markdown document via newRMarkdownDoc', async ({ rstudioPage: page }) => {
      // Original: test_creating_new_file_rmarkdown
      test.skip(missingPackages.includes('rmarkdown'), 'required R package not available: rmarkdown');
      await executeCommand(page, 'newRMarkdownDoc');
      await installDepIfPrompted(page, 2500);

      // Accept the New R Markdown dialog defaults (HTML document).
      const okBtn = page.locator(CONFIRM_BTN);
      await expect(okBtn).toBeEnabled({ timeout: 15000 });
      await okBtn.click();

      await expect(sourceTabs(page)).toHaveCount(2, { timeout: 20000 });
      await expect(sourceActions.sourcePane.publishBtn).toBeVisible({ timeout: 10000 });
      await expect(sourceActions.sourcePane.footerTable).toContainText('R Markdown');
      await consoleActions.resetSourcePane();
    });

    test('create Shiny app via newRShinyApp', async ({ rstudioPage: page }) => {
      // Original: test_creating_new_file_r_shiny_app
      test.skip(missingPackages.includes('shiny'), 'required R package not available: shiny');
      const appName = `shinyapp_${Date.now()}`;

      await executeCommand(page, 'newRShinyApp');
      await installDepIfPrompted(page, 2500);

      // Fill the application name; the parent dir defaults to "~", which the
      // fixture redirects into the sandbox user-home.
      const nameInput = page.locator('#rstudio_new_shiny_app_name');
      await expect(nameInput).toBeVisible({ timeout: 15000 });
      await nameInput.fill(appName);
      await page.locator(CONFIRM_BTN).click();

      await expect(sourceTabs(page)).toHaveCount(2, { timeout: 20000 });
      await expect(sourceActions.sourcePane.selectedTab).toContainText('app.R', { timeout: 15000 });
      await expect(sourceActions.sourcePane.footerTable).toContainText('R Script');
      await expect(sourceActions.sourcePane.publishBtn).toBeVisible({ timeout: 10000 });

      // Cleanup: close the doc and remove the generated app directory.
      await consoleActions.resetSourcePane();
      await consoleActions.executeInConsole(
        `unlink(file.path("~", "${appName}"), recursive = TRUE)`,
        { wait: true },
      );
    });

    test('create Rd documentation via newRDocumentationDoc', async ({ rstudioPage: page }) => {
      // WIP type in the Selenium source (never implemented there). No package needed.
      await executeCommand(page, 'newRDocumentationDoc');

      const dialog = page.locator('div.gwt-DialogBox[aria-label="New R Documentation File"]');
      await expect(dialog).toBeVisible({ timeout: 15000 });
      await page.locator('#rstudio_new_rd_name').fill('mytopic');
      // The template list defaults to "Function" (which would generate a
      // function shell from the workspace); pick "(Empty Topic)" for a plain,
      // deterministic Rd document.
      await page.locator('#rstudio_new_rd_template').selectOption('none');
      await page.locator(CONFIRM_BTN).click();

      await expect(sourceTabs(page)).toHaveCount(2, { timeout: 20000 });
      await expect(sourceActions.sourcePane.footerTable).toContainText('Rd File');
      await consoleActions.resetSourcePane();
    });

    test('create Plumber API via newRPlumberDoc', async ({ rstudioPage: page }) => {
      // WIP type in the Selenium source (never implemented there).
      test.skip(missingPackages.includes('plumber'), 'required R package not available: plumber');
      const apiName = `plumberapi_${Date.now()}`;

      await executeCommand(page, 'newRPlumberDoc');
      await installDepIfPrompted(page, 2500);

      const dialog = page.locator('div.gwt-DialogBox[aria-label="New Plumber API"]');
      await expect(dialog).toBeVisible({ timeout: 15000 });
      // The API-name box has no stable id; it's the first text input in the
      // dialog (the second is the directory chooser, which defaults to "~").
      await dialog.locator('input[type="text"]').first().fill(apiName);
      await page.locator(CONFIRM_BTN).click(); // OK button caption is "Create"

      await expect(sourceTabs(page)).toHaveCount(2, { timeout: 20000 });
      await expect(sourceActions.sourcePane.selectedTab).toContainText('plumber.R', { timeout: 15000 });
      await expect(sourceActions.sourcePane.footerTable).toContainText('R Script');

      // Cleanup: close the doc and remove the generated API directory.
      await consoleActions.resetSourcePane();
      await consoleActions.executeInConsole(
        `unlink(file.path("~", "${apiName}"), recursive = TRUE)`,
        { wait: true },
      );
    });

    // @skip: R Presentation creation opens a save-file dialog (native OS chooser
    // on Desktop, GWT chooser on Server) to pick the .Rpres path -- driving that
    // save dialog isn't supported by the harness yet. The Selenium source never
    // implemented this either (it was left as a WIP comment).
    test.fixme('create R Presentation via newRPresentationDoc', async () => {});
  });
});
