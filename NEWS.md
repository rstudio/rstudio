## RStudio 2026.07.0 "Pacific Dogwood" Release Notes

### New
- ([#17992](https://github.com/rstudio/rstudio/issues/17992)): The Find in Files replace preview and Replace All results no longer present matches whose proposed replacement is identical to the matched text; lines whose every match would be unchanged are omitted entirely.
- ([#10417](https://github.com/rstudio/rstudio/issues/10417)): RStudio can now reduce background file operations (recursive file monitoring, code indexing, and external-edit checks) for projects located on network or remote filesystems, improving responsiveness on slow drives. This applies automatically when a remote filesystem is detected, and can be configured globally (Tools > Global Options > General > Advanced) or overridden per-project (Project Options > General).

### Fixed
- ([#18033](https://github.com/rstudio/rstudio/issues/18033)): Fix ghost text set via `rstudioapi::setGhostText()` not being dismissed when navigating the cursor away or typing a non-matching character, which left it insertable by Tab even after it appeared to be gone.
- ([#17970](https://github.com/rstudio/rstudio/issues/17970)): Fix the Packages pane vulnerability dialog listing vulnerabilities that affect a different installed version of the clicked package; the dialog now shows only the vulnerabilities for that row's version. A package with vulnerability records from multiple repositories now shows a single combined warning icon and dialog instead of one per repository.
- ([#17985](https://github.com/rstudio/rstudio/issues/17985)): Fixed an issue where the Import Dataset preview dialog failed to display error messages when the readr preview subprocess encountered an error, causing the dialog to hang indefinitely waiting for data that would never arrive.
- ([#18003](https://github.com/rstudio/rstudio/issues/18003)): Fixed an issue where reformat-on-save with Air configured as an external formatter ignored the project's air.toml configuration.
- ([#6147](https://github.com/rstudio/rstudio/issues/6147)): The data viewer now scrolls continuously through the columns of a very wide data frame instead of paging through fixed blocks of columns, so a column's filter, sort, and pin are no longer limited to (or lost when leaving) the currently visible set of columns.
- ([#6147](https://github.com/rstudio/rstudio/issues/6147)): The data viewer's column-pagination arrows are replaced by a "Go to column" box in the toolbar that jumps to a column by name or number.
- ([#6147](https://github.com/rstudio/rstudio/issues/6147)): The data viewer's summary sidebar now lists every column of the frame, loading each column's summary as it scrolls into view.
- ([#6147](https://github.com/rstudio/rstudio/issues/6147)): The data viewer's summary sidebar now reflects the active filter and search, rather than always describing the whole frame.
- ([#17979](https://github.com/rstudio/rstudio/issues/17979)): Add a "Show Filter UI by default" toggle to the data viewer's Settings menu. When enabled, the filter bar is shown automatically each time a data frame is opened, instead of requiring a click to reveal it.
- ([#17993](https://github.com/rstudio/rstudio/issues/17993)): Add a "Use overlay scrollbars" toggle to the data viewer's Settings menu. When disabled, the data viewer uses native, always-visible scrollbars instead of the auto-hiding overlay scrollbars.
- ([#17807](https://github.com/rstudio/rstudio/issues/17807)): Fixed a startup stall and missing package vulnerability badges that could occur when an intermediary (such as a proxy) kept the connection to Posit Package Manager open after responding. RStudio's HTTP client now completes a response as soon as its full Content-Length body has been received, rather than waiting for the connection to close.
- ([#18019](https://github.com/rstudio/rstudio/issues/18019)): Fixed an issue where a transient connection failure while writing user state during startup could leave the IDE stuck on an empty grey screen.

### Dependencies
- Ace 1.43.5
- Copilot Language Server 1.509.1
- Electron 41.9.0
- Node.js 22.22.2 (copilot, Posit Assistant)
- Quarto 1.9.38
- xterm.js 6.0.0
