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
:: Usage: verify-nsis-plugins.cmd EXE
::
:: Fails unless every plugin DLL packed into the NSIS installer or uninstaller
:: EXE has a valid signature. makensis falls back to the unsigned copy in the
:: NSIS install for any plugin the template doesn't stage, and this catches it.
:: Needs 7z.exe and signtool.exe on PATH.
::
@echo off
setlocal

if "%~1" == "" (
    echo verify-nsis-plugins.cmd: no file given. Usage: verify-nsis-plugins.cmd EXE
    exit /b 1
)

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

signtool.exe verify /pa "%PLUGINS_DIR%\*.dll" || (
    echo verify-nsis-plugins.cmd: "%~1" packs a plugin DLL without a valid signature. A plugin missing from the staged plugins in NSIS.template.in is packed unsigned; add it there.
    set RESULT=1
)

:cleanup
if exist "%PLUGINS_DIR%" rmdir /s /q "%PLUGINS_DIR%"
exit /b %RESULT%
