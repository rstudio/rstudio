// Files pane Finder-alias navigation (#18158).
//
// macOS Finder aliases are bookmark files, not symlinks, so the filesystem
// does not resolve them. The backend resolves them during listing
// (createFileSystemItem in SessionModuleContext.cpp emits `alias_target`)
// and the Files pane follows the target on click: a directory alias
// navigates into the target folder, a file alias opens the target file.
//
// Aliases are created via `osascript -l JavaScript` (the ObjC bridge's
// NSURL bookmark API -- the same data Finder writes) rather than Finder
// scripting, which would require automation (TCC) permission. Creation runs
// through the R console so paths resolve against rsession's HOME, matching
// the directory the Files pane monitors (see file_monitor.test.ts).

import { test, expect } from '@fixtures/rstudio.fixture';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { executeCommand, waitForActiveDocument } from '@utils/commands';

// JXA script: argv[0] = target path, argv[1] = alias path to create.
// 1024 is NSURLBookmarkCreationSuitableForBookmarkFile (1 << 10); the raw
// value avoids depending on the ObjC bridge exposing the enum constant.
const CREATE_ALIAS_JXA = [
  'ObjC.import("Foundation");',
  'function run(argv) {',
  '  var data = $.NSURL.fileURLWithPath(argv[0])',
  '    .bookmarkDataWithOptionsIncludingResourceValuesForKeysRelativeToURLError(1024, $(), $(), null);',
  '  var ok = $.NSURL.writeBookmarkDataToURLOptionsError(data, $.NSURL.fileURLWithPath(argv[1]), 0, null);',
  '  return String(ok);',
  '}',
].join(' ');

const uniq = `${Date.now()}-${Math.random().toString(36).slice(2, 8)}`;
const targetDirName = `pw-alias-target-${uniq}`;
const markerName = `marker-${uniq}.txt`;
const targetDocName = `pw-alias-doc-${uniq}.txt`;
const dirAliasName = `pw-dir-alias-${uniq}`;
const fileAliasName = `pw-file-alias-${uniq}`;

test.describe('Files pane follows macOS Finder aliases @desktop_only', () => {
  test.skip(process.platform !== 'darwin', 'Finder aliases are macOS-only');

  let consoleActions: ConsolePaneActions;

  test.beforeAll(async ({ rstudioPage: page }) => {
    consoleActions = new ConsolePaneActions(page);

    // Create alias targets in ~: a directory holding a marker file (so
    // navigation into it is observable) and a text document.
    await consoleActions.executeInConsole(
      [
        'local({',
        'home <- path.expand("~")',
        `dir.create(file.path(home, "${targetDirName}"))`,
        `writeLines("marker", file.path(home, "${targetDirName}", "${markerName}"))`,
        `writeLines("alias target doc", file.path(home, "${targetDocName}"))`,
        `jxa <- '${CREATE_ALIAS_JXA}'`,
        'f <- tempfile(fileext = ".js")',
        'writeLines(jxa, f)',
        'mk <- function(target, alias) system2("osascript", c("-l", "JavaScript", f, shQuote(target), shQuote(alias)))',
        `mk(file.path(home, "${targetDirName}"), file.path(home, "${dirAliasName}"))`,
        `mk(file.path(home, "${targetDocName}"), file.path(home, "${fileAliasName}"))`,
        '})',
      ].join('; '),
      { wait: true },
    );
  });

  test.afterAll(async () => {
    await consoleActions.executeInConsole(
      `unlink(file.path(path.expand("~"), c("${targetDirName}", "${targetDocName}", "${dirAliasName}", "${fileAliasName}")), recursive = TRUE)`,
      { wait: true },
    );
  });

  test.beforeEach(async ({ rstudioPage: page }) => {
    // Re-home the pane each test (test 1 leaves it inside the target dir)
    // and bring it to front so the virtualized grid renders rows.
    await consoleActions.executeInConsole(
      '.rs.api.filesPaneNavigate(path.expand("~"))',
      { wait: true },
    );
    await executeCommand(page, 'activateFiles');
  });

  test('clicking a directory alias navigates to the target folder', async ({ rstudioPage: page }) => {
    const aliasRow = page.locator(`div[title="${dirAliasName}"]`);
    await expect(aliasRow).toBeVisible({ timeout: 15000 });
    await aliasRow.click();

    // The marker file only exists inside the target directory, so seeing it
    // proves the pane listed the alias's target rather than erroring on the
    // alias file itself.
    await expect(page.locator(`div[title="${markerName}"]`)).toBeVisible({ timeout: 15000 });
  });

  test('clicking a file alias opens the target file', async ({ rstudioPage: page }) => {
    const aliasRow = page.locator(`div[title="${fileAliasName}"]`);
    await expect(aliasRow).toBeVisible({ timeout: 15000 });
    await aliasRow.click();

    // The editor must land on the resolved target document, not the alias.
    await waitForActiveDocument(page, `~/${targetDocName}`, 15000);
  });
});
