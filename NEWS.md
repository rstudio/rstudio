## RStudio 2026.08.0 "Yellow Yarrow" Release Notes

### New
- ([#1634](https://github.com/rstudio/rstudio/issues/1634)): The Windows installer now lets you choose between installing for all users (the default, which prompts for administrator rights) and installing for the current user only (no administrator rights required). A current-user install no longer prompts for elevation, and uninstalling one now removes its files, Start Menu shortcut, and registry entries cleanly. Note for automated deployments: silent installs must now specify the install mode -- a bare `/S` exits with an error, so pass `/allusers /S` (the previous behavior) or `/currentuser /S`.
- ([#18153](https://github.com/rstudio/rstudio/issues/18153)): Added a preference (General > Basic) to disable the "What's New" window that RStudio Desktop shows after updating to a new version.
- ([#18158](https://github.com/rstudio/rstudio/issues/18158)): On macOS, the Files pane now follows Finder aliases: clicking an alias to a folder navigates to that folder, and clicking an alias to a file opens the file.
- ([#9924](https://github.com/rstudio/rstudio/issues/9924)): The Files pane now shows a link indicator on symbolic links and macOS Finder aliases, with the link target shown in the icon's tooltip.
- ([#8715](https://github.com/rstudio/rstudio/issues/8715)): Inline LaTeX / math previews in the source editor and visual editor are now rendered with MathJax 4 (previously MathJax 2.7), adding support for the TeX input extensions introduced in MathJax 3 and later. Rendered R Markdown documents using `mathjax = "local"` continue to use the bundled MathJax 2.7.

### Fixed
- ([#18152](https://github.com/rstudio/rstudio/issues/18152)): Fixed a compilation error when building RStudio Server against SOCI 4.1.4 or newer.
- ([#18174](https://github.com/rstudio/rstudio/issues/18174)): Fixed an error when viewing an object from the Object Explorer with the French user interface language enabled.
- ([#18198](https://github.com/rstudio/rstudio/issues/18198)): Fixed tar errors and warnings printed to the console after installing a package from a URL with `install.packages(..., repos = NULL)`.
- ([#18197](https://github.com/rstudio/rstudio/issues/18197)): Fixed an issue where the Render button failed to render a Quarto document living within a sub-directory of a Quarto project.
- ([#18208](https://github.com/rstudio/rstudio/issues/18208)): Fixed an issue in RStudio Server where requests could fail with "Unable to connect to service" for 30 seconds or more while a suspended session was relaunching.
- ([#17650](https://github.com/rstudio/rstudio/issues/17650)): Fixed unreadable label text in dark modal dialogs when using third-party themes (e.g. rsthemes) that style dialog labels for light backgrounds.

### Dependencies
- Ace 1.43.5
- MathJax 4.1.3 (inline LaTeX / math previews)
- Copilot Language Server 1.520.0
- Electron 41.9.0
- Node.js 22.22.2 (copilot, Posit Assistant)
- Quarto 1.9.38
- xterm.js 6.0.0
