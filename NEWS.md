## v1.1a - Release Notes

### Bug Fixes

- Fix sign-out issue when "stay signed in" is checked in RStudio Server (#1538)
- Fix hang when submitting empty passwords and password encryption is turned off (#1545)
- Fix dark theme partial application in data viewer when upgrading from 1.0 (#1573)
- Fix overlap between Refresh icon and plot title text (#1585)
- Fix slow File Open dialog on RStudio Desktop for Linux (#1587)
- Fix slow open of new files on Windows when using network drives (#1592)
- Fix build pane freeze when `Rcpp::compileAttributes` results in parse errors (#1601)
- Fix issue when migrating from RStudio 1.0 which could result in empty R files (#1623)
- Fix incorrect output type when compiling R script to non-HTML report (#1631)
- Improve spacing in account when username and servername are short (#1637)
- Fix incorrect background highlighting after reindent (#1643)
- Fix autocompletion of Rcpp C++ objects (#1654)
- Better "hand" cursor position on RStudio Desktop for Linux (#1659)
- Fix segfault that can occur when a notebook contains a Rcpp chunk with compilation failures (#1668)
- Fix problems with Git for Windows installations into sub-directories of home directory (#1679)
- Fix crashes when restarting R session while using a network drive (#1690)
- Don't suspend R sessions with active external pointer objects (#1696)
- Allow new "Copy To" command in Files Pane to overwite its target (#1722)
- Respect "Load All" command shortcut when the Terminal is open (#1723)
- Detect full-screen mode correctly in Terminal pane (#1725)
- Support Pandoc 2 in `RSTUDIO_PANDOC` (#1756)
- Allow installation of package dependencies from repos not named CRAN (#1762)
- Fix password entry in Subversion and other shell dialogs (#1810)
- Ensure R Markdown files are saved before publish (#1821)
- Reconnect terminal when disconnected via proxy timeout (#1844)
- Closing Shiny apps can crash RStudio Server (#2061)
- Fix hang when `.git` folder is not hidden on Windows (#2141)
- Setting non-default Knit Directory breaks R Markdown websites (#2158)
- Can't scroll through database objects if there are more than 25 (#2211)
- Pressing F1 on help autocompletions raises an error (#2261)
- Server Pro: Can't log in to load-balanced servers when password contains a space (Pro #338)

### Other

- Add support for RTools 3.5
