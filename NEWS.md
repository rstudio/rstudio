## RStudio 2025.04.0 "Mariposa Orchid" Release Notes

### New
#### RStudio
- RStudio now uses an alternate display for R errors, warnings, and messages. In addition, output on stderr (e.g. R messages) are colored the same as regular output on stdout. (#2574)
- RStudio now displays a message when the GitHub Copilot completion limit has been reached (typically on a free Copilot account). (#15848)
- RStudio now uses the RStudio project folder as the GitHub Copilot workspace root, providing more relevant completions. (#15816)
- RStudio now supports being built without GitHub Copilot support via the `RSTUDIO_ENABLE_COPILOT` CMake option. (#15869)
- On macOS, RStudio now uses the project directory / home directory for new RStudio sessions opened via the Dock menu's "Open in New Window" command. (#15409)
- RStudio Desktop now supports opening files via drag and drop. (#4037)
- RStudio installation on Windows now registers icons for many supported file types. (#12730)
- RStudio for Windows binaries now have digital signatures. (rstudio-pro#5772)
- RStudio now only writes the ProjectId field within a project's `.Rproj` file when required. Currently, this is for users who have configured a custom `.Rproj.user` location.
- Added memory limit monitoring to RStudio (Linux only for now). If `ulimit -m` is set, this limit is displayed in the Memory Usage Report. As the limit is approached, a warning is displayed, then an error when the limit is reached. When the system is low on memory, the session can abort itself by shutting down in a controlled way with a dialog to the user. See the new `"allow-over-limit-sessions` option in rsession.conf. (rstudio-pro#5019).
- RStudio now supports configuring a Kerberos Service Principal for the GitHub Copilot Language Server proxy settings. (#15823)
- RStudio now supports installation of Rtools45 for the upcoming R 4.5.0 release on Windows.
- RStudio now sets the environment variable `SPARK_CONNECT_USER_AGENT = posit-rstudio` in R sessions. (rstudio-pro#7732)
- RStudio now sets the environment variable `SF_PARTNER = posit_rstudio` in R sessions. (rstudio-pro#7577)

#### Posit Workbench
- Changed memory limit enforcement of `/etc/rstudio/profiles` `max-memory-mb` setting from limiting virtual memory (`ulimit -v`) to resident memory (`ulimit -m`) for more accuracy. This allows a session to run quarto 1.6, which uses a lot of virtual memory due to its underlying virtual machine. Unfortunately, resident memory is only enforceable at the kernel level in Linux versions that support cgroups. To make up for the loss of kernel enforcement, RStudio warns the user and stops over limit sessions. For more robust kernel enforcement, configure memory limits in the Job Launcher using cgroups (rstudio-pro#5019).
- RStudio will now preserve unknown fields in `.Rproj` files that are added by future versions of RStudio. (#15524)
- RStudio now sends an interrupt git processes when stopped via "Stop" during a git commit, rather than just terminating the processes. (#6471)
- RStudio now properly displays R Markdown render errors with newer versions of the `rmarkdown` and `rlang` packages. (#15579)
- An SELinux policy module is now available, allowing Workbench to run when enforcement is enabled. (#4937, rstudio-pro/#4749)
- Migrated the Posit Workbench Admin Guide Hardening: Set Up SSL content into the Access and Security: Secure Sockets (SSL) topic to promote content discovery and single source of truth. (rstudio-pro#6098)
- Adds secure browser storage to VS Code and Positron Pro sessions (vscode-server#174)
- Implemented User and Group profiles for VS Code and Positron Pro sessions (rstudio-pro#7468)
- Introduced custom bootstrap extensions to Positron Pro session to improve the Admin pre-configured extensions experience (rstudio-pro#7423)

### Fixed
#### RStudio
- Fixed an issue where execution of notebook chunks could fail if the `http_proxy` environment variable was set. (#15530)
- Fixed an issue where RStudio could hang when attempting to stage large folders from the Git pane on Windows. (#13222)
- Fixed an issue where RStudio could crash when attempting to clear plots while a new plot was being drawn. (#11856)
- Fixed an issuew here RStudio could crash if a project contained `.R` files with binary data. (#15801)
- Fixed an issue where the R startup banner was printed twice in rare cases. (#6907)
- Fixed an issue where RStudio Server could hang when navigating the Open File dialog to a directory with many (> 100,000) files. (#15441)
- Fixed an issue where the F1 shortcut would fail to retrieve documentation in packages. (#10869)
- Fixed an issue where some column names were not displayed following select() in pipe completions. (#12501)
- Fixed an issue where building with a newer version of Boost (e.g. Boost 1.86.0) would fail. (#15625)
- Fixed an issue where opening multiple copies of RStudio Desktop installed in different locations would cause RStudio to try to open itself as a script. (#15554)
- Fixed an issue where printing 0-row data.frames containing an 'hms' column from an R Markdown chunk could cause an unexpected error. (#15459)
- Fixed an issue where the Resources page in the Help pane was not legible with dark themes. (#10855)
- Fixed an issue where "Posit Workbench" was used instead of "RStudio Server" in a message shown when the user was signed out during a session. (#15698)
- Fixed an issue where the RStudio diagnostics system incorrectly inferred the scope for functions defined and passed as named arguments. (#15629)
- Fixed an issue where `locator()` and `grid.locator()` would produce incorrect coordinates with high DPI displays. (#10587)
- Fixed an issue where grid coordinates were not converted between different units correctly with high DPI displays. (#1908, #8559)
- Fixed an issue where locator points were not drawn on click. (#10025, #11103)
- Fixed an issue where RStudio would crash when using the MySQL ODBC Connector on Microsoft Windows. (#15674)
- Fixed an issue where autocompletion of R6 object names could fail with R6 2.6.0. (#15706)
- Fixed a WCAG 1.1.1 violation (unlabeled image in the Console toolbar) by marking it as cosmetic. [Accessibility] (#15757)
- Fixed Material theme's colors for selected word or text highlighting so they are more visible. [Accessibility] (#15753)
- Fixed an issue where .bib files with extra commas could be treated as binary files on RHEL9. (rstudio-pro/7521)
- Update NO_PROXY domain filter to be less restrictive and allow for expressions like `.local` and `.sub.example.local` (#15607)
- Fixed an issue where Copilot support on Apple Silicon Macs was running via Rosetta2 instead of natively. (#14156)
- Fixed an issue where the Copilot process was being started twice per RStudio session. (#15858))
- Fixed an issue where documents could open very slowly when many tabs were already open. (#15767)
- Fixed an issue where the download of Rtools44 could fail when using Posit Package Manager as the default R package repository. (#15803)
- Fixed an issue where messages produced by `rlang::inform()` were not separated by newlines.
- Fixed an issue where the `modifyRange` API function was not available, even though such a function was provided by `rstudioapi`.
- Fixed an issue where installing "required but not installed" packages could fail if those packages were available from an alternate (non-CRAN) package repository. (#10016)

#### Posit Workbench
- Fixed an issue where uploading a file to a directory containing an '&' character could fail. (#6830)
- Fixed an issue where unopened VSCode and Positron sessions wouldn't timeout when `session-timeout-kill-hours` was set and SSL was enabled. (rstudio-pro#7195)

### Dependencies
- Update Electron to version 34.5.1. (#15450)
- Update Quarto to version 1.6.42. (#15460)
- Updated Positron Pro sessions to 2025.04 with Code OSS 1.98
- Updated Code OSS to 1.99 for VS Code sessions (rstudio-pro#7882)
- Copilot Language Server 1.300.0. (rstudio-pro#7450)
- Updated Node in VS Code and Positron Pro sessions to 20.18.2 (rstudio-pro#7612)

### Deprecated / Removed
- No longer building RStudio Desktop or Desktop Pro for OpenSUSE 15, Ubuntu Focal, or RedHat 8. (rstudio-pro/#7445)
- No longer bundling node.js with RStudio Desktop, Desktop Pro, or RStudio Server. (rstudio-pro#7450)
