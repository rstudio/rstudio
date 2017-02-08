## v1.1 - Release Notes

### Data Import

* Add support for all file encodings supported by R

### R Markdown

* Added option to specify the working directory when executing R Markdown chunks

### R Notebooks

* Fixed an issue where non-R chunk output could become duplicated
* 'Rename in Scope' now works within R chunks

### Terminal

* New Terminal tab for fluid shell interaction within the IDE
* Support for xterm emulation including color output and full-screen console apps
* Support for multiple terminals, each with persistent scrollback buffer

### Miscellaneous

* Added support for custom, user-provided project templates
* Implement support for changing editor tabs with the mouse wheel
* Add option to knit in current working directory or project directory
* 'Insert assignment operator' shortcut now works on Firefox
* Cmd/Ctrl+Enter in Help pane now sends selected example code to console
* View(vignette(...)) now opens editor with vignette source
* Ctrl+P/Ctrl+N to visit previous/next console history line (like readline)
* Ctrl+R to search console history incrementally (like readline)
* New "Copy To" command in Files pane to copy and rename in one step
* Server Pro: Add option to disable file uploads

### Bug Fixes

* Fixed an issue where dragging tabs out multiple times could revert contents to older version
* macOS: fixed 'crash on wake' issue with late-2016 Macbooks
* Fixed mangling of YAML header string values containing backticks 
* File downloads from the internal browser are now saved correctly on Linux and Windows

