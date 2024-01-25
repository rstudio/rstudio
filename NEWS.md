## RStudio 2024.04.0 "Chocolate Cosmos" Release Notes

### New
#### RStudio
- RStudio Desktop on Windows and Linux supports auto-hiding the menu bar (#8932)
- RStudio's GWT sources can now be built with JDKs > 11 (#11242)
- R projects can be given a custom display name in Project Options (#1909)

#### Posit Workbench
- Show custom project names on Workbench homepage (rstudio-pro#5589)

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

#### Posit Workbench
-

### Dependencies
- Updated Electron to version 28.0.0 (#14055; Desktop)
- Updated GWT to version 2.10.0 (#11505; Desktop + Server)
- Updated NSIS to version 3.09 (#14123; Windows Desktop)
- Updated OpenSSL to version 3.1.4 (Windows Desktop)
