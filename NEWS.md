
## v0.99 - Release Notes

### Source Editing

* Improvements to R code completion:
    - Completions provided automatically without an explicit gesture
    - All objects visible from the current scope are now included in completions
    - Completions in more contexts including S3 and S4 methods and dplyr pipelines
    - Automatic insertion of closing parens when appropriate
    - Inline help tooltip for signatures of completed functions
    - Completion for statements spanning multiple lines
    - Specialized autocompletions provided for `library`, `data`, `vignette`, `?`
    - For Shiny applications, autocompletions for `ui.R` + `server.R` pairs
    - Completions for dimension names in `[`, `[[` calls
    - Completions from packages in `library`, `require` calls automatically
      inferred and supplied even when not loaded
* Improvements to C/C++ editing mode:
    - Code completion
    - F2 code navigation (go to definition)
    - Go to file/function for C/C++
    - Intelligent auto-indentation
    - Scope tree for quick intra-file navigation
* Improvements in file/function navigation:
    - Fuzzy matching on search terms
    - Navigate to file posititions using file:line:col
    - Include parameters in function navigation menu
* Editor tabs in the source pane can now be rearranged
* Alt+Enter to run code while retaining cursor position
* Comment/uncomment respects indent level when appropriate
* Shift+Enter now searches backwards in Find/Replace
* New option to control comment continuation on insertion of new lines

### Data Viewer

- Support for viewing large data sets (removed 1k row limit)
- Data can be filtered, searched, and sorted
- Viewer updates to reflect changes in data

### Workspace

* Improved handling of objects containing or consisting of NULL `externalptr`

### Debugging

* Allow 'debugSource' to be executed in user-specified environment
* Improved heuristics for locating the stack frame where errors originated
* Autocompletions now available when debugging

### Packages

* Improvements to New Package:
    - Generate cleaner packages with no warnings
    - Respect various devtools options
* Improved checking for supported protocol with packrat package
* Escape backslashes in library names when loading packages

### Plots

* Render plots using devicePixelRatio for retina and HDPI screens

### Miscellaneous

* Updated to pandoc 1.13.1
* Updated rendering engine to Qt 5.4 for improved performance
* Windows: updated to MSYS SSH 1000.18
* Bind Cmd+Shift+K shortcut to Compile PDF and Preview HTML
* When evaluating R strings ensure 'try' is called from base package
* Add Clear Recent Projects menu item to toolbar project menu
* Command to sync current working directory and Files pane to project directory
* Eliminated rstudio and manipulate packages (both now available on CRAN)

### Bug Fixes

* Prevent error dialog when getOption("repos") is an unnamed vector




