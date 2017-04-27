## v1.1 - Release Notes

### Connections

* New Connections tab for working with a wide variety of data sources
* Connections to data sources are saved and can be easily reconnected and reused
* Objects inside data sources can be browsed and contents viewed inside RStudio
* Works with ODBC data sources and Spark, and can integrate with other R packages

### Terminal

* New Terminal tab for fluid shell interaction within the IDE
* Support for xterm emulation including color output and full-screen console apps
* Support for multiple terminals, each with persistent scrollback buffer
* Web links in terminal can be clicked and opened in default browser (new tab for server)
* Windows terminal supports multiple terminal shell types
  * Git Bash, if installed
  * Command Prompt (cmd.exe), 32-bit and 64-bit depending on OS support
  * PowerShell, 32-bit and 64-bit depending on OS support
  * Bash on Windows Subsystem for Linux, if installed on 64-bit Windows 10
* Default Windows terminal shell type set in new Global Options/Terminal preferences pane

### Object Explorer

* New view for the exploration of hierarchical / deeply-nested R objects
* Can recursively inspect R lists, environments, functions, S4 objects
* Integration with the xml2 package for exploration of XML documents
* Generate R code that can be used to access a particular item within object

### Themes

* New flat, modern UI theme
* New dark theme option
* Retina-quality icons throughout

### Data Import

* Add support for all file encodings supported by R
* Restore text import dialog from previous releases (uses only base R functions)
* Code generator in import dialog now creates a relative path to data file 

### R Markdown

* Added option to specify the working directory when executing R Markdown chunks
* Added option to set preview mode (in viewer, window, etc.) in YAML header
* Added option to skip knitting before publishing in YAML header

### R Notebooks

* Python chunks now respect virtualenv if present
* 'Rename in Scope' now works within R chunks
* Fixed an issue where non-R chunk output could become duplicated
* Added option to set notebook mode in the document's YAML header
* Allow setting default chunk connection option to raw connection object

### Miscellaneous

* ANSI escape code support in console for colored output
* Add support for custom, user-provided project templates
* Add support for creating new Git branches and adding remotes
* Document cursor position is now saved and restored between RStudio sessions
* Add support for changing editor tabs with the mouse wheel
* Snippets can now be inserted in the R console
* Add option to knit in current working directory or project directory
* Cmd/Ctrl+Enter in Help pane now sends selected example code to console
* View(vignette(...)) now opens editor with vignette source
* Ctrl+P/Ctrl+N to visit previous/next console history line (like readline)
* Ctrl+R to search console history incrementally (like readline)
* New "Copy To" command in Files pane to copy and rename in one step
* F2 in source editor opens data frame under cursor in a new tab
* Highlight markdown inside ROxygen comments
* Improve performance of autocompletion for installed packages
* Add option to run multiple consecutive lines of R with Ctrl+Enter
* Add commands to run a line, statement, or consecutive lines 
* Add Clear Console button to top of Console pane
* Add option to wrap around when navigating to previous or next editor tab
* Allow opening directories as projects (Server and macOS only)
* Show disambiguation in overflow list when two editor tabs have the same filename
* Respect control characters in error output; makes e.g. curl output correct
* Add new cheat sheet links to Help: Data Import, Interfacing Spark
* Server Pro: Add option to disable file uploads
* Server Pro: Upgrade to TurboActivate 4.0; improves licensing
* Server Pro: Add support for floating (lease-based) licenses
* Server Pro: Show the size of suspended sessions
* Server Pro: Add user-defined session labels
* Server Pro: Upgrade to nginx 1.12.0
* Server Pro: Add support for NFSv4 Access Control Lists in Project Sharing

### Bug Fixes

* Fixed an issue where dragging tabs out multiple times could revert contents to older version
* macOS: fixed 'crash on wake' issue with late-2016 Macbooks
* Fixed mangling of YAML header string values containing backticks 
* File downloads from the internal browser are now saved correctly on Linux and Windows
* Rendering or running an R Markdown document no longer saves it unnecessarily
* 'Insert assignment operator' shortcut now works on Firefox
* Fix hang when replacing a misspelled word with word and punctuation
* Editor now responds correctly when renaming an open file using the Files pane
* Fixed an issue that could cause the data viewer cache to contain orphaned entries
* Fixed highlighting of Markdown text within Markdown blockquote
* Invoke R without --vanilla when building R Markdown websites
* Fixed an issue in which R Markdown documents could get stuck in notebook mode
* Fixed an issue preventing plain .md files from being published if not rendered
* Fixed runtime crashes in R packages that use Boost libraries
* Fixed startup crashes associated with Boost regular expressions
* Improve stability of crashed session recovery system
* Fixed issues arising from restoring a session suspended with a different R version
* Wait for index.lock file to clear before performing git operations (with recovery)
* Server Pro: Fix issue with dirty indicator/saving after collaborative editing ends

