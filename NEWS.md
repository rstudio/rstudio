## RStudio 2026.08.0 "Yellow Yarrow" Release Notes

### New
- ([#1634](https://github.com/rstudio/rstudio/issues/1634)): The Windows installer now lets you choose between installing for all users (the default, which prompts for administrator rights) and installing for the current user only (no administrator rights required). A current-user install no longer prompts for elevation, and uninstalling one now removes its files, Start Menu shortcut, and registry entries cleanly. Note for automated deployments: silent installs must now specify the install mode -- a bare `/S` exits with an error, so pass `/allusers /S` (the previous behavior) or `/currentuser /S`.
- ([#18158](https://github.com/rstudio/rstudio/issues/18158)): On macOS, the Files pane now follows Finder aliases: clicking an alias to a folder navigates to that folder, and clicking an alias to a file opens the file.

### Fixed
- ([#18174](https://github.com/rstudio/rstudio/issues/18174)): Fixed an error when viewing an object from the Object Explorer with the French user interface language enabled.

### Dependencies
- Ace 1.43.5
- Copilot Language Server 1.520.0
- Electron 41.9.0
- Node.js 22.22.2 (copilot, Posit Assistant)
- Quarto 1.9.38
- xterm.js 6.0.0
