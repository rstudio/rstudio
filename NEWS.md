## RStudio 2025.11.0 "Apple Blossom" Release Notes

### New
#### RStudio
- ([#10567](https://github.com/rstudio/rstudio/issues/10567)): RStudio now provides tools for searching within Console output and Build output
- ([#15928](https://github.com/rstudio/rstudio/issues/15928)): RStudio now uses Air for formatting R code in projects configured to use Air
- ([#16127](https://github.com/rstudio/rstudio/issues/16127)): ESC key now dismisses Copilot ghost text in source editor
- ([#16415](https://github.com/rstudio/rstudio/issues/16415)): RStudio now provides completions for the `_` placeholder in piped expressions
- ([#16281](https://github.com/rstudio/rstudio/issues/16281)): RStudio will no longer perform type inference on the completion results provided by .DollarNames methods for values where `attr(*, "suppressTypeInference")` is `TRUE`
- ([#16375](https://github.com/rstudio/rstudio/issues/16375)): Copilot Language Server (completions) is now launched via node.js instead of as a standalone binary
- ([#16427](https://github.com/rstudio/rstudio/issues/16427)): RStudio now provided a diagnostic warning for invocations of `paste()` with unexpected named arguments
- ([#16483](https://github.com/rstudio/rstudio/issues/16483)): In R Markdown / Quarto documents, RStudio now only used a paged-table view for auto-printed data objects; explicit invocations of `print()` will use the existing print method
- ([#16480](https://github.com/rstudio/rstudio/issues/16480)): RStudio now has a full-height "Sidebar" pane
- ([#16435](https://github.com/rstudio/rstudio/issues/16435)): The visibility of the Source column in the Packages pane can now be toggled via user preference

#### Posit Workbench
- ([#16218](https://github.com/rstudio/rstudio/issues/16218)) Workbench no longer uses Crashpad for collecting crash dumps

### Fixed
#### RStudio
- ([#16398](https://github.com/rstudio/rstudio/issues/16398)): Fixed issue with malformed ANSI codes being presented in warning messages captured while rendering plots
- ([#16320](https://github.com/rstudio/rstudio/issues/16320)): Fixed message shown when ssh keyphrases don't match
- ([#16331](https://github.com/rstudio/rstudio/issues/16331)): RStudio no longer removes previously-registered global calling handlers on startup
- ([#13470](https://github.com/rstudio/rstudio/issues/13470)): Avoid printing positioning data when creating patchwork objects in R Markdown chunks
- ([#16337](https://github.com/rstudio/rstudio/issues/16337)): Fixed an issue where R error output was not displayed in rare cases
- ([#15963](https://github.com/rstudio/rstudio/issues/15963)): Fixed an issue where an unsaved R Markdown document could erroneously be marked as saved after executing a chunk
- ([#16402](https://github.com/rstudio/rstudio/issues/16402)): Fixed an issue where the wrong Python installation was chosen during Quarto render in some cases
- ([#16457](https://github.com/rstudio/rstudio/issues/16457)): Adjusted width of the standalone Accessibility Options dialog to fully display the pane
- ([#16532](https://github.com/rstudio/rstudio/issues/16352)): Fixed an issue where ongoing R Markdown render output was lost when after a browser refresh
- ([#14510](https://github.com/rstudio/rstudio/issues/14510)): Fixed issue where gutter icons were incorrectly presented for spellcheck issues
- ([#16474](https://github.com/rstudio/rstudio/issues/16474)): Adjusted Pane Layout options UI to improve space utilization
- ([#16471](https://github.com/rstudio/rstudio/issues/16471)): Fixed issue where Copilot status messages appeared below editor when Copilot was disabled
- ([#16423](https://github.com/rstudio/rstudio/issues/16423)): Fixed issue where Copilot gave a warning when closing a document it was told to ignore
- ([#16509](https://github.com/rstudio/rstudio/issues/16509)): Fixed issue where Copilot gave a warning when closing an unmodified empty new document
- ([#16485](https://github.com/rstudio/rstudio/issues/16485)): Removed inoperative min/max controls from Source Columns
- ([#16516](https://github.com/rstudio/rstudio/issues/16516)): Fixed issue with extraneous space in object size for large objects in Environment pane
- ([#16542](https://github.com/rstudio/rstudio/issues/16542)): Improved accessibility of column splitters by adding accessible labels for screen readers
- ([#14683](https://github.com/rstudio/rstudio/issues/14683)): Changed RStudio Desktop on Linux to default to using native file and message dialog boxes
- ([#15340](https://github.com/rstudio/rstudio/issues/15340)): Fixed issue where native file dialogs on RStudio Desktop on Linux wouldn't show .Rproj files

#### Posit Workbench
- (#rstudio-pro/8919): Fixed an issue where duplicate project entries within a user's recent project list could cause their home page to fail to load
- (#rstudio-pro/8846): Improved contrast of check boxes on Workbench homepage to meet 3:1 minimum contrast (AA)

### Dependencies
- Copilot Language Server 1.381.0
- Electron 38.2.2
- Node.js 22.18.0
- Quarto 1.8.24
