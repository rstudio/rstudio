
## v0.99b - Release Notes


### Source Editor

* Snippets: pass parameters to snippet generating R functions
* Split into lines command for multiple cursors (Ctrl+Alt+A)
* Enhanced display of sections in R scope navigator


### R Markdown

* New run chunk and options buttons overlaid at the top right of chunks
* New shortcut for run current chunk (Cmd+Shift+Enter)


### Miscellaneous

* Raise limit on shinyapps uploads to 1GB from 100MB
* 'Edit -> Replace and Find' opens Find toolbar if not already open (e.g. with Cmd+Shift+J)
* Improve performance of console for large and/or rapdily updating output
* Roxygen quick reference available from the Help menu
* Links to RStudio cheat sheets available on the Help menu
* Add keyboard shortcut (Ctrl+Alt+`) to toggle toolbar visibility


### Server

* Include active project in document title (caption of browser tab) 
* Server Pro: Support for multiple concurrent R sessions per-user
* Server Pro: Don't close PAM sessions by default (configurable via an option)


### Bug Fixes

* Diagnostics: Avoid linting symbols in R formulas
* Diagnostics: Resolve functions in correct namespace 
* Enable outdenting in Rhtml documents
* Find all now respects active search/replace options
* Fix issue with cursor disappearing in Rmd chunks for ambiance theme
* Publish button shows in the editor and viewer at the appropriate times
* Avoid spurious R warnings when autocompletions requested
* Allow completions in statements following infix operators
* Completions in Install Packages are now correct for the case of multiple active repositories.
* Vim mode: prevent paste operation from entering visual mode via Ctrl+V cross-talk


