## RStudio 2026.09.0.0 "Autumn Hawkbit" Release Notes

### New
-

### Fixed
- ([#18474](https://github.com/rstudio/rstudio/issues/18474)): Fixed an issue where showing an inline LaTeX / math preview popup in the source editor leaked a keyboard handler each time the popup was rendered; after the popup was dismissed, the leaked handlers swallowed every subsequent Escape keypress in the IDE and raised a client exception ("Cannot read properties of null") on each one.
- ([#18472](https://github.com/rstudio/rstudio/issues/18472)): Fixed an issue where editing a line could leave stale syntax highlighting on the lines below it in plain Markdown and YAML documents -- for example, rainbow fenced div colors not updating below a newly-inserted fence, or a YAML multiline string keeping its old highlighting after its opener was re-indented.
- ([#18447](https://github.com/rstudio/rstudio/issues/18447)): In the Console, Home and End (plus Ctrl+A / Ctrl+E and Cmd+Left / Cmd+Right on macOS, and Alt+Left / Alt+Right on Windows and Linux) now move to the start and end of the whole command, rather than stopping where a long command wraps onto the next line. The matching selection shortcuts (Shift+Home, Shift+End, and friends) select to those same positions.
- ([#18469](https://github.com/rstudio/rstudio/issues/18469)): Fixed an issue where an R raw string (e.g. `r"(...)"`) corrupted internal editor state for all following lines, most visibly breaking header folding below the raw string in R Markdown documents.
- ([#18468](https://github.com/rstudio/rstudio/issues/18468)): Fixed an issue where pressing Enter inside a C++ (e.g. Rcpp) chunk of an R Markdown or Quarto document failed to indent the new line, raising an internal error, whenever another completed chunk appeared earlier in the document. Also fixed an internal error raised when reformatting R code containing Quarto `#|` comment lines.
- ([#18464](https://github.com/rstudio/rstudio/issues/18464)): Fixed rainbow fenced div highlighting in the source editor so that colors follow the nesting structure: nested divs now use a different color than their parent, and sibling divs at the same depth share a color, with opening and closing fences of the same div always matching.
- ([#18444](https://github.com/rstudio/rstudio/issues/18444)): Fixed an issue where the four-pane layout would collapse repeatedly, with any pane that raised itself -- after a package update, a render, or a help lookup -- taking over the whole window and hiding its neighbours. RStudio could be left tracking a zoom that the layout no longer showed, and it then re-applied that zoom to the next pane to raise itself. Escaping a zoom with the pane header's restore button, and showing the sidebar while a pane is zoomed, now end the zoom properly; and a zoom is no longer restored at startup, since the zoomed column widths were already deliberately not restored. Use View > Panes > Show All Panes to clear the bad state in an affected session.
- ([#18482](https://github.com/rstudio/rstudio/issues/18482)): Fixed an issue on Windows where the R session reported a crash as it exited, if a terminal was still running when the session closed. The session had already shut down and saved its state by that point, so nothing was lost, but the spurious report was recorded in the Windows event log and submitted to Windows Error Reporting.

### Dependencies
-

