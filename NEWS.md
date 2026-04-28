## RStudio 2026.05.0 "Golden Wattle" Release Notes

### New
- ([#15614](https://github.com/rstudio/rstudio/issues/15614)): The splash screen can again be dismissed with a mouse click or key press.
- ([#16541](https://github.com/rstudio/rstudio/issues/16541)): Section headers now fold hierarchically based on heading level, matching Positron's default behavior

### Fixed
- ([#17440](https://github.com/rstudio/rstudio/issues/17440)): Fixed an issue where triggering tab completion inside `[` on a large Matrix-package sparse matrix could hang RStudio and exhaust system memory
- ([#17176](https://github.com/rstudio/rstudio/issues/17176)): Fixed a startup hang when opening a Quarto project containing large directories (e.g. `_targets/`).
- ([#16067](https://github.com/rstudio/rstudio/issues/16067)): Raise the open file descriptor soft limit at session startup to avoid "Too many open files" errors during project file monitoring on Linux.
- ([#17417](https://github.com/rstudio/rstudio/issues/17417)): Added missing French translations for newer commands and preferences, removed an orphaned French key, deduplicated stale entries in the French application strings, and normalized line endings in five English string files.

### Dependencies
- Ace 1.43.5
- Copilot Language Server 1.459.0
- Electron 41.3.0
- Node.js 22.22.2 (copilot, Posit AI)
- Quarto 1.9.36
- xterm.js 6.0.0
