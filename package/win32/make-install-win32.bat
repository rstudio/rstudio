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

REM setup variables
setlocal
set WIN32_BUILD_PATH=build32
if "%CMAKE_BUILD_TYPE%" == "" set CMAKE_BUILD_TYPE=Release
if "%CMAKE_BUILD_TYPE%" == "Debug" set WIN32_BUILD_PATH=build32-debug
set INSTALL_PATH=%1
if "%INSTALL_PATH%" == "" set INSTALL_PATH=..\..\..\src\qtcreator-build\session

for %%A in (%*) do (
    if /I "%%A" == "clean" set CLEANBUILD=1
)

if defined CLEANBUILD (
    if exist %WIN32_BUILD_PATH% rmdir /s /q %WIN32_BUILD_PATH%
)

REM perform 32-bit build
if not exist %WIN32_BUILD_PATH% mkdir %WIN32_BUILD_PATH%
cd %WIN32_BUILD_PATH%

REM Build the project
set VS_TOOLS="%ProgramFiles(x86)%\Microsoft Visual Studio\2019\BuildTools\Common7\Tools"
if not exist %VS_TOOLS% set VS_TOOLS="%ProgramFiles(x86)%\Microsoft Visual Studio\2019\Community\Common7\Tools"
if not exist %VS_TOOLS% echo "Could not find VsDevCmd.bat. Please ensure Microsoft Visual Studio 2019 Build tools are installed." && exit /b 1

pushd %VS_TOOLS%
call VsDevCmd.bat -clean_env -no_logo || goto :error
call VsDevCmd.bat -arch=x86 -startdir=none -host_arch=x86 -winsdk=10.0.19041.0 -no_logo || goto :error
popd

cmake -G "Ninja" ^
      -DCMAKE_INSTALL_PREFIX:String=%INSTALL_PATH% ^
      -DCMAKE_BUILD_TYPE=%CMAKE_BUILD_TYPE% ^
      -DRSTUDIO_TARGET=SessionWin32 ^
      -DRSTUDIO_PACKAGE_BUILD=1 ^
      -DCMAKE_C_COMPILER=cl.exe ^
      -DCMAKE_CXX_COMPILER=cl.exe ^
      ..\..\.. || goto :error
cmake --build . --config %CMAKE_BUILD_TYPE% --target install -- %MAKEFLAGS% || goto :error
cd ..

endlocal

goto :EOF

:error
echo Failed to build 32bit components of RStudio! Error: %ERRORLEVEL%
exit /b %ERRORLEVEL%
