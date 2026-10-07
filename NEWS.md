## RStudio 2026.11.0 "Montauk Daisy" Release Notes

### New
-

### Fixed
- ([#19059](https://github.com/rstudio/rstudio/issues/19059)): RStudio is now compatible with (and can build against) OpenSSL 4.x.
- ([#17215](https://github.com/rstudio/rstudio/issues/17215)): Fixed an issue where color previews in the editor shifted the cursor and bracket highlighting to the right at fractional zoom levels or display scaling

### Deprecated / Removed
- ([#19041](https://github.com/rstudio/rstudio/issues/19041)): Removed the "What's New" window from RStudio Desktop, along with its **Help** > **What's New** command, its preference, and the `RSTUDIO_DISABLE_WHATS_NEW` environment variable. Use **Help** > **Release Notes** to see what changed in a release.

### Dependencies
- ([rstudio/rstudio-pro#13101](https://github.com/rstudio/rstudio-pro/issues/13101)): RStudio Desktop on macOS no longer bundles unused gettext and SQLite libraries, and RStudio Desktop Pro no longer requires the PostgreSQL client library (libpq) or Kerberos on macOS and Linux

