## v1.4 - Release Notes

### Python

* The default version of Python used by `reticulate` can now be customized via the Global Options pane.
* Python indentation rules are now applied to Python code within R Markdown documents. (#5945)
* Pressing F1 when the Python completion list is shown now opens the relevant Help documentation. (#5982)
* Python objects are now shown in the Environment Pane when `reticulate` REPL is active. (#6862)
* Python objects can now be viewed using the Data Viewer and Object Explorer. (#6862)
* The `matplotlib.pyplot.show()` function now displays PNG plots within the Plots pane. (#4965)
* Plots generated via `matplotlib` are now shown with a higher DPI in the Plots pane when appropriate.
* The autocompletion system can now auto-complete virtual environment names in `reticulate::virtualenv()`.

### Plots

* The default renderer used for the RStudio graphics device can now be customized. (#2142)
* The AGG renderer (as provided by the ragg package) is now a supported backend. (#6539)

### Workbench

* Any tab can be hidden from view through Global Options. (#6428)
* Accessibility preference to reduce focus rectangle display (#7242)
* Multiple source panes can be opened in the main window via Global Options. (#2854)
* Keyboard shortcut `F6` added to navigate focus to the next pane. (#7408)
* Accessibility preference to show a highlight around focused panel (#7881)

### Configurable Paths

* The user data folder `~/.rstudio` has been moved to `~/.local/share/rstudio`, and its location can now be customized with `XDG_DATA_HOME`. (#1846)
* `XDG_CONFIG_DIRS` can be used to specify alternate directories for server configuration files. (Pro #1607)
* It is now possible to specify the exact folder for user data and configuration files using new environment variables `RSTUDIO_DATA_HOME`, `RSTUDIO_CONFIG_DIR`, etc. (#7792)

### Miscellaneous

* The Files pane now sorts file names naturally, so that e.g. `step10.R` comes after `step9.R`. (#5766)
* Added command to File pane's "More" menu to copy path to clipboard (#6344)
* Table summaries are shown for `tibble` objects in R Notebooks. (#5970)
* RStudio now infers document type from shebang (e.g. #!/usr/bin/env sh) for R, Python and shell scripts (#5643)
* New option to configure soft wrapping for R Markdown files, and command to change the soft wrap mode of the editor on the fly (#2341)
* New Command Palette for searching and running build-in commands and add-ins (#5168)
* Colorize parentheses, braces, and brackets in assorted colors (#7027)
* Option to display Console error and message output in same color as regular output (#7029)
* Moved console options to a new pane in Global Options (#7047)
* The Data Viewer now uses the `format()` methods defined for columns entries when available (#7239)
* Add support for navigating source history with mouse forward/back buttons (#7272)
* Add ability to go directly to various Global Option panes via Command Palette (#7678)
* R6Class method definitions are now indexed and accessible by the fuzzy finder (Ctrl + .)
* The 'Preview' command for R documentation files now passes along RdMacros declared from the package DESCRIPTION file. (#6871)
* Some panes didn't have commands for making them visible, now they do (#5775)
* Show correct symbol for Return key in Mac menus (#6524)
* Added command and button for clearing Build pane output (#6636)

### RStudio Server

* The font used in the editor and console can now be customized on RStudio Server. (#2534)
* The new option `www-same-site` provides support for the `SameSite` attribute on cookies issued by RStudio. (#6608)
* New `X-RStudio-Request` header for specifying originating URL behind path-rewriting proxies (Pro #1579)
* New `X-RStudio-Root-Path` header or the new `www-root-path` for specifying the exact path prefixes added by a path-rewriting proxy (Pro #1410).
* The option `www-url-path-prefix` was deprecated and removed. Use `www-root-path` instead.
* Improved error logging of mistyped usernames when using PAM authentication (#7501)

### RStudio Server Pro

* SAML is now supported as an authentication mechanism (Pro #1194)
* OpenID Connect is now support as an authentication mechanism (Pro #1747)
* Visual Studio Code is now an available editor when using Launcher sessions (Pro #1423)
* New `auth-proxy-sign-out-url` option specified an endpoint to take the user to when "Sign Out" is clicked in the IDE user interface (Pro #1745)
* New user profile option `session-limit` allow limiting the maximum number of sessions a user can have (Pro #540)
* Project sharing is automatically disabled and a warning is issued when `server-multiple-sessions=0`. (Pro #1263)
* New `load-balancer` option `timeout` limits how long to wait for a response from a node, defaults to 10 seconds. (Pro #1642)
* New `load-balancer` option `verify-ssl-certs` for testing nodes with self-signed certificates when using SSL. (Pro #1504)
* New `launcher-verify-ssl-certs` and `launcher-sessions-callback-verify-ssl-certs` options for testing with self-signed certificates when using SSL. (Pro #1504)
* R sessions can now be renamed from within the session or the home page. (Pro #1572)
* Project Sharing now works on Launcher sessions.
* Remote session connections over HTTPS can now load certificates from the Apple Keychain. (Pro #1828)
* Improved session load balancing when using the Local Job Launcher plugin to evenly spread session load between Local plugin nodes. (Pro #1814)
* Update embedded nginx to v1.19.2 (Pro #1719)
* Changed the command to retrieve Slurm resource utilization to be run as the current user rather than the `slurm-service-user` (Pro #1527)
* Reduced supurflous log messages in the Slurm Launcher Plugin log file about non-RStudio jobs in Slurm (Pro #1528)

### Bugfixes

* UTF-8 character vectors are now properly displayed within the Environment pane. (#6877)
* Fixed issue where diagnostics system surface "Unknown or uninitialized column" warnings in some cases. (#7372)
* Fixed issue where hovering mouse cursor over C++ completion popup would steal focus. (#5941)
* Fixed issue where autocompletion could fail for functions masked by objects in global environments. (#6942)
* Fixed issue where UTF-8 output from Python chunks was mis-encoded on Windows. (#6254)
* Git integration now works properly for project names containing the '!' character. (#6160)
* Fixed issue where loading the Rfast package could lead to session hangs. (#6645)
* Fixed header resizing in Data Viewer (#1665)
* Fixed resizing last column in Data Viewer (#2642)
* Fixed inconsistencies in the resizing between a column and its header (#4361)
* Fixed submission of inconsistently indented Python blocks to `reticulate` (#5094)
* Fixed error when redirecting inside Plumber applications in RStudio Server Pro (Pro #1570)
* Fixed failure to open files after an attempt to open a very large file (#6637)
* Fixed Data Viewer getting out of sync with the underlying data when changing live viewer object (#1819)
* Fixed issue where attempts to plot could fail if R tempdir was deleted (#2214)
* Fixed issue that caused sessions to freeze due to slow I/O for monitor logs (Pro #1259)
* Added CSRF protection to sign-in pages (Pro #1469)
* Fixed issue that allowed multiple concurrent sign-in requests (#6502)
* Fixed issue where the admin logs page could sometimes crash due to a malformed log statement (Pro #1768)
* Fixed issue where the URL popped out by the Viewer pane was incorrect after navigation (#6967)
* Fixed issue where clicking the filter UI box would sort a data viewer column (#7299)
* Fixed issue where Windows shortcuts were not resolved correctly in file dialogs. (#7327)
* Fixed issue where failure to rotate a log file could cause a process crash (Pro #1779)
* Fixed issue where saving workspace could emit 'package may not be available when loading' warning (#7001)
* Fixed issue where indented Python chunks could not be run (#3731)
* Fixed disappearing commands and recent files/projects when RStudio Desktop opens new windows (#3968)
* Fixed issue where active repositories were not propagated to newly-created `renv` projects (#7136)
* Fixed issue where .DollarNames methods defined in global environment were not resolved (#7487)
* Reduced difference in font size and spacing between Terminal and Console (#6382)
* Fixed issue where path autocompletion in R Markdown documents did not respect Knit Directory preference (#5412)
* Fixed issue where Job Launcher streams could remain open longer than expected when viewing the job details page (Pro #1855)
* Fixed issue where `rstudioapi::askForPassword()` did not mask user input in some cases.
* Fixed issue where Job Launcher admin users would have `gid=0` in Slurm Launcher Sessions (Pro #1935)
* Fixed issue causing script errors when reloading Shiny applications from the editor toolbar (#7762)
* Fixed issue where saving a file or project located in a backed up directory (such as with Dropbox or Google Drive) would frequently fail and display an error prompt (#7131)
* Fixed issue causing C++ diagnostics to fail when Xcode developer tools were active (#7824)
* Added option for clickable links in Terminal pane (#6621)
* Fixed issue where R scripts containing non-ASCII characters in their path could not be sourced as a local job on Windows (#6701)
