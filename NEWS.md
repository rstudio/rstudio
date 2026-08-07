## RStudio 2026.09.0.0 "Autumn Hawkbit" Release Notes

### New
-

### Fixed
- ([#18468](https://github.com/rstudio/rstudio/issues/18468)): Fixed an issue where pressing Enter inside a C++ (e.g. Rcpp) chunk of an R Markdown or Quarto document failed to indent the new line, raising an internal error, whenever another completed chunk appeared earlier in the document. Also fixed an internal error raised when reformatting R code containing Quarto `#|` comment lines.
- ([#18464](https://github.com/rstudio/rstudio/issues/18464)): Fixed rainbow fenced div highlighting in the source editor so that colors follow the nesting structure: nested divs now use a different color than their parent, and sibling divs at the same depth share a color, with opening and closing fences of the same div always matching.

### Dependencies
-

