## RStudio 2026.11.0 "Montauk Daisy" Release Notes

### New
- ([#19030](https://github.com/rstudio/rstudio/pull/19030)): Reduced the work that blocks session startup: automatic Python discovery for reticulate now runs in the background, R and Quarto installation details are cached between launches, and SQL and Stan support loads on first use.

### Fixed
- ([#17215](https://github.com/rstudio/rstudio/issues/17215)): Fixed an issue where color previews in the editor shifted the cursor and bracket highlighting to the right at fractional zoom levels or display scaling

### Deprecated / Removed
- ([#19041](https://github.com/rstudio/rstudio/issues/19041)): Removed the "What's New" window from RStudio Desktop, along with its **Help** > **What's New** command, its preference, and the `RSTUDIO_DISABLE_WHATS_NEW` environment variable. Use **Help** > **Release Notes** to see what changed in a release.

### Dependencies
-

