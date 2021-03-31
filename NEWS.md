## RStudio 1.4 "Black Eyed Susan"

### Bugfixes

* Fixed an issue causing slow session startup and "Unable to connect to service" errors on RStudio Server (#9152)
* Fixed issue causing Project Sharing to fail to set Access Control Lists when using NFS v4 and `username@domain` security principals (Pro #2415)
* Fixed issue causing `verify-installation` to exit without showing the error that caused it to do so (Pro #2399)
* Add server homepage link and retry options to mitigate "Unable to connect to service" errors (Pro #2066)
* Fixed issue causing RStudio Server to create `.local/share/rstudio` folder with incorrect permissions when `session-timeout-kill-hours` is set (Pro #2388)
* Fixed issue causing spurious "Failed to reset ACL permission mask" errors to be logged outside shared projects on some filesystems (Pro #2406)
* Improved R session diagnostic logging; now records all instances of a session (Pro #2268)
* Log location of addins that raise parse errors at startup (#8012)


