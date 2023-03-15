
## RStudio 2023.05.0 "Mountain Hydrangea" Release Notes

### New

#### RStudio IDE
- Updated to Electron 23.1.2 (#12785)
- Moved Help panel font size setting to Appearance tab in Global Options (#12816)
- Update openssl to 1.1.1t for Windows (rstudio/rstudio-pro#3675)
- Improve visibility of focus rectangles on Server / Workbench Sign In page [Accessibility] (#12846)

#### Posit Workbench
- 

### Fixed

#### RStudio IDE
- Fixed display problems with Choose R dialog when UI language is French (#12717)
- Fixed focus switching to Help Pane search box after executing ? in the console [Accessibility] (#12741)
- Fixed initial focus placement in Help Pane [Accessibility] (#10600)
- Fixed invalid element role on session-suspended icon [Accessibility] (#12449)
- Improve screen-reader support for Console pane toolbar [Accessibility] (#12825)
- Fixed display problems with Choose R dialog when UI language is French #12717
- Background script jobs are now run using the global environment. This fixes the behaviour of `source()` in backgrounds jobs. (#11866)

#### Posit Workbench
- Fixed unlabeled buttons for screen reader users when page is narrow [Accessibility] (rstudio/rstudio-pro#4340)
- Removed redundant mouse-only New Session widget from accessibility tree [Accessibility] (rstudio/rstudio-pro#4338)
- Fixed launcher error details not showing on the homepage when clicking "Error Details" (rstudio/rstudio-pro#4333)
- Fixed theme button's semantics so it is meaningful to screen reader [Accessibility] (rstudio/rstudio-pro#4337)
- Fixed screen reader accessibility for the homepage theme dropdown menu [Accessibility] (rstudio/rstudio-pro#4339)
