import { test, expect } from '@fixtures/rstudio.fixture';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { useSuiteSandbox } from '@utils/sandbox';

// Regression test for issue #17178: clicking "Run examples" on a help page
// whose examples launch a blocking application (e.g. a Shiny app) used to lock
// up the session, because R's enhanced HTML help rendered the example inline on
// the R event loop and a blocking runApp() never returned. We now detect such
// examples and run them in the console (like example()) instead.
//
// To keep the test deterministic and self-contained we install a throwaway
// package whose example *statically* looks like it launches a Shiny app -- so
// our detection fires -- but guards the launcher with `if (FALSE)` and prints a
// marker. That way the example our fix dispatches to the console completes
// instantly without launching anything (and without needing shiny installed).

const sandbox = useSuiteSandbox();

const PKG = 'lockuptest';
const MARKER = 'LOCKUP-EXAMPLE-RAN';

let consoleActions: ConsolePaneActions;

function rPath(p: string): string {
  return p.replace(/\\/g, '/');
}

// Build a `writeLines(c(...), path)` command. Splitting on newlines and
// JSON.stringify-ing each line yields valid R string literals: JSON's escape
// rules for \\, \", and \n match R's, so backslash-heavy Rd content survives.
function writeFileCmd(path: string, content: string): string {
  const lines = content.split('\n').map((l) => JSON.stringify(l)).join(', ');
  return `writeLines(c(${lines}), ${JSON.stringify(path)})`;
}

test.describe('Help "Run examples" for blocking examples', () => {
  let installed = false;

  test.beforeAll(async ({ rstudioPage: page }) => {
    consoleActions = new ConsolePaneActions(page);

    const pkgDir = `${rPath(sandbox.dir)}/${PKG}`;

    const description = [
      `Package: ${PKG}`,
      'Type: Package',
      'Title: Lockup Test',
      'Version: 0.1.0',
      'Description: Fixture package for issue 17178.',
      'License: GPL-3',
      'Encoding: UTF-8',
    ].join('\n');

    // String.raw keeps the Rd backslashes literal. The example statically
    // mentions shiny::runApp/shinyApp (so detection fires) but never runs them.
    const testfnRd = String.raw`\name{testfn}
\alias{testfn}
\title{Test help example}
\usage{testfn(x)}
\arguments{\item{x}{An argument.}}
\description{A test function.}
\examples{
if (FALSE) {
  shiny::runApp(shiny::shinyApp(ui, server))
}
cat("${MARKER}\n")
}`;

    // Build a minimal source package directly from the session so it lands in
    // the session's library and its help is served by the dynamic help server.
    await consoleActions.executeInConsole(
      `unlink(${JSON.stringify(pkgDir)}, recursive = TRUE); ` +
        `dir.create(${JSON.stringify(`${pkgDir}/R`)}, recursive = TRUE, showWarnings = FALSE); ` +
        `dir.create(${JSON.stringify(`${pkgDir}/man`)}, recursive = TRUE, showWarnings = FALSE)`,
      { wait: true },
    );
    await consoleActions.executeInConsole(writeFileCmd(`${pkgDir}/DESCRIPTION`, description), {
      wait: true,
    });
    await consoleActions.executeInConsole(writeFileCmd(`${pkgDir}/NAMESPACE`, 'export(testfn)'), {
      wait: true,
    });
    await consoleActions.executeInConsole(
      writeFileCmd(`${pkgDir}/R/testfn.R`, 'testfn <- function(x) "this is a test"'),
      { wait: true },
    );
    await consoleActions.executeInConsole(writeFileCmd(`${pkgDir}/man/testfn.Rd`, testfnRd), {
      wait: true,
    });
    await consoleActions.executeInConsole(
      `install.packages(${JSON.stringify(pkgDir)}, repos = NULL, type = "source")`,
      { wait: true, timeout: 60000 },
    );

    installed =
      (await consoleActions.evalRLogical(
        `"${PKG}" %in% rownames(installed.packages())`,
      )) === true;
  });

  test.afterAll(async () => {
    if (installed)
      await consoleActions.uninstallPackage(PKG).catch(() => undefined);
  });

  test('routes a Shiny-launching example to the console instead of locking up', async ({
    rstudioPage: page,
  }) => {
    test.skip(!installed, `Could not install fixture package ${PKG}`);

    await consoleActions.clearConsole();
    await consoleActions.executeInConsole(`help("testfn", package = "${PKG}")`);

    const helpFrame = page.frameLocator('#rstudio_help_frame');
    const runExamples = helpFrame.getByRole('link', { name: 'Run examples' });
    await expect(runExamples).toBeVisible({ timeout: 15000 });

    await runExamples.click();

    // The help server returns our lightweight "running in the console" page
    // immediately, rather than blocking while it renders the example inline.
    await expect(helpFrame.locator('body')).toContainText(
      'Running example testfn in the R console',
      { timeout: 15000 },
    );

    // And the example actually ran in the console (the safe path), which we
    // can see from its marker output.
    await expect(consoleActions.consolePane.consoleOutput).toContainText(MARKER, {
      timeout: 15000,
    });
  });
});
