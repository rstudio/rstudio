## RStudio 2022-06.0 "Spotted Wakerobin" Release Notes

### New
- Source marker `message` can contain ANSI SGR codes for setting style and color (#9010)
- Linux/MacOS: Executing a code selection that encounters an error will stop execution of remaining code (#3014)
- Added support for hyperlinks in the console and build pane (#1941)
* "Clean and Rebuild" and "Install and Restart" have been merged into "Install Package", the "Project Options > Build" gains a "clean before install" option to toggle the --preclean flag.
* The Build toolbar changes to "Install | Test | Check | More v"
* The source button uses `cpp11::source_cpp()` on C++ files that have `[[cpp11::register]]` decorations (#10387)

#### R
- Added support for using the AGG renderer (as provided by the ragg package) as a graphics backend for inline plot execution; also added support for using the backend graphics device requested by the knitr `dev` chunk option (#9931)

### Fixed
- Fixed notebook execution handling of knitr `message=FALSE` chunk option to suppress messages if the option is set to FALSE (#9436)
- Fixed plot export to PDF options (#9185)
- `.rs.formatDataColumnDispatch()` iterates through classes of `x` (#10073)
- Fixed Find in Files whole-word replace option, so that when "Whole word" is checked, only file matches containing the whole word are replaced, as displayed in the preview (#9813)
- Adds support for POSIX extended regular expressions with backreferences in Find in Files find and replace modes, so that special regex characters such as `+` and `?`, `|`, `(`, etc do not need to be escaped, and other ERE escape sequences such as `\b`, `\w`, and `\d` are now supported. This matches the behavior of R's own `grep()` function, but note that backslashes do not need to be escaped (as they typically are in R strings) (#9344)
- `.rs.api.navigateToFile()` is now synchronous and returns document id (#8938)
* Add a -G option to `rsandbox` to allow configuring the effective group of the process (#3214)

### Deprecated / Removed

There is no deprecated or removed functionality in this release.
