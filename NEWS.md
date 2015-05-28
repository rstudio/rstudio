
## v0.99b - Release Notes


### Source Editor

* Snippets: pass parameters to snippet generating R functions
* Split into lines command for multiple cursors (Ctrl+Alt+A)
* Enhanced display of sections in R scope navigator


### R Markdown

* New run chunk button overlaid at the top right of chunks in the editor
* New shortcut for run current chunk (Cmd+Shift+Enter)


### Miscellaneous

* Improve performance of console for large and/or rapdily updating output
* Roxygen quick reference available from the Help menu
* Links to RStudio cheat sheets available on the Help menu

### Server

* Include active project in document title (caption of browser tab) 


### Miscelleous

* Raise limit on shinyapps uploads to 1GB from 100MB


### Bug Fixes

* Diagnostics: Avoid linting symbols in R formulas
* Diagnostics: Resolve functions in correct namespace 
* Enable outdenting in Rhtml documents
* Find all now respects active search/replace options
* Fix issue with cursor dissapearing in Rmd chunks for ambiance theme
* Publish button shows in the editor and viewer at the appropriate times

