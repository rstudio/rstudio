## v0.99c - Release Notes

### Editor

* Enabled auto-pairing of backticks (\`\`) in R documents
* Fixed regression re: folding of unnamed sections, e.g. '#####'
* Implemented folding for sub-sections in R documents
* Added option for display of 'end' fold markers
* Display function tooltip on mouse over of function name
* Added option to display function signature tooltip on cursor idle
* Autocompletion allows mismatch between '.', '\_' in token
* Improvements to refactoring utility 'Rename in Scope'

### Data Import

* Import dataset from text via readr
* Import dataset from Excel via readxl
* Import dataset from SAS, SPSS, and Stata via haven
* Preview data while importing datasets
* Explicitly set column types while importing datasets
* Preview and copy code while importing datasets
* Enable data import preview to be cancelled
* Enable data import to cache web files

### C / C++

* Provide autocompletion of header paths
* Update C++ unit testing library (catch) to v1.3.3 (fix gcc >=5 compilation errors)
* Syntax highlighting of raw, wide, unicode string literals (e.g R"hello")

### Miscellaneous

* Implement gt/gT bindings in Vim mode to switch to next/previous tab
* Always provide file completions for top-level current directory
* Prevent wrapping of text in Files pane display
* Indicate when object details in environment pane have been truncated

### Bug Fixes

* Fix for 'httpdPort' detection error sometimes seen with R built from source
* Autocompletion: avoid errors when retrieving completions in debugger
* Diagnostics: fix false positive errors with '{' following function calls
* Improved performance of document tokenization (fix laggy typing)
* Fix block commenting of Sweave chunks
* Fix highlighting of escaped '$' in inline Mathjax expressions
* Fix editor preview vanishing on zoom level change
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
* Avoid perturbing RNG state when invoking View()
* Fix unlinked directories in Files pane when other users' folders are browseable
* Prevent lines terminated by \r\n from entering editor (cause of many subtle problems)
* Fix error message when invoking View() on an object with no columns



