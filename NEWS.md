## v0.99c - Release Notes

### Data Import

* Import dataset from text via readr
* Import dataset from Excel via readxl
* Import dataset from SAS, SPSS, and Stata via haven
* Preview data while importing datasets
* Explicitly set column types while importing datasets
* Preview and copy code while importing datasets

### Profiler

* Profiling features based on the [profvis](https://github.com/rstudio/profvis) package
* Visualize profiling results within the IDE
* Profile menu enables easy stop/start of profiling or profiling of selected lines.
* Save and load of previous profiling sessions

### Source Editor

* Enabled auto-pairing of backticks (\`\`) in R documents
* Fixed regression re: folding of unnamed sections, e.g. '#####'
* Implemented folding for sub-sections in R documents
* Added option for display of 'end' fold markers
* Display function tooltip on mouse over of function name
* Added option to display function signature tooltip on cursor idle
* Autocompletion allows mismatch between '.', '\_' in token
* Improvements to refactoring utility 'Rename in Scope'
* Expand selection command learns how to expand to current statement
* Added (as unbound command) Sublime Text style 'Quick Add Next'
* The various 'yank' commands are now rebindable (Ctrl + Y, Ctrl + K, Ctrl + U)

### R Markdown

* Show custom formats in Knit menu
* Show options menu and Knit w/ Params for custom formats
* Use "Run Document" for custom formats with runtime: shiny
* Add option to suppress Knit preview entirely
* Add R Markdown pane to global options dialog
* Build tab and preview support for R Markdown websites
* Various enhancements for authoring with the bookdown package
* Update pandoc to 1.17.0.3

### C/C++

* Provide autocompletion of header paths
* Update C++ unit testing library (catch) to v1.3.3 (fix gcc >=5 compilation errors)
* Syntax highlighting of raw, wide, unicode string literals (e.g R"hello")

### Miscellaneous

* Files pane now has a fixed header row
* Published plots are larger and responsive to changes in browser size
* Implement gt/gT bindings in Vim mode to switch to next/previous tab
* Always provide file completions for top-level current directory
* Prevent wrapping of text in Files pane display
* Indicate when object details in environment pane have been truncated
* Improved keyboard navigation in browser file widgets
* Added option to limit length of lines in console history
* Improved performance when many lines of code sent to console
* 'save()' warnings no longer emitted when saving session state

### Bug Fixes

* Fixed a hang on Windows + 64bit R when errors occurred while resizing Plots pane
* Data Viewer: fixed a crash that could occur when calling 'View()' on matrix
* R Markdown: hide chunk toolbar when chunk is hidden by folded Markdown section
* R Markdown: fixed highlighting with multiple Markdown links on one line
* R Markdown: fixed issue where sections did not display in document outline when following empty bulleted list
* Reformat code: fixed an issue where '\*\*' was not recognized as a single token
* SVN: avoid recursive revert when reverting changes to modified directory properties
* Autocompletion: fixed errors printed in console when 'devtools::load_all()' active
* Autocompletion: fixed errors printed in console for functions 'force', 'identity'
* Autocompletion: ensure .DollarNames method respected when discovered
* Autocompletion: avoid errors when retrieving completions in debugger
* Viewer: Fixed issue in 'Export -> Copy to Clipboard' when IDE not at default zoom level
* Fix hang caused by large, heavily-nested S4 objects
* Fix for 'httpdPort' detection error sometimes seen with R built from source
* Fixed a hang on Windows with 64-bit R when attempting to load a corrupted workspace
* Fixed an issue where non-interactive addins could not be executed in popped out windows
* Diagnostics: fix false positive errors with '{' following function calls
* Diagnostics: engine better understands 'data()' calls (fixes 'no symbol in scope' false positive)
* Improved performance of document tokenization (fix laggy typing)
* Sweave: Fix block commenting of chunks
* Sweave: Fixed an issue where TOC headings weren't displayed in document outline
* Fix highlighting of escaped '$' in inline Mathjax expressions
* Fix editor preview vanishing on zoom level change
* Emacs mode: C-f now moves the cursor forward instead of opening Find dialog
* Emacs mode: C-r now performs a reverse isearch on Windows (rather than running code)
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
* Fix copy, cut, paste handling when Emacs mode enabled
* Avoid perturbing RNG state when invoking View()
* Fix unlinked directories in Files pane when other users' folders are browseable
* Fixed an issue where nested folds were not preserved correctly on save / load
* Prevent lines terminated by \r\n from entering editor (cause of many subtle problems)
* Fix error message when invoking View() on an object with no columns
* Fix blank screen on log-in when PAM username capitalization doesn't match system
* Prompt to save untitled (never saved) documents when closing source windows



