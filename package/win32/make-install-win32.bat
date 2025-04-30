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

call vcvarsall.bat x86_64
%RUN% subprocess "where cl.exe" CMAKE_C_COMPILER
%RUN% subprocess "where cl.exe" CMAKE_CXX_COMPILER
cmake -G "Ninja" ^
      -DCMAKE_BUILD_TYPE=%CMAKE_BUILD_TYPE% ^
      -DCMAKE_INSTALL_PREFIX:String=%INSTALL_PATH% ^
      -DRSTUDIO_TARGET=SessionWin32 ^
      -DRSTUDIO_PACKAGE_BUILD=1 ^
      ..\..\.. || goto :error
cmake --build . --config %CMAKE_BUILD_TYPE% --target install -- %MAKEFLAGS% || goto :error
cd ..

endlocal

goto :EOF

:error
echo Failed to build 32bit components of RStudio! Error: %ERRORLEVEL%
exit /b %ERRORLEVEL%
