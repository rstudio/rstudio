## RStudio 2025.11.0 "Apple Blossom" Release Notes

### New
#### RStudio
- ([#15928](https://github.com/rstudio/rstudio/issues/15928)): RStudio now uses Air for formatting R code in projects configured to use Air
- ([#16127](https://github.com/rstudio/rstudio/issues/16127)) ESC key now dismisses Copilot ghost text in source editor
- ([#16415](https://github.com/rstudio/rstudio/issues/16415)): RStudio now provides completions for the `_` placeholer in piped expressions
- ([#16281](https://github.com/rstudio/rstudio/issues/16281)): RStudio will no longer perform type inference on the completion results provided by .DollarNames methods for values where `attr(*, "suppressTypeInference")` is `TRUE`.
- ([#16375](https://github.com/rstudio/rstudio/issues/16375)) Copilot Language Server (completions) is now launched via node.js instead of as a standalone binary

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
- ([#16532](https://github.com/rstudio/rstudio/issues/16352)): Fixed an issue where ongoing R Markdown render output was lost when after a browser refresh

#### Posit Workbench
- (#rstudio-pro/8919): Fixed an issue where duplicate project entries within a user's recent project list could cause their home page to fail to load

### Dependencies
- Copilot Language Server 1.364.0
- Electron 37.3.1
- Node.js 22.18.0
