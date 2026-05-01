## RStudio 2026.05.0 "Golden Wattle" Release Notes

### New
- Added the `allow-package-source-recording` session option (default
  `true`); when set to `false`, RStudio will not annotate DESCRIPTION
  files of packages installed via `install.packages()` with the remote
  repository they came from.

### Fixed
- Reduced filesystem work performed by the `install.packages()` hook; the
  before/after scan is now scoped to the requested packages and their
  dependency closure rather than the entire library.
- Tightened the heuristic used to detect package-management commands at the
  console prompt, reducing spurious Packages pane refreshes triggered by
  identifiers like `updates <-` or `installed.packages()`.
- ([#15614](https://github.com/rstudio/rstudio/issues/15614)): The splash screen can again be dismissed with a mouse click or key press.
- ([#16541](https://github.com/rstudio/rstudio/issues/16541)): Section headers now fold hierarchically based on heading level, matching Positron's default behavior

### Fixed
- ([#17084](https://github.com/rstudio/rstudio/issues/17084)): The Shiny test commands (Record Test, Run Tests, Compare Results) now use the `shinytest2` package; `shinytest` has been deprecated.

### Fixed
- 
- ([rstudio/rstudio-pro#10805](https://github.com/rstudio/rstudio-pro/issues/10805)): Server: Enable TCP keepalive on accepted connections so the operating system reaps half-open sockets from disappeared clients (browser tab hibernation, NAT timeouts) instead of holding them indefinitely.
- ([#12235](https://github.com/rstudio/rstudio/issues/12235)): RStudio Desktop's Session > New Session now opens noticeably faster.
- ([#17440](https://github.com/rstudio/rstudio/issues/17440)): Fixed an issue where triggering tab completion inside `[` on a large Matrix-package sparse matrix could hang RStudio and exhaust system memory
- ([#17176](https://github.com/rstudio/rstudio/issues/17176)): Fixed a startup hang when opening a Quarto project containing large directories (e.g. `_targets/`).
- ([#16067](https://github.com/rstudio/rstudio/issues/16067)): Raise the open file descriptor soft limit at session startup to avoid "Too many open files" errors during project file monitoring on Linux.
- ([#17417](https://github.com/rstudio/rstudio/issues/17417)): Added missing French translations for newer commands and preferences, removed an orphaned French key, deduplicated stale entries in the French application strings, and normalized line endings in five English string files.
- ([#16966](https://github.com/rstudio/rstudio/issues/16966)): Restored the desktop terminal bell on Linux, now that the underlying Electron crash has been fixed.
- ([#3780](https://github.com/rstudio/rstudio/issues/3780)): The Files pane delete confirmation now reflects whether the file will be moved to the system Trash/Recycle Bin or permanently deleted, based on the "Delete files to Trash/Recycle Bin" preference.
- ([#3780](https://github.com/rstudio/rstudio/issues/3780)): When sending a file to the system Trash/Recycle Bin fails, the Files pane now reports the error and leaves the file on disk; previously it would silently fall back to permanently deleting the file.

### Dependencies
- Ace 1.43.5
- Copilot Language Server 1.480.0
- Electron 41.3.0
- Node.js 22.22.2 (copilot, Posit AI)
- Quarto 1.9.37
- xterm.js 6.0.0
