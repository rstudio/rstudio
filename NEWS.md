## RStudio 2026.10.0 "Blue Mistflower" Release Notes

### New
- ([#18692](https://github.com/rstudio/rstudio/issues/18692)): Active document and pane tabs now have a bold label and a blue overline to make them easier to identify. This is enabled by default and can be disabled in Global Options > General > Basic > Other.
- ([#17787](https://github.com/rstudio/rstudio/issues/17787)): Columns in the data viewer can now be hidden and shown from the summary panel, individually or all at once.
- ([#18738](https://github.com/rstudio/rstudio/issues/18738)): Reduced the disk space used by RStudio installations: Copilot Language Server binaries for other platforms are no longer installed, JavaScript source maps are no longer packaged, GWT symbol maps are stored compressed, and macOS application bundles are now transparently compressed on disk.
- ([#18739](https://github.com/rstudio/rstudio/issues/18739)): Optimized RStudio Desktop startup time.
- ([#12838](https://github.com/rstudio/rstudio/issues/12838)): Rendering a Quarto document with the preview option set to "(No Preview)" now runs a one-shot `quarto render` in a background job that finishes when the render does, instead of leaving a `quarto preview` server running.
- ([#14696](https://github.com/rstudio/rstudio/issues/14696)): Added a "Use smooth scrolling" option (Global Options > Code > Display) that animates source editor scrolling when the cursor moves off screen or when jumping to a line.
- ([#13505](https://github.com/rstudio/rstudio/issues/13505)): The document outline is now available for plain Markdown (.md) documents.
- ([#16386](https://github.com/rstudio/rstudio/issues/16386)): The console scrollback limit (Global Options > Console, "Maximum lines of output to keep in the console") now takes effect immediately rather than at the next session start, and the option no longer accepts values below the minimum the session enforces.
- ([#16102](https://github.com/rstudio/rstudio/issues/16102)): Added Code > Select Current Statement (Ctrl+Alt+Shift+S), which selects the whole R statement containing the cursor, including statements that span several lines.
- ([#12223](https://github.com/rstudio/rstudio/issues/12223)): Added Edit > Change Spelling Language... (also in the Command Palette) to switch the spelling dictionary in one step. Dictionary changes now take effect immediately in open documents, without restarting RStudio.
- ([#14226](https://github.com/rstudio/rstudio/issues/14226)): Files can now be opened in the source editor from the Terminal pane with `rstudio <file>[:line[:column]]` (macOS, Linux, and RStudio Server).
- ([#15261](https://github.com/rstudio/rstudio/issues/15261)): The Insert Pipe Operator command (Ctrl+Shift+M) now inserts `+` when the cursor is within a ggplot2 chain. This can be disabled in Global Options > Code > Editing.
- ([#12379](https://github.com/rstudio/rstudio/issues/12379)): Customized editor keyboard shortcuts (e.g. Remove Word Left) now also apply to the Console input.

### Fixed
- ([#18760](https://github.com/rstudio/rstudio/issues/18760)): Fixed an issue where long R version names in Global Options > General could push the R version selector outside the dialog.
- ([#18735](https://github.com/rstudio/rstudio/issues/18735)): Fixed an issue on Windows where RStudio failed to start when the command prompt was disabled by Group Policy.
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
- Electron 43.7.0
- Node.js 24.21.0 (GitHub Copilot, Posit Assistant)

