## v1.1 - Release Notes

### Data Import

* Add support for all file encodings supported by R
* Restore text import dialog from previous releases (uses only base R functions)

### R Markdown

* Added option to specify the working directory when executing R Markdown chunks

### R Notebooks

* Python chunks now respect virtualenv if present
* 'Rename in Scope' now works within R chunks
* Fixed an issue where non-R chunk output could become duplicated

### Terminal

* New Terminal tab for fluid shell interaction within the IDE
* Support for xterm emulation including color output and full-screen console apps
* Support for multiple terminals, each with persistent scrollback buffer

### Miscellaneous

* Added support for custom, user-provided project templates
* Implement support for changing editor tabs with the mouse wheel
* Snippets can now be inserted in the R console
* Add option to knit in current working directory or project directory
* Cmd/Ctrl+Enter in Help pane now sends selected example code to console
* View(vignette(...)) now opens editor with vignette source
* Ctrl+P/Ctrl+N to visit previous/next console history line (like readline)
* Ctrl+R to search console history incrementally (like readline)
* New "Copy To" command in Files pane to copy and rename in one step
* Debugger support for R 3.3.3 and above
* F2 in source editor opens data frame under cursor in a new tab
* Highlight markdown inside ROxygen comments
* Improve performance of autocompletion for installed packages
* Server Pro: Add option to disable file uploads
* Server Pro: Upgrade to TurboActivate 4.0; improves licensing
* Server Pro: Add support for floating (lease-based) licenses

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

