## RStudio 2026.09.0.0 "Autumn Hawkbit" Release Notes

### New
-

### Fixed
- ([#18472](https://github.com/rstudio/rstudio/issues/18472)): Fixed an issue where editing a line could leave stale syntax highlighting on the lines below it in plain Markdown and YAML documents -- for example, rainbow fenced div colors not updating below a newly-inserted fence, or a YAML multiline string keeping its old highlighting after its opener was re-indented.
- ([#18469](https://github.com/rstudio/rstudio/issues/18469)): Fixed an issue where an R raw string (e.g. `r"(...)"`) corrupted internal editor state for all following lines, most visibly breaking header folding below the raw string in R Markdown documents.
- ([#18468](https://github.com/rstudio/rstudio/issues/18468)): Fixed an issue where pressing Enter inside a C++ (e.g. Rcpp) chunk of an R Markdown or Quarto document failed to indent the new line, raising an internal error, whenever another completed chunk appeared earlier in the document. Also fixed an internal error raised when reformatting R code containing Quarto `#|` comment lines.
- ([#18464](https://github.com/rstudio/rstudio/issues/18464)): Fixed rainbow fenced div highlighting in the source editor so that colors follow the nesting structure: nested divs now use a different color than their parent, and sibling divs at the same depth share a color, with opening and closing fences of the same div always matching.

### Dependencies
-

