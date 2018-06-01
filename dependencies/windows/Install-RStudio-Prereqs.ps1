#
# Install-RStudio-Prereqs.ps1
#
# Copyright (C) 2009-18 by RStudio, Inc.
#
# Unless you have received this program directly from RStudio pursuant
# to the terms of a commercial license agreement with RStudio, then
# this program is licensed to you under the terms of version 3 of the
# GNU Affero General Public License. This program is distributed WITHOUT
# ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
# MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
# AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
# 

# ----------------------------------------------------------------------------
# Bootstraps a clean Windows-10 system for development of RStudio.
#
# Run this from an Administrator PowerShell prompt after enabling scripts
# via 'Set-ExecutionPolicy Unrestricted -force'.
#
# After this script finishes, you must clone the rstudio repo and run
# rstudio\dependencies\windows\install-dependencies.cmd to complete
# setting up the build environment.
# ----------------------------------------------------------------------------

function Test-Administrator
{
    $user = [Security.Principal.WindowsIdentity]::GetCurrent();
    (New-Object Security.Principal.WindowsPrincipal $user).IsInRole([Security.Principal.WindowsBuiltinRole]::Administrator)
}

If (-Not (Test-Administrator)) {
    Write-Host "Error: Must run this script as Administrator"
    exit
}

# install R
Write-Host "Downloading R..."
Invoke-WebRequest https://cran.rstudio.com/bin/windows/base/R-3.5.0-win.exe -OutFile c:\R-3.5.0-win.exe
Write-Host "Installing R..."
Start-Process c:\R-3.5.0-win.exe -Wait -ArgumentList '/VERYSILENT /DIR="C:\R\R-3.5.0\"'
Remove-Item c:\R-3.5.0-win.exe -Force
$env:path += ';C:\R\R-3.5.0\bin\i386\'
[Environment]::SetEnvironmentVariable('Path', $env:path, [System.EnvironmentVariableTarget]::Machine);

# install chocolatey
iex ((New-Object System.Net.WebClient).DownloadString('https://chocolatey.org/install.ps1'))
refreshenv

# install some deps via chocolatey
choco install -y cmake --installargs 'ADD_CMAKE_TO_PATH=""System""' --fail-on-error-output
refreshenv
choco install -y jdk8 ant windows-sdk-7.1 7zip git

# install nsis (version on chocolatey is too new)
Write-Host "Downloading NSIS..."
Invoke-WebRequest https://s3.amazonaws.com/rstudio-buildtools/test-qt-windows/nsis-2.50-setup.exe -OutFile C:\nsis-2.50-setup.exe
Write-Host "Installing NSIS..."
Start-Process c:\nsis-2.50-setup.exe -Wait -ArgumentList '/S'
Remove-Item c:\nsis-2.50-setup.exe

# install visual c++ build tools (MSVC 2015 32 and 64-bit)
Write-Host "Downloading Visual C++ Build Tools..."
Invoke-WebRequest https://s3.amazonaws.com/rstudio-buildtools/visualcppbuildtools_full.exe -OutFile C:\visualcppbuildtools_full.exe
Write-Host "Installing Visual C++ Build Tools..."
Start-Process c:\visualcppbuildtools_full.exe -Wait -ArgumentList '/Quiet'
Remove-Item c:\visualcppbuildtools_full.exe

# cpack (an alias from chocolatey) and cmake's cpack conflict.
Remove-Item -Force 'C:\ProgramData\chocolatey\bin\cpack.exe'

# install Qt and Qt Creator
Write-Host "Downloading Qt online installer..."
Invoke-WebRequest https://download.qt.io/archive/online_installers/3.0/qt-unified-windows-x86-3.0.4-online.exe -OutFile c:\qt.exe
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
    widget.deselectAll();
    widget.selectComponent("qt.qt5.5101.win32_msvc2015");
    widget.selectComponent("qt.qt5.5101.win64_msvc2015_64");
    widget.selectComponent("qt.qt5.5101.qtwebengine");
    widget.selectComponent("qt.qt5.5101.qtwebengine.win32_msvc2015");
    widget.selectComponent("qt.qt5.5101.qtwebengine.win64_msvc2015_64");
    widget.deselectComponent("qt.qt5.5101.src");
    widget.deselectComponent("qt.qt5.5101.doc");
    widget.deselectComponent("qt.qt5.5101.doc.qtwebengine");
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
Start-Process c:\qt.exe -Wait -ArgumentList '--script c:\qt.qs'
Remove-Item c:\qt.exe -Force
Remove-Item c:\qt.qs -Force

Write-Host "-----------------------------------------------------------"
Write-Host "Core dependencies installed. Next steps:"
Write-Host "(1) Start a non-adminstrator Command Prompt"
Write-Host "(2) git clone https://github.com/rstudio/rstudio"
Write-Host "(3) change working dir to rstudio\src\dependencies\windows"
Write-Host "(4) install-dependencies.cmd"
Write-Host "-----------------------------------------------------------"
