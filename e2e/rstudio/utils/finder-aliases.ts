// Shared fixture for macOS Finder-alias tests (#18158).
//
// Finder aliases are bookmark files, not symlinks. They are created via
// `osascript -l JavaScript` (the ObjC bridge's NSURL bookmark API -- the
// same data Finder writes) rather than Finder scripting, which would
// require automation (TCC) permission. Creation runs through the R console
// so paths resolve against rsession's HOME in every mode (see the rationale
// in file_monitor.test.ts).

import { Page, expect } from '@playwright/test';
import { ConsolePaneActions } from '@actions/console_pane.actions';

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

// Everything is created in ~: a directory holding a marker file (so
// navigation into the directory is observable), a text document, and one
// alias to each.
export interface FinderAliasFixture {
  targetDirName: string;
  markerName: string;
  targetDocName: string;
  dirAliasName: string;
  fileAliasName: string;
}

export function uniqueFinderAliasFixture(prefix: string): FinderAliasFixture {
  const uniq = `${prefix}-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`;
  return {
    targetDirName: `pw-${uniq}-target`,
    markerName: `marker-${uniq}.txt`,
    targetDocName: `pw-${uniq}-doc.txt`,
    dirAliasName: `pw-${uniq}-dir-alias`,
    fileAliasName: `pw-${uniq}-file-alias`,
  };
}

// Creates the fixture and fails loudly. Both failure channels are checked --
// osascript's exit status AND the JXA script's own true/false result (a
// bookmark write can fail with exit code 0) -- and a sentinel line is
// asserted, so an environmental setup problem reads as one clear failure
// here instead of a misleading visibility timeout in every test.
export async function createFinderAliasFixture(
  page: Page,
  consoleActions: ConsolePaneActions,
  fx: FinderAliasFixture,
): Promise<void> {
  await consoleActions.executeInConsole(
    [
      'local({',
      'home <- path.expand("~")',
      `dir.create(file.path(home, "${fx.targetDirName}"))`,
      `writeLines("marker", file.path(home, "${fx.targetDirName}", "${fx.markerName}"))`,
      `writeLines("target doc", file.path(home, "${fx.targetDocName}"))`,
      `jxa <- '${CREATE_ALIAS_JXA}'`,
      'f <- tempfile(fileext = ".js")',
      'writeLines(jxa, f)',
      'mk <- function(target, alias) { out <- suppressWarnings(system2("osascript", c("-l", "JavaScript", f, shQuote(target), shQuote(alias)), stdout = TRUE, stderr = TRUE)); is.null(attr(out, "status")) && identical(tail(out, 1), "true") }',
      `ok <- mk(file.path(home, "${fx.targetDirName}"), file.path(home, "${fx.dirAliasName}"))`,
      `ok <- ok && mk(file.path(home, "${fx.targetDocName}"), file.path(home, "${fx.fileAliasName}"))`,
      'writeLines(if (ok) "ALIAS-SETUP-OK" else "ALIAS-SETUP-FAILED")',
      '})',
    ].join('; '),
    { wait: true },
  );
  await expect(page.getByText('ALIAS-SETUP-OK', { exact: true })).toBeVisible({ timeout: 15000 });
}

export async function removeFinderAliasFixture(
  consoleActions: ConsolePaneActions,
  fx: FinderAliasFixture,
): Promise<void> {
  await consoleActions.executeInConsole(
    `unlink(file.path(path.expand("~"), c("${fx.targetDirName}", "${fx.targetDocName}", "${fx.dirAliasName}", "${fx.fileAliasName}")), recursive = TRUE)`,
    { wait: true },
  );
}
