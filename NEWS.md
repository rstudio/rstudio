## v1.3 - Release Notes

### Accessibility

* Dramatically improved accessibility for sight-impaired users, including:
  * Keyboard focus management and visibility upgrades
  * Improved keyboard navigation
  * Compatibility with popular screen readers
  * Compliant contrast ratios and other accessibility improvements
  * User preference for disabling user-interface animations such as when zooming panes

### Spell Check

* Real-time spell-checking engine for checking while editing
* Customizable dictionaries and word ignore lists preloaded with common R terms
* Inline correction suggestions

### Preferences and Configuration

* All user preferences and settings can now be set using a plain JSON file
* All user preferences can now have global defaults set by a system administrator
* New diagnostics commands for editing the prefs file, resetting state, and viewing pref system data
* Editor themes, snippets, file templates, and keybindings are now portable and can be installed by admins
* The content of new file templates (New R Script, New R Markdown, etc.) can now be controlled by users or administrators

### renv

* New projects can be initialized with renv, giving them an isolated project environment

### Server Security

* New `auth-timeout-minutes` option signs users out after a defined period of inactivity (Pro #667)
* CSRF hardening improvements including optional validation of the HTTP `Origin` header (Pro #1214)
* Add option `auth-cookies-force-secure` to always mark auth cookies as secure when SSL is terminated upstream (Pro #995)
* Set HTTP header `X-Content-Type-Options` to discourage MIME type sniffing (Pro #1219)
* Authentication cookies are now revoked after signout (Pro #606)
* File-serving resource endpoints are now more restrictive; added new `directory-view-whitelist` option (Pro #607)
* RStudio Server now uses 2048 bit RSA keys, for secure communication of encrypted credentials between server / session and client

### iPad OS 13

* Improved keyboard and touch support for iPadOS 13.1
* Support `Ctrl+[` as Esc key on iPadOS 13.1 keyboards lacking physical Esc key (#4663)
* Add ability to resize panes using keyboard arrow keys via View / Panes / Adjust Splitter

### Terminal Improvements

* User preference to configure initial working directory of new terminals (#1557)
* Command to open a new terminal at location of current editor file
* Command to insert the full path and filename of current editor file into terminal
* Command in File pane to open a new terminal at File pane's current location
* Command in to change terminal to current RStudio working directory (#2363)
* PowerShell Core option in terminal (Windows-only)
* Custom terminal shell option for Windows desktop (previously only on Mac, Linux, and server)
* Change shortcuts for Next/Previous terminal to avoid clash with common Windows shortcuts (#4892)
* Suppress macOS Catalina message about switching to zsh in Terminal pane (#6182)
* Add 'Close All Terminals' command to Terminal menu (#3564)
* Zsh option in terminal for Mac and Linux desktop, and RStudio Server (#5587)

### Diagnostics and Recovery

* Add automated crash handling and reporting
* Show detailed logs and process output when R fails to start (#2097)
* Add "Safe Mode" for opening sessions without profile scripts or workspace restoration (#4338)

### Tutorials

* Support for tutorials with the `learnr` package in a new Tutorials pane

### Background Jobs

* Add support for running Shiny applications as background jobs (#5190)
* Install missing package dependencies in a background job (#5584)

### Auto Save

* Changes automatically (and optionally) saved to disk after a few seconds or when editor loses focus (#5263)
* Option to disable real-time backup of unsaved changes to avoid conflicts with Google Drive, Dropbox, etc. (#3837)
* Option to adjust idle interval for backup or saving changes

### Miscellaneous

* Add global replace with live preview and regular expression support (#2066)
* Enable large file uploads (over 4GB) in RStudio Server (#3299)
* Improved 'Comment / Uncomment' handling of empty lines around selection (#4163)
* Files with extension '.q' are no longer indexed or parsed as R files (#4696)
* Add support for an API command to return the list of R packages RStudio depends on (#2332)
* Upgrade internal JSON parsing engine for speed improvements (#1830)
* Improved ergonomics for history prefix navigation (#2771)
* Make columns resizable in the Environment pane (#4020)
* Add Word Count command (#4237)
* Keyboard shortcuts for main menu items on RStudio Server (e.g. Ctrl+Alt+F for File menu)
* Show number of characters when entering version control commit messages (#5192)
* Update embedded Qt to 5.12.5 for Chromium update, stability and bugfixes (#5399)
* Add preference for changing font size on help pane (#3282)
* Warn when Xcode license has not been agreed to on macOS when command line tools required (#5481)
* Improved browser tab names (project name first, complete product name) (Pro #1172)
* The diagnostics system now understands referenced symbols in glue strings (#5270)
* Add preference for compiling .tex files with tinytex (#2788)
* Long menus and popups now scroll instead of overflowing (#1760, #1794, #2330)
* Sort package-installed R Markdown templates alphabetically (#4929)
* The 'Reopen with Encoding' command now saves unsaved changes before re-opening the document. (#5630)
* Autocomplete support for Plumber `#*` comment keywords (#2220)
* Automatically continue Plumber `#*` on successive lines (#2219)
* Comment / uncomment is now enabled for YAML documents (#3317)
* Reflow comment has been rebound to 'Ctrl + Shift + /' on macOS. (#2443)
* Allow fuzzy matches in help topic search (#3316)
* The diagnostics system better handles missing expressions (#5660)
* Keyboard shortcuts for debugging commands can be customized (#3539)
* Update Pandoc to 2.7.3 (#4512)
* Update SumatraPDF to version 3.1.2 (#3155)
* Allow previewing PDFs in fullscreen mode in Sumatra PDF (#4301)
* RStudio Server runtime files are stored in `/var/run`, or another configurable location, instead of `/tmp` (#4666)
* Errors encountered when attempting to find Rtools installations are handled more gracefully (#5720)
* Enable copying images to the clipboard from the Plots pane (#3142)
* Update minimum supported browser versions (#5593)
* Automatic refresh of the Git pane can now be enabled / disabled as required. (#4368)
* Target directory can be changed from within the 'Upload Files' dialog (RStudio Server)
* Zoom Left/Right Column commands for keyboard users (#5874)
* Increase maximum plot size for large, high-DPI displays (#4968; thanks to Jan Gleixner)
* Make maximum lines in R console configurable; was previously fixed at 1000 (#5919)
* Option to only show project name instead of full path in desktop window title (#1817)
* New `rstudio --version` command to return the version of RStudio Desktop (#3922)
* Scan R Markdown YAML header for R packages required to render document (#4779)
* Support use of F13 - F24 for custom keyboard shortcuts (full Mac keyboard has F13-F19, for example)
* Keyboard shortcuts for searching R help in Help pane, and next/previous help page (#5149)
* Keep keyboard focus in the console during debugging (#6039)
* Enable wrap-around for previous/next source tab by default (#6139)
* Provide full SHA in detail of Git commits (#6155)

### Bugfixes

* Fix issue where calling `install.packages()` without arguments would fail (#5154)
* Fix issue where C code in packages would incorrectly be diagnosed as C++ (#5418)
* Fix plot history when plot() called immediately after dev.off() (#3117)
* Fix debug stopping past breakpoint when source windows are open (#3683)
* Fix diagnostics error with multibyte characters in R Markdown documents on Windows (#1866)
* Fix stale processes when invoking child R processes with large command lines (#3414)
* Fix an issue where help tooltips could become corrupt when using prettycode (#5561)
* Fix an issue where signature tooltips were shown even when disabled by user preference (#5405)
* Fix an issue where Git did not work within projects whose paths contained multibyte characters (#2194)
* Fix an issue where RStudio would fail to preview self-contained bookdown books (#5371)
* Fix modal dialog boundaries extending out of the app window in certain cases (#1605)
* Fix issue where session restore could fail when using multiple user libraries
* Fix issue where library paths were not forwarded when building package documentation
* Restore ability to select and copy text in version control diffs (#4734)
* Fix incorrect column names when non-dataframes with a column named `x` were viewed (#3304)
* Fix inconsistent shading in R Markdown chunk backgrounds with folding (#2992)
* Fix list column display in columns past 50 with data viewer (#5851)
* Fix incorrect column type display when paging columns (#5479)
* Fix incorrect sorting in data viewer when paging columns (#4682)
* Fix carryover of light ANSI background colors (#6092)
* Fix issue where Shiny applications using `reticulate` on Windows could crash on run (#6140)

### RStudio Professional

* Logging improvements; log destinations and levels are more configurable and can be changed in real time
* RStudio Desktop Pro can now function as a client for RStudio Server Pro
* New tools for viewing and managing server users when using named user licensing
* Floating licensing can now pass through an HTTPS proxy
* The Launcher service now starts and runs automatically when the system starts
* New Kubernetes Launcher plugin feature to modify the generated job/pod specs (#1353)
* When containers are created when running RStudio Launcher sessions, user home directories are propagated to the container instead of requiring the home directories to be mounted at `/home`. This is a potentially breaking change, and will requiring updating the `/etc/rstudio/laucher-mounts` file to mount the home directory to the correct location (#1369)
* New Kubernetes Launcher plugin feature to allow the specification of a `requests` limit to allow for oversubscription of Kubernetes resources (#1360)
