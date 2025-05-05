## RStudio 2025.08.0 "Sandyland Bluebonnet" Release Notes

### New

#### RStudio

- RStudio now highlights all keywords from the SQL 2023 standard in SQL documents (#15841)
- Added a user preference to disable showing the splash screen at startup (#15945)
- The splash screen now closes when clicked with the mouse (#15614)

#### Posit Workbench

-

### Fixed

#### RStudio

- Fixed an issue where pkgdown websites built outside of the user directory could not be viewed from RStudio Server (#15133)
- Fixed an issue where the "Save As" dialog would not be visible when trying to save an older git revision of a file (#15955)
- Fixed an issue where code indentation stopped working following code chunks containing only Quarto comments (#15879)
- Fixed an issue where RStudio could hang when attempting to execute notebook chunks without a registered handler (#15979)

#### Posit Workbench

-

### Dependencies

- Copilot Language Server 1.311.0 (#15935)
- Electron 36.1.0 (#15933)
- Quarto 1.7.29 (#15934)

### Deprecated / Removed

- RStudio Server and Posit Workbench are no longer supported on Ubuntu Focal (#15940)
