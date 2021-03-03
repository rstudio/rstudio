## RStudio 1.4 "Black Eyed Susan"

## RStudio 1.4 "Juliet Rose" Release Notes


### R

* Show memory usage details in Environment pane (#4033)
* Added support for the `|>` pipe operator and the `=>` pipe-bind operator, proposed for R 4.1.0 (#8543)
* Added support for the `\(x)` function shorthand syntax, proposed for R 4.1.0 (#8543)
* Added preference toggle for inserting the `|>` pipe operator when the Insert Pipe Operator command is used (#8534)
* Compilation of Sweave documents now uses tinytex when requested (#2788)

### Python

* The Python REPL can now be interrupted (#8763, #8785)
* Python installs within `/opt/python` and `/opt/local/python` are now discovered by RStudio (#8852)
* Improved handling of unicode input on Windows (#8549)
* Fixed issue where inspecting a null Python object would cause emit errors to console (#8185)
* Detect active Python version when publishing content (#8636)
* Use active Python version when knitting R Markdown files (#8854)

### RStudio Workbench

* RStudio Server Pro has been renamed to RStudio Workbench to more accurately reflect its cross-language editing capabilities.
* Added support for JupyterLab 3 (Pro #2022)
* Added support for code-server 3.4.0+ (Pro # 1984)
* Added a new user settings template file for VSCode settings to allow administrators to specify a default user configuration for VSCode sessions (Pro #2014)
* Improved a Slurm Session Launch Delay that may occur due to buffering when using Slurm job steps (Pro #2331)
* Set enviornment variables `RS_URI_SCHEME`, `RS_SESSION_URL`, and `RS_HOME_URL` when VSCode is launched (Pro #2346)
* Updated LimeLM TurboActivate and TurboFloat to v4.4.3.

### RStudio Server

* **BREAKING:** RStudio when served via `http` erroneously reported its own address as `https` during redirects if the header `X-Forwarded-Proto` was defined by a proxy. That could lead to a confusing proxy setup. That has been fixed, but existing proxy installations with redirect rewite settings matching for `https` may have to be adjusted.
* **BREAKING:** RStudio Workbench's Linux packages have new file names, `rstudio-workbench-*` instead of `rstudio-server-pro-*`. The operating system package name remains `rstudio-server`, so installs and upgrades will work correctly. Scripts which refer to the `.deb` or `.rpm` file names directly will need to be updated.

### Misc

* Improve detection for crashes that occur early in session initialization (#7983)
* The mouse back / forward buttons can now be used to navigate within the Help pane (#8338)
* Right-click on document tab provides menu with close, close all, close others (#1664)
* Rename File added to document tab context menu (#8374)
* Copy Path command added to document tab context menu (#7289)
* Preliminary compatibility with UCRT builds of R-devel (#8461)
* Update Windows Desktop to openSSL 1.1.1i (#8574)
* Improve ordering of items in Command Palette list and search results (#7567, #7956)
* Update embedded Pandoc to v2.11.3.2
* Change default per-user install folder to %LocalAppData%\Programs on Windows (#8598)
* Cmd+U now toggles underlining in the visual editor on macOS (#8656)
* Improve YAML cursor position after omni-insert in the visual editor (#8670)
* Detect newer plumber tags when enabling plumber integration (#8118)
* Option to restore RStudio 1.2 tab key behavior in editor find panel; search in Command Palette for "Tab key behavior in find panel matches RStudio 1.2 and earlier" (#7295)
* Show `.renvignore` in Files pane (#8658)
* Make the set of always-shown files and extensions in the Files pane configurable (#3221)
* Log location of addins that raise parse errors at startup (#8012)
* Support dual/charcell-spaced editor fonts (e.g., Fira Code) on Linux desktop environments (#6894)
* Improve logging of session RPC failures (Pro #2248)
* Add support for `rstudioapi` methods enabling callbacks for command execution (Pro #1846)
* Add support for non-CRAN repositories when installing R packages in the background (#8946)
* Add server homepage link and retry options to mitigate "Unable to connect to service" errors (Pro #2066)

### Bugfixes

* Fixed an issue causing slow session startup and "Unable to connect to service" errors on RStudio Server (#9152)
* Fixed issue causing Project Sharing to fail to set Access Control Lists when using NFS v4 and `username@domain` security principals (Pro #2415)
* Fixed issue causing `verify-installation` to exit without showing the error that caused it to do so (Pro #2399)
* Add server homepage link and retry options to mitigate "Unable to connect to service" errors (Pro #2066)
* Fixed issue causing RStudio Server to create `.local/share/rstudio` folder with incorrect permissions when `session-timeout-kill-hours` is set (Pro #2388)
* Fixed issue causing spurious "Failed to reset ACL permission mask" errors to be logged outside shared projects on some filesystems (Pro #2406)
* Improved R session diagnostic logging; now records all instances of a session (Pro #2268)


