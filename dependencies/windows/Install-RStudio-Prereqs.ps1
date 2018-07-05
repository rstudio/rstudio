# ----------------------------------------------------------------------------
# Bootstrap a clean Windows-10 system for RStudio development.
#
# Run this from an Administrator PowerShell prompt after enabling scripts
# via 'Set-ExecutionPolicy Unrestricted -force'.
#
# See README.md for more details.
# ----------------------------------------------------------------------------

# Set to $false to keep downloads after installing; helpful for debugging script
$DeleteDownloads = $true

function Test-Administrator
{
    $user = [Security.Principal.WindowsIdentity]::GetCurrent();
    (New-Object Security.Principal.WindowsPrincipal $user).IsInRole([Security.Principal.WindowsBuiltinRole]::Administrator)
}

##############################################################################
# script execution starts here
##############################################################################
If (-Not (Test-Administrator)) {
    Write-Host "Error: Must run this script as Administrator"
    exit
}

# install R
if (-Not (Test-Path -Path "C:\R")) {
    $RSetupPackage = "C:\R-3.5.0-win.exe"
    if (-Not (Test-Path -Path $RSetupPackage)) {
        Write-Host "Downloading R..."
        Invoke-WebRequest https://cran.rstudio.com/bin/windows/base/R-3.5.0-win.exe -OutFile $RSetupPackage
    } else {
        Write-Host "Using previously downloaded R installer"
    }
    Write-Host "Installing R..."
    Start-Process $RSetupPackage -Wait -ArgumentList '/VERYSILENT /DIR="C:\R\R-3.5.0\"'
    if ($DeleteDownloads) { Remove-Item $RSetupPackage -Force }
    $env:path += ';C:\R\R-3.5.0\bin\i386\'
    [Environment]::SetEnvironmentVariable('Path', $env:path, [System.EnvironmentVariableTarget]::Machine);
} else {
    Write-Host "C:\R already exists, skipping R installation"
}

# install chocolatey
iex ((New-Object System.Net.WebClient).DownloadString('https://chocolatey.org/install.ps1'))
refreshenv

# install some deps via chocolatey
choco install -y cmake --installargs 'ADD_CMAKE_TO_PATH=""System""' --fail-on-error-output
refreshenv
choco install -y jdk8 ant windows-sdk-10.1  7zip git ninja

# install Visual C++ via chocolatey
# The workload step is failing in Docker so we're installing it via Visual Studio installer 
# later in this file. Leaving here as a reminder to try chocolatey again in the future.
# RUN choco install -y visualstudio2017buildtools
# RUN choco install -y visualstudio2017-workload-vctools

# install nsis (version on chocolatey is too new)
if (-Not (Test-Path -Path "C:\Program Files (x86)\NSIS")) {
    $NSISSetup = 'C:\nsis-2.50-setup.exe'
    Write-Host "Downloading NSIS..."
    if (-Not (Test-Path $NSISSetup)) {
        Invoke-WebRequest https://s3.amazonaws.com/rstudio-buildtools/test-qt-windows/nsis-2.50-setup.exe -OutFile $NSISSetup
    } else {
        Write-Host "Using previously downloaded NSIS installer"
    }
    Write-Host "Installing NSIS..."
    Start-Process $NSISSetup -Wait -ArgumentList '/S'
    if ($DeleteDownloads) { Remove-Item $NSISSetup -Force }
} else {
    Write-Host "NSIS already found, skipping"
}

# install visual c++
if (-Not (Test-Path -Path "C:\Program Files (x86)\Microsoft Visual Studio\2017")) {
    $VSSetup = "C:\vs_buildtools_2017.exe"
    Write-Host "Downloading VS Buildtools setup..."
    if (-Not (Test-Path $VSSetup)) {
        Invoke-WebRequest https://s3.amazonaws.com/rstudio-buildtools/vs_buildtools_2017.exe -OutFile $VSSetup
    } else {
        Write-Host "Using previously downloaded Visual Studio installer"
    }
    Write-Host "Installing Visual Studio Build Tools..."
    Start-Process $VSSetup -Wait -ArgumentList '--quiet --add Microsoft.VisualStudio.Workload.VCTools'
    if ($DeleteDownloads) { Remove-Item $VSSetup -Force }
} else {
    Write-Host "Visual Studio 2017 build tools already intalled, skipping"
}

# cpack (an alias from chocolatey) and cmake's cpack conflict.
Remove-Item -Force 'C:\ProgramData\chocolatey\bin\cpack.exe'

Write-Host "-----------------------------------------------------------"
Write-Host "Core dependencies successfully installed. Next steps:"
Write-Host "(1) Start a non-adminstrator Command Prompt"
Write-Host "(2) git clone https://github.com/rstudio/rstudio"
Write-Host "(3) change working dir to rstudio\src\dependencies\windows"
Write-Host "(4) install-dependencies.cmd"
Write-Host "(5) open Qt Creator, load rstudio\src\cpp\CMakelists.txt"
Write-Host "-----------------------------------------------------------"
