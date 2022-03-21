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

function Download-File ($url, $output) {
    (New-Object System.Net.WebClient).DownloadFile($url, $output)
}

##############################################################################
# script execution starts here
##############################################################################
If (-Not (Test-Administrator)) {
    Write-Host "Error: Must run this script as Administrator"
    exit
}

# PowerShell uses TLS 1.0 by default, which many sites will reject.
# Set TLS 1.2 (3072), then TLS 1.1 (768), then TLS 1.0 (192), finally SSL 3.0 (48) 
# Use integers because the enumeration values for TLS 1.2 and TLS 1.1 won't 
# exist in .NET 4.0, even though they are addressable if .NET 4.5+ is 
# installed (.NET 4.5 is an in-place upgrade).
try { 
    $securityProtocolSettingsOriginal = [System.Net.ServicePointManager]::SecurityProtocol
    [System.Net.ServicePointManager]::SecurityProtocol = 3072 -bor 768 -bor 192 -bor 48
} catch {
    Write-Warning 'Unable to set PowerShell to use TLS 1.2 and TLS 1.1 due to old .NET Framework installed.' 
}

# install R
if (-Not (Test-Path -Path "C:\R")) {
    $RSetupPackage = "C:\R-3.6.3-win.exe"
    if (-Not (Test-Path -Path $RSetupPackage)) {
        Write-Host "Downloading R 3.6.3..."
        Download-File https://cran.rstudio.com/bin/windows/base/old/3.6.3/R-3.6.3-win.exe $RSetupPackage
    } else {
        Write-Host "Using previously downloaded R installer"
    }
    Write-Host "Installing R..."
    Start-Process $RSetupPackage -Wait -ArgumentList '/VERYSILENT /DIR="C:\R\R-3.6.3\"'
    if ($DeleteDownloads) { Remove-Item $RSetupPackage -Force }
    $env:path += ';C:\R\R-3.6.3\bin\i386\'
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
choco install -y jdk8
choco install -y -i ant
choco install -y 7zip
choco install -y git
choco install -y ninja
choco install -y windows-sdk-10.1 --version 10.1.19041.0
choco install -y visualstudio2019buildtools --version 16.11.10.0
choco install -y visualstudio2019-workload-vctools --version 1.0.1
choco install -y nsis
choco install -y python

# cpack (an alias from chocolatey) and cmake's cpack conflict.
# Newer choco doesn't have this so don't fail if not found
Remove-Item -Force 'C:\ProgramData\chocolatey\bin\cpack.exe' -ea ig

[System.Net.ServicePointManager]::SecurityProtocol = $securityProtocolSettingsOriginal

Write-Host "-----------------------------------------------------------"
Write-Host "Core dependencies successfully installed. Next steps:"
Write-Host "(1) Install Qt 5.12.8 from https://qt.io for MSVC 2017 64-bit with QtWebEngine"
Write-Host "(2) Start a non-administrator Command Prompt"
Write-Host "(3) git clone https://github.com/rstudio/rstudio"
Write-Host "(4) change working dir to rstudio\src\dependencies\windows"
Write-Host "(5) install-dependencies.cmd"
Write-Host "(6) open Qt Creator, load rstudio\src\cpp\CMakelists.txt"
Write-Host "-----------------------------------------------------------"
