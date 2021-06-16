
## RStudio 1.5 "Ghost Orchid" Release Notes

### Logging

* In an effort to help make the RStudio Team products more cohesive, logging has been changed significantly.
* By default, server logs will now be written to file instead of syslog. Warnings and errors will still log to syslog. This can be controlled by the `warn-syslog` parameter in `logging.conf`.
* By default, server logs will now be written under `/var/log/rstudio/rstudio-server`.
* Logs can now be written in JSON lines format.
* Reloading the logging.conf configuration by sending a SIGHUP to the `rserver` process will now cause all RStudio managed subprocesses to also refresh their logging configuration.
* Logging can now be partially configured using environment variables (`RS_LOGGER_TYPE`, `RS_LOG_LEVEL`, `RS_LOG_MESSAGE_FORMAT`, and `RS_LOG_DIR`).
* Log files will now rotate by time in addition to the existing rotation by file size. This can be controlled by the `rotate-days` parameter in `logging.conf`.
* For more information, see section 2 of the Admin Guide.

### Bugfixes

* Fixed issue where running Python chunk in notebook did not validate reticulate was installed (#9471)
* Fixed issue where .md, .py, .sql, and .stan files had duplicate event handlers (#9106)
* Fixed issue where output when running tests could be emitted in wrong order in Build pane (#5126)
* Fixed issue where RStudio could crash when viewing a malformed data.frame (#9364)
* Fixed issue where completion tooltip was erroneously shown in multi-line strings in some cases (#8677)
* Fixed issue with autocompletion of column names within native-piped R expressions (#9385)
* Fixed issue where help requests for Python objects would fail with reticulate 1.20 (#9311)
* Fixed issue where busy sessions can't be interrupted and block basic file operations (#2038)
* Fixed issue where R Markdown template skeletons with a '.rmd' extension were not discovered (Pro #1607)
* Removed the breaking change introduced in Juliet Rose that changed the behavior of the X-Forwarded-Proto header when RSW is behind a proxy server (Pro #2657)

### Misc

* Add option to synchronize the Files pane with the current working directory in R (#4615)
* Add new *Set Working Directory* command to context menu for source files (#6781)
* Local background jobs can now be replayed (#5548)
* Improved display of R stack traces in R functions invoked internally by RStudio (#9307)
* High DPI ("Retina") plots are now supported on RStudio Server (#3896)
* The "auto-detect indentation" preference is now off by default (#9211) 
* Prevent user preferences from setting CRAN repos when `allow-cran-repos-edit=0` (Pro #1301)
* Updated embedded nginx in Server Pro to 1.20.1 (Pro #2676)
* **BREAKING:** RStudio Desktop Pro only supports activation with license files (Pro #2300)
* Added AWS Cognito support to openid integration (Pro #2313)
* Add file uploads and downloads to session audit log (Pro #2226)
* Make Cmd+Shift+0 the shortcut for restarting session on MacOS (#7695)
