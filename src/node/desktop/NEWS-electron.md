# RStudio "Electron" Release Notes

## Error Dialogs
To show error dialogs please use the `createStandaloneErrorDialog` function by importing it with

`import { createStandaloneErrorDialog } from './utils';`

Instead of using the `showErrorBox` function from the `electron` module.

## Command-line

* `--log-level=LEVEL`: control logging verbosity; from least verbose to 
most: `OFF`, `ERR`, `WARN`, `INFO`, `DEBUG` (the default is ERR)
* `--version-json`: output version information on RStudio desktop components, in JSON format, and exit
