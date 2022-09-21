
## RStudio 2022-10.0 "Elsbeth Geranium" Release Notes

### R

* Whether pending console input is discarded on error can now be controlled via a preference in the Console pane. (#10391)
* Improved handling of diagnostics within pipeline expressions. (#11780)
* Improved handling of diagnostics within glue() expressions. 
* RStudio now provides autocompletion results for packages used but not loaded within a project.
* Improved handling of missing arguments for some functions in the diagnostics system.
* Code editor can show previews of color in strings (R named colors e.g. "tomato3" or of the forms "#rgb", "#rrggbb", "#rrggbbaa")
  when `Options > Code > Display > [ ] Show color preview` is checked. 
* Fixes the bug introduced with `rlang` >= 1.03 where Rmd documents show the error message `object 'partition_yaml_front_matter' not found` upon project startup (#11552)
* Name autocompletion following a `$` now correctly quotes names that start with underscore followed by alphanumeric characters (#11689)
  
### Python

- RStudio attempts to infer the appropriate version of Python when "Automatically activate project-local Python environments" is checked and the user has not requested a specific version of Python. This Python will be stored in the environment variable "RETICULATE_PYTHON_FALLBACK", available from the R console, the Python REPL, and the RStudio Terminal (#9990)

### Quarto

- Support for v2 format of Quarto crossref index

### Deprecated / Removed

- Removed the Tools / Shell command (#11253)

### Fixed

- Fixed an issue where Find in Files could fail to find results with certain versions of git (#11822)
- Fixed visual mode outline missing nested R code chunks (#11410)
- Fixed an issue where chunks containing multibyte characters was not executed correctly (#10632)
- Fixed bringing main window under active secondary window when executing background command (#11407)
- Fix for schema version comparison that breaks db in downgrade -> upgrade scenarios (rstudio-pro#3572)
- Fixed an issue in the Electron build of the IDE on Macs where users could not clone a git repository via password-protected SSH or HTTPS (#11693)
