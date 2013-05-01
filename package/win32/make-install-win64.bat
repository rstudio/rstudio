REM setup variables
setlocal
set WIN64_BUILD_PATH=build64
IF "%CMAKE_BUILD_TYPE%" == "" set CMAKE_BUILD_TYPE=Release
IF "%CMAKE_BUILD_TYPE%" == "Debug" set WIN64_BUILD_PATH=build64-debug
set INSTALL_PATH=%1%

REM perform 64-bit build 
if "%2" == "clean" rmdir /s /q %WIN64_BUILD_PATH%
setlocal
mkdir %WIN64_BUILD_PATH%
cd %WIN64_BUILD_PATH%
del CMakeCache.txt
cmake -G"MinGW Makefiles" ^
      -DCMAKE_INSTALL_PREFIX:String=%INSTALL_PATH% ^
      -DRSTUDIO_TARGET=SessionWin64 ^
      -DCMAKE_BUILD_TYPE=%CMAKE_BUILD_TYPE% ^
      -DRSTUDIO_PACKAGE_BUILD=1 ^
      ..\..\..

%CD%\..\..\dependencies\windows\mingw64-4.7.2\x64\bin\mingw32-make install

cd ..
endlocal


