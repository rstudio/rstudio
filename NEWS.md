
## RStudio 2023-03.0 "Cherry Blossom" Release Notes

### New
- Adopt panmirror (visual editor) migration to quarto-dev/quarto project #12316
- Upgrade Electron to 22.0.0 #11734
- Session components no longer link to libpq or its dependencies (rstudio/rstudio-pro#2138)

### Fixed

- Fixed an issue where Git / SVN actions requiring user input could fail in some situations. (#12390)
- Fixed an issue where the plots displayed in the inline preview were incorrectly scaled on devices with high DPI (#4521, #2098, #10825, #7028, #4913).
- Fixed background jobs not showing the entire output #12389

### Accessibility Improvements

- Editor Selection widget in New Session dialog is now usable via keyboard (rstudio-pro #4205)
- Editor Selection widget in New Session dialog supports screen reader usage (rstudio-pro #4206)
- Editor icons in New Session dialog are marked as cosmetic for screen readers (rstudio-pro #4207)
- Homepage modal dialogs are now implemented correctly for keyboard and screen reader use (rstudio-pro #4208)
- Focus and keyboard-focus styles have been improved on the homepage
- Keyboard support has been added to the job summary drop-down in session list items on the homepage
