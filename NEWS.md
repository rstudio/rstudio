## v1.4 - Release Notes

### Plots

* The default renderer used for the RStudio graphics device can now be customized. (#2142)
* The AGG renderer (as provided by the ragg package) is now a supported backend. (#6539)

### Workbench

* Any tab can be hidden from view through Global Options. (#6428)

### Miscellaneous

* The Files pane now sorts file names naturally, so that e.g. `step10.R` comes after `step9.R`. (#5766)
* Added command to File pane's "More" menu to copy path to clipboard (#6344)
* Table summaries are shown for `tibble` objects in R Notebooks. (#5970)
* The user data folder `~/.rstudio` has been moved to `~/.local/share/rstudio`, and its location can now be customized with `XDG_DATA_HOME`. (#1846)
* The font used in the editor and console can now be customized on RStudio Server. (#2534)
* `XDG_CONFIG_DIRS` can be used to specify alternate directories for server configuration files. (Pro #1607)
* For added security, all cookies are now marked as `SameSite=Lax`. The new option `www-iframe-embedding` marks cookies as `SameSite=None` so RStudio can be used embedded in an IFrame. The new option `www-legacy-cookies` provides a behavior compatible with older browsers. (#6608)
* RStudio now infers document type from shebang (e.g. #!/usr/bin/env sh) for R, Python and shell scripts (#5643)
* New option to configure soft wrapping for R Markdown files, and command to change the soft wrap mode of the editor on the fly (#2341)

### RStudio Server Pro

* SAML is now supported as an authentication mechanism (Pro #1194)
* New option `server-project-sharing-root-dir` allows project sharing outside user home directories (Pro #1340)
* New `X-RSP-Request` header for specifying originating URL behind path-rewriting proxies (Pro #1579)
* New user profile option `session-limit` allow limiting the maximum number of sessions a user can have (Pro #540)
* Project sharing is automatically disabled and a warning is issued when `server-multiple-sessions=0`. (Pro #1263)
* New `load-balancer` option `timeout` limits how long to wait for a response from a node, defaults to 10 seconds. (Pro #1642)
* New `load-balancer` option `verify-ssl-certs` for testing nodes with self-signed certificates when using SSL. (Pro #1504)
* New `launcher-verify-ssl-certs` and `launcher-sessions-callback-verify-ssl-certs` options for testing with self-signed certificates when using SSL. (Pro #1504)

### Bugfixes

* Fixed an issue where hovering mouse cursor over C++ completion popup would steal focus. (#5941)
* Git integration now works properly for project names containing the '!' character. (#6160)
* Fixed header resizing in Data Viewer (#1665)
* Fixed resizing last column in Data Viewer (#2642)
* Fixed inconsistencies in the resizing between a column and its header (#4361)
* Fixed submission of inconsistently indented Python blocks to `reticulate` (#5094)
* Fixed error when redirecting inside Plumber applications in RStudio Server Pro (Pro #1570)
* Fixed failure to open files after an attempt to open a very large file (#6637)
* Fixed Data Viewer getting out of sync with the underlying data when changing live viewer object (#1819)
* Fixed issue where attempts to plot could fail if R tempdir was deleted (#2214)
