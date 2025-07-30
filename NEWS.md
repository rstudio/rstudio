## RStudio 2025.08.0 "Cucumberleaf Sunflower" Release Notes

### New

#### RStudio

- ([#15841](https://github.com/rstudio/rstudio/issues/15841)): RStudio now highlights all keywords from the SQL 2023 standard in SQL documents 
- ([#15919](https://github.com/rstudio/rstudio/issues/15919)): RStudio now uses lobstr when computing object sizes
- ([#16138](https://github.com/rstudio/rstudio/issues/16138)): RStudio now supports version 17 of the R graphics engine
- ([#16213](https://github.com/rstudio/rstudio/issues/16213)): Improves legibility of highlighted code when RStudio debugger is active
- ([#15945](https://github.com/rstudio/rstudio/issues/15945)): Adds a user preference to disable showing the splash screen at startup
- ([#15614](https://github.com/rstudio/rstudio/issues/15614)): The splash screen closes when clicked with the mouse
- ([#16111](https://github.com/rstudio/rstudio/issues/16111)): Increases the default console buffer size to 10000 lines
- ([#11661](https://github.com/rstudio/rstudio/issues/11661)): The Zoom button in the RStudio Desktop plots pane now brings an existing zoomed plot window to the foreground 
- ([#16009](https://github.com/rstudio/rstudio/issues/16009)): When manually checking for updates (after previously ignoring an update), an option to stop ignoring updates prompts
- ([#15988](https://github.com/rstudio/rstudio/issues/15988)): Posit Product Documentation theme v7.0.0; adds cookie consent, several style updates, accessibility fixes, dark theme improvements
- ([#16214](https://github.com/rstudio/rstudio/issues/16214)): Introduces a new structure for release note entries
- ([#16226](https://github.com/rstudio/rstudio/issues/16226)): Added option (off by default) to zoom RStudio Desktop UI with Ctrl/Cmd+mouse wheel

#### Posit Workbench

- ([#14083](https://github.com/rstudio/rstudio/issues/14083)): Allows for custom certificate bundles for GitHub Copilot
- (rstudio-pro#5357): Allows strict enforcement of the user limit specified by the Posit Workbench product license
- (rstudio-pro#7533): Posit Product Documentation theme v7.0.1; adds cookie consent, several style updates, accessibility fixes, dark theme improvements
- (rstudio-pro#8186): Enables Positron Pro sessions by default
- (rstudio-pro#7599): Backs up existing Positron Pro configuration files and installs defaults during upgrade
- (rstudio-pro#8090): Improves integration with Posit Package Manager, including: repository discovery, authentication, and improved Package Manager feature visibility in the RStudio IDE
- (rstudio-pro#5068): Adds support for just-in-time provisioning of local groups for Proxied Auth, OpenID Connect, and SAML Authentication
- (rstudio-pro#7817): R and Python scripts in Positron and VS Code now have a `Run Script in a Workbench Job` action in the editor toolbar (and matching command).
- (rstudio-pro#8463): The Workbench Jobs pane in Positron and VS Code is now organized by script, similar to the Explorer pane, and uses more familiar VS Code UI conventions. Clicking on a script will open it in the editor, while clicking on a job will open a virtual `Job Details` document
- (rstudio-pro#7862): Completed jobs can now be individually "dismissed" from the Workbench Jobs pane in Positron and VS Code. There are also new `Dismiss Completed Jobs` and `Restore Completed Jobs` view actions to (un)hide *all* completed jobs. Note that hidden jobs are not persistent across sessions
- (rstudio-pro#8464): A new setting allows users to launch Workbench jobs from Positron and VS Code that make use of managed credentials. This feature is in preview and is not enabled by default

### Fixed

#### RStudio

- ([#15482](https://github.com/rstudio/rstudio/issues/15482)): Fixed an issue where RStudio would display a "Cannot reinitialise DataTable" error when viewing data sets 
- ([#15133](https://github.com/rstudio/rstudio/issues/15133)): Fixed an issue where pkgdown websites built outside of the user directory could not be viewed from RStudio Server
- ([#14113](https://github.com/rstudio/rstudio/issues/14113)): RStudio no longer displays factors with more than 64 levels as though they were character vectors
- ([#15955](https://github.com/rstudio/rstudio/issues/15955)): Fixed an issue where the "Save As" dialog would not be visible when trying to save an older git revision of a file
- ([#15879](https://github.com/rstudio/rstudio/issues/15879)): Fixed an issue where code indentation stopped working following code chunks containing only Quarto comments
- ([#15979](https://github.com/rstudio/rstudio/issues/15979)): Fixed an issue where RStudio could hang when attempting to execute notebook chunks without a registered handler
- ([#12545](https://github.com/rstudio/rstudio/issues/12545)): (Windows) "Use default 32bit / 64bit version of R" now always uses the default version of R set in the registry
- ([#15923](https://github.com/rstudio/rstudio/issues/15923)): Show an error message when the GitHub Copilot language server is missing
- ([#15895](https://github.com/rstudio/rstudio/issues/15895)): Fixed an issue where GitHub Copilot was unaware of files already loaded in the source editor before Copilot starts
- ([#16133](https://github.com/rstudio/rstudio/issues/16133)): Fixed an issue where RStudio's Update Packages dialog could report packages were out-of-date for packages installed into multiple library paths
- (rstudio-pro#8159): Fixed an issue where attempting to attach or detach a package using the Packages pane could cause UI to become out-of-sync with actual package state
- ([#16119](https://github.com/rstudio/rstudio/issues/16119)): Fixed an issue where GitHub Copilot's status was incorrectly reported as an error in the Preferences dialog
- ([#16128](https://github.com/rstudio/rstudio/issues/16128)): Fixed an issue where GitHub Copilot would not index project files when Copilot was started while the project is open
- ([#15901](https://github.com/rstudio/rstudio/issues/15901)): Fixed an issue where the entire document was sent to GitHub Copilot after each edit instead of just the changes
- ([#16129](https://github.com/rstudio/rstudio/issues/16129)): Fixed an issue where RStudio would send multiple didOpen messages to GitHub Copilot for the same file
- ([#2900](https://github.com/rstudio/rstudio/issues/2900)): Fixed issue where new R package projects did not inherit "Generate documentation with Roxygen" preference
- ([#15919](https://github.com/rstudio/rstudio/issues/15919)): Fixed an issue where large character vectors were shown with an NaN size in the environment pane
- ([#15444](https://github.com/rstudio/rstudio/issues/15444)): Fixed an issue where hitting the Escape key to close the "Update Available" dialog would exit RStudio
- ([#16191](https://github.com/rstudio/rstudio/issues/16191)): Fixed an issue where the splash screen would not close and the RStudio main window would not show when starting RStudio Desktop
- ([#16198](https://github.com/rstudio/rstudio/issues/16198)): Fixed an issue where the "Switch Focus between Source/Console" command would not work when the Visual Editor was active
- ([#12470](https://github.com/rstudio/rstudio/issues/12470)): Fixed an issue in RStudio Desktop on Windows where creating multiple cursors using Alt + the mouse would move focus to the menu bar
- ([#15797](https://github.com/rstudio/rstudio/issues/15797)): Changed label in readr-based text import dialog from "factors" to "factor levels"
- (#16266): RStudio now resolves xdg-open on the PATH, rather than assuming it is located at /usr/bin/xdg-open

#### Posit Workbench

- (rstudio-pro#8144): Fixed an issue where Positron State wasn't being loaded on login
- (rstudio-pro#7368): Fixed an issue where Shiny for Python and other applications would reguarly experience websocket failures in VS Code and Positron sessions
- (rstudio-pro#7818): Workbench jobs launched from Positron and VS Code now set the working directory correctly even for scripts outside of the current workspace
- (rstudio-pro#7861): Completed jobs in the Workbench Jobs pane in Positron and VS Code no longer have a non-functional `Stop Workbench Job` action
- (rstudio-pro#8088): Workbench jobs launched from Positron and VS Code no longer ignore queues, resource limits, or resource profiles
- (rstudio-workbench-vscode-ext#291): Long-running Workbench jobs launched from Positron and VS Code no longer display incorrect runtimes

### Upgrade Instructions

#### Posit Workbench

With this release, Positron Pro sessions are moving from preview to General Availability (GA). The upgrade process will automatically back up any existing Positron Pro configuration files to ensure production-ready defaults are properly installed.

During package upgrade, the following files will be backed up if they exist:
- `/etc/rstudio/positron.conf` as `/etc/rstudio/positron.conf.bak`
- `/etc/rstudio/positron-user-settings.conf` as `/etc/rstudio/positron-user-settings.conf.bak`

After completing the package upgrade, carefully review the backed up files and the new default configuration files and merge any customizations as needed.

### Dependencies

- ([#15935](https://github.com/rstudio/rstudio/issues/15935)): Copilot Language Server 1.349.0
- ([#15933](https://github.com/rstudio/rstudio/issues/15933)): Electron 37.2.4
- ([#16062](https://github.com/rstudio/rstudio/issues/13924)): GWT 2.12.2
- ([#15934](https://github.com/rstudio/rstudio/issues/15934)): Quarto 1.7.32

### Deprecated / Removed

- ([#15940](https://github.com/rstudio/rstudio/issues/15940)): RStudio Server and Posit Workbench are no longer supported on Ubuntu Focal
- ([#16104](https://github.com/rstudio/rstudio/issues/16104)): Removed the "Limit visible console output" feature
- (rstudio-pro#8257): Removed publishing to Posit Cloud
- ([#16233](https://github.com/rstudio/rstudio/issues/16233)): Removed the "Send automated crash reports" feature from RStudio Desktop
