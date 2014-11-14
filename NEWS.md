
## v0.99 - Release Notes

### Source Editing

* Improvements to R code completion:
    - All objects available within the current scope are now included in completions
    - Completions now supported in more contexts including S3 and S4 methods and dplyr pipelines
    - Automatic insertion of closing parens when appropriate
    - Completion for statements spanning multiple lines
as auto-completions
* Improvements to C/C++ editing mode:
    - Code completion
    - F2 code navigation (go to definition)
    - Go to file/function for C/C++
    - Intelligent auto-indentation
    - Scope tree for quick intra-file navigation
* Fuzzy matching for go to file/function
* Include parameters in function navigation menu
* Alt+Enter to run code while retaining cursor position


### Workspace

* Improved handling of objects containing or consisting of NULL `externalptr`

### Debugging

* Allow 'debugSource' to be executed in user-specified environment
* Improved heuristics for locating the stack frame where errors originated

### Packages

* Improved checking for supported protocol with packrat package
* Escape backslashes in library names when loading packages

### Miscellaneous

* Bind Cmd+Shift+K shortcut to Compile PDF and Preview HTML
* When evaluating R strings ensure 'try' is called from base package
* Add Clear Recent Projects menu item to toolbar project menu
* Update to MSYS SSH 1000.18
* Update to pandoc 1.13.1

### Bug Fixes

* Prevent error dialog when getOption("repos") is an unnamed vector




