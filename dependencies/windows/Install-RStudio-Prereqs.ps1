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

function Test-Qt-Installed([String] $QtVersion)
{
    $Qt1 = "C:\Qt\$QtVersion"
    $Qt2 = "C:\Qt$QtVersion"
    $Qt3 = "C:\Qt\Qt$QtVersion"

    (Test-Path -Path $Qt1) -or (Test-Path -Path $Qt2) -or (Test-Path -Path $Qt3)
}

function Install-Qt {
    $QtInstaller = "C:\qt.exe"
    if (-Not (Test-Path -Path $QtInstaller)) {
        Write-Host "Downloading Qt online installer..."
        Invoke-WebRequest https://download.qt.io/archive/online_installers/3.0/qt-unified-windows-x86-3.0.4-online.exe -OutFile $QtInstaller
    } else {
        Write-Host "Using previously downloaded Qt online installer"
    }
    $QtScript = @"

function Controller() {
    installer.autoRejectMessageBoxes();
    installer.installationFinished.connect(function() {
        gui.clickButton(buttons.NextButton);
    })
}

Controller.prototype.WelcomePageCallback = function() {
    gui.clickButton(buttons.NextButton, 4000);
}

Controller.prototype.CredentialsPageCallback = function() {
    gui.clickButton(buttons.NextButton);
}

Controller.prototype.IntroductionPageCallback = function() {
    gui.clickButton(buttons.NextButton);
}

Controller.prototype.TargetDirectoryPageCallback = function()
{
    gui.clickButton(buttons.NextButton);
}

Controller.prototype.ComponentSelectionPageCallback = function() {
    var widget = gui.currentPageWidget();
    widget.selectComponent("qt.qt5.5101.win32_msvc2015");
    widget.selectComponent("qt.qt5.5101.win64_msvc2015_64");
    widget.selectComponent("qt.qt5.5101.qtwebengine");
    widget.selectComponent("qt.qt5.5101.qtwebengine.win32_msvc2015");
    widget.selectComponent("qt.qt5.5101.qtwebengine.win64_msvc2015_64");
    widget.deselectComponent("qt.qt5.5101.src");
    gui.clickButton(buttons.NextButton);
}

Controller.prototype.LicenseAgreementPageCallback = function() {
    gui.currentPageWidget().AcceptLicenseRadioButton.setChecked(true);
    gui.clickButton(buttons.NextButton);
}

Controller.prototype.StartMenuDirectoryPageCallback = function() {
    gui.clickButton(buttons.NextButton);
}

Controller.prototype.ReadyForInstallationPageCallback = function()
{
    gui.clickButton(buttons.NextButton);
}

Controller.prototype.FinishedPageCallback = function() {
    var checkBoxForm = gui.currentPageWidget().LaunchQtCreatorCheckBoxForm
    if (checkBoxForm && checkBoxForm.launchQtCreatorCheckBox) {
        checkBoxForm.launchQtCreatorCheckBox.checked = false;
    }
    gui.clickButton(buttons.FinishButton);
}
"@
    $QtScript | Out-File -FilePath C:\qt.qs -Encoding ASCII
    Write-Host "Starting Qt installation. Be patient, don't click on the buttons!"
    Start-Process $QtInstaller -Wait -ArgumentList '--script c:\qt.qs'
    if ($DeleteDownloads) { Remove-Item $QtInstaller -Force }
    Remove-Item c:\qt.qs -Force
}

function Broadcast-SettingChange {

    # broadcast WM_SETTINGCHANGE so Explorer picks up new path from registry
    Add-Type -TypeDefinition @"
        using System;
        using System.Runtime.InteropServices;

        public class NativeMethods
        {
            [DllImport("user32.dll", SetLastError = true, CharSet = CharSet.Auto)]
            public static extern IntPtr SendMessageTimeout(
                IntPtr hWnd, uint Msg, UIntPtr wParam, string lParam,
                uint fuFlags, uint uTimeout, out UIntPtr lpdwResult);
        }
"@

    $HWND_BROADCAST = [IntPtr] 0xffff
    $WM_SETTINGCHANGE = 0x1a
    $SMTO_ABORTIFHUNG = 0x2
    $result = [UIntPtr]::Zero

    [void] ([Nativemethods]::SendMessageTimeout($HWND_BROADCAST, $WM_SETTINGCHANGE, [UIntPtr]::Zero, 'Environment', $SMTO_ABORTIFHUNG, 5000, [ref] $result))
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
choco install -y jdk8 ant windows-sdk-10.1 vcbuildtools 7zip git

# fixups for Windows SDK
mv "C:\Program Files (x86)\Windows Kits\10\bin\x86" "C:\Program Files (x86)\Windows Kits\10\bin\x86_orig"
mv "C:\Program Files (x86)\Windows Kits\10\bin\x64" "C:\Program Files (x86)\Windows Kits\10\bin\x64_orig"
cmd /c mklink /J "C:\Program Files (x86)\Windows Kits\10\bin\x86" "C:\Program Files (x86)\Windows Kits\10\bin\10.0.15063.0\x86"
cmd /c mklink /J "C:\Program Files (x86)\Windows Kits\10\bin\x64" "C:\Program Files (x86)\Windows Kits\10\bin\10.0.15063.0\x64"

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
    if ($DeleteDownloads) { Remove-Item $NSISSetup }
} else {
    Write-Host "NSIS already found, skipping"
}

# cpack (an alias from chocolatey) and cmake's cpack conflict.
Remove-Item -Force 'C:\ProgramData\chocolatey\bin\cpack.exe'

# install Qt and Qt Creator
$QtInstallTries = 5
if (Test-Qt-Installed("5.10.1")) {
    Write-Host "Qt already installed, skipping"
} else {
    # Qt online installer has a high failure rate, so try several times
    for ($i = 0; $i -le $QtInstallTries; $i++) {
        Install-Qt
        if (Test-Qt-Installed("5.10.1")) {break}
    }        
}

# Qt installation doesn't always work (maybe a timeout?)
if (-Not (Test-Qt-Installed("5.10.1"))) {
    Write-Host "Qt not installed; either install yourself or re-run this script to try again" -ForegroundColor Red
} else {

    # Add QtCreator binaries to path, needed for building with JOM
    $oldpath = (Get-ItemProperty -Path ‘Registry::HKEY_LOCAL_MACHINE\System\CurrentControlSet\Control\Session Manager\Environment’ -Name PATH).path
    $newpath = "$oldpath;C:\Qt\Tools\QtCreator\bin"
    Set-ItemProperty -Path ‘Registry::HKEY_LOCAL_MACHINE\System\CurrentControlSet\Control\Session Manager\Environment’ -Name PATH -Value $newpath
    Broadcast-SettingChange

    Write-Host "-----------------------------------------------------------"
    Write-Host "Core dependencies successfully installed. Next steps:"
    Write-Host "(1) Start a non-adminstrator Command Prompt"
    Write-Host "(2) git clone https://github.com/rstudio/rstudio"
    Write-Host "(3) change working dir to rstudio\src\dependencies\windows"
    Write-Host "(4) install-dependencies.cmd"
    Write-Host "(5) open Qt Creator, load rstudio\src\cpp\CMakelists.txt"
    Write-Host "-----------------------------------------------------------"
}
