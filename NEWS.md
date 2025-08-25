## RStudio 2025.11.0 "Apple Blossom" Release Notes

### New
#### RStudio
- ([#16127](https://github.com/rstudio/rstudio/issues/16127)) ESC key now dismisses Copilot ghost text in source editor
- ([#16281](https://github.com/rstudio/rstudio/issues/16281)): RStudio will no longer perform type inference on the completion results provided by .DollarNames methods for values where `attr(*, "suppressTypeInference")` is `TRUE`.

#### Posit Workbench
- ([#16218](https://github.com/rstudio/rstudio/issues/16218)) Workbench no longer uses Crashpad for collecting crash dumps

### Fixed
#### RStudio
- ([#16320](https://github.com/rstudio/rstudio/issues/16320)): Fixed message shown when ssh keyphrases don't match
- ([#16331](https://github.com/rstudio/rstudio/issues/16331)): RStudio no longer removes previously-registered global calling handlers on startup
- ([#13470](https://github.com/rstudio/rstudio/issues/13470)): Avoid printing positioning data when creating patchwork objects in R Markdown chunks
- ([#16337](https://github.com/rstudio/rstudio/issues/16337)): Fixed an issue where R error output was not displayed in rare cases
- ([#15963](https://github.com/rstudio/rstudio/issues/15963)): Fixed an issue where an unsaved R Markdown document could erroneously be marked as saved after executing a chunk

#### Posit Workbench
- (#rstudio-pro/8919): Fixed an issue where duplicate project entries within a user's recent project list could cause their home page to fail to load

### Dependencies
- Copilot Language Server 1.362.0
- Electron 37.3.0
