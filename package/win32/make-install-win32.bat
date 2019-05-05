@echo off

REM setup variables
setlocal
set WIN32_BUILD_PATH=build32
if "%CMAKE_BUILD_TYPE%" == "" set CMAKE_BUILD_TYPE=Release
if "%CMAKE_BUILD_TYPE%" == "Debug" set WIN32_BUILD_PATH=build32-debug
set INSTALL_PATH=%1
if "%INSTALL_PATH%" == "" set INSTALL_PATH=..\..\..\src\qtcreator-build\session

if "%2" == "clean" rmdir /s /q %WIN32_BUILD_PATH%

setlocal

REM perform 32-bit build
mkdir %WIN32_BUILD_PATH%
cd %WIN32_BUILD_PATH%
if exist CMakeCache.txt del CMakeCache.txt

REM Build the project
cmake -G"Visual Studio 15 2017" ^
      -T host=x86 ^
      -DCMAKE_INSTALL_PREFIX:String=%INSTALL_PATH% ^
      -DCMAKE_BUILD_TYPE=%CMAKE_BUILD_TYPE% ^
      -DRSTUDIO_TARGET=SessionWin32 ^
      -DRSTUDIO_PACKAGE_BUILD=1 ^
      ..\..\.. || goto :error
cmake --build . --config %CMAKE_BUILD_TYPE% --target install || goto :error
cd ..

endlocal

goto :EOF

:error
echo Failed to build 32bit components of RStudio! Error: %ERRORLEVEL%
exit /b %ERRORLEVEL%
