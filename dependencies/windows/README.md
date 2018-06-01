Configure Windows for RStudio Development
=============================================================================

These instructions are intended to be used on a clean Windows-10 machine. The
scripts may not behave correctly if any dependencies are already installed.

A summary of the steps:

1. Run PowerShell script to bootstrap essential tools
2. Clone the RStudio repo
3. Run batch file from the repo to install the remaining dependencies
4. Configure the project in Qt Creator and build

Install-RStudio-Prereqs.ps1
=============================================================================
- Open an Administrator PowerShell and enter these commands
- `Set-ExecutionPolicy Unrestricted -force`
- `iex ((New-Object System.Net.WebClient).DownloadString('https://github.com/rstudio/rstudio/blob/master/dependencies/windows/Install-RStudio-Prereqs.ps1'))` 
- wait for the script to complete, it runs unattended

Clone the Repo and Run Batch File
=============================================================================
- Open a regular Command Prompt (non-administrator); this needs to be opened after running the PowerShell script above in order to pick up changes to the path, etc.
- Change directories to the location you want the repo
- `git clone https://github.com/rstudio/rstudio`
- `cd rstudio\dependencies\windows`
- `install-dependencies.cmd`
- wait for the script to complete, it runs unattended

Build
=============================================================================
- TODO - write instructions on building the Java/Gwt code and building and debugging with Qt Creator

Package Build
=============================================================================
This is not necessary for regular development work, but can be used to fully test your installation. This builds RStudio and bundles it up in a setup package.

In a non-administrator command prompt:
- `cd rstudio\package\win32`
- `make-package.bat`

When done, the setup is `rstudio\package\build\RStudio-99.9.9.exe`.

