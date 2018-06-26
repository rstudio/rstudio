Configure Windows for RStudio Development
=============================================================================

These instructions are intended for a clean Windows-10 machine and may not
produce a successful build environment if any dependencies are already 
installed.

Bootstrap
=============================================================================
- Save the file `https://github.com/rstudio/rstudio/blob/master/dependencies/windows/Install-RStudio-Prereqs.ps1` to the Windows machine. 
- Open an Administrator PowerShell and enter this command:
    - `Set-ExecutionPolicy Unrestricted -force`
- Execute the downloaded `Install-RStudio-Prereqs.ps1` script 
- Wait for the script to complete (UI will be shown when installing Qt, but
don't interact with it, the script will make the selections)

Clone the Repo and Run Batch File
=============================================================================
- Open Command Prompt (non-administrator); do this **after** running the 
PowerShell bootstrapping script above to pick up environment changes
- `cd` to the location you want the repo
- Clone the repro, e.g. `git clone https://github.com/rstudio/rstudio`
- `cd rstudio\dependencies\windows`
- `install-dependencies.cmd`
- Wait for the script to complete

Build Java/Gwt
=============================================================================
- `cd rstudio\src\gwt`
- `ant draft` or for iterative development of Java/Gwt code, `ant desktop`

Build C++
=============================================================================
- Open Qt Creator
- Open Project and select rstudio\src\cpp\CMakelists.txt
- Select the 64-bit kit (RStudio for Windows is 64-bit only)
- (Optional but recommended): Change the `CMake generator` for the kit to 
`Ninja` for faster incremental builds
- Click Configure, then build

Run RStudio
=============================================================================
- From command prompt, `cd` to the build location, and run `rstudio.bat`
- To run RStudio in Qt Creator, select the rstudio run configuration and
change the working directory to be the root of the build output directory,
i.e. the parent of the `desktop` directory containing rstudio.exe 

Debug RStudio
=============================================================================
- Debug using Qt's debugger

Package Build
=============================================================================
This is not necessary for regular development work, but can be used to fully 
test your installation. This builds RStudio and bundles it up in a setup package.

In a non-administrator command prompt:
- `cd rstudio\package\win32`
- `make-package.bat`

When done, the setup is `rstudio\package\build\RStudio-99.9.9.exe`.

