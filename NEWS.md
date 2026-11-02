## RStudio 2026.12.0 "Parrot Balsam" Release Notes

### New
-

### Fixed
- ([#15965](https://github.com/rstudio/rstudio/issues/15965)): Fixed several issues that prevented building with `-DRSTUDIO_USE_SYSTEM_DEPENDENCIES=ON`, where Hunspell, RapidJSON, utf8cpp, GTest and libgit2 were not found or not linked. Hunspell, dtl and websocketpp, which an installed package cannot provide, are now always fetched, and individual dependencies can be excluded with `-DRSTUDIO_USE_SYSTEM_<NAME>=OFF` (e.g. `-DRSTUDIO_USE_SYSTEM_YAML_CPP=OFF`). Thanks to [@Flamefire](https://github.com/Flamefire).
- ([#19362](https://github.com/rstudio/rstudio/issues/19362)): Fixed an issue where the console could show the warning "converting NULL pointer to R NULL" from `rs_getBrowserEnv` shortly after a session started.

### Dependencies
-

