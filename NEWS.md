## RStudio 2024.07.0 "Cranberry Hibiscus" Release Notes

### New

#### RStudio


#### Posit Workbench

- Quitting Jupyter and VS Code sessions from the home page no longer force quits them immediately; instead, they are given time to exit gracefully, as with RStudio Pro sessions (rstudio-pro#5317)
- Workbench jobs launched on Kubernetes or on Slurm with Singularity support will now default to using the same container image as the parent session (rstudio-pro#5875)
- Workbench's Prometheus metrics now track R and Python jobs launched, in addition to sessions (rstudio-pro#6271)

### Fixed

#### RStudio

- Fixed an issue where the context menu sometimes did not display when right-clicking a word in the editor. (#14575)
- Fixed an issue where the "Go to directory..." button brought up the wrong dialog (#14501; Desktop)
- Remove superfluous Uninstall shortcut and Start Menu folder (#1900; Desktop installer on Windows)
- Hide Refresh button while Replace All operation is running in the Find in Files pane (#13873)
- Stop the File Pane's "Copy To" operation from deleting the file when source and destination are the same (#14525)
- Removed extra spaces after package names in warning message about required packages (#14608)

#### Posit Workbench

- Workbench jobs now set resource profiles correctly (rstudio-pro#5217)

### Dependencies

- Updated Electron to version 30.x (#14582; Desktop)

