::
:: make-install-winarm64.bat
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
@echo off
setlocal EnableDelayedExpansion

if not defined CMAKE_BUILD_TYPE (
    set CMAKE_BUILD_TYPE=Release
)

REM The ARM64 session links against an ARM64 R.
if not defined LIBR_HOME_ARM64 (
    echo LIBR_HOME_ARM64 is not set; set it to the home folder of an ARM64 R
    exit /b 1
)

REM setup variables
if not defined ARM64_BUILD_PATH (
    if "%CMAKE_BUILD_TYPE%" == "Debug" (
        set ARM64_BUILD_PATH=build-arm64-debug
    ) else (
        set ARM64_BUILD_PATH=build-arm64
    )
)

REM normalize slashes
set "ARM64_BUILD_PATH=%ARM64_BUILD_PATH:/=\%"

set INSTALL_PATH=%1
for %%A in (%*) do (
    if /I "%%A" == "clean" (
        set CLEANBUILD=1
    )
)

if defined CLEANBUILD (
    if exist %ARM64_BUILD_PATH% (
        rmdir /s /q %ARM64_BUILD_PATH%
    )
)

REM Prepare the build directory.
if not exist %ARM64_BUILD_PATH% (
    mkdir %ARM64_BUILD_PATH%
)

cd %ARM64_BUILD_PATH%

REM Native compiler on an ARM64 machine; elsewhere (or with RSTUDIO_ARM64_CROSS=1)
REM the x64-hosted cross compiler, which can't run the ARM64 R or the ARM64 programs.
set VCVARS_ARCH=x64_arm64
if /I "%PROCESSOR_ARCHITECTURE%" == "ARM64" set VCVARS_ARCH=arm64
if /I "%PROCESSOR_ARCHITEW6432%" == "ARM64" set VCVARS_ARCH=arm64
if "%RSTUDIO_ARM64_CROSS%" == "1" set VCVARS_ARCH=x64_arm64
set CROSS_COMPILE_ARG=
if "%VCVARS_ARCH%" == "x64_arm64" set CROSS_COMPILE_ARG=-DRSTUDIO_CROSS_COMPILE=ON

REM Configure for an ARM64 session-only build.
call vcvarsall.bat %VCVARS_ARCH%
cmake -G Ninja ^
      -DCMAKE_BUILD_TYPE=%CMAKE_BUILD_TYPE% ^
      -DCMAKE_INSTALL_PREFIX=%INSTALL_PATH% ^
      -DCMAKE_C_COMPILER=cl.exe ^
      -DCMAKE_CXX_COMPILER=cl.exe ^
      -DRSTUDIO_TARGET=SessionWinARM64 ^
      -DRSTUDIO_PACKAGE_BUILD=1 ^
      "-DLIBR_HOME=%LIBR_HOME_ARM64%" ^
      %CROSS_COMPILE_ARG% ^
      %RSTUDIO_PROJECT_ROOT% || goto :error

REM Perform the build.
cmake --build . --config %CMAKE_BUILD_TYPE% --target install -- %MAKEFLAGS% || goto :error

cd ..

endlocal
goto :EOF

:error
echo Failed to build ARM64 components of RStudio! Error: %ERRORLEVEL%
exit /b %ERRORLEVEL%
