## RStudio 2023.08.0 "Desert Sunflower" Release Notes

### New
- Prometheus metrics are available in Posit Workbench (rstudio-pro:#3273)
- Update to Electron 25.5.0 (#13457)
- Additional support for publishing new content types to Posit Cloud (rstudio-pro#4541)
- Add support Quarto Manuscript projects, a new Quarto feature that will be released in Quarto 1.4.
- Added option to sign Git commits (#1865)
- Add section to theme generation that will use theme-specific color(s) to set colors of vim/emacs-mode cursor

### Fixed
- Fixed issue where `DYLD_FALLBACK_LIBRARY_PATH` was not properly forwarded on macOS (#13085)
- Fixed issue where 'ggrepel' plot annotations could disappear when a plot was redrawn in an R Markdown document (#4330)
- Fixed issue where whitespace in error messages was not properly preserved (#13239)
- Fixed issue where the Data Viewer could fail to render data.frames containing AsIs matrices (#13215)
- Fixed issue where error messages were not properly translated on Windows in some cases (#10308)
- Fixed issue where Electron menubar commands are not disabled when modals are displayed (#12972)
- Fixed bug causing invalid/empty `cacheKey` error when accessing a dataframe variable (#13188)
- Fixed bug preventing dataframe column navigation past the default number of display columns (#13220)
- Fixed intermittent rsession crash when the linux nscd service was enabled (rstudio-pro:#4648)
- Fixed bug when resuming session not restoring current working directory for Terminal pane (rstudio-pro:#4027)
- Fixed bug preventing files from being saved when user `HOME` path includes trailing slashes on Windows (#13105)
- Fixed broken Help pane after navigating to code demo (#13263)
- Fixed bug preventing Update Available from displaying (#13347)
- Fixed bug causing dataframe help preview to fail for nested objects (#13291)
- Fixed issue where changes to binary files were not presented in Git History view (#13126)
- Fixed bug where clicking "Ignore Update" would fail to ignore the update (#13379)
- Fixed bug preventing `HOME` from being modified in system init scripts (rstudio-pro:#4584)
- Fixed issue with alignment of R argument names in Help pane (#13474)
- Fixed issue where user was not warned of missing Rosetta installation on Apple silicon (#12791)
- Fixed bug with modals disabling copy/paste (#13365)
- Fixed issue where column names weren't provided as completion candidates for DBI tables. (#12577)
- Fixed an issue where parameter name completions were not provided within `dplyr` joins. (#13415)
- Removed unnecessary files from install packages (rstudo-pro:#4943)
- Fixed issue where R sessions containing large 'igraph' objects could become slow (#13489)

### Performance
- Improved performance of group membership tests (rstudio-pro:#4643)
- Increased read buffer size for rserver proxy (rstudio-pro:#4764)

### Accessibility Improvements
-

