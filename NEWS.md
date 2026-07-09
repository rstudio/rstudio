## RStudio 2026.08.0 "Yellow Yarrow" Release Notes

### New
- ([#1634](https://github.com/rstudio/rstudio/issues/1634)): The Windows installer now lets you choose between installing for all users (the default, which prompts for administrator rights) and installing for the current user only (no administrator rights required). A current-user install no longer prompts for elevation, and uninstalling one now removes its files, Start Menu shortcut, and registry entries cleanly. Note for automated deployments: silent installs must now specify the install mode -- a bare `/S` exits with an error, so pass `/allusers /S` (the previous behavior) or `/currentuser /S`.

### Fixed

### Dependencies
- Ace 1.43.5
- Copilot Language Server 1.509.1
- Electron 41.9.0
- Node.js 22.22.2 (copilot, Posit Assistant)
- Quarto 1.9.38
- xterm.js 6.0.0
