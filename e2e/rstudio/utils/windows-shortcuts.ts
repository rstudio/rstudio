// Shared fixture for Windows .lnk shortcut tests (#7327).
//
// Shortcuts are shell objects, not symlinks. The fixture creates real .lnk
// files through PowerShell's WScript.Shell COM interface -- the same data
// Explorer writes -- rather than hand-rolling the shell link binary format.
// Creation runs through the R console so paths resolve against rsession's
// HOME in every mode (see the rationale in file_monitor.test.ts).

import { Page, expect } from '@playwright/test';
import { ConsolePaneActions } from '@actions/console_pane.actions';

// PowerShell script (written to a temp .ps1 by R): argv = target path, .lnk
// path to create. Prints 'true' when the .lnk exists after Save. Paths
// arrive pre-normalized to backslashes (the shell APIs store native
// separators). None of these lines contain double quotes or backslashes, so
// they embed directly into R double-quoted strings below.
const CREATE_SHORTCUT_PS1_LINES = [
  'param([string]$Target, [string]$Link)',
  '$ws = New-Object -ComObject WScript.Shell',
  '$sc = $ws.CreateShortcut($Link)',
  '$sc.TargetPath = $Target',
  '$sc.Save()',
  "if (Test-Path -LiteralPath $Link) { 'true' } else { 'false' }",
];

// Everything is created in ~: a directory holding a marker file (so
// navigation into the directory is observable), a text document, and one
// shortcut to each. Shortcut names keep their .lnk extension -- the Files
// pane shows the full filename, so row locators use these names verbatim.
export interface WindowsShortcutFixture {
  targetDirName: string;
  markerName: string;
  targetDocName: string;
  dirShortcutName: string;
  fileShortcutName: string;
}

export function uniqueWindowsShortcutFixture(prefix: string): WindowsShortcutFixture {
  const uniq = `${prefix}-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`;
  return {
    targetDirName: `pw-${uniq}-target`,
    markerName: `marker-${uniq}.txt`,
    targetDocName: `pw-${uniq}-doc.txt`,
    dirShortcutName: `pw-${uniq}-dir-shortcut.lnk`,
    fileShortcutName: `pw-${uniq}-file-shortcut.lnk`,
  };
}

// Creates the fixture and fails loudly. Both failure channels are checked --
// powershell's exit status AND the script's own true/false result (a Save()
// can fail with exit code 0) -- and a sentinel line is asserted, so an
// environmental setup problem reads as one clear failure here instead of a
// misleading visibility timeout in every test.
export async function createWindowsShortcutFixture(
  page: Page,
  consoleActions: ConsolePaneActions,
  fx: WindowsShortcutFixture,
): Promise<void> {
  const psVector = CREATE_SHORTCUT_PS1_LINES.map((line) => `"${line}"`).join(', ');
  await consoleActions.executeInConsole(
    [
      'local({',
      'home <- path.expand("~")',
      `dir.create(file.path(home, "${fx.targetDirName}"))`,
      `writeLines("marker", file.path(home, "${fx.targetDirName}", "${fx.markerName}"))`,
      `writeLines("target doc", file.path(home, "${fx.targetDocName}"))`,
      'f <- tempfile(fileext = ".ps1")',
      `writeLines(c(${psVector}), f)`,
      'mk <- function(target, link) { target <- gsub("/", "\\\\", target, fixed = TRUE); link <- gsub("/", "\\\\", link, fixed = TRUE); out <- suppressWarnings(system2("powershell", c("-NoProfile", "-NonInteractive", "-ExecutionPolicy", "Bypass", "-File", shQuote(f), shQuote(target), shQuote(link)), stdout = TRUE, stderr = TRUE)); is.null(attr(out, "status")) && identical(tail(out, 1), "true") }',
      `ok <- mk(file.path(home, "${fx.targetDirName}"), file.path(home, "${fx.dirShortcutName}"))`,
      `ok <- ok && mk(file.path(home, "${fx.targetDocName}"), file.path(home, "${fx.fileShortcutName}"))`,
      'writeLines(if (ok) "SHORTCUT-SETUP-OK" else "SHORTCUT-SETUP-FAILED")',
      '})',
    ].join('; '),
    { wait: true },
  );
  await expect(page.getByText('SHORTCUT-SETUP-OK', { exact: true })).toBeVisible({ timeout: 15000 });
}

export async function removeWindowsShortcutFixture(
  consoleActions: ConsolePaneActions,
  fx: WindowsShortcutFixture,
): Promise<void> {
  await consoleActions.executeInConsole(
    `unlink(file.path(path.expand("~"), c("${fx.targetDirName}", "${fx.targetDocName}", "${fx.dirShortcutName}", "${fx.fileShortcutName}")), recursive = TRUE)`,
    { wait: true },
  );
}
