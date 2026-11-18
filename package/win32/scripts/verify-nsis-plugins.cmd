::
:: verify-nsis-plugins.cmd
::
:: Copyright (C) 2026 by Posit Software, PBC
::
:: Unless you have received this program directly from Posit Software pursuant
:: to the terms of a commercial license agreement with Posit Software, then
:: this program is licensed to you under the terms of version 3 of the
:: GNU Affero General Public License. This program is distributed WITHOUT
:: ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
:: MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
:: AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
::
:: Usage: verify-nsis-plugins.cmd EXE STAGED_DIR [signed]
::
:: Fails if the NSIS installer or uninstaller EXE packs a plugin DLL that isn't
:: in STAGED_DIR, since makensis falls back to the unsigned copy in the NSIS
:: install for any plugin the template doesn't stage. With "signed", also fails
:: unless every packed plugin has a valid signature. Needs 7z.exe on PATH, and
:: signtool.exe too with "signed".
::
@echo off
setlocal

if "%~2" == "" goto :usage
if not "%~3" == "" if /i not "%~3" == "signed" goto :usage

set "PLUGINS_DIR=%TEMP%\rstudio-nsis-plugins-%RANDOM%%RANDOM%"
set RESULT=0

7z.exe e -y -bso0 -o"%PLUGINS_DIR%" "%~1" "$PLUGINSDIR\*.dll" || (
    echo verify-nsis-plugins.cmd: could not extract the plugin DLLs from "%~1"
    set RESULT=1
    goto :cleanup
)

if not exist "%PLUGINS_DIR%\*.dll" (
    echo verify-nsis-plugins.cmd: found no plugin DLLs in "%~1"; is it an NSIS installer?
    set RESULT=1
    goto :cleanup
)

for %%F in ("%PLUGINS_DIR%\*.dll") do (
    if not exist "%~2\%%~nxF" (
        echo verify-nsis-plugins.cmd: "%~1" packs %%~nxF, which isn't staged, so it can't be signed. Add it to the staged plugins in NSIS.template.in.
        set RESULT=1
    )
)
if "%RESULT%" == "1" goto :cleanup

if /i "%~3" == "signed" (
    signtool.exe verify /pa "%PLUGINS_DIR%\*.dll" || (
        echo verify-nsis-plugins.cmd: "%~1" packs a plugin DLL without a valid signature.
        set RESULT=1
    )
)

:cleanup
if exist "%PLUGINS_DIR%" rmdir /s /q "%PLUGINS_DIR%"
exit /b %RESULT%

:usage
echo verify-nsis-plugins.cmd: expected an NSIS executable and the staged plugins folder.
echo Usage: verify-nsis-plugins.cmd EXE STAGED_DIR [signed]
exit /b 1
