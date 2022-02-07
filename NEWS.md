## RStudio 2022-06.0 "Spotted Wakerobin" Release Notes

### New
- Source marker `message` can contain ANSI SGR codes for setting style and color (#9010)
- Linux/MacOS: Executing a code selection that encounters an error will stop execution of remaining code (#3014)

#### R
- Added support for using the AGG renderer (as provided by the ragg package) as a graphics backend for inline plot execution; also added support for using the backend graphics device requested by the knitr `dev` chunk option (#9931)

### Fixed
- Fixed notebook execution handling of knitr `message=FALSE` chunk option to suppress messages if the option is set to FALSE (#9436)
- Fixed plot export to PDF options (#9185)
- `.rs.formatDataColumnDispatch()` iterates through classes of `x` (#10073)
- Fixed Find in Files whole-word replace option, so that when "Whole word" is checked, only file matches containing the whole word are replaced, as displayed in the preview (#9813)
- Adds support for POSIX extended regular expressions with backreferences in Find in Files find and replace modes, so that special regex characters such as `+` and `?`, `|`, `(`, etc do not need to be escaped, and other ERE escape sequences such as `\b`, `\w`, and `\d` are now supported (#9344). This matches the behavior of R's own `grep()` function, but note that backslashes do not need to be escaped (as they typically are in R strings).
- `.rs.api.navigateToFile()` is now synchronous and returns document id (#8938)

### Breaking

#### Visual Mode

* Improved handling of table markdown in visual editor (#9830)
* Added option to show line numbers in visual mode code chunks (#9387)
* Made visual code chunks collapsible (#8613)
* Show source diagnostics in visual code chunks (#9874)
* Fixed code execution via selection in indented visual mode code chunks (#9108)
* Fixed detection of HTTP(S) URLs on Windows in the image resolver (#9837)
* Improved behavior of citekey removal in Insert Citation dialog (#9124)
* Fix issue with unicode characters in citation data (#9745)
* Fix issue with unicode characters in citekeys (#9754)
* Fix issue with delay showing newly added Zotero references when inserting citations (#9800)
* Add ability to insert citation for R Packages (#8921)
* Fixed BetterBibTeX detection on Linux (#10007)
* Fixed DT tables being squashed in the viewer pane (#10276)
* "Clean and Rebuild" and "Install and Restart" have been merged into "Install Package", the "Project Options > Build" gains a "clean before install" option to toggle the --preclean flag. 
* The Build toolbar changes to "Install | Test | Check | More v"

#### RStudio Workbench

* Added support for setting the `subPath` on Kubernetes sessions using `KubernetesPersistentVolumeClaim` mounts in `/etc/rstudio/launcher-mounts` (Pro #2976).
* Added support for Slurm 21.08 to the Slurm Launcher plugin
* Fixed a bug where Slurm Launcher jobs that exited with a non-zero exit code would still have a zero exit code (#203)
* Fixed a bug where Slurm Launcher jobs with standard error would never be written to the output file (#203)
* Fixed a bug where Slurm Launcher jobs that exited due to a signal would not show the exit code as 128+signal (#203)
* Fixed a bug where Launcher log files could be stuck being owned by the root user (#9728)
* Added `license-warning-days` setting to make it possible to adjust or disable the license warnings that appear two weeks prior to expiration (Pro #440)
* When an R version defined in `r-versions` uses an environment module, the name of the module is displayed in the version select menus instead of the system R version name. (Pro #2687)
* Clicking on a session entry in the RSW homepage will always attempt to launch it -- the title is no longer a link. Clicking on "Info" will always show info. (Pro #3082)
* With the options `launcher-sessions-create-container-user`, and `launcher-sessions-container-forward-groups` enabled, RSW will now add a group to the user even if the group with a matching id exists but with a different name. (Pro #2971)
* Added SSL communication between RSW and remote sessions (using the job launcher). It's enabled by default and can be disabled in rserver.conf by setting session-ssl-enabled=0. Certificates are generated for each job by default or can be manually configured. (Pro #3026)
* Disable session SSL for Code Server 3.9.3 and support auth changes in Code Server 3.11.0 (Pro #3111)
* Show user's full name, or proxied auth display name, in Project Sharing presence indicator (Pro #3121)
* Allow users to specify R version in launcher jobs (Pro #1046)
* Show additional environment information in the IDE Job Launcher dialog (Pro #3110)
* Allow users to specify custom path to R_HOME in the IDE Job Launcher when the target cluster or image do not match the current environment (Pro #3110)
* Fixed bug with customized display names and launcher sessions (Pro #3217)
* Removed some unnecessary warnings in the RStudio VS Code Extension when using Dash (Ext #98)
* Added a link to a help article about using VS Code Sessions in RStudio Workbench

#### R

* RStudio now supports the experimental UTF-8 UCRT builds of R (#9824)
* Preliminary support for R graphics engine version 15 in R 4.2.0. (#10058)
* Default file download method in Windows for R 4.2 and above changed from `wininet` to `libcurl` (#10163)
* `list.files()` and `list.dirs()` now handle international characters on Windows (#10451)

#### Misc

* Add commands to open selected files in columns or active editor (#7920)
* Add *New Blank File* command to Files pane to create empty files of selected type in the directory (#1564)
* Rename CSRF token header `X-CSRF-Token` and cookie `csrf-token` to `X-RS-CSRF-Token` and `rs-csrf-token`, respectively, to avoid clashing with similarly named headers and cookies in other services (#7319)
* Use double indent for function parameters to align with Tidyverse style (#9766)
* Sessions that attempt to automatically suspend, but were blocked by some operation, will report what's blocking suspension in the IDE in the R Console toolbar (Pro #2618)
* Recognize `id_ed25519` key file in Version Control options UI (#9991)
* Updated Files Pane buttons to resize and remain visible at smaller widths (#9870)
* Remove 'Classic' IDE theme (#9738)
* Added support for Amazon Linux 2 (Pro #2474)
* Treat Alt and Caption fields differently depending on file type (#9713)
* Fixed shortcut conflict on German keyboard (#9276)
* Updated shinymod snippet for Shiny modules (#10009)
* Fixed an issue where `conda install` could fail within a Git Bash terminal on Windows (#10283)
* Add a -G option to `rsandbox` to allow configuring the effective group of the process (#3214)

### Fixed

* Fixed an issue that could cause calls to `grid` functions to fail after restart (#2919)
* Fixed errors when uploading files/directory names with invalid characters (Pro #698)
* Added error when rsession may be running a different version of R than expected (Pro #2477)
* Fixed "No such file or directory" errors when auto-saving R Notebook chunks while running them (#9284)
* Fixed issue causing unnecessary document switching when evaluating statements in debugger (#9918)
* Fixed scrolling past long sub-content (like kables) in RMD files. User must interact with sub-content in order to scroll through it (#2202)
* Fixed custom shortcuts not appearing correctly in menus (#9915)
* Fixed custom shortcuts not appearing correctly in "Keyboard Shortcuts Help" and Electron menus. (#9953)
* Fixed header scrolling in data viewer tables not following table contents in unfocused windows (#8208)
* Fixed permissions on Mac Desktop application so all user accounts can launch it (#9945, #10267)
* Fixed logging directory permissions to be more restrictive (775 instead of 777) (#3099)
* Fixed Duplicate --session-collab-server when launching R session (pro #3106)
* Fixed errors when opening or saving Rmarkdown documents when R is busy (#9868)
* Fixed issue with SLES 12 builds using OpenSSL 1.1 instead of 1.0.2
* The source button uses `cpp11::source_cpp()` on C++ files that have `[[cpp11::register]]` decorations (#10387)

### Breaking

* Remove --session-collab-server and filebase-path (pro #3181)

### Deprecated / Removed

There is no deprecated or removed functionality in this release.

