## RStudio 2026.05.0 "Golden Wattle" Release Notes

### New
- ([#17539](https://github.com/rstudio/rstudio/issues/17539)): The data viewer is now faster and supports pinnable columns, a Summary sidebar with type-aware stats and sparkline histograms, keyboard navigation, and clipboard copy.
- ([#17539](https://github.com/rstudio/rstudio/issues/17539)): Added the `data_viewer_show_summary` user preference, which controls the default visibility of the data viewer's Summary sidebar.
- ([#17539](https://github.com/rstudio/rstudio/issues/17539)): Raised the default value of `data_viewer_max_columns` from 50 to 200.
- ([#17477](https://github.com/rstudio/rstudio/issues/17477)): Posit Assistant: the `ui/openDocument` JSON-RPC method now accepts an optional 1-based `line` parameter, and RStudio advertises the `ui/openDocument/line` capability so the assistant can open documents at a specific line.
- ([#17478](https://github.com/rstudio/rstudio/issues/17478)): Posit Assistant: added the `ui/revealInFilesPane` JSON-RPC method, which navigates the Files pane to a directory (or to a file's parent directory) and brings the pane to the front.
- ([#17479](https://github.com/rstudio/rstudio/issues/17479)): Posit Assistant: added the `ui/previewUrl` JSON-RPC method, which navigates the Viewer pane to an `http(s)` URL (e.g., a local Shiny app); supports an optional `height` parameter that mirrors `rstudioapi::viewer()` (`-1` for maximize, `0` for no change, positive for pixels).
- ([#17514](https://github.com/rstudio/rstudio/issues/17514)): Added the `allow-package-source-recording` session option (default `true`); when set to `false`, RStudio will not annotate DESCRIPTION files of packages installed via `install.packages()` with the remote repository they came from.

### Fixed
- ([#17481](https://github.com/rstudio/rstudio/issues/17481)): Fixed two debugger regressions: top-level breakpoints (e.g. via `debugSource()`) no longer fail to show the debug highlight or call stack, and multi-line input at the `Browse[N]>` prompt no longer clears the captured browser context.
- ([#3780](https://github.com/rstudio/rstudio/issues/3780)): The Files pane delete confirmation now reflects whether the file will be moved to the system Trash/Recycle Bin or permanently deleted, based on the "Delete files to Trash/Recycle Bin" preference.
- ([#3780](https://github.com/rstudio/rstudio/issues/3780)): When sending a file to the system Trash/Recycle Bin fails, the Files pane now reports the error and leaves the file on disk; previously it would silently fall back to permanently deleting the file.
- ([#12235](https://github.com/rstudio/rstudio/issues/12235)): RStudio Desktop's Session > New Session now opens noticeably faster.
- ([#15614](https://github.com/rstudio/rstudio/issues/15614)): The splash screen can again be dismissed with a mouse click or key press.
- ([#16067](https://github.com/rstudio/rstudio/issues/16067)): Raise the open file descriptor soft limit at session startup to avoid "Too many open files" errors during project file monitoring on Linux.
- ([#16541](https://github.com/rstudio/rstudio/issues/16541)): Section headers now fold hierarchically based on heading level, matching Positron's default behavior.
- ([#16966](https://github.com/rstudio/rstudio/issues/16966)): Restored the desktop terminal bell on Linux, now that the underlying Electron crash has been fixed.
- ([#17084](https://github.com/rstudio/rstudio/issues/17084)): The Shiny test commands (Record Test, Run Tests, Compare Results) now use the `shinytest2` package; `shinytest` has been deprecated.
- ([#17128](https://github.com/rstudio/rstudio/issues/17128)): The Windows installer's File Version now matches its Product Version; previously the installer concatenated the major and minor version components into a single value that exceeded NSIS's 16-bit-per-component limit and was truncated to an unrelated number.
- ([#17176](https://github.com/rstudio/rstudio/issues/17176)): Fixed a startup hang when opening a Quarto project containing large directories (e.g. `_targets/`).
- ([#17322](https://github.com/rstudio/rstudio/issues/17322)): Posit Assistant: invoking Uninstall Posit Assistant when it is not installed now shows the message "Posit Assistant is not installed." and skips the restart, instead of silently restarting RStudio.
- ([#17417](https://github.com/rstudio/rstudio/issues/17417)): Added missing French translations for newer commands and preferences, removed an orphaned French key, deduplicated stale entries in the French application strings, and normalized line endings in five English string files.
- ([#17440](https://github.com/rstudio/rstudio/issues/17440)): Fixed an issue where triggering tab completion inside `[` on a large Matrix-package sparse matrix could hang RStudio and exhaust system memory.
- ([#17506](https://github.com/rstudio/rstudio/issues/17506)): Source-marker messages in the Build, Markers, and Compile PDF panes are now rendered as plain text by default; only messages that the server explicitly marks as HTML (e.g. C++ Find Usages highlighting) are rendered via `innerHTML`.
- ([#17508](https://github.com/rstudio/rstudio/issues/17508)): Enabled the MathJax `Safe` extension in the IDE, the HTML preview, the presentation preview, and the rendered R Markdown viewer.
- ([#17510](https://github.com/rstudio/rstudio/issues/17510)): Data Import: column names, character options, locale values, and import URLs are now encoded as R string literals when generating preview and import code.
- ([#17530](https://github.com/rstudio/rstudio/issues/17530)): The spell-check context menu now shows the full set of correction suggestions; previously a stray loop increment caused every other suggestion to be skipped, capping the menu at 3 items instead of 5.
- ([#17534](https://github.com/rstudio/rstudio/issues/17534)): Fix uninformative error in `rstudio::sourceMarkers()` when `marker$type` is not a length-one vector.
- ([#4402](https://github.com/rstudio/rstudio/issues/4402)): Reindenting C/C++ code with a brace-less control-flow statement (e.g. a `for` whose body is a single braced `if`) no longer over-indents the line that follows the body.
- ([rstudio/rstudio-pro#10805](https://github.com/rstudio/rstudio-pro/issues/10805)): Server: Enable TCP keepalive on accepted connections so the operating system reaps half-open sockets from disappeared clients (browser tab hibernation, NAT timeouts) instead of holding them indefinitely.
- ([rstudio/rstudio-pro#10771](https://github.com/rstudio/rstudio-pro/issues/10771)): Reduced filesystem work performed by the `install.packages()` hook; the before/after scan is now scoped to the requested packages and their dependency closure rather than the entire library.
- ([rstudio/rstudio-pro#10771](https://github.com/rstudio/rstudio-pro/issues/10771)): Tightened the heuristic used to detect package-management commands at the console prompt, reducing spurious Packages pane refreshes triggered by identifiers like `updates <-` or `installed.packages()`.

### Dependencies
- Ace 1.43.5
- Copilot Language Server 1.480.0
- Electron 41.5.0
- Node.js 22.22.2 (copilot, Posit AI)
- Quarto 1.9.37
- xterm.js 6.0.0
