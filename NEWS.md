## RStudio 2026.06.0 "Blue Plumbago" Release Notes

### New
- ([#15830](https://github.com/rstudio/rstudio/issues/15830)): Add a Code menu and command-palette entry to re-enable the editor's missing-package banner for a file after dismissing it, and rename the banner's dismissal label to "Don't show for this file" to make the per-file scope explicit.
- ([#17734](https://github.com/rstudio/rstudio/issues/17734)): Support em dashes and box-drawing characters as native R code section delimiters.

### Fixed
- ([#14202](https://github.com/rstudio/rstudio/issues/14202)): Fixed an issue where RStudio Desktop could hang on startup when offline or on an unreliable network connection.
- ([#17701](https://github.com/rstudio/rstudio/issues/17701)): Honor newline characters in `rstudioapi::showDialog()` and `rstudioapi::showPrompt()` messages.
- ([#17729](https://github.com/rstudio/rstudio/issues/17729)): Show distinct copy in the Posit Assistant chat pane and preferences install prompt when the recommended Posit Assistant version is older than the installed one.
- ([#17738](https://github.com/rstudio/rstudio/issues/17738)): Fix `file.edit()` (and other Source pane open events) silently dropping when invoked during an in-flight `closeAllSourceDocs`.
- ([#17745](https://github.com/rstudio/rstudio/issues/17745)): Fix the bundled `air` formatter being copied without the `.exe` extension on Windows, which prevented format-on-save with Air from working.
- ([#17737](https://github.com/rstudio/rstudio/issues/17737)): Restore the "Show .Last.value in environment listing" preference, which had stopped surfacing `.Last.value` in the Environment pane.
- ([#17743](https://github.com/rstudio/rstudio/issues/17743)): Fix the debugger failing to capture the browser environment for functions loaded by the `box` package.
- ([#17712](https://github.com/rstudio/rstudio/issues/17712)): Fix Quarto rendering failing on Windows when the user's home folder path contains an ampersand.
- ([#17669](https://github.com/rstudio/rstudio/issues/17669)): Avoid re-scanning the watched directory on every Files pane notification on macOS by enabling FSEvents per-file event reporting for non-recursive watches.

### Dependencies
- Ace 1.43.5
- Copilot Language Server 1.480.0
- Electron 41.7.0
- Node.js 22.22.2 (copilot, Posit AI)
- Quarto 1.9.38
- xterm.js 6.0.0
