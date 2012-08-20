## Changes in RStudio v0.97

### Package development tools

This release includes a variety of tools to support R package development, including:
 
- A new Build tab with various package development commands and a view of build output and errors
- Build and Reload command that rebuilds the package and reloads it in a fresh R session
- Incremental reload is very fast and preserves the state of the previous R session, providing quick turnarounds and a much more interactive workflow for package development
- Additional commands for checking packages and building source and binary packages
- Ability to create a new package based on existing R source files
     
There are also a number of features aimed at making it easier to write R documentation:
  
- Preview and optional automatic preview on save for Rd files
- Spell-checking for Rd files
- Syntax highlighting, code-completion, and re-flowing for [Roxygen](http://roxygen.org/) comments
- Ability to automatically invoke Roxygen prior to package builds
 
There is also integration with the [devtools](https://github.com/hadley/devtools) package, including:

- Load All command and keyboard shortcut (Ctrl+Shift+L) that calls `devtools::load_all`
- All package installations respect the development library established by `devtools::dev_mode`
- Automatic restoration of `devtools::dev_mode` when reloading and restarting R sessions

We've also added support for building packages with [Rcpp](http://dirk.eddelbuettel.com/code/rcpp.html), including:

- Syntax highlighting for C/C++
- Quick navigation to gcc errors and warnings
- New project option to create "Package w/Rcpp"

### Source Editor

- Vim editing mode
- New options to show whitespace and indent guides 
- Middle-button paste for Linux
- Preserve editor selection and scroll position across reloads of the IDE

### Miscellaneous

- New Restart R command
- Install Packages dialog can now install both source and binary packages
- Support for OS X Lion full screen mode
- Always use native R png device on Linux
- Improved auto-scroll behavior in the console

### Bug Fixes

- Don't allow console width to be set to an invalid value
- Prevent selection of user-interface elements (only text can be selected)



