# BRAT Automation

BRAT = "Built-in RStudio Automated Tests"

The `testthat` folder contains RStudio integration tests written in R. This document describes how
to run these tests both on an RStudio Developer machine (i.e. against RStudio you have built from
source) and on any system using an official build of RStudio.

## Running on an RStudio Developer Machine

These tests can be executed in a development environment against either `rserver-dev` (not supported
on Windows), or against the Electron Desktop application on all supported platforms.

The following examples assume you have already successfully built RStudio.

### Server (rserver-dev)

Run the tests from the folder containing the C++ build output, for example `rstudio/src/cpp/build`.

To run all the tests:

```bash
./rserver-dev --run-automation
```

To run only the tests in a given test file (using `test-automation-build-pane.R` as an example):

```bash
./rserver-dev --run-automation --automation-filter="build-pane"
```

### Desktop

Run the tests from the folder containing the desktop project, `rstudio/src/node/desktop`.

To run all the tests:

```bash
npm run automation
```

To run only the tests in a given test file (using `test-automation-build-pane.R` as an example):

```bash
npm run automation -- --automation-filter="build-pane"
```

### Test Markers

Invividual tests may be annotated with zero or more markers for use in selecting a subset of tests.

For example, to mark a test with "wip" so you can run just that test:

```R
.rs.markers("wip")
.rs.test("some super great test", {
    ...
}
```

Separate multiple markers with commas:

```R
.rs.markers("apple", "banana")
.rs.test("some even better super great test", {
    ...
}
```

Examples of running only tests with specified marker(s):

- `./rserver-dev --run-automation --automation-markers="wip"`
- `./rserver-dev --run-automation --automation-filter="rmarkdown" --automation-markers="wip"`
- `npm run automation -- --automation-markers="wip"`

If multiple markers are specified, tests that have any of the markers will be run:

- `./rserver-dev --run-automation --automation-markers="apple banana"`
- `npm run automation -- --automation-markers="apple banana"`

## Testing an Official Build

You can run the BRAT tests against an installed copy of RStudio (i.e. an official build).
In this scenario, the tests matching the commit of the RStudio build will be downloaded from
GitHub automatically.

### Testing Desktop

To run all tests against RStudio.app on macOS:

```bash
/Applications/RStudio.app/Contents/MacOS/RStudio --run-automation
```

More to come...
