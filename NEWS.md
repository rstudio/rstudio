## RStudio 2026.04.0 "Globemaster Allium" Release Notes

### New
- ([#16657](https://github.com/rstudio/rstudio/issues/16657)): Added color preview support for YAML files, highlighting hex colors and named R colors
- ([#16734](https://github.com/rstudio/rstudio/issues/16734)): Added mouse wheel support for scrolling pane tabs when there are more tabs than can fit in the visible area
- (#rstudioapi/316): The documentNew API now permits arbitrary file types and extensions
- ([#3780](https://github.com/rstudio/rstudio/issues/3780)): Deleting files from Files pane on Linux Desktop now sends files to the Trash
- ([#3780](https://github.com/rstudio/rstudio/issues/3780)): On RStudio Desktop, the Files pane dropdown menu has a new option to control if deleted files go to Trash/Recycle Bin or are permanently deleted
- ([#16903](https://github.com/rstudio/rstudio/issues/16903)): Changed the default location of the Sidebar pane to be on the left side of the window
- ([#16942](https://github.com/rstudio/rstudio/issues/16942)): Enforce a minimum width for the Sidebar pane

### Fixed
- ([#16714](https://github.com/rstudio/rstudio/issues/16714)): Fixed an issue where formatting edits with air did not behave well with the editor undo stack
- ([#16732](https://github.com/rstudio/rstudio/issues/16732)): Fixed an issue where TabSet1 with no tabs assigned would show the Sidebar title
- ([#16733](https://github.com/rstudio/rstudio/issues/16733)): Fixed an issue where a Presentation tab would be added to TabSet2 when it was assigned to the Sidebar
- ([#16771](https://github.com/rstudio/rstudio/issues/16771)): Clarified in documentation that additional source columns are added to the left
- ([#8531](https://github.com/rstudio/rstudio/issues/8531)): Fixed an issue where table chunk outputs did not use all available space when printing
- ([#16740](https://github.com/rstudio/rstudio/issues/16740)): Fixed an issue with opening files from operating system file manager when RStudio had a secondary window open
- ([#16688](https://github.com/rstudio/rstudio/issues/16688)): Fixed an issue with pane layout when exiting RStudio with a zoomed column region
- ([#16798](https://github.com/rstudio/rstudio/issues/16798)): Fixed an issue where whole-word search and replace would not correctly match search terms containing dots
- ([#16814](https://github.com/rstudio/rstudio/issues/16814)): Fixed an issue where apostrophes in file names were displayed as HTML entities in the Files pane
- ([#16834](https://github.com/rstudio/rstudio/issues/16834)): Fixed an issue where an error message was shown on Windows when using "Write Diagnostics File"
- ([#16839](https://github.com/rstudio/rstudio/issues/16839)): Fixed an issue where an inaccessible folder in the PATH on Windows would lead to unnecessary file access attempts and error logging

### Dependencies
- Ace 1.43.5
- Copilot Language Server 1.409.0
- Electron 39.2.7
- Node.js 22.22.0 (copilot completions)
- Quarto 1.8.26
- xterm.js 6.0.0
