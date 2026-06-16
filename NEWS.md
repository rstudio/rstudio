## RStudio 2026.07.0 "Pacific Dogwood" Release Notes

### New

### Fixed
- ([#6147](https://github.com/rstudio/rstudio/issues/6147)): The data viewer now scrolls continuously through the columns of a very wide data frame instead of paging through fixed blocks of columns, so a column's filter, sort, and pin are no longer limited to (or lost when leaving) the currently visible set of columns. The column-pagination arrows are replaced by a "Go to column" box in the toolbar that jumps to a column by name or number, and the summary sidebar now lists every column of the frame, loading each column's summary as it scrolls into view. The summary sidebar also reflects the active filter and search rather than always describing the whole frame.

### Dependencies
- Ace 1.43.5
- Copilot Language Server 1.500.0
- Electron 41.7.2
- Node.js 22.22.2 (copilot, Posit AI)
- Quarto 1.9.38
- xterm.js 6.0.0
