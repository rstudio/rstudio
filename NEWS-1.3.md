## v1.3 Patch 2 (Giant Goldenrod) - Release Notes


### Misc

- Compatible with R 4.1.0's graphics engine (#7253)
- Add realtime spellchecking to plain markdown files (#6988)
- Compatible with R graphics engine version 13 (R 4.1.0+) (#7253)
- R 4.0.0's raw string literals are now handled properly by the diagnostics engine. (#6788)
- Allow projects to reopen after a crash (#3220)
- Added spellcheck blacklist item for preview Latvian dictionary (#6594)
- Allow multiple space-separated domains in `www-frame-origin` for Tutorial API (Pro)
- Update `rstudioapi` highlightUi call to accept a callback variable containing an R script to be executed after the highlight element has been selected (#67565)
- Adds class attributed to RMarkdown chunks, their control buttons, and their output based on their given labels. (#6787)
- Add option `www-url-path-prefix` to force a path on auth cookies (Pro #1608)
- Add additional keyboard shortcut (Ctrl+`) for Focus Console Output accessibility command (#6850)
- Always set application role for screen readers and removed related accessibility preference checkbox (#6863)
- Update `NOTICE` for Ace editor license (#7102)

### Bugfixes

- Fix issue where files could not be uploaded when using RStudio Server load balancing (Pro #1751)
- Fix issue where the Crash Handler notification prompt would never go away (#7243)

### RStudio Server Pro

