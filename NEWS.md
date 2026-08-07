## RStudio 2026.09.0.0 "Autumn Hawkbit" Release Notes

### New
-

### Fixed
- ([#18474](https://github.com/rstudio/rstudio/issues/18474)): Fixed an issue where showing an inline LaTeX / math preview popup in the source editor leaked a keyboard handler each time the popup was rendered; after the popup was dismissed, the leaked handlers swallowed every subsequent Escape keypress in the IDE and raised a client exception ("Cannot read properties of null") on each one.

### Dependencies
-

