# RStudio "Electron"

## Version Number Update:

To update the version number, just run `./scripts/update-json-version.sh "1.2.3"`

## Error Dialogs

To show error dialogs please use the `createStandaloneErrorDialog` function by importing it with

`import { createStandaloneErrorDialog } from './utils';`

Instead of using the `showErrorBox` function from the `electron` module.

## Formatting

To format the entire project just run `yarn format`. You are encouraged to run this before every commit to avoid unnecessary merge conflicts in the future.
Some code errors shall also arise due to it. Please install `Prettier` VS Code extension so you can run the formatter from a shortcut, or run it from the terminal.

## Command-line

- `--log-level=LEVEL`: control logging verbosity; from least verbose to
  most: `OFF`, `ERR`, `WARN`, `INFO`, `DEBUG` (the default is ERR)
- `--version-json`: output version information on RStudio desktop components, in JSON format, and exit
