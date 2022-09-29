
## RStudio 2022-10.0 "Elsbeth Geranium" Release Notes

### R

* Whether pending console input is discarded on error can now be controlled via a preference in the Console pane. (#10391)
* RStudio now drops pending console output when the console is interrupted. (#12059)
* Improved handling of diagnostics within pipeline expressions. (#11780)
* Improved handling of diagnostics within glue() expressions. 
* RStudio now provides autocompletion results for packages used but not loaded within a project.
* Improved handling of missing arguments for some functions in the diagnostics system.
* Code editor can show previews of color in strings (R named colors e.g. "tomato3" or of the forms "#rgb", "#rrggbb", "#rrggbbaa")
  when `Options > Code > Display > [ ] enable preview of named and hexadecimal colors` is checked. 
* Fixes the bug introduced with `rlang` >= 1.03 where Rmd documents show the error message `object 'partition_yaml_front_matter' not found` upon project startup (#11552)
* Name autocompletion following a `$` now correctly quotes names that start with underscore followed by alphanumeric characters (#11689)
* Suspended sessions will now default to using the project directory, rather than the user home directory, if the prior working directory is no longer accessible. (#11960)
* The fuzzy finder indexes C(++) macros (#11981)
  
### Python

- RStudio attempts to infer the appropriate version of Python when "Automatically activate project-local Python environments" is checked and the user has not requested a specific version of Python. This Python will be stored in the environment variable "RETICULATE_PYTHON_FALLBACK", available from the R console, the Python REPL, and the RStudio Terminal (#9990)
- Shiny for Python apps now display a "Run App" button on the Source editor toolbar. (Requires `shiny` Python package v0.2.7 or later.)

### Quarto

- Support for v2 format of Quarto crossref index

### Posit Workbench

- Adds `-l` (long) option to `rserver-url`. When `/usr/lib/rstudio-server/bin/rserver-url -l <port number>` is executed within a VS Code or Jupyter session, the full URL where a user can view a server proxied at that port is displayed (rstudio-pro#3620)
- Sets the `UVICORN_ROOT_PATH` environment variable to the proxied port URL for port 8000 in VS Code and Jupyter sessions, allowing FastAPI applications to run without additional configuration. (rstudio-pro#2660)

#### Posit Workbench VS Code Extension

- Introduce Workbench Job management to VS Code Extension (rstudio/rstudio-pro#3784, rstudio/rstudio-pro#3565)
- Added a pop-up notification when working with certain relevant filetypes that makes it easier to find the Workbench Extension. This notification is a one-time view per user. It can be re-enabled in the user settings (vscode-ext#96).
- Rebranded the interface to match Posit Software, PBC's new branding terminology and iconography
- Fixed extension servers appearing in Proxied Servers list (vscode-ext#116)

### Deprecated / Removed

- Removed the Tools / Shell command (#11253)

### Fixed

- Fixed an issue where the console history scroll position was not preserved when switching focus to a separate application (#1638)
- Fixed an issue where Find in Files could omit matches in some cases on Windows (#11736)
- Fixed an issue where the Git History window inverted the display of merge diffs (#10150)
- Fixed an issue where Find in Files could fail to find results with certain versions of git (#11822)
- Fixed visual mode outline missing nested R code chunks (#11410)
- Fixed an issue where chunks containing multibyte characters was not executed correctly (#10632)
- Fixed bringing main window under active secondary window when executing background command (#11407)
- Fix for schema version comparison that breaks db in downgrade -> upgrade scenarios (rstudio-pro#3572)
- Fixed an issue in the Electron build of the IDE on Macs where users could not clone a git repository via password-protected SSH or HTTPS (#11693)
- Fixed scroll speed sensitivity for Mac and Linux and added a preference to adjust it (#11578)

### Jupyter

* The Jupyter Notebook and JupyterLab extensions have been updated to match with the new Posit Software, PBC branding (rstudio-pro#3645)
