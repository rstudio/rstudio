## RStudio 2026.05.0 "Golden Wattle" Release Notes

### New
- Added the `allow-package-source-recording` session option (default
  `true`); when set to `false`, RStudio will not annotate DESCRIPTION
  files of packages installed via `install.packages()` with the remote
  repository they came from.

### Fixed
- Reduced filesystem work performed by the `install.packages()` hook; the
  before/after scan is now scoped to the requested packages and their
  dependency closure rather than the entire library.
- Tightened the heuristic used to detect package-management commands at the
  console prompt, reducing spurious Packages pane refreshes triggered by
  identifiers like `updates <-` or `installed.packages()`.

### Fixed
- 

### Dependencies
-

- Ace 1.43.5
- Copilot Language Server 1.459.0
- Electron 39.8.7
- Node.js 22.22.2 (copilot, Posit AI)
- Quarto 1.9.36
- xterm.js 6.0.0
