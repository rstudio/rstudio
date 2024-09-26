## RStudio 2024.10.0 "Kousa Dogwood" Release Notes

### New
#### RStudio
- RStudio now supports the inclusion of environment variables when publishing applications to Posit Connect (#13032)
- RStudio now supports new features from the R graphics engine (groups, glyphs) when supported by the underlying device (#14613)
- The .Rproj.user folder location can now be customized globally both by users and administrators (#15098)
- RStudio now supports code formatting using the 'styler' R package, as well via other external applications (#2563)
- Use native file and message dialogs by default on Linux desktop (#14683; Linux Desktop)
- Added www-socket option to rserver.conf to enable server to listen on a Unix domain socket (#14938; Open-Source Server)
- RStudio now supports syntax highlighting for Fortran source files (#10403)
- Display label "Publish" next to the publish icon on editor toolbar (#13604)
- RStudio supports `usethis.description` option values when creating projects via the RStudio New Project wizard (#15070)
- The font size used for the document outline can now be customized [Accessibility] (#6887)
- The RStudio diagnostics system now supports destructuring assignments as implemented and provided in the `dotty` package
- The "Insert Chunk" button now acts as a menu in Quarto documents as well as R Markdown documents (#14785)
- Improved support for highlighting of nested chunks in R Markdown and Quarto documents (#10079)
- Improved support for variable-width chunk headers and footers in R Markdown / Quarto documents (#15191)
- The "Include all function arguments in completion list" user preference can be used to control whether RStudio includes function arguments which appear to have already been used in the current context (#13065)
- Available environment variables are now provided as completion suggestions within `Sys.unsetenv()` (#15215)
- RStudio IDE User Guide and RStudio & Posit Workbench Release Notes now includes release version in navigation bar reference URLs. (#15223)

#### Posit Workbench

- Posit Workbench Administration Guide & User Guide and Posit Workbench Licenses guide now includes release version in navigation bar reference URLs. (rstudio-pro#6826)

### Fixed
#### RStudio
- "Run All" now only executes R chunks when "Chunk Output in Console" is set (#11995)
- Fixed an issue where the chunk options popup didn't recognize chunk labels preceded by a comma (#15156)
- Fixed an issue where the chunk options popup was confused by quoted strings containing spaces (#6829)
- Fixed an issue where the chunk options popup was confused by spaces around equals signs (#2673)
- Fixed an issue where an R Markdown chunk label containing dashes was truncated in Visual Editor (#15162)
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
- Remove focus-visible polyfill and instead use native browser :focus-visible pseudoclass [Accessibility] (#14352)
- Fixed an issue where completion types for objects with a `.DollarNames` method were not properly displayed (#15115)
- Fixed an issue where the Console header label was not properly layed out when other tabs (e.g. Terminal) were closed (#15106)
- Auto-saves will no longer trim trailing whitespace on the line containing the cursor (#14829)
- Fixed an issue where pressing Tab would insert a literal tab instead of indenting a multi-line selection (#15046)
- Fixed scroller behavior in Data Viewer when viewing very large datasets (#12834)
- Fixed an issue where quoted variable names were not completed properly in dplyr pipes (#15161)
- Fixed issue with highlight of `tikz` code chunks in R Markdown documents (#15019)
- RStudio now uses current session repositories when installing package dependencies via background jobs (#10016)
- Fixed issue where collapsed raw chunks were displayed with an incorrect label in the Visual Editor (#14594)
- Fixed issue where test errors were duplicated when presented in Issues tab of Build pane (#14564)
- Fixed issue where certain Python variable names were incorrectly quoted when inserted via autocompletion (#14560)
- RStudio now includes Markdown headers without any label in the document outline (#14552)
- Clicking in the editor gutter to toggle a breakpoint no longer also selects the associated line (#15226)
- RStudio no longer logs warning / error messages related to disabled R actions (e.g. ReadConsole) in forked sessions (#15221)

#### Posit Workbench
- Fixed an issue with Workbench login not respecting "Stay signed in when browser closes" when using Single Sign-On (rstudio-pro#5392)

### Dependencies

- Updated GWT to version 2.10.1 (#15011)
- Updated Electron to version 31.5.0 (#14982; Desktop)

### Deprecated / Removed
- Removed user preference for turning off focus indicator rectangles (#14352)

