## RStudio 2026.04.0 "Globemaster Allium" Release Notes

### New
- ([#17070](https://github.com/rstudio/rstudio/issues/17070)): Added support for the ANSI Erase in Line (EL / CSI K) escape sequence in the console, improving rendering of progress bars and status updates from CLI tools
- ([#16657](https://github.com/rstudio/rstudio/issues/16657)): Added color preview support for YAML files, highlighting hex colors and named R colors
- ([#16734](https://github.com/rstudio/rstudio/issues/16734)): Added mouse wheel support for scrolling pane tabs when there are more tabs than can fit in the visible area
- (#rstudioapi/316): The documentNew API now permits arbitrary file types and extensions
- ([#3780](https://github.com/rstudio/rstudio/issues/3780)): Deleting files from Files pane on Linux Desktop now sends files to the Trash
- ([#3780](https://github.com/rstudio/rstudio/issues/3780)): On RStudio Desktop, the Files pane dropdown menu has a new option to control if deleted files go to Trash/Recycle Bin or are permanently deleted
- ([#16903](https://github.com/rstudio/rstudio/issues/16903)): Changed the default location of the Sidebar pane to be on the left side of the window
- ([#16942](https://github.com/rstudio/rstudio/issues/16942)): Enforce a minimum width for the Sidebar pane
- ([#17000](https://github.com/rstudio/rstudio/issues/17000)): Always show .positai folder in Files pane even when set to hide hidden files

### Fixed
- ([#17005](https://github.com/rstudio/rstudio/issues/17005)): Fixed an issue where the Packages pane was empty when non-PPM repositories were configured alongside PPM repositories and certain packages were installed
- ([#16632](https://github.com/rstudio/rstudio/issues/16632)): Fixed an issue where not all new files would appear in the Files pane after a git pull
- ([#16714](https://github.com/rstudio/rstudio/issues/16714)): Fixed an issue where formatting edits with air did not behave well with the editor undo stack
- ([#16732](https://github.com/rstudio/rstudio/issues/16732)): Fixed an issue where TabSet1 with no tabs assigned would show the Sidebar title
- ([#16733](https://github.com/rstudio/rstudio/issues/16733)): Fixed an issue where a Presentation tab would be added to TabSet2 when it was assigned to the Sidebar
- ([#16771](https://github.com/rstudio/rstudio/issues/16771)): Clarified in documentation that additional source columns are added to the left
- ([#8531](https://github.com/rstudio/rstudio/issues/8531)): Fixed an issue where table chunk outputs did not use all available space when printing
- ([#16740](https://github.com/rstudio/rstudio/issues/16740)): Fixed an issue with opening files from operating system file manager when RStudio had a secondary window open
- ([#16688](https://github.com/rstudio/rstudio/issues/16688)): Fixed an issue with pane layout when exiting RStudio with a zoomed column region
- ([#16798](https://github.com/rstudio/rstudio/issues/16798)): Fixed an issue where whole-word search and replace would not correctly match search terms containing dots
- ([#16814](https://github.com/rstudio/rstudio/issues/16814)): Fixed an issue where apostrophes in file names were displayed as HTML entities in the Files pane
- ([#16834](https://github.com/rstudio/rstudio/issues/16834)): Fixed an issue where an error message was shown on Windows when using "Write Diagnostics File"
- ([#16839](https://github.com/rstudio/rstudio/issues/16839)): Fixed an issue where an inaccessible folder in the PATH on Windows would lead to unnecessary file access attempts and error logging
- ([#16845](https://github.com/rstudio/rstudio/issues/16845)): Fixed issue where adding source columns or changing Sidebar visibility in Pane Layout options caused Console column to fill entire window
- ([#16885](https://github.com/rstudio/rstudio/issues/16885)): Fixed an issue where console warning annotation boxes could include excess content
- ([#16842](https://github.com/rstudio/rstudio/issues/16842)): Fixed issue where Packages pane checkboxes did not display the correct state for renv projects using a shared cache
- ([#16985](https://github.com/rstudio/rstudio/issues/16985)): Fixed an issue where the Environment pane would drop the first list element when a user-defined `str` method existed in the global environment
- ([#16995](https://github.com/rstudio/rstudio/issues/16995)): Fixed an issue where the chunk toolbar could be duplicated when pressing Enter after a chunk header at the end of a document
- ([#17026](https://github.com/rstudio/rstudio/pull/17026)): Fixed an issue where roxygen parentheses interfered with Ctrl+Enter execution
- ([#14626](https://github.com/rstudio/rstudio/issues/14626)): Fixed an issue where RStudio-specific file type icons (e.g. .Rmd, .qmd, .Rpres) were not shown in file managers on Linux
- ([#15609](https://github.com/rstudio/rstudio/issues/15609)): Fixed an issue where RStudio startup on Windows was delayed by several seconds on systems with endpoint security software
- ([#16838](https://github.com/rstudio/rstudio/issues/16838)): Fixed an issue where inline notebook and Quarto plots were blurry on HiDPI displays when custom figure dimensions were specified

### Dependencies
- Ace 1.43.5
- Copilot Language Server 1.425.0
- Electron 39.6.0
- Node.js 22.22.0 (copilot completions)
- Quarto 1.8.26
- xterm.js 6.0.0
