## RStudio 2026.06.0 "Blue Plumbago" Release Notes

### New
-

### Fixed
- ([#17701](https://github.com/rstudio/rstudio/issues/17701)): Honor newline characters in `rstudioapi::showDialog()` and `rstudioapi::showPrompt()` messages.
- ([#17729](https://github.com/rstudio/rstudio/issues/17729)): Show distinct copy in the Posit Assistant chat pane and preferences install prompt when the recommended Posit Assistant version is older than the installed one.
- ([#17738](https://github.com/rstudio/rstudio/issues/17738)): Fix `file.edit()` (and other Source pane open events) silently dropping when invoked during an in-flight `closeAllSourceDocs`.

### Dependencies
- Ace 1.43.5
- Copilot Language Server 1.489.0
- Electron 41.5.2
- Copilot Language Server 1.480.0
- Electron 41.7.0
- Node.js 22.22.2 (copilot, Posit AI)
- Quarto 1.9.37
- xterm.js 6.0.0
