## RStudio 2023.12.0 "Ocean Storm" Release Notes

### New
#### RStudio
- Updated to Boost 1.83.0. (#13577)
- RStudio now supports highlighting of inline YAML chunk options in R Markdown / Quarto documents. (#11663)
- Improved support for development documentation when a package has been loaded via `devtools::load_all()`. (#13526)

#### Posit Workbench
-

### Fixed
#### RStudio
- Fixed an issue preventing the object explorer from exploring httr request objects. (#13348)
- RStudio will no longer attempt to automatically activate the default system installation of Python. (#13497)
- Improved performance of R Markdown chunk execution for projects running on networked filesystems. (#8034)
- Fixed an issue where underscores in file names were not displayed correctly in menu items. (#13662)
- Fixed an issue where previewed plots were not rendered at the correct DPI. (#13387)

#### Posit Workbench
-

