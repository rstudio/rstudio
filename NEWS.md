## RStudio 2021-11 "Prairie Trillium" Release Notes

### Visual Mode

* Improved handling of table markdown in visual editor (#9830)
* Added option to show line numbers in visual mode code chunks (#9387)
* Made visual code chunks collapsible (#8613)
* Fixed code execution via selection in indented visual mode code chunks (#9108)
* Fixed detection of HTTP(S) URLs on Windows in the image resolver (#9837)

### RStudio Workbench

* Added support for setting the `subPath` on Kubernetes sessions using `KubernetesPersistentVolumeClaim` mounts in `/etc/rstudio/launcher-mounts` (Pro #2976).
* Fixed custom shortcuts not appearing correctly in menus 

### Misc

* Add commands to open selected files in columns or active editor (#7920)
* Add *New Blank File* command to Files pane to create empty files of selected type in the directory (#1564)
* Rename CSRF token header `X-CSRF-Token` and cookie `csrf-token` to `X-RS-CSRF-Token` and `rs-csrf-token`, respectively, to avoid clashing with similarly named headers and cookies in other services (#7319)
* Use double indent for function parameters to align with Tidyverse style (#9766)

### Bugfixes

* Fixed errors when uploading files/directory names with invalid characters (Pro #698)
* Added error when rsession may be running a different version of R than expected (Pro #2477)
* Fixed "No such file or directory" errors when auto-saving R Notebook chunks while running them (#9284)
