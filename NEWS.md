## RStudio 2024.10.0 "Kousa Dogwood" Release Notes

### New
#### RStudio
- RStudio now supports code formatting using the 'styler' R package, as well via other external applications (#2563)
- Use native file and message dialogs by default on Linux desktop (#14683; Linux Desktop)
- Added www-socket option to rserver.conf to enable server to listen on a Unix domain socket (#14938; Open-Source Server)
- RStudio now supports syntax highlighting for Fortran source files (#10403)
- Display label "Publish" next to the publish icon on editor toolbar (#13604)
- RStudio supports `usethis.description` option values when creating projects via the RStudio New Project wizard (#15070)

#### Posit Workbench
-

### Fixed
#### RStudio
- Fixed an issue in the data viewer where list-column cell navigation worked incorrectly when a search filter was active (#9960)
- Fixed an issue where debugger breakpoints did not function correctly in some cases with R 4.4 (#15072)
- Fixed an issue where autocompletion results within piped expressions were incorrect in some cases (#13611)
- Fixed being unable to save file after cancelling the "Choose Encoding" window (#14896)
- Fixed problems creating new files and projects on a UNC path (#14963, #14964; Windows Desktop)
- Prevent attempting to start Copilot on a non-main thread (#14952)
- Reformat Code no longer inserts whitespace around '^' operator (#14973)
- Prompt for personal access token instead of password when using github via https (#14103)
- RStudio now forward the current 'repos' option for actions taken in the Build pane (#5793)
- Executing `options(warn = ...)` in an R code chunk now persists beyond chunk execution (#15030)

#### Posit Workbench
- Fixed an issue with Workbench login not respecting "Stay signed in when browser closes" when using Single Sign-On (rstudio-pro#5392)

### Dependencies

- Updated GWT to version 2.10.1 (#15011)
- Updated Electron to version 31.4.0 (#14982; Desktop)
