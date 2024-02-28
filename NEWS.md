## RStudio 2024.04.0 "Chocolate Cosmos" Release Notes

### New

#### RStudio
- RStudio now requires R 3.6.0 or newer. (#14210)
- RStudio's auto-completion system now supports ggplot2 aesthetic names and data columns (#8444)
- RStudio's auto-completion system now supports the display of the "label" attribute (#14242)
- RStudio Desktop on Windows and Linux supports auto-hiding the menu bar (#8932)
- RStudio Desktop on Windows and Linux supports full-screen mode via F11 (#3243)
- R projects can be given a custom display name in Project Options (#1909)
- RStudio now highlights and lints Quarto chunk options in Python code chunks
- RStudio no longer highlights `\[ \]` and `\( \)` Mathjax equations; prefer `$$ $$` and `$ $` instead (#12862)
- Added cmake option to build RStudio without the check for updates feature (#13236)
- Allow choosing R from non-standard location at startup (#14180; Windows Desktop)
- Add `EnvironmentFile` support to systemd service definitions (#13819)
- RStudio's GWT sources can now be built with JDKs > 11 (#11242)
- Show grey background instead of solid-white during Desktop startup (#13768)
- The 'restartSession()' API method gains the 'clean' argument. (#2841)
- 'dot' chunks in R Markdown documents are now executable (#14063)
- (rstudioapi) Fixed an issue where selectFile() did not parse filter strings in a cross-platform way (#13994)
- Show Quarto version information in the About dialog (#14263)
- RStudio now reports repository validation errors (if any) when adding secondary repositories in the Global Options -> Packages pane (#13842)
- The working directory of a background job now defaults to the .Rproj location when a project is open (#12600)
- Add search results copy button and search results breadcrumbs to RStudio User Guide (#13618, #14069)

#### Posit Workbench
- Show custom project names on Workbench homepage (rstudio-pro#5589)
- Add search results copy button and search results breadcrumbs to Workbench Administration Guide, Workbench User Guide, RStudio Desktop Pro Administration Guide (#5088, #5603)

### Fixed
#### RStudio
- Fixed an issue where Stage chunk and Stage line in the Review Changes UI failed in some scenarios (#5476)
- Fixed shortcut for inserting an assignment operator to work on non-US English keyboards (#12457)
- Fixed an issue where the menubar would show on secondary windows if Alt key was pressed (#13973)
- Fixed Windows installer to delete Start Menu shortcut during uninstall (#13936)
- Fixed current Git branch not always showing correctly in external editor windows (#14029)
- Fixed tooltip to show correct keyboard shortcut when hovering over URLs in the editor (#12504)
- Fixed Save As dialog on Windows not showing Save As Type field when extensions are hidden (#12965)
- Fixed GitHub Copilot project preferences not showing correct status message (#14064)
- Fixed an issue where Quarto chunk option completions were not displayed at the start of a comment (#14074)
- Fixed an issue where pipes containing a large number of comments were not indented correctly (#12674)
- Fixed an issue where RStudio would unnecessarily list directory contents when opening a file (#14096)
- Localize Copilot-related user interface strings into French (#14092)
- Improved highlighting of YAML chunk options for Quarto Documents (#13836)
- Removed obsolete "Use Internet Explorer library/proxy" checkbox from Packages settings (#13250)
- Improved error handling for Desktop Pro license handling (rstudio-pro#4873)
- Fixed exception being logged when copying or cutting from editor in a separate window (#14140)
- Fixed an issue where RStudio's R diagnostics warned about potentially missing arguments even when disabled via preferences (#14046)
- Fixed an issue where the Visual Editor's toolbar controls were duplicated on format change (#12227)
- Fixed regression that caused extra whitespace at bottom of some popups (#14223)
- Fix type dropdowns not working in dataset import when user-interface is in French (#14224)
- Fixed an issue where RStudio failed to retrieve help for certain S3 methods (#14232)
- Fixed a regression where the Data Viewer did not display 'variable.labels' for columns (#14265)
- Fixed an issue where autocompletion help was not properly displayed for development help topics (#14273)
- Fixed an issue where Shiny onSessionEnded callbacks could be interrupted when stopped in RStudio (#13394)
- Fixed Copyright date ranges for Release Notes and RStudio IDE User Guide (#14078)
- Fixed mis-encoded Hunspell dictionaries (#8147)
- Improved responsiveness of C / C++ editor intelligence features when switching Git branches (#14320)

#### Posit Workbench

- Fixed Copyright date ranges for Workbench Administrator Guide and Workbench User Guide (rstudio-pro#5865)

### Dependencies
- Updated Ace to version 1.32.5 (#14227; Desktop + Server)
- Updated Electron to version 28.2.2 (#14055; Desktop)
- Updated GWT to version 2.10.0 (#11505; Desktop + Server)
- Updated NSIS to version 3.09 (#14123; Windows Desktop)
- Updated OpenSSL to version 3.1.4 (Windows Desktop)

