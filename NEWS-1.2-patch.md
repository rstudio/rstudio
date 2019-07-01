
## RStudio v1.2 Patch

### Bug Fixes

- Fix pane configuration being reset after customization (#2101)
- Fix issue where middle click failed to close editor tabs (#4379)
- Fix incorrect application of C++ project compilation options to non-project files (#4404)
- Fix issue where C++ diagnostics failed to work on macOS in some system configurations
- Fix issues on MacOS with command line tool headers when `/usr/include` is missing (#4405)
- Fix failure to start on R 3.6.0 when the `error` option is set in `.Rprofile` (#4441)
- Fix issue where attempts to run R debugger in `.Rprofile` could hang RStudio (#4443)
- Fix issue where RStudio window could display off-screen if display configuration had changed (#4856)
- Fix parsing multi-line expressions with single brackets inside strings (#4452)
- Improve detection of remote sessions on Windows (#4466)
- Fix issue where resize gripper would remain visible when attempting to copy plot to clipboard (#4864)
- Fix issue where text in prompts would fail to display on macOS Mojave (#4497)
- Fix "Reload App" button for Shiny applications in Firefox on RStudio Server (#4552)
- Fix issue where themes without names would not use the file name as a name instead (#4553)
- Fix NULL in code preview on first attempt to import data (#4563)
- Fix issue where attempts to print code could overflow page margins (#4815)
- Prompt for permissions on macOS Mojave when R packages require them (#4579)
- Add explicit dependency on required `libxkbcommon` package on Linux (#4610)
- Fixed inability to execute editor commands from menu in RStudio Server (#4622)
- Remove unnecessary dependency on `rprojroot` package (#4628)
- Fix failure to launch RStudio Desktop when started as root user (#4631)
- Fixed an issue where the Files pane occasionally would fail to scroll to bottom (#4662)
- Fixed an issue where RStudio would always use the discrete GPU on macOS (#4672)
- Fix startup failure when using multiple CRAN repos (#4751)
- Fix console display issue with certain mixed color output patterns (#4777)
- Fix issue where PageDown failed to scroll down in console history (#4894)
- Fix issue where BH package could not be installed in Packrat projects (#1864)
- Fix issue where Cmd+W would not close tabs in popped-out source windows (#4727)
- Fix issue where deployments could fail with R 3.4.4 when using custom repositories (#4916)
- Fix issue where Tablet clicks would be interpreted as double-clicks. (#4935)
- Fix issue where keyboard shortcuts would not work in Data Viewer (#4944)

### Miscellaneous

- Restore capability to use 32-bit R on Windows (#3150)
- Add new builds for RHEL8 and Fedora 28+ (#4612)
- RStudio now prepends its own version information to the default R user agent (#4826)
- Improve detection of current working directory for terminals on macOS (#4570)
- Update to Pandoc 2.7.2 on Windows to address occasional segfaults (#4618)
- Qt support libraries for Wayland are now bundled on Linux (#4686)
- Set secure flag when clearing cookies for consistency (Pro #964)
- Server Pro: Update to nginx 1.17 
