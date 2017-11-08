## v1.2 - Release Notes

### Miscellaneous

* Git 'Create Branch' dialog defaults to remote associated w/current branch (if any)
* Added link to purrr cheat sheet (in Help) and link to browse all cheat sheets
* Added option to temporarily disable environment pane refreshing
* Improve NSE detection for dplyr (better understands S3 dispatch and idioms)
* Add ability to search for displayed database objects in Connections tab (#1549)
* Linux desktop and server releases are now [signed with GPG](https://www.rstudio.com/code-signing/) (#1619)
* Add button to open profiler output in an external browser (#1657)
* Add option to show the R Markdown render command used when knitting (#1658)

### Bug Fixes

* Fix "Invalid byte sequence" when spell checking
* Fix incorrect Git status pane display when git detects that a file has been copied
* Fix hang when submitting empty passwords and password encryption is turned off (#1545)
* Fix HTTP 500 error when navigating to directories such as /js/ (#1561)
* Fix inability to sign out when using a new browser window and "stay signed in" (#1538)
* Fix issue where Build pane would get 'stuck' on failed Rcpp::compileAttributes() call (#1601)
* Fix low/no-contrast colors with HTML widgets in notebooks with a dark theme (#1615)
* Fix invalid YAML in some cases when changing R Markdown output type via dialog (#1609)
* Fix issue preventing Compile Report from R Script to export to PDF (#1631)
* Fix error when quitting while a function named `q()` is present (#1647)
* Fix crash when executing multiple R Notebook chunks with a failing Rcpp chunk (#1668)
* Fix missing blank lines in code chunks in R Notebook preview (#1556)
* Fix selection in Files pane when files are modified while checked (#1715)
* Fix incorrect truncation of some R object descriptions in Environment pane (#1703)
* Fix "File Exists" error when using Copy To to overwrite a file (#1722)
* Fix duplicate prompts in each window when using RStudio API `showPrompt` (#1706)

### RStudio Server Pro

* Overhauled R versions, allowing you to specify version labels, load environment modules, and execute a prelaunch script when loading specific versions.
* New rsession-diagnostics-enabled option for rserver.conf to enable session launch diagnostics mode to help diagnose session launch failures.
* Added support for auth-pam-sessions-use-password option in a load balanced setup.
* Added ability to suspend sessions from user home page.
* Added hmac signature verification for proxy auth mode with new auth-proxy-require-hmac option in rserver.conf.


