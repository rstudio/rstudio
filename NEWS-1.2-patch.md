
## RStudio v1.2 Patch 2 "Elderflower"

### Misc

* Fully reset Connections pane objects when refreshing (#2136)	
* Unset `DYLD_INSERT_LIBRARIES` after launch on macOS to prevent spurious library load errors (#5313)
* Eliminate warnings when using `_R_CHECK_LENGTH_1_LOGIC2_` (#5268, #5363)
* Fix plain serif/sans-serif font rendering on macOS Catalina (#5525)

### Server Pro

* Add ability to specify multiple R versions with same path but differing labels (#1034)
* Include child processes and timing information in session diagnostic traces (#1192) 
* Adds better Session UI/UX including sorting, label viewing, and creation visibility (#1215)
