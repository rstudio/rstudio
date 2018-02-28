## v1.2 - Release Notes

### Python and Notebooks

* Use a shared Python session to execute Python chunks via `reticulate`
* Simple bindings to access R objects from Python chunks and vice versa
* Show `matplotlib` plots emitted by Python chunks 

### Keyring

* Store passwords and secrets securely with `keyring` by calling `rstudioapi::askForSecret()`
* Install `keyring` directly from dialog prompt

### Miscellaneous

* Git 'Create Branch' dialog defaults to remote associated w/current branch (if any)
* Added link to purrr cheat sheet (in Help) and link to browse all cheat sheets
* Added option to temporarily disable environment pane refreshing
* Improve NSE detection for dplyr (better understands S3 dispatch and idioms)
* Add ability to search for displayed database objects in Connections tab (#1549)
* Add button to open profiler output in an external browser (#1657)
* Add option to show the R Markdown render command used when knitting (#1658)
* Allow renames that change only file case on Windows (#1886)
* Remember scroll position when navigating in Help pane (#1947)
* Show warning when attempting to edit a generated file (#2082)
* Allow opening .ini files with `file.edit` (#2116)
* Add `shinymod` snippet for Shiny modules (#2078)
* Allow changing zoom level without reloading (#2125)
* New command 'Pull with Rebase' to pull and rebase a branch in a single step (#2151)
* Click on promises in the Environment pane now calls `force` on the promise
* Add Rename command to File menu for quick rename of current file (#2199)
* Numeric filtering in data viewer shows value distribution and supports user-entered values (#2230)

### Bug Fixes

* Fix "Invalid byte sequence" when spell checking
* Fix incorrect Git status pane display when git detects that a file has been copied
* Fix hang when submitting empty passwords and password encryption is turned off (#1545)
* Fix HTTP 500 error when navigating to directories such as /js/ (#1561)
* Fix issue where Build pane would get 'stuck' on failed Rcpp::compileAttributes() call (#1601)
* Fix low/no-contrast colors with HTML widgets in notebooks with a dark theme (#1615)
* Fix invalid YAML in some cases when changing R Markdown output type via dialog (#1609)
* Fix error when quitting while a function named `q()` is present (#1647)
* Fix crash when executing multiple R Notebook chunks with a failing Rcpp chunk (#1668)
* Fix missing blank lines in code chunks in R Notebook preview (#1556)
* Fix selection in Files pane when files are modified while checked (#1715)
* Fix incorrect truncation of some R object descriptions in Environment pane (#1703)
* Fix duplicate prompts in each window when using RStudio API `showPrompt` (#1706)
* Fix `file.edit` failures with Chinese filenames on Windows (#1868)
* Fix errors when importing non-ASCII filenames in base Import Dataset (#1910)
* Fix `rserver` crash that can occur when proxying websockets to Shiny apps (#2061)
* Fix empty column titles when viewing matrices without column names (#2086)
* Fix error when pressing F1 on non-function autocomplete results (#2127)
* Fix hang when autocompleting filenames in large directories (#2236)

### RStudio Server Pro

* Overhauled R versions, allowing you to specify version labels, load environment modules, and execute a prelaunch script when loading specific versions.
* New rsession-diagnostics-enabled option for rserver.conf to enable session launch diagnostics mode to help diagnose session launch failures.
* Added support for auth-pam-sessions-use-password option in a load balanced setup.
* Added ability to suspend sessions from user home page.
* Added hmac signature verification for proxy auth mode with new auth-proxy-require-hmac option in rserver.conf.


