## RStudio 2026.12.0 "Parrot Balsam" Release Notes

### New
-

### Fixed
- ([#15965](https://github.com/rstudio/rstudio/issues/15965)): Fixed several issues that prevented building with `-DRSTUDIO_USE_SYSTEM_DEPENDENCIES=ON`, where Hunspell, RapidJSON, utf8cpp, GTest and libgit2 were not found or not linked. Hunspell, dtl and websocketpp, which an installed package cannot provide, are now always fetched, and individual dependencies can be excluded with `-DRSTUDIO_USE_SYSTEM_<NAME>=OFF` (e.g. `-DRSTUDIO_USE_SYSTEM_YAML_CPP=OFF`). Thanks to [@Flamefire](https://github.com/Flamefire).
- ([#19362](https://github.com/rstudio/rstudio/issues/19362)): Fixed an issue where the console could show the warning "converting NULL pointer to R NULL" from `rs_getBrowserEnv` shortly after a session started.
- ([#19386](https://github.com/rstudio/rstudio/issues/19386)): Fixed an issue where the Terminal's right-click menu on Windows and Linux Desktop listed Ctrl+C and Ctrl+V for Copy and Paste, which the terminal sends to the shell, instead of Ctrl+Shift+C and Ctrl+Shift+V.
- ([#19367](https://github.com/rstudio/rstudio/issues/19367)): C and C++ diagnostics and completions no longer stop after the first 19 errors in a file; RStudio now passes `-ferror-limit=0` to libclang. Extra flags can be supplied through a `compile_flags.txt` file (one argument per line, the clangd convention) in the source file's directory or, within a project, any parent up to the project directory; these are appended to the generated flags, so they can override them.

### Dependencies
-

