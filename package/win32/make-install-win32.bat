::
:: make-install-win32.bat
::
:: Copyright (C) 2022 by Posit Software, PBC
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

if not defined CMAKE_BUILD_TYPE (
    set CMAKE_BUILD_TYPE=Release
)

REM setup variables
if not defined WIN32_BUILD_PATH (
    if "%CMAKE_BUILD_TYPE%" == "Debug" (
        set WIN32_BUILD_PATH=build32-debug
    ) else (
        set WIN32_BUILD_PATH=build32
    )
)

set INSTALL_PATH=%1
for %%A in (%*) do (
    if /I "%%A" == "clean" set CLEANBUILD=1
)

if defined CLEANBUILD (
    if exist %WIN32_BUILD_PATH% rmdir /s /q %WIN32_BUILD_PATH%
)

REM perform 32-bit build
if not exist %WIN32_BUILD_PATH% mkdir %WIN32_BUILD_PATH%
cd %WIN32_BUILD_PATH%

cmake -G "Visual Studio 17 2022" -A Win32 ^
      -DCMAKE_BUILD_TYPE=%CMAKE_BUILD_TYPE% ^
      -DCMAKE_INSTALL_PREFIX=%INSTALL_PATH% ^
      -DRSTUDIO_TARGET=SessionWin32 ^
      -DRSTUDIO_PACKAGE_BUILD=1 ^
      -DLIBR_HOME=C:\R\R-3.6.3 ^
      %RSTUDIO_PROJECT_ROOT% || goto :error
cmake --build . --config %CMAKE_BUILD_TYPE% --target install -- %MAKEFLAGS% || goto :error
cd ..

endlocal
goto :EOF

:error
echo Failed to build 32bit components of RStudio! Error: %ERRORLEVEL%
exit /b %ERRORLEVEL%
