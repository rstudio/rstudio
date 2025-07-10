## RStudio 2025.08.0 "Cucumberleaf Sunflower" Release Notes

### New

#### RStudio

- (#15841): RStudio now highlights all keywords from the SQL 2023 standard in SQL documents 
- (#15919): RStudio now uses lobstr when computing object sizes
- (#16138): RStudio now supports version 17 of the R graphics engine
- (#16213): Improves legibility of highlighted code when RStudio debugger is active
- (#15945): Adds a user preference to disable showing the splash screen at startup
- (#15614): The splash screen closes when clicked with the mouse
- (#16111): Increases the default console buffer size to 10000 lines
- (#11661): The Zoom button in the RStudio Desktop plots pane now brings an existing zoomed plot window to the foreground 
- (#16009): When manually checking for updates (after previously ignoring an update), an option to stop ignoring updates prompts

#### Posit Workbench

- (#14083): Allows for custom certificate bundles for GitHub Copilot
- (rstudio-pro#5357): Allows strict enforcement of the user limit specified by the Posit Workbench product license

### Fixed

#### RStudio

- (#15482): Fixed an issue where RStudio would display a "Cannot reinitialise DataTable" error when viewing data sets 
- (#15133): Fixed an issue where pkgdown websites built outside of the user directory could not be viewed from RStudio Server
- (#14113): RStudio no longer displays factors with more than 64 levels as though they were character vectors
- (#15955): Fixed an issue where the "Save As" dialog would not be visible when trying to save an older git revision of a file
- (#15879): Fixed an issue where code indentation stopped working following code chunks containing only Quarto comments
- (#15979): Fixed an issue where RStudio could hang when attempting to execute notebook chunks without a registered handler
- (#12545): (Windows) "Use default 32bit / 64bit version of R" now always uses the default version of R set in the registry
- (#15923): Show an error message when the GitHub Copilot language server is missing
- (#15895): Fixed an issue where GitHub Copilot was unaware of files already loaded in the source editor before Copilot starts
- (#16133): Fixed an issue where RStudio's Update Packages dialog could report packages were out-of-date for packages installed into multiple library paths
- Fixed an issue where attempting to attach or detach a package using the Packages pane could cause UI to become out-of-sync with actual package state
- (#16119): Fixed an issue where GitHub Copilot's status was incorrectly reported as an error in the Preferences dialog
- (#16128): Fixed an issue where GitHub Copilot would not index project files when Copilot was started while the project is open
- (#15901): Fixed an issue where the entire document was sent to GitHub Copilot after each edit instead of just the changes
- (#16129): Fixed an issue where RStudio would send multiple didOpen messages to GitHub Copilot for the same file
- (#2900): Fixed issue where new R package projects did not inherit "Generate documentation with Roxygen" preference
- (#15919): Fixed an issue where large character vectors were shown with an NaN size in the environment pane
- (#15444): Fixed an issue where hitting the Escape key to close the "Update Available" dialog would exit RStudio
- (#16191): Fixed an issue where the splash screen would not close and the RStudio main window would not show when starting RStudio Desktop
- (#16198): Fixed an issue where the "Switch Focus between Source/Console" command would not work when the Visual Editor was active

#### Posit Workbench

- (rstudio-pro#8144): Fixed an issue where Positron State wasn't being loaded on login
- (rstudio-pro#7368): Fixed an issue where Shiny for Python and other applications would reguarly experience websocket failures in VS Code and Positron sessions

### Dependencies

- (#15935): Copilot Language Server 1.342.0
- (#15933): Electron 37.2.0
- (#16062): GWT 2.12.2
- (#15934): Quarto 1.7.32

### Deprecated / Removed

- (#15940): RStudio Server and Posit Workbench are no longer supported on Ubuntu Focal
- (#16104): Removed the "Limit visible console output" feature
- (rstudio-pro#8257): Removed publishing to Posit Cloud
