## RStudio 2026.09.0.0 "Autumn Hawkbit" Release Notes

### New
- ([#18649](https://github.com/rstudio/rstudio/issues/18649)): Renamed references to the Posit AI service to Posit AI Pass in the IDE user interface.
- ([#2129](https://github.com/rstudio/rstudio/issues/2129)): Added horizontal and vertical split source editor views, with shared edits and independent navigation state.

### Fixed
- ([#18635](https://github.com/rstudio/rstudio/issues/18635)): Running code from a popped-out source window no longer moves focus to the console when "Focus console after executing code from the source pane" is off.
- ([#18356](https://github.com/rstudio/rstudio/issues/18356)): Declining the Posit Assistant install prompt now restores the previously selected code assistant.
- ([#18631](https://github.com/rstudio/rstudio/issues/18631)): RStudio Desktop now shows What's New once per release instead of after every patch update.
- ([#18602](https://github.com/rstudio/rstudio/issues/18602)): Fixed folding and outline hierarchy for section headers decorated with `#` characters.
- ([#18597](https://github.com/rstudio/rstudio/issues/18597)): Posit Assistant code suggestions now honor `RSTUDIO_POSIT_AI_PATH` and system-wide installations.
- ([#18451](https://github.com/rstudio/rstudio/issues/18451)): Truncated AI assistant startup errors and logs to avoid dumping minified bundles while retaining useful diagnostics.
- ([#18570](https://github.com/rstudio/rstudio/issues/18570)): Posit Assistant manifest download errors now show the underlying DNS, proxy, or TLS failure.
- ([#18610](https://github.com/rstudio/rstudio/issues/18610)): Posit Assistant install and update downloads now preserve detailed errors, report warning-only failures, and honor configured timeouts.
- ([#18531](https://github.com/rstudio/rstudio/issues/18531)): Fixed breakpoints in methods registered on S7 generics.
- ([#18572](https://github.com/rstudio/rstudio/issues/18572)): Prevented RStudio Server session relaunches from starting duplicate orphaned sessions.
- ([#18571](https://github.com/rstudio/rstudio/issues/18571)): Fixed Posit Assistant chat incorrectly remaining in an "update in progress" state after an R session restart.
- ([#18425](https://github.com/rstudio/rstudio/issues/18425)): Fixed snippet help and insertion errors when the cursor moves between language modes.
- ([#18526](https://github.com/rstudio/rstudio/issues/18526)): Fixed S7 method breakpoints on R versions before 4.3.0 and for package-scoped classes.
- ([#18559](https://github.com/rstudio/rstudio/issues/18559)): The Plots pane now activates after a busy command finishes even when the plot was rendered earlier.
- ([#18533](https://github.com/rstudio/rstudio/issues/18533)): Prevented spurious "Error Listing Objects" dialogs after RStudio Server session restarts.
- ([#18527](https://github.com/rstudio/rstudio/issues/18527)): On Windows, environment variable updates and removals made by RStudio are now visible to R and in-process libraries.
- ([#18527](https://github.com/rstudio/rstudio/issues/18527)): On Windows, sessions started in untrusted directories no longer read user-level `.Renviron` files.
- ([#10756](https://github.com/rstudio/rstudio/issues/10756)): Improved crash reporting and thread safety during session startup.
- ([#18507](https://github.com/rstudio/rstudio/issues/18507)): Reduced macOS file-monitor overload by excluding high-churn and nested-worktree directories, batching events, and debouncing recovery scans.
- ([#18515](https://github.com/rstudio/rstudio/issues/18515)): Restored 32-bit session binaries to the Windows installer.
- ([#18512](https://github.com/rstudio/rstudio/issues/18512)): Added a code signature to `rsession.dll` on Windows.
- ([#18493](https://github.com/rstudio/rstudio/issues/18493)): Fixed invisible checkbox and radio button states in Windows high-contrast dark themes.
- ([#18474](https://github.com/rstudio/rstudio/issues/18474)): Fixed a math-preview keyboard handler leak that consumed subsequent Escape keypresses.
- ([#17806](https://github.com/rstudio/rstudio/issues/17806)): Improved Data Viewer searching, filtering, scrolling, and Summary panel performance.
- ([#17806](https://github.com/rstudio/rstudio/issues/17806)): Fixed a Data Viewer memory leak when viewed objects change.
- ([#18472](https://github.com/rstudio/rstudio/issues/18472)): Fixed stale syntax highlighting after edits in Markdown and YAML documents.
- ([#18447](https://github.com/rstudio/rstudio/issues/18447)): Console line-navigation shortcuts now move or select to the ends of the whole command instead of the wrapped visual line.
- ([#18469](https://github.com/rstudio/rstudio/issues/18469)): Fixed R raw strings corrupting editor state and breaking later header folds.
- ([#18468](https://github.com/rstudio/rstudio/issues/18468)): Fixed indentation in C++ chunks and formatting of Quarto `#|` comment lines.
- ([#18464](https://github.com/rstudio/rstudio/issues/18464)): Fixed rainbow fenced-div colors to reflect nesting.
- ([#18501](https://github.com/rstudio/rstudio/issues/18501)): Restricted localhost proxy resolution to the requested IP address family.
- ([#18444](https://github.com/rstudio/rstudio/issues/18444)): Fixed repeated pane-layout collapse caused by stale zoom state.
- ([#18448](https://github.com/rstudio/rstudio/issues/18448)): Fixed pane zoom state after column, sidebar, source-column, and pane-size changes.
- ([#18482](https://github.com/rstudio/rstudio/issues/18482)): Prevented spurious Windows crash reports when closing an R session with a running terminal.
- ([#18546](https://github.com/rstudio/rstudio/issues/18546)): RStudio Server's localhost proxy now streams response bodies instead of buffering them in memory.

### Dependencies
- Copilot Language Server 1.531.0
- Electron 42.10.1
- Quarto 1.10.18

### Deprecated / Removed
- ([#18605](https://github.com/rstudio/rstudio/issues/18605)): Marked RPubs and ShinyApps.io publishing destinations as deprecated ahead of their sunset; see [the migration guide](https://posit.co/blog/migrating-connect-cloud-posits-unified-publishing-solution).
- ([#18658](https://github.com/rstudio/rstudio/issues/18658)): Removed the "Uninstall Posit Assistant" command.
