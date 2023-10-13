## RStudio 2023.12.0 "Ocean Storm" Release Notes

### New
#### RStudio
- Updated Ace to version 1.28. (#13708)
- Updated Boost to version 1.83.0. (#13577)
- Updated Electron to version 26.2.4. (#13577)
- Updated GWT to version 2.10.0 (#11505)
- RStudio now supports highlighting of inline YAML chunk options in R Markdown / Quarto documents. (#11663)
- Improved support for development documentation when a package has been loaded via `devtools::load_all()`. (#13526)
- RStudio now supports autocompletion following `@` via `.AtNames`. (#13451)
- RStudio now supports the execution and display of GraphViz (`dot`) graphs in R Markdown / Quarto chunks. (#13187)
- RStudio now supports the execution of chunks with the 'file' option set. (#13636)
- With screen reader support enabled, hitting ESC key allows Tabbing away from editor. [accessibility] (#13593)

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

#### Posit Workbench
- Fixed opening job details in new windows more than once for Workbench jobs on the homepage. (rstudio/rstudio-pro#5179)
- Fixed accessibility issues with empty Session and Project lists on the homepage. [accessibility] (rstudio/rstudio-pro#5214)
- Fixed accessibility issues with Project controls on the homepage when not using launcher sessions. [accessibility] (rstudio/rstudio-pro#5215)
- Fixed unlabeled input field in Rename Session modal dialog on the homepage. [accessibility] (rstudio/rstudio-pro#5178)
- Fixed mismatched label on "Join session when ready" checkbox in New Session dialog. [accessibility] (rstudio/rstudio-pro#5221)
- Fixed issue that caused start up crash in environments with encrypted database passwords. (rstudio-pro#5228)
- Fixed an issue where Shift+Tab wouldn't wrap focus in the New Session dialog. [accessibility] (rstudio-pro#4488)
- Fixed an issue where sessions couldn't be started with keyboard with Kubernetes or Slurm. [accessibility] (rstudio-pro#4360)
- Fixed label on button for showing and hiding launcher details so it is available to screen reader. [accessibility] (rstudio-pro#5268)
- Improved the accessible label on launcher job details buttons. [accessibility] (rstudio-pro#5270)

