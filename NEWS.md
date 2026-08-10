## RStudio 2026.09.0.0 "Autumn Hawkbit" Release Notes

### New
-

### Fixed
- ([#18464](https://github.com/rstudio/rstudio/issues/18464)): Fixed rainbow fenced div highlighting in the source editor so that colors follow the nesting structure: nested divs now use a different color than their parent, and sibling divs at the same depth share a color, with opening and closing fences of the same div always matching.
- ([#18501](https://github.com/rstudio/rstudio/issues/18501)): Fixed the localhost proxy (`/p/`, `/p6/`) to restrict resolution of "localhost" to the requested address family, so a request no longer risks connecting over the wrong IP version when the resolver returns both an A and an AAAA record.

### Dependencies
-

