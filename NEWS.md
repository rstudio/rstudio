## v0.99c - Release Notes

### R Notebooks

* Authoring tools for [R Notebooks](http://rmarkdown.rstudio.com/r_notebooks.html)
* Inline display for text, latex, tabular data, graphics, and htmlwidgets in source editor
* All code and output saved within a single notebook HTML file (.nb.html)
* Multiple language engines including Python, Bash, SQL, Rcpp, and Stan
* Tools for running various combinations of chunks (current, next, previous, remaining)

### Data Import

* Import dataset from text via [readr](https://github.com/hadley/readr)
* Import dataset from Excel via [readxl](https://github.com/hadley/readxl)
* Import dataset from SAS, SPSS, and Stata via [haven](https://github.com/hadley/haven)
* Preview data while importing datasets
* Explicitly set column types while importing datasets
* Preview and copy code while importing datasets

### Profiler

* Profiling features based on the [profvis](https://github.com/rstudio/profvis) package
* Visualize profiling results within the IDE
* Profile menu enables easy stop/start of profiling or profiling of selected lines.
* Save and load of previous profiling sessions

### RStudio Connect

* Publish reports, applications, and plots to [RStudio Connect](https://www.rstudio.com/products/connect/)
* One-click deployment of Shiny applications
* Publish and schedule periodic execution of R Markdown reports 
* Set which users and/or groups have permission to view content

### Spark 

* Integrated support for the [sparklyr](http://spark.rstudio.com) package
* Create and manage connections to Spark clusters and local Spark instances
* Browse tables and columns of Spark DataFrames
* Preview the first 1,000 rows of Spark DataFrames

### Source Editor

* Ctrl+Enter now executes current R statement when selection is empty
* Use Shift+Click to open web links in source editor (Command+Click on OS X)
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
* Insert assignment operator (Alt+-, '<-') is now rebindable
* Insert pipe operator (Cmd+Shift+M, '%>%') is now rebindable
* Enable implementors of .DollarNames to provide custom types

### R Markdown

* Inline preview for MathJax equations
* Show custom formats in Knit menu
* Show options menu and Knit w/ Params for custom formats
* Use "Run Document" for custom formats with runtime: shiny
* Add option to suppress Knit preview entirely
* Add R Markdown pane to global options dialog
* Build tab and preview support for R Markdown websites
* Various enhancements for authoring with the bookdown package
* Add command to clear knitr cache
* Update pandoc to 1.17.2
* Update MathJax to 2.6.1

### C/C++

* Provide autocompletion of header paths
* Syntax highlighting of raw, wide, unicode string literals (e.g R"hello")

### Server

* Added session-default-working-dir and session-default-new-project-dir options 
* Added sudo, lsb-release, and libssl1.0.0 packages to Debian dependencies
* Server Pro: Auditing for authentication and session start/suspend/exit events
* Server Pro: Ability to configure per-user and per-group session timeouts
* Server Pro: Ability to include custom HTML within server sign-in page
* Server Pro: Update to Nginx 1.10.1

### Miscellaneous

* Improved performance of 'Find in Files' tool
* Attempting to cut or copy with an empty selection no longer clears the clipboard
* Files pane now has a fixed header row
* Attempting to check out remote git branch now checks out local copy tracking remote
* Published plots are larger and responsive to changes in browser size
* Implement gt/gT bindings in Vim mode to switch to next/previous tab
* Always provide file completions for top-level current directory
* Prevent wrapping of text in Files pane display
* Indicate when object details in environment pane have been truncated
* Improved keyboard navigation in browser file widgets
* Added option to limit length of lines in console history
* Improved performance when many lines of code sent to console
* 'save()' warnings no longer emitted when saving session state
* Update C++ unit testing library (catch) to v1.3.3 (fix gcc >=5 compilation errors)
* Change default max.print to 1000 (was 10000)

### Bug Fixes

* Fixed a hang on Windows + 64bit R when errors occurred while resizing Plots pane
* Fixed an issue where a user-defined git executable was sometimes not respected on OS X
* Data Viewer: fixed a crash that could occur when calling 'View()' on matrix
* R Markdown: hide chunk toolbar when chunk is hidden by folded Markdown section
* R Markdown: fixed highlighting with multiple Markdown links on one line
* R Markdown: improved highlighting for '\*', '\_' text
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
* Don't depend on number of call frames when detecting install.packages call
* Fix failure to install packages with long install.packages commands after restart



