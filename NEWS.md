## v0.99c - Release Notes

### Data Import

* Import dataset from text via readr
* Import dataset from Excel via readxl
* Import dataset from SAS, SPSS, and Stata via haven
* Preview data while importing datasets
* Explicitly set column types while importing datasets
* Preview and copy code while importing datasets

### Miscellaneous

* Update C++ unit testing library (catch) to v1.3.3 (fix gcc >=5 compilation errors)
* Implement gt/gT bindings in Vim mode to switch to next/previous tab
* Always provide file completions for top-level current directory
* Prevent wrapping of text in Files pane display

### Bug Fixes

* Autocompletion: avoid errors when retrieving completions in debugger
* Diagnostics: fix false positive errors with '{' following function calls
* Avoid over-eager re-rendering + tokenization of documents
* Fix block commenting of Sweave chunks
* Fix highlighting of escaped '$' in inline Mathjax expressions
* Emacs mode: C-f now moves the cursor forward instead of opening Find dialog
* Ensure that modal dialogs capture all input even in the presence of multiple modals
* Filter out "00LOCK" directories from package name completions
* Provide completions for 'lazydata' objects within namespaces
* Prevent warnings from leaking when accessing refClass elements for completions
* Fix inablity to get active frame for completions when package lfe was loaded
* Prevent completion popup from appearing offscreen for very long completion names
* Don't pass encoding="" to knitr for .Rpres files not in a project
* Avoid duplicating keydown handlers in image list boxes (e.g. New R Markdown template)
* Ensure editor completion popups are hidden when editor loses focus
* Cleanup R auto-indentation for plain () or [] (i.e., when not in a function call)
* Suppress httpd warnings on invalid help queries 
* Correct syntax highlighting for operator << in C++ mode
* Fix highlighting in R mode for numbers with only a trailing decimal (e.g. '1.')
* Ensure that SparkR DataFrames appear as data in environment pane
* Avoid firing active bindings in completion system


