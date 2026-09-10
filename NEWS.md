## RStudio 2026.10.0 "Blue Mistflower" Release Notes

### New
- ([#18692](https://github.com/rstudio/rstudio/issues/18692)): Active document and pane tabs now have a bold label and a blue overline to make them easier to identify. This is enabled by default and can be disabled in Global Options > General > Basic > Other.
- ([#17787](https://github.com/rstudio/rstudio/issues/17787)): Columns in the data viewer can now be hidden and shown from the summary panel, individually or all at once.
- ([#18738](https://github.com/rstudio/rstudio/issues/18738)): Reduced the disk space used by RStudio installations: Copilot Language Server binaries for other platforms are no longer installed, JavaScript source maps are no longer packaged, GWT symbol maps are stored compressed, and macOS application bundles are now transparently compressed on disk.
- ([#18739](https://github.com/rstudio/rstudio/issues/18739)): Optimized RStudio Desktop startup time.
- ([#11622](https://github.com/rstudio/rstudio/issues/11622)): A minimized Console pane is minimized again after a successful render (R Markdown or Quarto) instead of staying open; it stays open when the render fails so the output can be inspected.

### Fixed
- ([#18744](https://github.com/rstudio/rstudio/issues/18744)): RStudio Desktop now uses link-based file locks by default on macOS and Linux, matching RStudio Server, so sessions from both editions sharing a data directory or a project on a network drive no longer mistake each other's live locks for stale ones.
- ([#18677](https://github.com/rstudio/rstudio/issues/18677)): Fixed Windows subprocess detection reporting unrelated processes as children after process ID reuse.
- ([#18742](https://github.com/rstudio/rstudio/issues/18742)): Fixed an issue where the Global Options dialog could push the OK and Apply buttons off screen in short windows.
- ([#18736](https://github.com/rstudio/rstudio/issues/18736)): Fixed an issue where delayed document outline initialization could reactivate a closed Source column and leave a new document without an active editor
- ([#18718](https://github.com/rstudio/rstudio/issues/18718)): Fixed an issue where an R error during session initialization could leave the session hung forever at startup, with RStudio Server reporting that R was taking longer than usual to start.
- ([#14363](https://github.com/rstudio/rstudio/issues/14363)): Fixed an issue where hexadecimal literals with binary exponents (e.g. `0x1p3`), fractions, uppercase `0X` prefixes, or an imaginary suffix were reported as parse errors
- ([#18717](https://github.com/rstudio/rstudio/issues/18717)): Fixed an issue where the diagnostics system reported a spurious parse error for indexed numeric literals, e.g. `1[TRUE]`
- ([#18722](https://github.com/rstudio/rstudio/issues/18722)): Fixed an issue where the diagnostics system reported "unexpected end of document" for R scripts ending with a semicolon

### Dependencies
- Copilot Language Server 1.544.0
- Node.js 24.21.0 (GitHub Copilot, Posit Assistant)

