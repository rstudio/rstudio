## v1.3 Patch 1 (Water Lily) - Release Notes


### Misc

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

- Announce text of warnings bar via screen reader (#6963)
- Fix 'truncating string with embedded nuls' warning being emitted when saving R Notebook (#6932)
- Fix Compare Results and other incompatibility with newer versions of the `shinytest` package (#6960)
- Fix Terminal to work with both Git-Bash and RTools4 MSYS2 installed on Windows (#6696, #6809)
- Fix auto-activation of JAWS screen reader virtual cursor in Console Output region (#6884)
- Fix dependency installation for untitled buffers (#6762)
- Fix failure to open source files when debugging some functions in R 4.0.0 (work around R bug in `deparse()`) (#6854)
- Fix failure to use the first project template and default open files (#6865)
- Fix issue where R_LIBS_SITE could be forcibly set empty, overriding the value in /etc/R/REnviron (#6982)
- Fix keybinding failure when global keybindings exists but user keybindings don't (#6870)
- Fixed Chromium issue when using RStudio Desktop on Linux systems with newer glibc (#6379)
- Fixed install issue where service scripts would not be created if there was no /lib/systemd path (Pro #6710)
- Fixed issue where an attempt to create more sessions than the license limit would fail with a generic error (Pro #1680)
- Fixed issue where users could not save files in home directory if specified by UNC path (#6598)
- Fixed issue where file upload would fail when the file already existed (#7015)
- Fix sign out from the Admin Dashboard when behind a path-rewriting proxy (Pro #1709)
- Fix "Login as user" from the Admin Dashboard when using Launcher sessions (Pro #1710)
- Fix issue with first esc keypress being ignored (#7045)
- Fix issue with spellcheck not working with realtime turned off (#7068)
- Fix error when some HTML comments are included in R Markdown documents (#6997)
- Fix issue where toolbar buttons were missing on initialization (#7076)
- Fix error in Viewer pane when previewing Distill blogs (#6945)
- Fix misalignment of some number cells in the data viewer (#6975)
- Fix C++ autocompletion results missing on macOS in some contexts (#7097)
- Fix misleading errors report for `verify-installation` without Launcher (Pro #1718)

### RStudio Server Pro

- New option `server-project-sharing-root-dir` allows project sharing outside user home directories (Pro #1340)
- Update embedded nginx to 1.17.10
