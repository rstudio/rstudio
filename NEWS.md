## RStudio 2024.07.0 "Cranberry Hibiscus" Release Notes

### New

- Posit Product Documentation theme v4.0.2; adds dark mode and several accessibility improvements (rstudio-pro#6373)

#### RStudio

- You can now toggle between R and Python in the Console by clicking on the language's logo in the Console pane header. (#11613)
- Restart commands are now run after restoring the search path + global environment by default. (#14636)
- The "Save as Image" dialog now includes a checkbox "Use device pixel ratio", controlling whether plots are scaled according to the current display's DPI. (#14727)
- The "Soft-wrap R source files" preference now applies to all source files, and has been re-labelled appropriately. (#10940)
- RStudio now supports Electron flags set via `~/.config/rstudio/electron-flags.conf` or `~/.config/electron-flags.conf` on Linux / macOS. On Windows, the paths are `%LocalAppData%\RStudio\electron-flags.conf` and `%LocalAppData%\electron-flags.conf`. (#14641)

#### Posit Workbench

- Quitting Jupyter and VS Code sessions from the home page no longer force quits them immediately; instead, they are given time to exit gracefully, as with RStudio Pro sessions (rstudio-pro#5317)
- Workbench jobs launched on Kubernetes or on Slurm with Singularity support will now default to using the same container image as the parent session (rstudio-pro#5875)
- Workbench's Prometheus metrics now track R and Python jobs launched, in addition to sessions (rstudio-pro#6271)
- When launching sessions on Slurm, environment variables set by the Slurm scheduler itself should now be available to sessions (rstudio-pro#5148, rstudio-pro#4692, rstudio-pro#3255)
- The `launcher-sessions-forward-container-environment` flag has been deprecated and will be removed in a future version. Use `launcher-sessions-forward-environment=0` to disable environment variable forwarding instead (rstudio-pro#5895)
- The New Project dialog in RStudio Pro now defaults to the R version of the current session, rather than the system default (rstudio-pro#4244)
- Workbench jobs launched from RStudio Pro now default to the R version of the current session, rather than the system default (rstudio-pro#5903)
- Added support for Jupyter Notebook 7 (rstudio-pro#6266)
- Replaced code server binary with PWB Code Server. PWB Code Server is bundled with the PWB VS Code Extension, so a separate install is no longer required. It is a fork of VS Code 1.89.1. (rstudio-pro#6265)
- Disabled Jupyter Notebooks by default on fresh installs of PWB (rstudio-pro#6269)

### Fixed

#### RStudio

- The RStudio diagnostics system no longer automatically loads packages when encountering calls of the form `dplyr::mutate()`. (#9692)
- Fixed an issue where Build output from 'Run Tests' was not appropriately coloured. (#13088)
- Fixed an issue where various editor commands (Reindent Lines; Run Chunks) could fail in a document containing Quarto callout blocks. (#14640)
- Fixed an issue where end fold markers were not rendered correctly in Quarto documents. (#14699)
- Fixed an issue where the context menu sometimes did not display when right-clicking a word in the editor. (#14575)
- Fixed an issue where the "Go to directory..." button brought up the wrong dialog (#14501; Desktop)
- Fixed an issue where "View plot after saving" in the Save Plot as Image dialog sometimes did not work. (#14702)
- Fixed an issue where the IDE could hang when navigating the Files pane to a directory containing a very large number of files. (#13426)
- Fixed an issue where RStudio could trigger active bindings in environments when requesting completions. (#14784)
- Fixed an issue where the editor scroll speed had inadvertently been decreased. (#14664)
- Fixed an issue where external links couldn't be opened from a popped-out Help pane window. (#14801; Desktop)
- Remove superfluous Uninstall shortcut and Start Menu folder (#1900; Desktop installer on Windows)
- Hide Refresh button while Replace All operation is running in the Find in Files pane (#13873)
- Stop the File Pane's "Copy To" operation from deleting the file when source and destination are the same (#14525)
- Fix keyboard shortcuts acting on the wrong source file when using Visual Editor in source columns or separate windows (#12581, #11684)
- Fix startup error due to invalid zoom setting triggering a config schema violation (#14690) 
- Removed extra spaces after package names in warning message about required packages (#14608)
- Moved the "Sign commit" checkbox to Git/Svn global options panel (##14559)
- RStudio's editor highlighting no longer accepts embedded spaces in '#|' comment prefixes. (#14592)

#### Posit Workbench

- Workbench jobs now set resource profiles correctly (rstudio-pro#5217)
- When launching a Workbench job from RStudio Pro, changing fields in the Options tab no longer resets the selected R version in the Environment tab (rstudio-pro#5218)
- Fixed bug that prevented users from receiving the admin-configured default settings when launching VS Code sessions (rstudio-pro#6207)


### Dependencies

- Updated MathJax to version 2.7.9 (#11535)
- Updated Electron to version 30.1.0 (#14582; Desktop)
