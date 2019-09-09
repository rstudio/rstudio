## v1.3 - Release Notes

### renv

* New projects can be initialized with renv, giving them an isolated project environment

### Miscellaneous

* RStudio builds on macOS are now notarized and use the hardened runtime
* Improved 'Comment / Uncomment' handling of empty lines around selection (#4163)
* Files with extension '.q' are no longer indexed or parsed as R files (#4696)
* Add automated crash handling and reporting
* Upgrade internal JSON parsing engine for speed improvements (#1830)
* Improved ergonomics for history prefix navigation (#2771)
* Make columns resizable in the Environment pane (#4020)
* Add Word Count command (#4237)
* Add "Safe Mode" for opening sessions without profile scripts or workspace restoration (#4338)
* PowerShell Core option in terminal (Windows-only)
* Custom terminal shell option for Windows desktop (previously only on Mac, Linux, and server)
* Keyboard shortcuts for main menu items on RStudio Server (e.g. Ctrl+Alt+F for File menu)
* Show number of characters when entering version control commit messages (#5192)

### Bugfixes

* Fix plot history when plot() called immediately after dev.off() (#3117)
* Fix diagnostics error with multibyte characters in R Markdown documents on Windows (#1866)
* Fix stale processes when invoking child R processes with large command lines (#3414)

