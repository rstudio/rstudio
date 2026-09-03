## RStudio 2026.10.0 "Blue Mistflower" Release Notes

### New
-

### Fixed
- ([#14363](https://github.com/rstudio/rstudio/issues/14363)): Fixed an issue where hexadecimal literals with binary exponents (e.g. `0x1p3`), fractions, uppercase `0X` prefixes, or an imaginary suffix were reported as parse errors
- ([#18717](https://github.com/rstudio/rstudio/issues/18717)): Fixed an issue where the diagnostics system reported a spurious parse error for indexed numeric literals, e.g. `1[TRUE]`
- ([#18722](https://github.com/rstudio/rstudio/issues/18722)): Fixed an issue where the diagnostics system reported "unexpected end of document" for R scripts ending with a semicolon

### Dependencies
-

