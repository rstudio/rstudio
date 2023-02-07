
## RStudio 2023-03.0 "Cherry Blossom" Release Notes

### New
- Adopt panmirror (visual editor) migration to quarto-dev/quarto project #12316
- Upgrade Electron to 22.0.0 #11734
- Session components no longer link to libpq or its dependencies (rstudio/rstudio-pro#2138)
- roxygen2 completions show description and complete as a snippet #11957
- data frame and data frame columns completions show more information #12308
- Support more "complex" start of pipe chains in completions #9612, #10538
- Revisit order of completions, i.e. columns before search path secundary completions #12292
- Improved `[` and data.table related completions #12289, 11202
- Support column completions for arrow objects #11629
- Support searching for roxygen specific information in fuzzy finder (Go to File/Function) #12190
- "Rainbow" fenced divs, controled by Options > R Markdown > [v] Use rainbow fenced divs #12115
- Disable argument tooltips in script editor for unknown functions #12160
- Sessions now have lower CPU priority during suspension on macOS and Linux #12623
- Sessions run on Kubernetes or Slurm will no longer exit with nonzero codes under normal circumstances (rstudio/rstudio-pro#3375)

### Fixed

- Fixed an issue where Git / SVN actions requiring user input could fail in some situations. (#12390)
- Fixed an issue where the plots displayed in the inline preview were incorrectly scaled on devices with high DPI (#4521, #2098, #10825, #7028, #4913).
- Fixed background jobs not showing the entire output #12389
- Fixed mixed-case CSS colors #12562
- Fixed indentation of coloured text #12489
- Fixed (and speedup) identification of presence of external pointers #12328
- Fixed rendering of custom source markers #12425
- Fixed handling of backticks in data frame columns in auto completion #8675
- Fixed group_by() specific completions #12356
- Fixed auto completions when named arguments already in use #12326
- Fixed column name completions when package name is specified #9786
- Fixed Excel import preview #3955
- Fixed indentation of raw strings #12127
- Removed empty spell check tooltips #11306
- Fixed package completion tooltips #12147
- Fixed setting rsession log level using command-line argument or logging.conf #12557
- Fixed issue that allowed users to overwrite their home directory in server mode #12653
- Fixed "Check for updates" incorrectly reports that there are no updates (rstudio-pro #3388)

### Accessibility Improvements

- Editor Selection widget in New Session dialog is now usable via keyboard (rstudio-pro #4205)
- Editor Selection widget in New Session dialog supports screen reader usage (rstudio-pro #4206)
- Editor icons in New Session dialog are marked as cosmetic for screen readers (rstudio-pro #4207)
- Homepage modal dialogs are now implemented correctly for keyboard and screen reader use (rstudio-pro #4208)
- Posit logo on home page marked as cosmetic for screen readers (rstudio-pro #4209)
- Focus and keyboard-focus styles have been improved on the homepage
- Keyboard support has been added to the job summary drop-down in session list items on the homepage
- Improved alt-text and updated link to posit.co on sign-in page logo (rstudio-pro #4096)
- Help pane home page switches to single-column display at narrow widths (#12643)
