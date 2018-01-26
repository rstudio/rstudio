@echo off

REM setup variables
setlocal
set WIN64_BUILD_PATH=build64
if "%CMAKE_BUILD_TYPE%" == "" set CMAKE_BUILD_TYPE=Release
if "%CMAKE_BUILD_TYPE%" == "Debug" set WIN64_BUILD_PATH=build64-debug
set INSTALL_PATH=%1
if "%INSTALL_PATH%" == "" set INSTALL_PATH=..\..\..\src\qtcreator-build\session

if "%2" == "clean" rmdir /s /q %WIN64_BUILD_PATH%

setlocal

REM perform 64-bit build 
mkdir %WIN64_BUILD_PATH%
cd %WIN64_BUILD_PATH%
if exist CMakeCache.txt del CMakeCache.txt
cmake -G"Visual Studio 14 2015 Win64" ^
      -DCMAKE_INSTALL_PREFIX:String=%INSTALL_PATH% ^
      -DCMAKE_BUILD_TYPE=%CMAKE_BUILD_TYPE% ^
      -DRSTUDIO_TARGET=SessionWin64 ^
      -DRSTUDIO_PACKAGE_BUILD=1 ^
      ..\..\.. || goto :error
cmake --build . --config %CMAKE_BUILD_TYPE% --target install || goto :error
cd ..
endlocal

goto :EOF

:error
echo Failed to build 64bit components of RStudio! Error: %ERRORLEVEL%
exit /b %ERRORLEVEL%

