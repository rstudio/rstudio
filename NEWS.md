## v1.4 - Release Notes

### Plots

* The default renderer used for the RStudio graphics device can now be customized. (#2142)
* The AGG renderer (as provided by the ragg package) is now a supported backend. (#6539)

### Workbench

* Any tab can be hidden from view through Global Options. (#6428)

### Misc

* The Files pane now sorts file names naturally, so that e.g. `step10.R` comes after `step9.R`. (#5766)
* Added command to File pane's "More" menu to copy path to clipboard (#6344)
* Table summaries are shown for `tibble` objects in R Notebooks. (#5970)
* The user data folder `~/.rstudio` has been moved to `~/.local/share/rstudio`, and its location can now be customized with `XDG_DATA_HOME`. (#1846)
* The font used in the editor and console can now be customized on RStudio Server. (#2534)
* `XDG_CONFIG_DIRS` can be used to specify alternate directories for server configuration files. (Pro #1607)

### RStudio Server Pro

* SAML is now supported as an authentication mechanism (Pro #1194)
* New option `server-project-sharing-root-dir` allows project sharing outside user home directories (Pro #1340)
* New `X-RSP-Request` header for specifying originating URL behind path-rewriting proxies (Pro #1579)

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
