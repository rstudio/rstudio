## v1.0b - Release Notes

### R Notebooks

* Improved sizing of htmlwidgets in R Notebooks
* Allow changing to R Notebook mode without closing and reopening the file
* Add support for knitr code chunks defined in external .R files
* Add support for raw relative paths such as ".." in knitr root.dir option
* Improve scrolling past htmlwidgets in the editor
* Always show chunk output preferences (inline or console)
* Add support for variable height HTML widgets (non-knitr figures)

### Data Import

* Add support for all readr locale settings (date/time format, decimal, etc.)
* Prompt for a comma-separated list of factors instead of a collection

### Miscellaneous

* Add Ctrl+Tab / Ctrl+Shift+Tab shortcuts to navigate tabs (Desktop only)
* Include .scala files in fuzzy file finder (Ctrl + .)
* Add support for pre-rendered Shiny documents (shiny_prerendered)
* Update Stan editor mode to support Stan 2.12.0

### Bug Fixes

* Improve resilience against malformed YAML header in R Markdown documents
* Avoid triggering active bindings in environments from the Environment pane
* Fix encoding of R_HOME, R_USER, and R_LIBS_USER on Windows
* Fix project-based file completion on Windows
* Respect loaded packages in R help topic completion
* Fix positioning of editor toolbar buttons in Safari and Mac OS client
* Fix incorrect height of R Notebook outputs when run above viewport
* Detect multi-line data.table chains properly  
* Allow git staging for filenames containing a space on Windows
* Allow git staging for filenames containing a dollar sign ($)
* Use Windows proxy settings to serve requests made by htmlwidgets
* Server Pro: Allow server user to have group name differing from user name
* Server Pro: Don't require LDAP to support user enumeration when `auth-required-user-group` is set

