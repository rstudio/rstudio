## RStudio 2024.04.0 "Chocolate Cosmos" Release Notes

### New
#### RStudio
-

#### Posit Workbench
-

### Fixed
#### RStudio
- Fixed an issue preventing the object explorer from exploring httr request objects. (#13348)
- RStudio will no longer attempt to automatically activate the default system installation of Python. (#13497)
- Improved performance of R Markdown chunk execution for projects running on networked filesystems. (#8034)
- Fixed an issue where underscores in file names were not displayed correctly in menu items. (#13662)
- Fixed an issue where previewed plots were not rendered at the correct DPI. (#13387)
- Fixed an issue where warnings could be emitted when parsing YAML options within R Markdown code chunks. (#13326)
- Fixed an issue where inline YAML chunk options were not properly parsed from SQL chunks. (#13240)
- Fixed an issue where help text in the autocompletion popup was not selectable. (#13674)
- Fixed an issue where a file could be opened twice when debugging functions sourced from other directories. (#13719)
- Fixed an issue preventing publishing standalone Quarto content from within larger Quarto projects. (#13637)
- Fixed an issue that prevented RStudio from opening PDF vignettes from the Help pane. (#13041)
- Inline chunk execution now respects YAML style plot options styled with hyphens. (#11708)
- Fixed a bug where project options updated in the Project Options pane were not properly persisted in RStudio 2023.09.0. (#13757)
- Improved screen reader support when navigating source files in the editor. [accessibility] (#7337)
- Fixed viewing or blaming file on GitHub for a newly created branch. (#9798)
- Fixed an issue on macOS where '-ne' was erroneously printed to the console with certain versions of Bash. (#13809)
- Fixed an issue where attempts to open files containing non-ASCII characters from the Files pane could fail on Windows. (#13855, #12467)
- Fixed an issue where color highlight for Console input could not be disabled. (#13118)
- Fixed an issue that could cause the RStudio IDE to crash if a large amount of Console output was serialized with a suspended session. (#13857)
- RStudio now records the deployment target for newly-published documents, even when deployment fails due to an error in the document. (#12707)
- Fixed an issue where Find in Files results were not presented in rare cases. (#12657)
- Fixed an issue that could cause errors to occur if an R Markdown document was saved while a chunk was running. (#13860)
- Fixed an issue where console output could be dropped when rendering large ANSI links. (#13869)
- Fixed an issue preventing users from copying code from the History pane. (#3219)
- Fixed WSL terminals not starting on RStudio Desktop for Windows. (#13918)
- Fixed Windows installer to delete Start Menu shortcut during uninstall (#13936)
- Fixed an issue that prevented users from opening files and vignettes with non-ASCII characters in their paths. (#13886)
- Fixed an issue where large, heavily-nested objects could slow down code execution in the R session. (#13965)
- Fixed performance problem locating RStudio projects that live under a large directory tree (rstudio-pro#5435)
- Session Protocol Debug in Tools -> Command Palette turns on log-level=debug when set (rstudio-pro#5095)
- Reduce overhead of session suspension checks and writes to the executing file (#13534, rstudio-pro#4922)
- Reset session keyring on linux platforms to ensure credentials added in a session are isolated (rstudio-pro#5485)
-

#### Posit Workbench
-

