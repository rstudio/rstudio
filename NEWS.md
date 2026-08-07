## RStudio 2026.09.0.0 "Autumn Hawkbit" Release Notes

### New
-

### Fixed
- ([#17806](https://github.com/rstudio/rstudio/issues/17806)): Further Data Viewer performance improvements: searching and filtering no longer recompute per-column summary statistics when the Summary panel is hidden (and superseded summary requests are now cancelled); scrolling with a sorted or filtered view no longer re-sorts and re-filters the data after every console command; mouse-wheel scrolling over the frozen (row name / pinned) columns no longer waits on the main thread; and each scroll frame no longer recomputes the Summary panel's scrollbar layout.
- ([#17806](https://github.com/rstudio/rstudio/issues/17806)): Fixed a memory leak in the Data Viewer: each time a viewed object was observed to change (e.g. reassigned at the console), the previous copy of the object stayed pinned in memory for the remainder of the session.

### Dependencies
-

