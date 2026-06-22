## RStudio 2026.07.0 "Pacific Dogwood" Release Notes

### New
- ([#17992](https://github.com/rstudio/rstudio/issues/17992)): The Find in Files replace preview and Replace All results no longer present matches whose proposed replacement is identical to the matched text; lines whose every match would be unchanged are omitted entirely.
- ([#15830](https://github.com/rstudio/rstudio/issues/15830)): Add a Code menu and command-palette entry to re-enable the editor's missing-package banner for a file after dismissing it, and rename the banner's dismissal label to "Don't show for this file" to make the per-file scope explicit.
- ([#17734](https://github.com/rstudio/rstudio/issues/17734)): Support em dashes and box-drawing characters as native R code section delimiters.
- ([#8541](https://github.com/rstudio/rstudio/issues/8541)): Add a "Change active editor tab with mouse wheel" preference (General > Basic > Other) to disable switching the active editor tab by scrolling the mouse wheel over the tab bar.
- ([#2350](https://github.com/rstudio/rstudio/issues/2350)): Add an Appearance pane to Project Options for setting a project-specific editor theme; leaving it at "(Default)" uses the global theme. A new "Ignore project-specific appearance settings" option (Global Options > Appearance) keeps the global editor theme even when a project sets its own.
- ([#17923](https://github.com/rstudio/rstudio/pull/17923)): Posit Assistant now runs R code expression by expression, interleaving each expression with its output in the chat pane, rather than running the whole block at once and showing all output at the end.
- ([#14965](https://github.com/rstudio/rstudio/issues/14965)): The Insert Pipe Operator command (Ctrl+Shift+M / Cmd+Shift+M) now inserts the native R pipe operator `|>` by default; the magrittr pipe `%>%` can be restored via the "Use R's native pipe operator, |>" preference.
- ([#17501](https://github.com/rstudio/rstudio/issues/17501)): Move the Find in Files refresh button to the right side of the toolbar, matching the convention used by other panes, and make the replace preview's match highlight colors theme-aware so they no longer look out of place under dark editor themes.
- ([#17912](https://github.com/rstudio/rstudio/issues/17912)): Add a `project-trust-required` session option (off by default) that treats all projects as untrusted by default -- including projects in the user's home directory -- prompting users to trust each project when opened even if it contains no auto-executing files. Additionally, the visual editor is now disabled in untrusted projects, whether or not this option is set; only the source editor is available for markdown documents.
- ([#17748](https://github.com/rstudio/rstudio/issues/17748)): When "Use Air for code formatting" is enabled, Air now formats R documents even when no `air.toml` file is present, using the editor's indentation settings (indent style and width). The previous behavior of formatting only in projects containing an `air.toml` can be restored with the new "Only use Air when an air.toml file is found" option (Global Options > Code > Formatting).

### Fixed
- ([#17985](https://github.com/rstudio/rstudio/issues/17985)): Fixed an issue where the Import Dataset preview dialog failed to display error messages when the readr preview subprocess encountered an error, causing the dialog to hang indefinitely waiting for data that would never arrive.
- ([#18003](https://github.com/rstudio/rstudio/issues/18003)): Fixed an issue where reformat-on-save with Air configured as an external formatter ignored the project's air.toml configuration.
- ([#6147](https://github.com/rstudio/rstudio/issues/6147)): The data viewer now scrolls continuously through the columns of a very wide data frame instead of paging through fixed blocks of columns, so a column's filter, sort, and pin are no longer limited to (or lost when leaving) the currently visible set of columns.
- ([#6147](https://github.com/rstudio/rstudio/issues/6147)): The data viewer's column-pagination arrows are replaced by a "Go to column" box in the toolbar that jumps to a column by name or number.
- ([#6147](https://github.com/rstudio/rstudio/issues/6147)): The data viewer's summary sidebar now lists every column of the frame, loading each column's summary as it scrolls into view.
- ([#6147](https://github.com/rstudio/rstudio/issues/6147)): The data viewer's summary sidebar now reflects the active filter and search, rather than always describing the whole frame.
- ([#17979](https://github.com/rstudio/rstudio/issues/17979)): Add a "Show Filter UI by default" toggle to the data viewer's Settings menu. When enabled, the filter bar is shown automatically each time a data frame is opened, instead of requiring a click to reveal it.
- ([#17993](https://github.com/rstudio/rstudio/issues/17993)): Add a "Use overlay scrollbars" toggle to the data viewer's Settings menu. When disabled, the data viewer uses native, always-visible scrollbars instead of the auto-hiding overlay scrollbars.
- ([#18019](https://github.com/rstudio/rstudio/issues/18019)): Fixed an issue where a transient connection failure while writing user state during startup could leave the IDE stuck on an empty grey screen.

### Dependencies
- Ace 1.43.5
- Copilot Language Server 1.500.0
- Electron 41.7.2
- Node.js 22.22.2 (copilot, Posit AI)
- Quarto 1.9.38
- xterm.js 6.0.0
