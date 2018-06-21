## v1.2 - Release Notes

### Python and Notebooks

* Use a shared Python session to execute Python chunks via `reticulate`
* Simple bindings to access R objects from Python chunks and vice versa
* Show `matplotlib` plots emitted by Python chunks 

### Keyring

* Store passwords and secrets securely with `keyring` by calling `rstudioapi::askForSecret()`
* Install `keyring` directly from dialog prompt

### D3

* Author D3 visualizations in RStudio and preview in the Viewer pane
* Use [r2d3](https://rstudio.github.io/r2d3/) D3 visualizations in R Notebook chunks

### Jobs

* Run any R script as a background job in a clean R session
* Monitor progress and see script output in real time
* Optionally give jobs your global environment when started, and export values back when complete

### SQL

* Author SQL queries in RStudio and preview in the SQL Results pane

### Testing

* *Run Tests* command in [testthat](https://github.com/r-lib/testthat) R scripts for direct running
* testthat output in the *Build* pane with navigable issue list
* Integration with [shinytest](https://github.com/rstudio/shinytest) to record and run Shiny app tests

### PowerPoint

* Create PowerPoint presentations with R Markdown

### Package Management

* Specify a primary CRAN URL and secondary CRAN repos from the package preferences pane.
* Link to a package's primary CRAN page from the packages pane.
* Configure CRAN repos with a repos.conf configuration file and the `r-cran-repos-file` option.
* Suggest additional secondary CRAN repos with the `r-cran-repos-url` option.

### Plumber

* Create [Plumber APIs](https://www.rplumber.io/) in RStudio
* Execute Plumber APIs within RStudio to view Swagger documentation and make test calls to the APIs
* Publish Plumber APIs to [RStudio Connect](https://www.rstudio.com/products/connect/)

### Miscellaneous

* Git 'Create Branch' dialog defaults to remote associated w/current branch (if any)
* Added link to purrr cheat sheet (in Help) and link to browse all cheat sheets
* Added option to temporarily disable environment pane refreshing
* Improve NSE detection for dplyr (better understands S3 dispatch and idioms)
* Add ability to search for displayed database objects in Connections tab (#1549)
* Add button to open profiler output in an external browser (#1657)
* Add option to show the R Markdown render command used when knitting (#1658)
* Add option to show hidden files in the Files pane (#1769)
* Upgrade embedded Pandoc to 2.2.1 (#1807)
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
* Improved support for custom `knitr` engines in R Notebooks (#2401)
* Add support for viewing external web URLs in the Viewer pane (#2252)
* Add option to disable drag-and-drop for text in the editor (#2428)
* Add option to disable cursor save/load; improves performance on some Windows machines (#2778)
* R startup files (e.g. .Rprofile) are now always saved with trailing newlines (#3029)
* Update embedded libclang to 5.0.2 (Windows only)
* RStudio now a 64-bit application on Windows (Linux and Mac are already 64-bit)
* New SSL options for authenticating and publishing to RStudio Connect servers using self-signed certs or internal CAs (#3040)

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
* Fix proxy timeouts with the websocket used for the Terminal, via keepalive messages (#1860)
* Fix `file.edit` failures with Chinese filenames on Windows (#1868)
* Fix errors when importing non-ASCII filenames in base Import Dataset (#1910)
* Fix `rserver` crash that can occur when proxying websockets to Shiny apps (#2061)
* Fix hang on some Linux systems caused by X11 clipboard monitoring w/ option to disable monitoring (#2068)
* Fix empty column titles when viewing matrices without column names (#2086)
* Fix error when pressing F1 on non-function autocomplete results (#2127)
* Fix hang when autocompleting filenames in large directories (#2236)
* Fix inability to copy content from Viewer pane and data viewer in IE11 (#2351)
* Fix errant addition of `msys-ssh` to path on non-Windows platforms (#2352)
* Fix buggy behavior with \r when ANSI colors are present (#2387)
* Fix external process slowness (git, etc.) when open file limit `RLIMIT_NOFILE` is high (#2470)
* Fix issue caused by resolving symlinks when choosing Git path (#2476)
* Fix display of consecutive spaces in the Data Viewer (#2499)
* Fix issue where '#' in YAML strings would be highlighted as comments (#2591)
* Fix over-eager loading of `yaml` package when IDE starts up (#2602)
* Fix issue on Windows with R dialogs showing behind RStudio window (#2901)
* Fix incorrect insertion of mousewheel handler into HTML widget JavaScript (#2634)
* Fix unresponsive buttons in Connections pane when connection deletion is cancelled (#2644)
* Fix RStudio hang when installing packages (e.g. BH) in Packrat projects on Windows (#1864)

### RStudio Server Pro

* Overhauled R versions, allowing you to specify version labels, load environment modules, and execute a prelaunch script when loading specific versions.
* New rsession-diagnostics-enabled option for rserver.conf to enable session launch diagnostics mode to help diagnose session launch failures.
* Added support for auth-pam-sessions-use-password option in a load balanced setup.
* Added ability to suspend sessions from user home page.
* Added hmac signature verification for proxy auth mode with new auth-proxy-require-hmac option in rserver.conf.
* Add nodes to RStudio Server Pro load-balanced clusters without service interruptions.

### RStudio Pro Drivers

* Discover, install and uninstall RStudio Pro Drivers from the New Connection wizard.
