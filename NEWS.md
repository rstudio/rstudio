## v0.99b - Release Notes

### Source Editor

* New Emacs editing mode
* Editor and IDE keyboard shortcuts can now be customized
* Support for multiple source windows (tear editor tabs off main window)
* New global and per-project options for line feed conversion
* Snippets: pass parameters to snippet generating R functions
* Split into lines command for multiple cursors (Ctrl+Alt+A)
* New keyboard shortcuts to expand/contract current selection
    * Cmd+Alt+Shift+{Up/Down} OS X
    * Ctrl+Shift+{Up/Down} otherwise
* Enhanced display of sections in R scope navigator
* Added document outline display to R and C++ documents
* New Close All Except Current command
* Rename variable in scope (Cmd+Shift+Alt+M)
* More context-sensitive highlighting of R keywords
* Option to enable highlighting of R function calls
* F2 now navigates into files (e.g. within calls to source)
* Yank before/after (Ctrl+K, Ctrl+U) now use system clipboard on RStudio Desktop
* Yank after cursor (Ctrl+K) no longer eats end of line character
* Added option controlling 'surround on text insertion' behaviour

### R Markdown

* New run chunk and options buttons overlaid at the top right of chunks
* New shortcut for run current chunk (Cmd+Shift+Enter)
* Outline view for quick navigation between sections/code chunks
* Ctrl+PageUp and Ctrl+PageDown navigate between sections within Rmd, Rpres
* Enabled comment/uncomment (Cmd+Shift+C) for Markdown documents
* Knit with Parameters command for previewing with varying parameters
* Run All now executes chunks in console (rather than calling e.g. knitr::purl)
* Reorganize toolbar commands/menu for improved discoverability
* Added Run Setup Chunk command

### Miscellaneous

* Ctrl+A, Ctrl+E now move cursor to beginning, end of line in console on all platforms
* New Session command (create new R session with same project or working directory)
* Open project in a new window from the projects recently used menu
* Data Viewer: Filter factor columns by text or level
* Raise limit on shinyapps uploads to 1GB from 100MB
* 'Edit -> Replace and Find' opens Find toolbar if not already open (e.g. with Cmd+Shift+J)
* Improve performance of console for large and/or rapidly updating output
* Roxygen quick reference available from the Help menu
* Links to RStudio cheat sheets available on the Help menu
* Scan for Rtools in both HKCU and HKLM (for non-Admin installs of Rtools)
* Run app command and shortcut now works for running single file Shiny applications 
* Move running Shiny apps between IDE panes and windows without restarting the app
* Add support for single-file, standalone Shiny applications
* Parse TeX magic comments that start with "%%" (ESS compatibility).
* Change default Rpres template to specify autosize: true
* Automatically create ~/.ssh directory if necessary on Windows
* Added Makefile mode (used for Makefile, Makevars)
* Always use LF for line endings in Unix Makefiles in R packages
* Return environment variables as completions within Sys.getenv(), Sys.setenv() calls
* Add 'R Scripts' preset filter to Find in Files dialog
* OS X: Enable creation of directories in folder picker dialog
* Added ability to zoom panes (e.g. Ctrl+Shift+1 to zoom source pane)
* Add Console on Left/Right commands for quick relocation of Console
* Add product and version metadata to Windows installer

### Server

* Include active project in document title (caption of browser tab) 
* Quit session command now accessible from global toolbar
* Added option to control how many days users stay signed in for
* Allow specification of multiple groups in auth-required-user-group option
* Suspend and resume running R sessions when server is restarted
* Add kill-session and kill-all admin commands
* Server Pro: Shared Projects (including concurrent multi-user editing)
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
* Diagnostics: Respect // [[Rcpp::export]] functions used in R code
* Fix grid metrics issues (e.g. text too small) by using res of 96 rather than 72
* Rcpp: Parse attributes when generating diagnostics for header files
* Enable outdenting in Rhtml documents
* Find all now respects active search/replace options
* Fix issue with cursor disappearing in Rmd chunks for ambiance theme
* Publish button shows in the editor and viewer at the appropriate times
* Avoid spurious R warnings when autocompletions requested
* Allow completions in statements following infix operators
* Completions in Install Packages are now correct for the case of multiple active repositories
* Vim mode: prevent paste operation from entering visual mode via Ctrl+V cross-talk
* Fixed chunk highlighter issues that occurred when editing chunk label
* Correctly handle call to edit() with no arguments
* Fix inability to start up on OS X when multiple conflicting R versions are on the library search path
* Prevent crash when cancelling out of q() prompt on Windows
* OSX: Viewer now correctly recognizes session temp dir even when prefixed by /private
* Fix issue with rstudioapi previewRd function when path included spaces
* R 3.3: Don't call setInternet2 or use --internet2 flag for child R processes
* Linux, Windows: ensure native printer used (don't default to PDF printing)
* Prevent spurious navigation when user cancels from the file upload dialog
* Don't include H3 (and higher) headers when creating presentation slide preview navigation menu






