
## RStudio 2022-10.0 "Elsbeth Geranium" Release Notes

### R

* Improved handling of diagnostics within glue() expressions. 
* RStudio now provides autocompletion results for packages used but not loaded within a project.
* Improved handling of missing arguments for some functions in the diagnostics system.
* Code editor can show previews of color in strings (R named colors e.g. "tomato3" or of the forms "#rgb", "#rrggbb", "#rrggbbaa")
  when `Options > Code > Display > [ ] Show color preview` is checked. 

### Deprecated / Removed

- Removed the Tools / Shell command (#11253)

### Fixed

- Fixed visual mode outline missing nested R code chunks (#11410)
- Fixed an issue where chunks containing multibyte characters was not executed correctly (#10632)
- Fixed bringing main window under active secondary window when executing background command (#11407)
