## v1.4 - Release Notes

### Plots

* The default renderer used for the RStudio graphics device can now be customized. (#2142)
* The AGG renderer (as provided by the ragg package) is now a supported backend. (#6539)

### RStudio Server

* The font used in the editor and console can now be customized. (#2534)

### Workbench

* Any tab can be hidden from view through Global Options. (#6428)

### Misc

* The Files pane now sorts file names naturally, so that e.g. `step10.R` comes after `step9.R`. (#5766)
* Added command to File pane's "More" menu to copy path to clipboard (#6344)

### Bugfixes

* Fixed an issue where hovering mouse cursor over C++ completion popup would steal focus. (#5941)
* Git integration now works properly for project names containing the '!' character. (#6160)
* Fixed header resizing in Data Viewer (#1665)
* Fixed resizing last column in Data Viewer (#2642)
* Fixed inconsistencies in the resizing between a column and its header (#4361)
