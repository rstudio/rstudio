
## v0.99 - Release Notes

### Data Viewer

- Support for viewing large data sets (removed 1k row limit)
- Data can be filtered, searched, and sorted
- Viewer updates to reflect changes in data

### R Code Completion

 - Completions provided automatically without an explicit gesture
 - All objects visible from the current scope are now included in completions
 - Completions in more contexts including S3 and S4 methods and dplyr pipelines
 - Automatic insertion of closing parens when appropriate
 - Inline help tooltip for signatures of completed functions
 - Completion for statements spanning multiple lines
 - Specialized autocompletions provided for library, data, vignette, ?
 - For Shiny applications, autocompletions for ui.R + server.R pairs
 - Completions for dimension names in [, [[ calls
 - Completions from packages in library, require calls automatically
   inferred and supplied even when not loaded
 - Completions for knitr options, e.g. in opts_chunk$get(), are now supplied
 - Completions for dynamic symbols within .C, .Call, .Fortran, .External

### Source Editor

* Improvements in file/function navigation:
    - Fuzzy matching on search terms
    - Navigate to file posititions using file:line:col
    - Include parameters in function navigation menu
* Multiple cursors:
   - Create a new cursor above / below the current cursor with CTRL + ALT + {up / down}
   - Move the active cursor up / down with CTRL + SHIFT + ALT + {up / down}
   - Create multiple cursors by pressing CTRL + ALT and clicking + dragging the mouse
* Improved Vim mode:
    - Various bug fixes
    - Visual block selection (CTRL + v)
    - Multiple-cursor aware
    - Macros (q)
    - Marks (m)
    - Quick find (/)
    - Support a subset of commands in :
    - Use :help for documentation on available commands
* Editor tabs in the source pane can now be rearranged
* Support for multiple cursors via Ctrl+Alt+Up/Down
* Alt+Enter to run code while retaining cursor position
* Comment/uncomment respects indent level when appropriate
* New Reformat Code command for R scripts
* Shift+Enter now searches backwards in Find/Replace
* Find All option added in Find/Replace
* New option to control comment continuation on insertion of new lines
* Updated to Ace (source editor component) v1.1.8
* Syntax highlighting modes for many new languages including Clojure, CoffeeScript, C#, Graphviz, Go, Groovy, Haskell, Java, Julia, Lisp, Lua, Matlab, Perl, Ruby, Rust, and Scala.
* A wide variety of new editor themes (color schemes) are now available.
* Increase file size limit to 5MB (was previously 2MB)

### C/C++ Development
    
 - Code completion
 - F2 code navigation (go to definition)
 - Go to file/function for C/C++
 - Find usages for C++ symbols
 - Intelligent auto-indentation
 - Scope tree for quick intra-file navigation

### Workspace

* Improved handling of objects containing or consisting of NULL externalptr

### Debugging

* Allow 'debugSource' to be executed in user-specified environment
* Improved heuristics for locating the stack frame where errors originated
* Autocompletions now available when debugging
* Improved debug stepping through statements wrapped in tryCatch()
* Better call frame selection when using recover()

### Packages

* Improvements to New Package:
    - Generate cleaner packages with no warnings
    - Respect various devtools options
* Support for roxygen2 'vignette' roclet
* Default to roxygenize for Build and Reload
* Improved checking for supported protocol with packrat package
* Escape backslashes in library names when loading packages
* Call to library after Build and Reload respects --library argument.

### Plots

* Render plots using devicePixelRatio for retina and HDPI screens

### R Markdown

* Updated to pandoc 1.13.1
* Improved handling of lists in editor
* Make publishing UI easier to discover
* Updated internal PDF viewer (PDF.js) to version 1.0.1040 

### Miscellaneous

* Updated rendering engine to Qt 5.4 for improved performance
* Windows: updated to MSYS SSH 1000.18
* Bind Cmd+Shift+K shortcut to Compile PDF and Preview HTML
* When evaluating R strings ensure 'try' is called from base package
* Add Clear Recent Projects menu item to toolbar project menu
* Command to sync current working directory and Files pane to project directory
* Eliminated rstudio and manipulate packages (both now available on CRAN)
* Added global RStudio.Version function for getting basic version info
* Diagram previews using the DiagrammeR package (requires recent version from GitHub).
* Added Markers pane and sourceMarker API for externals tools (e.g. linters)
* Enable specification of Sweave driver in Rnw magic comment

### Server

* Improved installation by reducing dependencies and providing additional platform-specific builds (e.g. SUSE, RHEL5 vs. RHEL6/7)
* Server Pro: Support for SPDY protocol

### Bug Fixes

* Prevent error dialog when getOption("repos") is an unnamed vector
* Fix for regex Find/Replace lockup with empty strings 
* Find in Files now always activates result pane
* Correctly reflow comments in Rmd C++ code chunks
* Ensure that rmarkdown documents render within input directory
* Eliminate race condition that could cause crash when polling child processes


