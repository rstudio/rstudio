
## v0.99b - Release Notes


### Source Editor

* Support for multiple source windows (tear editor tabs off main window)
* Snippets: pass parameters to snippet generating R functions
* Split into lines command for multiple cursors (Ctrl+Alt+A)
* New keyboard shortcuts for expand/contract selection
* Enhanced display of sections in R scope navigator
* Added document outline display to R and C++ documents
* New Close All Except Current command
* Rename variable in scope (Cmd+Shift+Alt+M)
* New Emacs editing mode
* More context-sensitive highlighting of R keywords
* Option to enable highlighting of R function calls


### R Markdown

* New run chunk and options buttons overlaid at the top right of chunks
* New shortcut for run current chunk (Cmd+Shift+Enter)
* Outline view for quick navigation between sections/code chunks
* Support for htmlwidgets in R Presentations
* Added Run Setup Chunk command

### Data Viewer

* Improved interface for filtering factor columns; can now filter by either text or level

### Miscellaneous

* Editor and IDE keyboard shortcuts can now be customized
* New Session command (create new R session with same project or working directory)
* Open project in a new window from the projects recently used menu
* Increase the number of items on file and project recently used menus to 15
* Raise limit on shinyapps uploads to 1GB from 100MB
* 'Edit -> Replace and Find' opens Find toolbar if not already open (e.g. with Cmd+Shift+J)
* Improve performance of console for large and/or rapidly updating output
* Roxygen quick reference available from the Help menu
* Links to RStudio cheat sheets available on the Help menu
* Scan for Rtools in both HKCU and HKLM (for non-Admin installs of Rtools)
* Move running Shiny apps between IDE panes and windows without restarting the app
* Parse TeX magic comments that start with "%%" (ESS compatibility).
* Change default Rpres template to specify autosize: true
* Automatically create ~/.ssh directory if necessary on Windows
* Added `Makefile` mode (used for `Makefile`, `Makevars`)


### Server

* Include active project in document title (caption of browser tab) 
* Quit session command now accessible from global toolbar
* Added option to control how many days users stay signed in for
* Server Pro: Support for multiple concurrent R sessions per-user
* Server Pro: Support for running against multiple versions of R
* Server Pro: Don't close PAM sessions by default (configurable via an option)
* Server Pro: Remove Google OpenID auth (deprecated by Google in favor of OAuth)
* Server Pro: Add option to specify client-id for Graphite metrics back end
* Server Pro: Ability to record user console history for audit purposes


### Bug Fixes

* Diagnostics: Avoid linting symbols in R formulas
* Diagnostics: Resolve functions in correct namespace 
* Diagnostics: Fix invalid diagnostics within formulas
* Rcpp: Parse attributes when generating diagnostics for header files
* Enable outdenting in Rhtml documents
* Find all now respects active search/replace options
* Fix issue with cursor disappearing in Rmd chunks for ambiance theme
* Publish button shows in the editor and viewer at the appropriate times
* Avoid spurious R warnings when autocompletions requested
* Allow completions in statements following infix operators
* Completions in Install Packages are now correct for the case of multiple active repositories.
* Vim mode: prevent paste operation from entering visual mode via Ctrl+V cross-talk


