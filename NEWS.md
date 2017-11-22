## v1.1a - Release Notes

### Bug Fixes

- Fix sign-out issue when "stay signed in" is checked in RStudio Server (#1538)
- Fix overlap between Refresh icon and plot title text (#1585)
- Fix slow File Open dialog on RStudio Desktop for Linux (#1587)
- Fix slow open of new files on Windows when using network drives (#1592)
- Fix build pane freeze when `Rcpp::compileAttributes` results in parse errors (#1601)
- Fix incorrect output type when compiling R script to non-HTML report (#1631)
- Improve spacing in account when username and servername are short (#1637)
- Fix autocompletion of Rpp C++ objects (#1654)
- Better "hand" cursor position on RStudio Desktop for Linux (#1659)
- Fix segfault that can occur when a notebook contains a Rcpp chunk with compilation failures (#1668)
- Fix problems with Git for Windows installations into sub-directories of home directory (#1679)
- Don't suspend R sessions with active external pointer objects (#1696)
- Allow new "Copy To" command in Files Pane to overwite its target (#1722)
- Respect "Load All" command shortcut when the Terminal is open (#1723)
- Detect full-screen mode correctly in Terminal pane (#1725)
