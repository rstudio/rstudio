## RStudio 2024.10.0 "Kousa Dogwood" Release Notes

### New
#### RStudio
- Use native file and message dialogs by default on Linux desktop (#14683; Linux Desktop)

#### Posit Workbench
-

### Fixed
#### RStudio
- Fixed being unable to save file after cancelling the "Choose Encoding" window (#14896)
- Fixed problems creating new files and projects on a UNC path (#14963, #14964; Windows Desktop)
- Prevent attempting to start Copilot on a non-main thread (#14952)
- Reformat Code no longer inserts whitespace around '^' operator (#14973)
- Prompt for personal access token instead of password when using github via https (#14103)

#### Posit Workbench
-

### Dependencies

- Updated Electron to version 31.2.1 (#14982; Desktop)
