## RStudio 2024.07.0 "Cranberry Hibiscus" Release Notes

### New

#### RStudio


#### Posit Workbench

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
- Remove superfluous Uninstall shortcut and Start Menu folder (#1900; Windows Desktop installer)
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
- Fixed Copyright date ranges for Workbench Administrator and Workbench User Guide, and RStudio Desktop Pro Administration Guide (#5614)
- Fixed mis-encoded Hunspell dictionaries (#8147)
- Fixed an issue where the RStudio debugger failed to step through statements within a tryCatch() call (#14306)
- Improved responsiveness of C / C++ editor intelligence features when switching Git branches (#14320)

- Fixed an issue where the context menu sometimes did not display when right-clicking a word in the editor. (#14575)
- Fixed an issue where the "Go to directory..." button brought up the wrong dialog (#14501; Desktop)

#### Posit Workbench

### Dependencies

- Updated Electron to version 30.x (#14582; Desktop)

