## RStudio 2026.11.0 "Montauk Daisy" Release Notes

### New
-

### Fixed
- ([rstudio/rstudio-pro#13167](https://github.com/rstudio/rstudio-pro/issues/13167)): Fixed an issue where the Packages pane could show a stale package list (e.g. a blank Package Manager metadata column for a just-installed package) when two package-list refreshes overlapped; the superseded list is now discarded instead of overwriting the newer one.
- ([#17196](https://github.com/rstudio/rstudio/issues/17196)): "Run All Chunks Above", "Run All Chunks Below", and "Run All" now run Python chunks, and switch the console between R and Python as needed, when chunk output is sent to the console rather than shown inline.
- ([#19059](https://github.com/rstudio/rstudio/issues/19059)): RStudio is now compatible with (and can build against) OpenSSL 4.x.
- ([#17215](https://github.com/rstudio/rstudio/issues/17215)): Fixed an issue where color previews in the editor shifted the cursor and bracket highlighting to the right at fractional zoom levels or display scaling
- ([#19054](https://github.com/rstudio/rstudio/issues/19054)): Fixed an issue where saving a document could clear its spelling and diagnostic markers; the R Markdown toolbar now also picks up output format changes when autosave is enabled
- ([#19056](https://github.com/rstudio/rstudio/issues/19056)): Fixed an issue where R diagnostics were very slow for large documents when the document or the working directory was on a slow filesystem (e.g. a network drive, or a Windows drive mounted within WSL), as the filesystem was consulted for every function call in the document

### Deprecated / Removed
- ([#19041](https://github.com/rstudio/rstudio/issues/19041)): Removed the "What's New" window from RStudio Desktop, along with its **Help** > **What's New** command, its preference, and the `RSTUDIO_DISABLE_WHATS_NEW` environment variable. Use **Help** > **Release Notes** to see what changed in a release.

### Dependencies
- Electron 44.7.0
- Quarto 1.10.19
- RStudio Desktop on macOS no longer bundles unused gettext and SQLite libraries, and RStudio Desktop Pro no longer requires the PostgreSQL client library (libpq) or Kerberos on macOS and Linux
- Updated OpenSSL on Windows from 3.1.4 to 3.5.9
- Updated OpenSSL on macOS from 3.6.3 to 3.5.9, the long-term support release

