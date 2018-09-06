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
        Write-Host "Downloading R 3.5.0..."
        Invoke-WebRequest https://cran.rstudio.com/bin/windows/base/old/3.5.0/R-3.5.0-win.exe -OutFile $RSetupPackage
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
if (-Not (Test-Path -Path "C:\R"))
{
    Write-Host "Error: Unable to install R"
    exit
}

# install chocolatey
iex ((New-Object System.Net.WebClient).DownloadString('https://chocolatey.org/install.ps1'))
refreshenv

# install some deps via chocolatey
choco install -y cmake --version 3.12.1 --installargs 'ADD_CMAKE_TO_PATH=""System""' --fail-on-error-output
refreshenv
choco install -y jdk8 ant 7zip git ninja
choco install -y windows-sdk-10.1 --version 10.1.17134.12
choco install -y visualstudio2017buildtools --version 15.8.2.0
choco install -y visualstudio2017-workload-vctools --version 1.3.0

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
