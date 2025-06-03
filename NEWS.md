## RStudio 2025.08.0 "Cucumberleaf Sunflower" Release Notes

### New

#### RStudio

- RStudio now highlights all keywords from the SQL 2023 standard in SQL documents (#15841)
- Improved legibility of highlighted code when RStudio debugger is active
- Added a user preference to disable showing the splash screen at startup (#15945)
- The splash screen now closes when clicked with the mouse (#15614)

#### Posit Workbench

- Allow for custom certificate bundles for GitHub Copilot (#14083)

### Fixed

#### RStudio

- Fixed an issue where pkgdown websites built outside of the user directory could not be viewed from RStudio Server (#15133)
- RStudio no longer displays factors with more than 64 levels as though they were character vectors (#14113)
- Fixed an issue where the "Save As" dialog would not be visible when trying to save an older git revision of a file (#15955)
- Fixed an issue where code indentation stopped working following code chunks containing only Quarto comments (#15879)
- Fixed an issue where RStudio could hang when attempting to execute notebook chunks without a registered handler (#15979)
- (Windows) "Use default 32bit / 64bit version of R" now always uses the default version of R set in the registry (#12545)
- Show an error message when the GitHub Copilot language server is missing (#15923)
- Fixed an issue where GitHub Copilot was unaware of files already loaded in the source editor before Copilot starts (#15895)

#### Posit Workbench

- Fixed an issue where Positron State wasn't being loaded on login (rstudio-pro#8144)
- Fixed an issue where Shiny for Python and other applications would reguarly experience websocket failures in VS Code and Positron sessions (rstudio-pro#7368)

### Dependencies

- Copilot Language Server 1.328.0 (#15935)
- Electron 36.3.2 (#15933)
- GWT 2.12.2 (#16062)
- Quarto 1.7.31 (#15934)

### Deprecated / Removed

- RStudio Server and Posit Workbench are no longer supported on Ubuntu Focal (#15940)
- The "Limit visible console output" feature has been removed (#16104)
