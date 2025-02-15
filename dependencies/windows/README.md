# Configure Windows for RStudio Development

These instructions are intended for a clean Windows-11 x64 (not ARM) machine and may not produce
a successful build environment if different versions of any dependencies are already installed.

## Bootstrap

- Open an Administrator PowerShell and execute this command:
  - `Set-ExecutionPolicy Bypass -Scope Process -Force; iex ((New-Object System.Net.WebClient).DownloadString('https://raw.githubusercontent.com/rstudio/rstudio/main/dependencies/windows/Install-RStudio-Prereqs.ps1'))`
- Wait for the script to complete

## Clone the Repo and Run Batch File

- Open Command Prompt (non-administrator); do this **after** running the PowerShell bootstrapping 
  script above to pick up environment changes
- optional: if you will be making commits, configure git (your email address, name, ssh keys, etc.)
  make pull requests from that
- `cd` to the location you want the repo
- Clone the repro, e.g. `git clone https://github.com/rstudio/rstudio`
  - note: if you are not a Posit employee and want to submit changes you will
      need to fork the repo and work against that
- `cd rstudio\dependencies\windows`
- `install-dependencies.cmd`
- Wait for the script to complete

## Build Java/Gwt

- `cd rstudio\src\gwt`
- `ant draft` or for iterative development of Java/Gwt code, `ant desktop`

## Build C++ From Command-Prompt

- `cd rstudio\src`
- `mkdir build`
- `cd build`
- `..\cpp\tools\windows-dev.cmd`
- `cmake ..\cpp -GNinja`
- `ninja`

## Build and run Electron

- Ensure that `node.js` is installed and available on the path. It is
  recommended to use the same version as set in globals.cmake (root of the repo), search for
  `RSTUDIO_NODE_VERSION` to find the current value
- `cd rstudio\src\node\desktop`
- `npm i`
- `npm start`

## Package Build

This is not necessary for regular development work, but can be used to fully
test your installation. This builds RStudio and bundles it up in a setup package.

In a non-administrator command prompt:

- `cd rstudio\package\win32`
- `make-package.bat`

When done, the setup is `rstudio\package\build\RStudio-99.9.9.exe`.
