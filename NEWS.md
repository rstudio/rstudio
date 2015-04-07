
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
 - Completions for object names for 'formula' arguments, e.g. lm(|, data = mtcars)

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
* Configurable snippets for fast insertion of common blocks of code
* Editor tabs in the source pane can now be rearranged
* Support for multiple cursors via Ctrl+Alt+Up/Down
* Alt+Enter to run code while retaining cursor position
* Ctrl+Shift+P to expand selection to matching paren / brace
* Ctrl+Alt+Shift+P to select within matching parens / braces
* Comment/uncomment respects indent level when appropriate
* New Reformat Code command for R scripts
* New integrated linter for R scripts
* Shift+Enter now searches backwards in Find/Replace
* Find All option added in Find/Replace
* Ctrl+E now focuses the editor after finding from selection
* New option to control comment continuation on insertion of new lines
* Reflow text (comment) for markdown and plain text modes
* Updated to Ace (source editor component) v1.1.8
* Syntax highlighting modes for many new languages including Clojure,
  CoffeeScript, C#, Go, Groovy, Haskell, Java, Julia, Lisp, Lua, Matlab, Perl,
  Ruby, Rust, and Scala.
* Syntax highlighting for GraphViz and mermaid.js diagrams. 
* Diagram previews using the `DiagrammeR` package (requires recent version from GitHub).
* Syntax highlighting modes for many new languages including Clojure, CoffeeScript, C#, Graphviz, Go, Groovy, Haskell, Java, Julia, Lisp, Lua, Matlab, Perl, Ruby, Rust, Scala, and Stan.
* Keyword and text based code completion for many languages including JavaScript, HTML, CSS, Python, and SQL.
* A wide variety of new editor themes (color schemes) are now available.
* Increase file size limit to 5MB (was previously 2MB)

### C/C++ Development
    
 - Code completion
 - F2 code navigation (go to definition)
 - Go to file/function for C/C++
 - Find usages for C++ symbols
 - Intelligent auto-indentation
 - Scope tree for quick intra-file navigation
 
### Web Development (HTML/CSS/JavaScript)

 - Code completion
 - Inline diagnostics (JSHint) on syntax and other issues

### Workspace

* Improved handling of objects containing or consisting of NULL externalptr

### Debugging

* Allow 'debugSource' to be executed in user-specified environment
* Improved heuristics for locating the stack frame where errors originated
* Autocompletions now available when debugging
* Improved debug stepping through statements wrapped in tryCatch()
* Better call frame selection when using recover()
* Keyboard shortcuts for Step Into (Shift+F4) and Step Out (Shift+F6)

### Packages

* Improvements to New Package:
    - Generate cleaner packages with no warnings
    - Respect various devtools options
* Support for roxygen2 'vignette' roclet
* Insert Roxygen Skeleton command (Ctrl+Alt+Shift+R)
* Default to roxygenize for Build and Reload
* Improved checking for supported protocol with packrat package
* Escape backslashes in library names when loading packages
* Call to library after Build and Reload respects --library argument
* Validate that required versions are available for prompted installs

### Plots

* Render plots using devicePixelRatio for retina and HDPI screens

### R Markdown

* Updated to pandoc 1.13.1
* Ensure that .RData from Rmd directory isn't loaded during Knit
* Improved handling of lists in editor
* Syntax highlighting for comments in markdown documents
* Make publishing UI easier to discover
* Require save before previewing Rmd file
* Support for deploying single interactive documents (not just directories)
* Updated internal PDF viewer (PDF.js) to version 1.0.1040 

### Miscellaneous

* Updated rendering engine to Qt 5.4 for improved performance
* Windows: updated to MSYS SSH 1000.18
* Windows: check HKCU in addition to HKLM when scanning for R versions
* Windows: Use Rtools 3.3 when running under R 3.2
* Bind Cmd+Shift+K shortcut to Compile PDF and Preview HTML
* When evaluating R strings ensure 'try' is called from base package
* Default to current working directory for New Project from existing directory
* Add Clear Recent Projects menu item to toolbar project menu
* Command to sync current working directory and Files pane to project directory
* Eliminated rstudio and manipulate packages (both now available on CRAN)
* Added global RStudio.Version function for getting basic version info
* Diagram previews using the DiagrammeR package (requires recent version from GitHub).
* Added Markers pane and sourceMarker API for externals tools (e.g. linters)
* Enable specification of Sweave driver in Rnw magic comment
* Re-map prev/next tab shortcuts to eliminate conflicts with window managers
* Run App command for single file Shiny applications
* Deprecated 'source.with.encoding' in favor of source(..., encoding = "...")

### Server

* Add server-set-umask option to control whether the server sets its umask to 022 at startup
* Improved installation by reducing dependencies and providing additional platform-specific builds (e.g. SUSE, RHEL5 vs. RHEL6/7)
* Server Pro: Support for SPDY protocol
* Server Pro: Custom header name for proxied authentication
* Server Pro: Option to eliminate "stay signed in" option for PAM authentication.

### Bug Fixes

* Prevent error dialog when getOption("repos") is an unnamed vector
* Fix for regex Find/Replace lockup with empty strings 
* Fix for console text unselectable in Firefox
* Find in Files now always activates result pane
* Correctly reflow comments in Rmd C++ code chunks
* Don't warn when saving C/C++ file with .hpp extension on OS X
* Ensure that rmarkdown documents render within input directory
* Eliminate race condition that could cause crash when polling child processes
* Correct handling for breakpoints in files with non-ascii filenames on Windows
* Next/previous word behavior in Rmd is now consistent with behavior in R scripts
* Don't parse YAML front matter if not preceded by whitespace
* Only hide object files (e.g. *.o) in Files pane when in package src directory
* Ensure cursor is always visible after source navigation
* Server: Ensure that LANG is populated from system default when not inherited
* Server: Provide required domain socket permissions during startup
* IE 11: Strip unprintable unicode characters in Rmd front matter dates
* Only filter object file listings in 'src' directory
* Fix crash in R tokenizer when source files have binary 0xFFF
* Correctly navigate to package inst/include directory for template errors 
* Fix visual debugging issues when code isn't saved in system encoding


