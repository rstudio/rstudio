## RStudio 2025.04.0 "Mariposa Orchid" Release Notes

### New
#### RStudio
- On macOS, RStudio now uses the project directory / home directory for new RStudio sessions opened via the Dock menu's "Open in New Window" command. (#15409)
- RStudio installation on Windows now registers icons for many supported file types. (#12730)
- RStudio for Windows binaries now have digital signatures. (rstudio-pro#5772)
- RStudio now only writes the ProjectId field within a project's `.Rproj` file when required. Currently, this is for users who have configured a custom `.Rproj.user` location.

#### Posit Workbench
-

### Fixed
#### RStudio

- Fixed an issue where the R startup banner was printed twice in rare cases. (#6907)
- Fixed an issue where RStudio Server could hang when navigating the Open File dialog to a directory with many (> 100,000) files. (#15441)
- Fixed an issue where the F1 shortcut would fail to retrieve documentation in packages. (#10869)
- Fixed an issue where some column names were not displayed following select() in pipe completions. (#12501)
- Fixed an issue where building with a newer version of Boost (e.g. Boost 1.86.0) would fail. (#15625)
- Fixed an issue where opening multiple copies of RStudio Desktop installed in different locations would cause RStudio to try to open itself as a script. (#15554)
- Fixed an issue where printing 0-row data.frames containing an 'hms' column from an R Markdown chunk could cause an unexpected error. (#15459)

#### Posit Workbench
-

