## RStudio 2026.06.0 "Blue Plumbago" Release Notes

### New
- ([#15830](https://github.com/rstudio/rstudio/issues/15830)): Add a Code menu and command-palette entry to re-enable the editor's missing-package banner for a file after dismissing it, and rename the banner's dismissal label to "Don't show for this file" to make the per-file scope explicit.
- ([#17734](https://github.com/rstudio/rstudio/issues/17734)): Support em dashes and box-drawing characters as native R code section delimiters.
- ([#8541](https://github.com/rstudio/rstudio/issues/8541)): Add a "Change active editor tab with mouse wheel" preference (General > Basic > Other) to disable switching the active editor tab by scrolling the mouse wheel over the tab bar.

### Fixed
- ([#14202](https://github.com/rstudio/rstudio/issues/14202)): Fixed an issue where RStudio Desktop could hang on startup when offline or on an unreliable network connection.
- ([#17701](https://github.com/rstudio/rstudio/issues/17701)): Honor newline characters in `rstudioapi::showDialog()` and `rstudioapi::showPrompt()` messages.
- ([#17729](https://github.com/rstudio/rstudio/issues/17729)): Show distinct copy in the Posit Assistant chat pane and preferences install prompt when the recommended Posit Assistant version is older than the installed one.
- ([#17738](https://github.com/rstudio/rstudio/issues/17738)): Fix `file.edit()` (and other Source pane open events) silently dropping when invoked during an in-flight `closeAllSourceDocs`.
- ([#17745](https://github.com/rstudio/rstudio/issues/17745)): Fix the bundled `air` formatter being copied without the `.exe` extension on Windows, which prevented format-on-save with Air from working.
- ([#17737](https://github.com/rstudio/rstudio/issues/17737)): Restore the "Show .Last.value in environment listing" preference, which had stopped surfacing `.Last.value` in the Environment pane.
- ([#17743](https://github.com/rstudio/rstudio/issues/17743)): Fix the debugger failing to capture the browser environment for functions loaded by the `box` package.
- ([#17712](https://github.com/rstudio/rstudio/issues/17712)): Fix Quarto rendering failing on Windows when the user's home folder path contains an ampersand.
- ([#17669](https://github.com/rstudio/rstudio/issues/17669)): Avoid re-scanning the watched directory on every Files pane notification on macOS by enabling FSEvents per-file event reporting for non-recursive watches.
- ([#17768](https://github.com/rstudio/rstudio/issues/17768)): Handle the ANSI cursor-movement escape sequences cursor-up (CSI A), cursor-down (CSI B), cursor-next-line (CSI E), cursor-previous-line (CSI F), and cursor-horizontal-absolute (CSI G) in the console so that multiple `cli` progress bars render on separate lines instead of overwriting each other.
- ([#17225](https://github.com/rstudio/rstudio/issues/17225)): Fix duplicate entries appearing in the recent projects list when the same project was recorded under both aliased (`~/...`) and absolute path forms.
- ([#17777](https://github.com/rstudio/rstudio/issues/17777)): Fix "Import Dataset" from the Environment pane failing with "could not find function '.rs.digest'" when previewing a CSV (or other readr/readxl/haven source).
- ([#3798](https://github.com/rstudio/rstudio/issues/3798)): The Windows terminal now uses the native Windows pseudoconsole (ConPTY) instead of the legacy winpty library, improving Ctrl+C handling in console programs.
- ([#17800](https://github.com/rstudio/rstudio/issues/17800)): Fix the data viewer's horizontal scrollbar staying hidden after returning to the tab, and fix Ctrl+C/Cmd+C copying only the active cell instead of the user's multi-cell text selection.
- ([#17806](https://github.com/rstudio/rstudio/issues/17806)): Fix the data viewer becoming slow to open, scroll, and search -- and freezing for several seconds when opening very wide data frames -- when there are many columns or the summary panel is shown. The grid now renders only the rows and columns currently in view.
- ([#17835](https://github.com/rstudio/rstudio/issues/17835)): Fix a pinned column in the data viewer jumping to a different column after paging to another set of columns in a very wide data frame; the pinned column now stays visible and tracks its original column across column pagination.
- ([#17278](https://github.com/rstudio/rstudio/issues/17278)): Fix a `data.table` returned invisibly from a `:=` update (e.g. `dt[, x := y]`) being auto-printed in notebook chunks, when its output should be suppressed as it is in the console.
- ([#17810](https://github.com/rstudio/rstudio/issues/17810)): Fix the "Run Tests" button modifying the active test file's timestamp even when the file had no unsaved changes.
- ([#17735](https://github.com/rstudio/rstudio/issues/17735)): Hide the redundant column-summary sidebar in the data frame preview shown in the Help pane.
- ([#17066](https://github.com/rstudio/rstudio/issues/17066)): Fix the Assistant preferences pane showing a stale code-assistant account after switching back and forth between GitHub Copilot and Posit Assistant.
- ([#17493](https://github.com/rstudio/rstudio/issues/17493)): Fix the comment/uncomment shortcut inserting the comment character at the start of an empty indented line instead of after the indent.

### Dependencies
- Ace 1.43.5
- Copilot Language Server 1.500.0
- Electron 41.7.0
- Node.js 22.22.2 (copilot, Posit AI)
- Quarto 1.9.38
- xterm.js 6.0.0
