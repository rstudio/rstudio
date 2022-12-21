
## RStudio 2023-03.0 "Cherry Blossom" Release Notes

### New
- Adopt panmirror (visual editor) migration to quarto-dev/quarto project #12316
- Upgrade Electron to 22.0.0 #11734
- Session components no longer link to libpq or its dependencies (rstudio/rstudio-pro#2138)

### Fixed

- Fixed an issue where Git / SVN actions requiring user input could fail in some situations. (#12390)
- Fixed an issue where the plots displayed in the inline preview were incorrectly scaled on devices with high DPI (#4521, #2098, #10825, #7028, #4913).
- Fixed background jobs not showing the entire output #12389
