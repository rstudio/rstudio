## RStudio 2024.04.0 "Chocolate Cosmos" Release Notes

### New
#### RStudio
- Updated Ace to version 1.28. (#13708)
- Updated Boost to version 1.83.0. (#13577)
- Updated Electron to version 26.2.4. (#13577)
- Updated the default version of the GitHub Copilot agent to 1.10.3. (#13729)
- Update openssl to 1.1.1w on Mac and Windows (#13683)
- RStudio now supports highlighting of inline YAML chunk options in R Markdown / Quarto documents. (#11663)
- Improved support for development documentation when a package has been loaded via `devtools::load_all()`. (#13526)
- RStudio now supports autocompletion following `@` via `.AtNames`. (#13451)
- RStudio now supports the execution and display of GraphViz (`dot`) graphs in R Markdown / Quarto chunks. (#13187)
- RStudio now supports the execution of chunks with the 'file' option set. (#13636)
- With screen reader support enabled, hitting ESC key allows Tabbing away from editor. [accessibility] (#13593)
- RStudio now supports `LuaLaTeX` to compile Sweave/Rnw documents. (#13812)
- Better error message when user preferences fail to save due to folder permissions. (#12974)
- Update Electon Forge to 6.4.2 and Webpack to 5.89.0. (rstudio-pro#5383)
- The GWT code can now be built with JDKs > 11 (#11242)
- RStudio now supports pasting of file paths for files copied to the clipboard. (#4572)
- RStudio now supports duplicate connection names for Posit drivers. (rstudio-pro#5437)
-

#### Posit Workbench
-

### Fixed
#### RStudio
- Fixed Windows installer to delete Start Menu shortcut during uninstall (#13936)

#### Posit Workbench
-
