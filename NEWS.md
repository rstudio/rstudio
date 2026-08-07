## RStudio 2026.09.0.0 "Autumn Hawkbit" Release Notes

### New
- ([#2129](https://github.com/rstudio/rstudio/issues/2129)): The source editor can now be split, showing two views of the same document side by side or one above the other, so different parts of a file can be viewed and edited at the same time. Use the new split button in the editor toolbar, the View > Split Editor menu, or the new commands (Split Editor Right, Split Editor Down, Remove Editor Split, Toggle Editor Split, Focus Other Editor Split); when a source editor already has focus, Move Focus to Source (Ctrl+1) toggles focus between the two views. Edits, undo history, and saved state are shared between the two views, while cursor, selection, scroll position, and folds are per-view; the split is remembered per document. Splits apply to source mode; inline chunk output in R Markdown / Quarto documents continues to display in the primary view only, breakpoints and debug highlighting are managed through the primary view, and Find/Replace searches the primary view.

### Fixed
-

### Dependencies
-

