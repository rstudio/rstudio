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
- Fixed an issue where RStudio could become unresponsive when rendering very large graphics. (#15276)
- Fixed an issue where the F1 shortcut would fail to retrieve documentation in packages. (#10869)
- Fixed an issue where some column names were not displayed following select() in pipe completions. (#12501)
- Fixed an issue where building with a newer version of Boost (e.g. Boost 1.86.0) would fail. (#15625)
- Fixed an issue where opening multiple copies of RStudio Desktop installed in different locations would cause RStudio to try to open itself as a script. (#15554)

#### Posit Workbench
-

