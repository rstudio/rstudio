## RStudio 2026.11.0 "Montauk Daisy" Release Notes

### New
-

### Fixed
- ([#17215](https://github.com/rstudio/rstudio/issues/17215)): Fixed an issue where color previews in the editor shifted the cursor and bracket highlighting to the right at fractional zoom levels or display scaling
- ([#19056](https://github.com/rstudio/rstudio/issues/19056)): Fixed an issue where R diagnostics were very slow for large documents when the document or the working directory was on a slow filesystem (e.g. a network drive, or a Windows drive mounted within WSL), as the filesystem was consulted for every function call in the document

### Dependencies
-

