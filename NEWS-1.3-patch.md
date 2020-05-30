## v1.3 Patch 1 (Water Lily) - Release Notes


### Misc

- Allow projects to reopen after a crash (#3220)
- Added spellcheck blacklist item for preview Latvian dictionary (#6594)
- Allow multiple space-separated domains in `www-frame-origin` for Tutorial API (Pro)
- Fix dependency installation for untitled buffers (#6762)
- Update `rstudioapi` highlightUi call to accept a callback variable containing an R script to be executed after the highlight element has been selected (#67565)
- Adds class attributed to RMarkdown chunks, their control buttons, and their output based on their given labels. (#6787)
- Add option `www-url-path-prefix` to force a path on auth cookies (Pro #1608)
- Add additional keyboard shortcut (Ctrl+`) for Focus Console Output accessibility command (#6850)
- Always set application role for screen readers and removed related accessibility preference checkbox (#6863)

### Bugfixes

- Fix 'truncating string with embedded nuls' warning being emitted when saving R Notebook (#6932)
- Fix dependency installation for untitled buffers (#6762)
- Fix Terminal to work with both Git-Bash and RTools4 MSYS2 installed on Windows (#6696, #6809)
- Fixed install issue where service scripts would not be created if there was no /lib/systemd path (Pro #6710)
- Fixed Chromium issue when using RStudio Desktop on Linux systems with newer glibc (#6379)
- Fixed issue where users could not save files in home directory if specified by UNC path (#6598)
- Fix failure to use the first project template and default open files (#6865)
- Fix keybinding failure when global keybindings exists but user keybindings don't (#6870)
- Fix failure to open source files when debugging some functions in R 4.0.0 (work around R bug in `deparse()`) (#6854)
- Fixed issue where an attempt to create more sessions than the license limit would fail with a generic error (Pro #1680)
- Fix auto-activation of JAWS screen reader virtual cursor in Console Output region (#6884)
- Announce text of warnings bar via screen reader (#6963)
- Fix issue where R_LIBS_SITE could be forcibly set empty, overriding the value in /etc/R/REnviron (#6982)

### RStudio Server Pro

- New option `server-project-sharing-root-dir` allows project sharing outside user home directories (Pro #1340)
- Fix issue where Launcher address could not be set to an external load balancer due to missing Host header (Pro #1681)
