## RStudio 2026.08.0 "Yellow Yarrow" Release Notes

### New
- ([#1634](https://github.com/rstudio/rstudio/issues/1634)): The Windows installer now lets you choose between installing for all users (the default, which prompts for administrator rights) and installing for the current user only (no administrator rights required). A current-user install no longer prompts for elevation, and uninstalling one now removes its files, Start Menu shortcut, and registry entries cleanly. Note for automated deployments: silent installs must now specify the install mode -- a bare `/S` exits with an error, so pass `/allusers /S` (the previous behavior) or `/currentuser /S`.

### Fixed
- ([#18174](https://github.com/rstudio/rstudio/issues/18174)): Fixed an error when viewing an object from the Object Explorer with the French user interface language enabled.
- ([#18255](https://github.com/rstudio/rstudio/issues/18255)): Fixed a regression where an unprivileged RStudio Server would fail to start when it could not read a system secure key file (for example, a root-owned key baked into an HPC container image), rather than falling back to its per-user cache.

### Dependencies
- Ace 1.43.5
- Copilot Language Server 1.520.0
- Electron 41.9.0
- Node.js 22.22.2 (copilot, Posit Assistant)
- Quarto 1.9.38
- xterm.js 6.0.0
