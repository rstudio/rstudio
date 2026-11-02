## RStudio 2026.12.0 "Parrot Balsam" Release Notes

### New
- ([#17173](https://github.com/rstudio/rstudio/issues/17173)): Open-source RStudio Desktop on macOS and Windows can now download an available update in the background and install it on restart ("Install and Restart"), instead of sending you to the download page. Administrators can keep the notification but turn off in-app installation with the `allow-in-app-updates=0` option in `rsession.conf` or the `RSTUDIO_DISABLE_IN_APP_UPDATES` environment variable.

### Fixed
- ([#15965](https://github.com/rstudio/rstudio/issues/15965)): Fixed several issues that prevented building with `-DRSTUDIO_USE_SYSTEM_DEPENDENCIES=ON`, where Hunspell, RapidJSON, utf8cpp, GTest and libgit2 were not found or not linked. Hunspell, dtl and websocketpp, which an installed package cannot provide, are now always fetched, and individual dependencies can be excluded with `-DRSTUDIO_USE_SYSTEM_<NAME>=OFF` (e.g. `-DRSTUDIO_USE_SYSTEM_YAML_CPP=OFF`). Thanks to [@Flamefire](https://github.com/Flamefire).
- ([#19362](https://github.com/rstudio/rstudio/issues/19362)): Fixed an issue where the console could show the warning "converting NULL pointer to R NULL" from `rs_getBrowserEnv` shortly after a session started.
- ([#19386](https://github.com/rstudio/rstudio/issues/19386)): Fixed an issue where the Terminal's right-click menu on Windows and Linux Desktop listed Ctrl+C and Ctrl+V for Copy and Paste, which the terminal sends to the shell, instead of Ctrl+Shift+C and Ctrl+Shift+V.

### Dependencies
-

