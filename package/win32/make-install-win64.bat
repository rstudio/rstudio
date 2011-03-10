REM setup variables
setlocal
set WIN64_BUILD_PATH=build64 
set MINGW64_PATH=%CD%\..\..\dependencies\windows\mingw64\bin
set INSTALL_PATH=%1%

REM perform 64-bit build 
if "%2" == "clean" rmdir /s /q %WIN64_BUILD_PATH%
setlocal
set PATH=%MINGW64_PATH%;%PATH%
mkdir %WIN64_BUILD_PATH%
cd %WIN64_BUILD_PATH%
del CMakeCache.txt
cmake -G"MinGW Makefiles" ^
      -DCMAKE_INSTALL_PREFIX:String=%INSTALL_PATH% ^
      -DRSTUDIO_TARGET=SessionWin64 ^
      -DCMAKE_BUILD_TYPE=Release ^
      -DRSTUDIO_PACKAGE_BUILD=1 ^
      ..\..\..
mingw32-make install
cd ..
endlocal


