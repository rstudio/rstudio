## RStudio 2026.02.0 "Globemaster Allium" Release Notes

### New
#### RStudio
- ([#16734](https://github.com/rstudio/rstudio/issues/16734)): Added mouse wheel support for scrolling pane tabs when there are more tabs than can fit in the visible area
- (#rstudioapi/316): The documentNew API now permits arbitrary file types and extensions

#### Posit Workbench
-

### Fixed
#### RStudio
- ([#16714](https://github.com/rstudio/rstudio/issues/16714)): Fixed an issue where formatting edits with air did not behave well with the editor undo stack
- ([#16732](https://github.com/rstudio/rstudio/issues/16732)): Fixed an issue where TabSet1 with no tabs assigned would show the Sidebar title
- ([#16733](https://github.com/rstudio/rstudio/issues/16733)): Fixed an issue where a Presentation tab would be added to TabSet2 when it was assigned to the Sidebar
- ([#8531](https://github.com/rstudio/rstudio/issues/8531)): Fixed an issue where table chunk outputs did not use all available space when printing
- ([#16740](https://github.com/rstudio/rstudio/issues/16740)): Fixed an issue with opening files from operating system file manager when RStudio had a secondary window open
 
#### Posit Workbench
- 

### Upgrade Instructions

#### Posit Workbench

### Dependencies
- Ace 1.43.5
- Copilot Language Server 1.406.0
- Electron 38.7.0
- Node.js 22.21.1 (copilot completions)
- Node.js 22.20.0 (vscode server)
- Quarto 1.8.25
- Launcher 2.21.0
- rserver-saml 0.9.2
