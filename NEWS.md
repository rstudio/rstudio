## RStudio 2026.05.0 "Golden Wattle" Release Notes

### New
- Support for the upcoming R 4.6.0 release.
- Modal dialogs (Global Options, Project Options, etc.) now support dark theme styling when a dark editor theme is active, controlled by the new `use_dark_theme_modal_dialogs` preference
- ([#10296](https://github.com/rstudio/rstudio/issues/10296)): Modal dialogs (Global Options, Project Options, etc.) now support dark theme styling when a dark editor theme is active, controlled by the new `use_dark_theme_modal_dialogs` preference
- ([#17070](https://github.com/rstudio/rstudio/issues/17070)): Added support for the ANSI Erase in Line (EL / CSI K) escape sequence in the console, improving rendering of progress bars and status updates from CLI tools
- ([#16657](https://github.com/rstudio/rstudio/issues/16657)): Added color preview support for YAML files, highlighting hex colors and named R colors
- ([#16734](https://github.com/rstudio/rstudio/issues/16734)): Added mouse wheel support for scrolling pane tabs when there are more tabs than can fit in the visible area
- (#rstudioapi/316): The documentNew API now permits arbitrary file types and extensions
- ([#3780](https://github.com/rstudio/rstudio/issues/3780)): Deleting files from Files pane on Linux Desktop now sends files to the Trash
- ([#3780](https://github.com/rstudio/rstudio/issues/3780)): On RStudio Desktop, the Files pane dropdown menu has a new option to control if deleted files go to Trash/Recycle Bin or are permanently deleted
- ([#16903](https://github.com/rstudio/rstudio/issues/16903)): Changed the default location of the Sidebar pane to be on the left side of the window
- ([#16942](https://github.com/rstudio/rstudio/issues/16942)): Enforce a minimum width for the Sidebar pane
- ([#17269](https://github.com/rstudio/rstudio/issues/17269)): Added a close button to the Sidebar pane toolbar for quickly hiding the sidebar
- ([#17000](https://github.com/rstudio/rstudio/issues/17000)): Always show .positai folder in Files pane even when set to hide hidden files
- ([#16069](https://github.com/rstudio/rstudio/issues/16069)): Function auto-completion will now include arguments inherited via @inheritDotParams.
- ([#16530](https://github.com/rstudio/rstudio/issues/16530)): Added Posit Connect Cloud as a publishing target
- ([#15360](https://github.com/rstudio/rstudio/issues/15360)): Added `r-max-connections` session option to configure the maximum number of R connections (requires R >= 4.4.0)
- ([#17330](https://github.com/rstudio/rstudio/issues/17330)): Added Help > Release Notes command to open the RStudio release notes in the browser
- ([#17344](https://github.com/rstudio/rstudio/issues/17344)): Console process dialogs now support dark theme styling
- ([#16541](https://github.com/rstudio/rstudio/issues/16541)): Section headers now fold hierarchically based on heading level, matching Positron's default behavior
-

### Fixed
- 

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
