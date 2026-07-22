# Files pane patterns

Read this when driving the Files pane (`tests/panes/files/`) or the Open File
dialog.

## Driving the pane

- Bring it to front first with `executeCommand(page, 'activateFiles')` -- the
  virtualized grid renders no rows while the pane is hidden.
- A file row is `div[title="<filename>"]`. Clicking the *name*
  navigates/opens; to run a command on a file instead, check the row checkbox:

  ```typescript
  const nameDiv = page.locator('div[title="mydata.R"]');
  await page.locator('tr', { has: nameDiv }).locator('input[type="checkbox"]').check();
  await executeCommand(page, 'openFilesInSinglePane');
  ```

- Re-home the pane per test with `.rs.api.filesPaneNavigate(path.expand("~"))`
  in the console.
- Count source columns via `[class*='rstudio_source_panel']`.

## Shortcut/alias fixtures exist -- reuse them

- **macOS Finder aliases**: `@utils/finder-aliases`
  (`uniqueFinderAliasFixture` / `createFinderAliasFixture` /
  `removeFinderAliasFixture`) creates real aliases via `osascript -l
  JavaScript` and the NSURL bookmark API -- the JXA route avoids the TCC
  automation prompt that Finder scripting would trigger.
- **Windows `.lnk` shortcuts**: `@utils/windows-shortcuts`
  (`uniqueWindowsShortcutFixture` / `createWindowsShortcutFixture` /
  `removeWindowsShortcutFixture`) creates real shortcuts via PowerShell's
  WScript.Shell COM interface, driven through the R console.

## Open File dialog: normalize the start directory

The GWT Open File dialog opens at the last dialog-opened file's directory,
which is nondeterministic across specs sharing a session. Use
`openFileDialogAtHome` (`@utils/file-dialogs`): it types `~` and accepts,
waiting on the filename box clearing -- the dialog's navigation-complete
signal. Dialog selector: `.gwt-DialogBox[aria-label="Open File"]`.
