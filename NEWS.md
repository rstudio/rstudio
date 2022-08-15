## RStudio 2022-07.2 "Spotted Wakerobin" Release Notes

### Quarto

* Support for v2 format of Quarto crossref index

### Fixed

* Fix for schema version comparison that breaks db in downgrade -> upgrade scenarios (rstudio-pro#3572)
* Fix for Quarto crossref indexing/completion not working with Quarto v1.0
* Fixed homepage session status problems (rstudio-pro#3644, #3671, and #3669)
* Fixed regression in spotted-wakerobin that prevented sessions from starting when launcher-sessions-use-password=1 (rstudio-pro#3664)
