## v1.3 - Release Notes

### Miscellaneous

* Files with extension '.q' are no longer indexed or parsed as R files. (#4696)
* Add automated crash handling and reporting
* Upgrade internal JSON parsing engine for speed improvements (#1830)
* Improved ergonomics for history prefix navigation (#2771)
* Make columns resizable in the Environment pane (#4020)
* Add Word Count command (#4237)
* Add "Safe Mode" for opening sessions without profile scripts or workspace restoration (#4338)
* PowerShell Core option in terminal (Windows-only)
* Custom terminal shell option for Windows desktop (previously only on Mac, Linux, and server)
* Keyboard shortcuts for main menu items on RStudio Server (e.g. Ctrl+Alt+F for File menu)

### Bugfixes

* Fix stale processes when invoking child R processes with large command lines (#3414)

