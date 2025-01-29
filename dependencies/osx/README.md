
System Requirements
=============================================================================

These instructions *should* work on both an Intel Mac or an Apple Silicon Mac,
running a recent macOS release, but we recommend using an Apple Silicon Mac,
as it is required to create a Universal build.

Caveat: We are not actively testing building RStudio on Intel machines, so it is
possible it will stop working at some point.

Install Required Applications for MacOS
=============================================================================

Building RStudio requires installation of several components. The following
must be installed manually:

- R:         https://www.r-project.org/
- XCode:     https://developer.apple.com/xcode/

Note that after installing XCode you should also be sure to install the XCode
Command Line Tools, which can be done by running the following:

```bash
xcode-select --install
```

Installing Homebrew
=============================================================================
Before running the dependency scripts in the next section, you must install
the Homebrew package manager.

First, install Homebrew using the command given on the website at
https://brew.sh/, currently:

```bash
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
```

Next, if you are on an Apple Silicon Mac (as opposed to an older Intel-based Mac), re-run
that command, preceding it with `arch -x86_64`, for example:

```bash
arch -x86_64 /bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
```

Satisfy Additional Dependencies
=============================================================================

Additional dependencies can be satisfied by running the following script:

```bash
./install-dependencies-osx
```

Note that this script includes download, extraction, and compilation of
boost so can take some time to complete.

Building the RStudio Distribution
=============================================================================

With dependencies satisfied, you should be able to produce a full packaged
build of RStudio as follows (assuming the repo is cloned to `~/rstudio`):

```bash
cd ~/rstudio/package/osx
./make-package
```

When completed, the resulting `.dmg` and standalone `.zip` can be found in
`~/rstudio/package/osx/build`.

Run `./make-package --help` for more information.
