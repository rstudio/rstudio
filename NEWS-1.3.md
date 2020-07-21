## v1.3 Patch 2 (Giant Goldenrod) - Release Notes

### Bugfixes

- Fix issue where files could not be uploaded when using RStudio Server load balancing (Pro #1751)
- Fix issue where the Crash Handler notification prompt would never go away (#7243)
- Fix issue with slow shutdown on Windows (#7117)
- Fix issue where Launcher debug logs could contain user's plain text password (Pro #1687)
- Fix issue where some log entries could not be displayed on the admin logs page (Pro #1783)
- Fix "TypeError" when sign-in using IE11 (#7359)
- Fix problem with moving Console between left and right columns (#7246)
- Fix issue with `rstudioapi::setCursorPosition()` not scrolling cursor into view (#7317)
- Fix issue with `rmarkdown` and `packrat` packages being eagerly loaded on IDE launch (#7265)
- Fix issue with folded chunk outputs getting stuck at top of IDE (#7293)
