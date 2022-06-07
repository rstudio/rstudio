
## RStudio 2022-06.0 "Spotted Wakerobin" Release Notes

### New

- Linux: Support for Ubuntu 22.04, Fedora 35. (#10902, #9854)
- Source marker `message` can contain ANSI SGR codes for setting style and color (#9010)
- Linux/MacOS: Executing a code selection that encounters an error will stop execution of remaining code (#3014)
- Added support for hyperlinks in the console and build pane (#1941)
- Added support for blurred text (#11019)
- "Clean and Rebuild" and "Install and Restart" have been merged into "Install Package", the "Project Options > Build" gains a "clean before install" option to toggle the --preclean flag. The Build toolbar changes to "Install | Test | Check | More v" (#4289)
- The source button uses `cpp11::source_cpp()` on C++ files that have `[[cpp11::register]]` decorations (#10387)
- New *Relative Line Numbers* preference for showing line numbers relative to the current line, rather than the first line (#1774)
- Upgraded SOCI library dependency from version 4.0.0 to 4.0.3 (#10792)
- (macOS only) RStudio now reads the PATH from the user's default shell on startup (#10551)
- (experimental) Option to display the user interface in French (#10455)
- Label commit timestamps as being in UTC (#2544)
- The choice of pipe operator (`magrittr` or native R 4.1+) inserted with the "Insert Pipe Operator" keyboard shortcut can now be configured at the project level as well as the global level (#9409)
- The Git/SVN pane now supports creating ED25519-encrypted SSH keys by default. Newly created RSA SSH keys will now be 4096 bits instead of 2048 to increase security (#8255)
- Read only R and C++ files (marked by "do not edit by hand") are ignored by the fuzzy file finder (#10912)
- Linux: For compatibility with newer versions of glibc (>= 2.34), the seccomp filter sandbox is disabled. See https://chromium.googlesource.com/chromium/src/+/0e94f26e8/docs/linux_sandboxing.md#the-sandbox-1 for more details.
- Changed "Jobs" tab in IDE to "Background Jobs" (#11296)
- The fuzzy finder shows `test_that()` calls when the search term starts with "t "
- Calls to test_that() appear in the source file outline (#11082)
- Windows: Update embedded libclang to 13.0.1 (#11186)
- Added a warning when renv is actively overriding repository settings in Global Options (#9947)
- Workbench now supports project sharing in single-session mode (i.e. when `server-multiple-sessions=0`) (rstudio-pro#1263)

#### Find in Files

- Fixed Find in Files whole-word replace option, so that when "Whole word" is checked, only file matches containing the whole word are replaced, as displayed in the preview (#9813)
- Adds support for POSIX extended regular expressions with backreferences in Find in Files find and replace modes, so that special regex characters such as `+` and `?`, `|`, `(`, etc do not need to be escaped, and other ERE escape sequences such as `\b`, `\w`, and `\d` are now supported. This matches the behavior of R's own `grep()` function, but note that backslashes do not need to be escaped (as they typically are in R strings) (#9344)
- The "Common R source files" option in Find in Files has been updated to "Common source files", with support for searching Markdown (of any type, including .Rmd), JS, and YAML files (#10526)
- Updated support for searching paths and filenames with Unicode characters on Windows, including Chinese and non-Latin characters (#9881)
- Add a refresh button to the Find in Files pane to enable manual refresh of Find in Files search results (#3240)

#### R

- Added support for the `_` placeholder as used by the R pipe-bind operator, to be introduced with R 4.2.0. (#10757)
- Added support for using the AGG renderer (as provided by the ragg package) as a graphics backend for inline plot execution; also added support for using the backend graphics device requested by the knitr `dev` chunk option (#9931)
- rstudioapi functions are now always evaluated in a clean environment, and will not be masked by objects in the global environment (#8031)
- Removed support for versions of R earlier than R 3.3.0. (rstudio-pro#2887)
- Chunk options in the body of a code chunk, prefaced by `#|` will be respected during inline code execution, and will take precedence over conflicting chunk options in the chunk header (#10645). Both YAML `tag: value` syntax and valid R expressions will be parsed.
- Fixed issue in R debugger that caused RStudio to lose focus out of source code when interacting with the console in certain ways, such as evaluating an expression (#10664)

#### Python

- RStudio attempts to infer the appropriate version of Python when "Automatically activate project-local Python environments" is checked and the user has not requested a specific version of Python. This Python will be stored in the environment variable "RETICULATE_PYTHON_FALLBACK", available from the R console, the Python REPL, and the RStudio Terminal (#9990)

### Fixed

- Fixed an issue where vignette content was illegible when viewed with a dark theme. (#11164)
- Fixed an issue where previewing a plot as PDF could fail after a session restart. (#1905)
- Fixed logging of `HRESULT` error values by logging them as hexadecimal instead of decimal (#10310)
- Fixed notebook execution handling of knitr `message=FALSE` chunk option to suppress messages if the option is set to FALSE (#9436)
- Fixed plot export to PDF options (#9185)
- `.rs.formatDataColumnDispatch()` iterates through classes of `x` (#10073)
- `.rs.api.navigateToFile()` is now synchronous and returns document id (#8938)
- The `Session > Load Workspace` menu option now explicitly namespaces `base::load` if the `load` function has been masked in the global environment (#10089)
- The data viewer truncates large list cells to 50 characters by default, this can be changed with the command palette or `rstudioapi::writeRStudioPreference("data_viewer_max_cell_size", 10L)` (#5100)
- The R version and logo displayed in the top left of the console will update to the current R version whenever the R session is restarted (#10458)
- Fixed issue where `core::system::userBelongsToGroup` errors under specific sssd configurations (`ignore_group_members = true`) (#10829)
- Fixed a security issue where shiny apps and vscode sessions remained active after signout (rstudio-pro#3287)
- Fixed an intermittent hang when invoking `rstudio-server verify-installation` which caused stale `rserver` processes to exist (rstudio-pro#3041) 
- (Windows only) Fixed an issue where multiple instances of RStudio launched at the same time could bind to the same session. (#10488)
- Fixed unintended change of date/time formatting in the VCS commit history (#10810)
- Fixed an issue where code of the form '1:2:3' was diagnosed incorrectly. (#10979)
- Add back link to the title of sessions so that users can easily open sessions in new tabs and copy session links (rstudio-pro#3290)
- (Linux Only) License-manager now works in a installer-less context (rstudio-pro#3150)
- Fixed an issue where R raw strings were not highlighted correctly in R Markdown documents. (#11087)
- Fixed issue with using RStudio server behind multi-level proxy servers (#11010)
- Fixed an issue with project sharing where other users' actions could prevent a session's auto suspend (rstudio-pro#3362)
- Fixed a regression in which the "(Use Default Version)" option was not present in some R version selector drop downs (rstudio-pro#3451)
- Fix opening a remote session via downloaded rdprsp file in Mac Desktop Pro when it (RDP) is already open (rstudio-pro#3291)
- Fixed several error marker issues in visual mode where they did not display (#10949 #10483)
- Allow Jupyter and VScode sessions to be renamed from the homepage (rstudio-pro#1686)
- Fixed a user-facing error and added logging when the a session fails to launch due to a misconfigured launcher (rstudio-pro#1684)

### RStudio Workbench

- Add a -G option to `rsandbox` to allow configuring the effective group of the process (#3214)
- When resuming a suspended session with the Kubernetes Launcher Plugin, the container image that was previously being used will now be selected by default (#1520)
- Upgrade the default version of `code-server` to 4.2.0 to resolve issue with the latest Python VS Code extension (Pro #3320)
- JupyterLab Sessions are now configured to be run with the `rsw_jupyterlab` extension. If not globally installed, this will be auto-installed for users on their first JupyterLab session launch (Pro #3429)

### Deprecated / Removed

- The minimum supported R version for the IDE has been increased from R 3.0.1 to R 3.3.0 (rstudio-pro#2887)
- **BREAKING:** Block port proxy requests at `/proxy/<port>` for Jupyter sessions - previously only available if [Jupyter Server Proxy](https://github.com/jupyterhub/jupyter-server-proxy) was installed (Pro #3339)
- No longer support Debian 9 ("stretch") for Desktop, Server, and Workbench (#10981)
