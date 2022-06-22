
## RStudio 1.4 "Juliet Rose" Release Notes


### R

* Add native support for Apple Silicon (aarch64) builds of R on macOS (#8652)
* Show memory usage details in Environment pane (#4033)
* Added support for the `|>` pipe operator and the `=>` pipe-bind operator, proposed for R 4.1.0 (#8543)
* Added support for the `\(x)` function shorthand syntax, proposed for R 4.1.0 (#8543)
* Added preference toggle for inserting the `|>` pipe operator when the Insert Pipe Operator command is used (#8534)
* Compilation of Sweave documents now uses tinytex when requested (#2788)
* Preliminary support for R graphics engine version 14 in R 4.1.0. (#9251)

### Python

* The Python REPL can now be interrupted (#8763, #8785)
* Python installs within `/opt/python` and `/opt/local/python` are now discovered by RStudio (#8852)
* Improved handling of unicode input on Windows (#8549)
* Fixed issue where inspecting a null Python object would cause emit errors to console (#8185)
* Detect active Python version when publishing content (#8636)
* Use active Python version when knitting R Markdown files (#8854)
* The active version of Python is now placed on the PATH for new Terminal sessions (#9188)

### RStudio Workbench

* RStudio Server Pro has been renamed to RStudio Workbench to more accurately reflect its cross-language editing capabilities.
* **BREAKING:** RStudio Workbench's Linux packages have new file names, `rstudio-workbench-*` instead of `rstudio-server-pro-*`. The operating system package name remains `rstudio-server`, so installs and upgrades will work correctly. Scripts which refer to the `.deb` or `.rpm` file names directly will need to be updated.
* Added support for JupyterLab 3 (Pro #2022)
* Improved a Slurm Session Launch Delay that may occur due to buffering when using Slurm job steps (Pro #2331)
* Add support for using OpenID and SAML authentication schemes when a proxy is required for outbound requests (Pro #2427)
* Updated product licensing engine (LimeLM) to TurboActivate and TurboFloat to v4.4.3.
* Licenses can now be applied and updated without a restart (Pro #2468)
* Improved R session diagnostic logging; now records all instances of a session (Pro #2268)
* Improved troubleshooting logging for PostgreSQL encrypted password configuration (Pro #2441)
* Improved `locktester` file locking diagnostic utility; now tries all lock types and recommends configuration (Pro #2400)
* Added new `rstudio-server reload` command to reload some server configuration settings without a restart (Pro #2139)
* Added `pool-size` option in `database.conf` to control size of database connection pool; avoid creating a large pool on systems with many CPUs (Pro #2494)
* Display hidden characters in filenames when logging config files with `run-diagnostics` (Pro #2509)

### Visual Studio Code

* Visual Studio Code is fully supported on RStudio Workbench and no longer in beta
* Added support for Dash, Streamlit, and local web development servers in VS Code sessions
* Added RStudio Workbench navigation tools in VS Code sessions
* Added support for code-server 3.4.0+ (Pro # 1984)
* Added a new user settings template file for VSCode settings to allow administrators to specify a default user configuration for VSCode sessions (Pro #2014)
* Set environment variables `RS_URI_SCHEME`, `RS_SESSION_URL`, and `RS_HOME_URL` when VSCode is launched (Pro #2346)

### RStudio Server Open Source

* **BREAKING:** RStudio when served via `http` erroneously reported its own address as `https` during redirects if the header `X-Forwarded-Proto` was defined by a proxy. That could lead to a confusing proxy setup. That has been fixed, but existing proxy installations with redirect rewite settings matching for `https` may have to be adjusted.
* **BREAKING:** RStudio Server no longer supports Internet Explorer 11. 
* Added `session-suspend-on-incomplete-statement` option to enable more aggressive session suspension behavior (Pro #1934)
* New `env-vars` configuration file simplifies setting environment variables for the main RStudio server process. (Pro #2433)
* Refreshed style on sign-in page; sign-in page now has a dark mode that syncs with the operating system's (#9092, #9093)

### Visual Editor

* Code chunks in the visual editor now respect the "Tab Key Always Moves Focus" accessibility setting (#8584)
* The commands "Execute Previous Chunks" and "Execute Subsequent Chunks" now work when the cursor is outside a code chunk in the visual editor (#8500)
* The commands "Go to Next Chunk" and "Go to Previous Chunk" now work in the visual editor (#9005)
* Fixed issue causing the document to scroll unpredictably when a code chunk inside a list item is executed in the visual editor (#8883)
* Cmd+U now toggles underlining in the visual editor on macOS (#8656)
* Improve YAML cursor position after omni-insert in the visual editor (#8670)
* Fix issues finding words with punctuation in visual mode (#8655)
* Fix spurious image insertion when pasting into visual mode from Excel (#8665)
* Fix issue causing spell check to stop identifying misspellings when switching between visual and source modes (#8473)
* Fixed issue importing dataset author data from DOIs in the visual editor (#9059)
* Fixed issue where the Insert Citation dialog in the visual editor would clear selected citation when typeahead searching (#8521)
* Fixed issue where the bibliography path is assumed to be document relative when inserting citations in the visual editor (#8847)
* Added `roxytest` tags `@tests` and `@testexamples` (#10266)

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
* Change default per-user install folder to `%LocalAppData%\Programs` on Windows (#8598)
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
* Add support for commenting and uncommenting code in C (`.c` and `.h`) files (#4109, thanks to @cm421)
* The R session binary (`rsession`) now has a `--version` option for reporting its version (Pro #2410)
* RStudio Desktop startup diagnostics now include the RStudio version/platform and an option to copy to plain text (#6628, #9117)
* Improved Spellcheck package to be compatible with all known Hunspell-compatible dictionaries, improved spellcheck initial loading and large file performance (#9161)

### Bugfixes

* Fixed an issue that could cause RStudio to crash when generating plots on Windows (#9113)
* Fixed an issue causing slow session startup and "Unable to connect to service" errors on RStudio Server (#9152)
* Fix Windows Desktop installer to support running from path with characters from other codepages (#8421)
* Fixed issue where R code input could be executed in the wrong order in some cases (#8837)
* Fixed issue where debugger could hang when debugging functions called via `do.call()` (#5158)
* Fixed issue where rendering .tex document with tinytex would fail on Windows (#8725)
* Fixed issue where reinstalling an already-loaded package could cause errors (#8265)
* Fixed issue where RStudio would inappropriately autoload the 'yaml' and 'tinytex' packages (#8698)
* Fixed issue where right-assignment with multi-line strings gave false-positive diagnostic errors (#8307)
* Fixed issue where restoring R workspace could fail when project path contained non-ASCII characters (#8321)
* Fixed issue where forked R sessions could hang after a package was loaded or unloaded (#8361)
* Fixed issue where attempting to profile lines ending in comment would fail (#8407)
* Fixed issue where warnings + messages were mis-encoded in chunk outputs on Windows (#8565)
* Fixed issue where C++ compilation database was not invalidated when compiler was updated (#8588)
* Fixed issue where SQL chunks containing non-ASCII characters could fail to run on Windows (#8900)
* Fixed issue where 'case:' statements were not outdented when rainbow parentheses were active. (#8846)
* Fixed issue where Stan completion handlers were duplicated on save (#9106)
* Improved checks for non-writable R library paths on startup (Pro #2184)
* Fixed issue preventing R Notebook chunks from being queued for execution if they had never been previously run (#4238)
* Fix various issues when the "Limit Console Output" performance setting was enabled, and enable it by default (#8544, #8504, #8529, #8552)
* Fix display of condition messages (errors and warnings) in some character encodings (#8546)
* Fix out-of-date tooltip when renaming files (#8490, #8491)
* Fix incorrect keyboard shortcuts shown in some places in the Command Palette (#8735)
* Fixed an issue where Load Balanced Local Launcher instances could get into a state where they would no longer receive job updates from other nodes due to silent network drops (Pro #2281)
* Fixed issue with formatting of closing braces when inserting newline in C++ code (#8770)
* Add 'whole word' filter to Find in Files. (#8594)
* Fixed issue where empty panes would remain open and pop open unexpectedly (#8460)
* Fixed an issue where the Kubernetes Launcher could hang in Azure Kubernetes Service (AKS) environments by lowering the watch-timeout-seconds parameter default down to 3 minutes instead of 5 (Pro #2312)
* Fixed issue where 'continue comment on newline' would treat Markdown headers as comments (#6421)
* Fixed issue where Set Working Directory command could fail if path contained quotes (#6004)
* Fixed issue where Pro database drivers will not install if `~/odbcinst.ini` is missing (Pro #2284)
* Fixed issue causing the mouse cursor to become too small in certain areas on Linux Desktop (#8781)
* Fixed issue causing Run Tests command to do nothing unless the Build tab was available (#8775)
* Fixed issue where paging in the DataViewer would throw errors and current columns wouldn't update (#9078)
* Fixed issue causing RStudio Server to create `.local/share/rstudio` folder with incorrect permissions when `session-timeout-kill-hours` is set (Pro #2388)
* Fixed issue causing `verify-installation` to exit without showing the error that caused it to do so (Pro #2399)
* Fixed issue causing spurious "Failed to reset ACL permission mask" errors to be logged outside shared projects on some filesystems (Pro #2406)
* Fixed issue where sending code from Python History pane would switch Console to R mode (#8693)
* Fixed issue where Open File dialog would fail to open files whose names were explicitly typed (#4059)
* Fixed issue causing Project Sharing to fail to set Access Control Lists when using NFS v4 and `username@domain` security principals (Pro #2415)
* Fixed issue where dialog boxes [e.g. Git commit, Installing Pro Database drivers] could fail to show output with Limit Console Output turned on (Pro #2284)
* Fixed issue preventing Kubernetes sessions from starting due to incorrect SSL certificate checking on websocket connections; make websocket connections support the `verify-ssl-certs` option (Pro #2463)
* Fixed issue where uploading a .zip archive when unzip was not on PATH would cause a cryptic error. (#9151)
* Errors that occur when R packages update the Connections pane are now better handled and reported (#9219)
* Fixed error in visual mode spellcheck where underline would disappear when hitting backspace (#9187)
* Fixed an issue where the Kubernetes Launcher can sometimes get in a state where communication with Kubernetes hangs until a restart (Pro #2548)
* Fixed an issue where Kubernetes Launcher services could sometimes leak and never be cleaned up (Pro #2548)
