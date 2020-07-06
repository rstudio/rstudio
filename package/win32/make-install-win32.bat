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
call "%ProgramFiles(x86)%\Microsoft Visual Studio\2017\BuildTools\Common7\Tools\VsDevCmd.bat" -clean_env -no_logo || goto :error
call "%ProgramFiles(x86)%\Microsoft Visual Studio\2017\BuildTools\Common7\Tools\VsDevCmd.bat" -arch=x86 -startdir=none -host_arch=x86 -winsdk=10.0.17134.0 -no_logo || goto :error
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
