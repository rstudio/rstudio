# ----------------------------------------------------------------------------
# Bootstrap a clean Windows-11 system for RStudio development.
#
# Run this from an Administrator PowerShell prompt after enabling scripts
# via 'Set-ExecutionPolicy Unrestricted -force'.
#
# See README.md for more details.
# ----------------------------------------------------------------------------

# Set to $false to keep downloads after installing; helpful for debugging script
$DeleteDownloads = $true

function Test-Administrator {
    [CmdletBinding()]
    $user = [Security.Principal.WindowsIdentity]::GetCurrent();
    (New-Object Security.Principal.WindowsPrincipal $user).IsInRole([Security.Principal.WindowsBuiltinRole]::Administrator)
}

function Invoke-DownloadFile {
    [CmdletBinding()]
    param(
        [Parameter(Mandatory = $true)]
        [string]$url,
        
        [Parameter(Mandatory = $true)]
        [string]$output
    )
    Write-Verbose "Downloading from $url to $output"
    try {
        Invoke-WebRequest -Uri $url -OutFile $output -ErrorAction Stop
        Write-Verbose "Download completed successfully"
    }
    catch {
        Write-Error "Download failed: $_"
    }
}

function Install-ChocoPackageIfMissing {
    [CmdletBinding()]
    param(
        [Parameter(Mandatory=$true)]
        [string]$PackageName,
        
        [Parameter(Mandatory=$true)]
        [string]$TestCommand
    )
    
    if (!(Get-Command $TestCommand -ErrorAction SilentlyContinue)) {
        Write-Host "$TestCommand not found, installing $PackageName via chocolatey..."
        choco install -y $PackageName
    } else {
        Write-Host "$TestCommand already installed, skipping $PackageName installation"
    }
}

##############################################################################
# script execution starts here
##############################################################################
If (-Not (Test-Administrator)) {
    Write-Host "Error: Must run this script as Administrator"
    exit
}
if ($PSVersionTable.PSVersion.Major -lt 5) {
    Write-Host "Error: Requires PowerShell 5.0 or newer"
}

# install R
if (-Not (Test-Path -Path "C:\R")) {
    $RSetupPackage = "C:\R-3.6.3-win.exe"
    if (-Not (Test-Path -Path $RSetupPackage)) {
        Invoke-DownloadFile https://rstudio-buildtools.s3.amazonaws.com/R/R-3.6.3-win.exe $RSetupPackage -Verbose
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
Invoke-Expression ((New-Object System.Net.WebClient).DownloadString('https://chocolatey.org/install.ps1'))
refreshenv

# install some deps via chocolatey
choco install -y cmake --installargs 'ADD_CMAKE_TO_PATH=""System""' --fail-on-error-output
refreshenv
choco install -y temurin11
choco install -y -i ant
choco install -y 7zip
choco install -y ninja
choco install -y nsis
choco install -y python313
choco install -y jq
Install-ChocoPackageIfMissing -PackageName "git" -TestCommand "git"

# cpack (an alias from chocolatey) and cmake's cpack conflict.
# Newer choco doesn't have this so don't fail if not found
$ChocoCPack = 'C:\ProgramData\chocolatey\bin\cpack.exe'
if (Test-Path $ChocoCPack) { Remove-Item -Force $ChocoCPack }

#### install msvc 2019 and Windows SDK
Write-Host "Downloading vs_buildtools.exe"
Invoke-DownloadFile `
    https://aka.ms/vs/16/release/vs_buildtools.exe `
    vs_buildtools.exe
Write-Host "Installing Visual C++ Build Tools and Windows SDK (be patient)..."
$buildToolsArgs = @(
    '--includeRecommended',
    '--passive',
    '--wait',
    '--norestart',
    '--nocache',
    '--add', 'Microsoft.VisualStudio.Workload.VCTools',
    '--remove', 'Microsoft.VisualStudio.Component.Windows10SDK.10240',
    '--remove', 'Microsoft.VisualStudio.Component.Windows10SDK.10586',
    '--remove', 'Microsoft.VisualStudio.Component.Windows10SDK.14393',
    '--remove', 'Microsoft.VisualStudio.Component.Windows81SDK'
)
$process = Start-Process -FilePath "vs_buildtools.exe" -ArgumentList $buildToolsArgs -Wait -PassThru
if ($process.ExitCode -ne 0 -and $process.ExitCode -ne 3010) {
    Write-Host "Visual Studio Build Tools installation failed with exit code $($process.ExitCode)"
    exit $process.ExitCode
}
Write-Host "Build Tools installation completed."
if ($DeleteDownloads -and (Test-Path "vs_buildtools.exe")) {
    Remove-Item "vs_buildtools.exe" -Force
}

Write-Host "-----------------------------------------------------------"
Write-Host "Core dependencies successfully installed. Next steps:"
Write-Host "(1) Start a non-administrator Command Prompt"
Write-Host "(2) git clone https://github.com/rstudio/rstudio"
Write-Host "(3) change working dir to rstudio\dependencies\windows"
Write-Host "(4) install-dependencies.cmd"
Write-Host "-----------------------------------------------------------"
