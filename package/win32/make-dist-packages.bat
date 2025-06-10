::
:: make-dist-packages.bat
::
:: Copyright (C) 2025 by Posit Software, PBC
::
:: Unless you have received this program directly from Posit Software pursuant
:: to the terms of a commercial license agreement with Posit Software, then
:: this program is licensed to you under the terms of version 3 of the
:: GNU Affero General Public License. This program is distributed WITHOUT
:: ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
:: MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
:: AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
::
@echo off
setlocal

if "%1" == "--help" goto :showhelp
if "%1" == "-h" goto :showhelp
if "%1" == "help" goto :showhelp
if "%1" == "/?" goto :showhelp

if "%PACKAGE_DIR%" == "" set PACKAGE_DIR=%~dp0
if "%BUILD_DIR%" == "" set BUILD_DIR=build
if "%CMAKE_BUILD_TYPE%" == "" set CMAKE_BUILD_TYPE=RelWithDebInfo
if "%CMAKE_BUILD_TYPE%" == "Debug" set BUILD_DIR=build-debug
if "%PKG_TEMP_DIR%" == "" set PKG_TEMP_DIR=C:/rsbuild

echo DEBUG: make-dist-packages.bat using following values:
echo DEBUG:     PACKAGE_DIR=%PACKAGE_DIR%
echo DEBUG:     BUILD_DIR=%BUILD_DIR%
echo DEBUG:     CMAKE_BUILD_TYPE=%CMAKE_BUILD_TYPE%
echo DEBUG:     PKG_TEMP_DIR=%PKG_TEMP_DIR%

if not exist %BUILD_DIR% (
    echo ERROR: Build directory not found at "%BUILD_DIR%"
    goto :error
)

pushd %BUILD_DIR%
set "BUILD_DIR=%CD%"

if not defined QUICK (
    echo Creating NSIS setup package...
    cpack -C "%CMAKE_BUILD_TYPE%" -G NSIS
    REM emit NSIS error output if present
    if exist "%PKG_TEMP_DIR%\_CPack_Packages\win64\NSIS\NSISOutput.log" type "%PKG_TEMP_DIR%\_CPack_Packages\win64\NSIS\NSISOutput.log"
    if not defined RSTUDIO_DOCKER_DEVELOPMENT_BUILD (
        move "%PKG_TEMP_DIR%\*.exe" "%BUILD_DIR%"
    )
)

if not defined NOZIP (
    if "%CMAKE_BUILD_TYPE%" == "RelWithDebInfo" (
        echo Creating ZIP package...
        cpack -C "%CMAKE_BUILD_TYPE%" -G ZIP
        if not defined RSTUDIO_DOCKER_DEVELOPMENT_BUILD (
            move "%PKG_TEMP_DIR%\*.zip" "%BUILD_DIR%"
        )
    )
)

popd

endlocal
goto :EOF

:showhelp
echo.
echo make-dist-packages
echo.
echo. Produces the RStudio setup package and zip file (installerless) distributables using
echo. already-built binaries.
echo.
echo  Must be invoked from the "package\win32" folder (in the cloned RStudio repository).
echo.
exit /b 0

:error
echo ERROR: Failed to package RStudio!
exit /b 1
