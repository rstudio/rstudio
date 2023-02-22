# RStudio "Electron"

## Version Number Update:

To update the version number, just run `./scripts/update-json-version.sh "1.2.3"`

## Error Dialogs

To show error dialogs please use the `createStandaloneErrorDialog` function by importing it with

`import { createStandaloneErrorDialog } from './utils';`

Instead of using the `showErrorBox` function from the `electron` module.

## Linting and Formatting

Refer to the [Coding Standards (js, ts) Wiki page](https://github.com/rstudio/rstudio/wiki/Coding-Standards-(js,-ts)) for guidelines on code format and naming conventions.

Follow the steps in the [hooks README](/git_hooks/README.md) to set up the pre-commit hook, which will run the linter and formatter each time you `git commit` some code.

To format the entire project run `npm run format`. You are encouraged to run this before every commit to avoid unnecessary merge conflicts in the future, otherwise please install the pre-commit hook to automatically run the linter and formatter.

Code errors may arise due to running the linting and formatting. You can install the `Prettier` VS Code extension so you can run the formatter from a shortcut, or run it from the terminal.

## Command-line

- `--log-level=LEVEL`: control logging verbosity; from least verbose to
  most: `OFF`, `ERR`, `WARN`, `INFO`, `DEBUG` (the default is ERR)
- `--version-json`: output version information on RStudio desktop components, in JSON format, and exit
