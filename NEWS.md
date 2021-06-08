
## RStudio 1.5 "Ghost Orchid" Release Notes

### Bugfixes

* Fixed issue where .md, .py, .sql, and .stan files had duplicate event handlers (#9106)
* Fixed issue where output when running tests could be emitted in wrong order in Build pane (#5126)
* Fixed issue where RStudio could crash when viewing a malformed data.frame (#9364)
* Fixed issue where completion tooltip was erroneously shown in multi-line strings in some cases (#8677)
* Fixed issue with autocompletion of column names within native-piped R expressions (#9385)
* Fixed issue where help requests for Python objects would fail with reticulate 1.20 (#9311)
* Fixed issue where busy sessions can't be interrupted and block basic file operations (#2038)
* Fixed issue where R Markdown template skeletons with a '.rmd' extension were not discovered (Pro #1607)

### Misc

* Add option to synchronize the Files pane with the current working directory in R (#4615)
* Add new *Set Working Directory* command to context menu for source files (#6781)
* Local background jobs can now be replayed (#5548)
* Improved display of R stack traces in R functions invoked internally by RStudio (#9307)
* The "auto-detect indentation" preference is now off by default. (#9211) 
* Prevent user preferences from setting CRAN repos when `allow-cran-repos-edit=0` (Pro #1301)
* **BREAKING:** RStudio Desktop Pro only supports activation with license files (Pro #2300)
* Added AWS Cognito support to openid integration (Pro #2313)
* Add file uploads and downloads to session audit log (Pro #2226)
