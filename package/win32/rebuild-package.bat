
set PACKAGE_DIR="%CD%"

REM Prepend Qt 5.4 SDK Mingw to path
setlocal
set PATH=C:\Qt\Qt5.4.1\Tools\mingw491_32\bin;%PATH%

REM Strip Rtools out of the path (persume it's in the default location)
CALL SET PATH=%PATH:C:\Rtools\bin;=%
CALL SET PATH=%PATH:C:\Rtools\gcc-4.6.3\bin;=%

REM Establish build dir
set BUILD_DIR=build
IF "%CMAKE_BUILD_TYPE%" == "" set CMAKE_BUILD_TYPE=Release
IF "%CMAKE_BUILD_TYPE%" == "Debug" set BUILD_DIR=build-debug

REM perform 32-bit build 
cd "%PACKAGE_DIR%"
mkdir "%BUILD_DIR%"
cd "%BUILD_DIR%"
cmake -G"MinGW Makefiles" -DRSTUDIO_TARGET=Desktop -DCMAKE_BUILD_TYPE=%CMAKE_BUILD_TYPE% -DRSTUDIO_PACKAGE_BUILD=1 ..\..\..
mingw32-make -j4
cd ..

REM perform 64-bit build and install it into the 32-bit tree
REM (but only do this if we are on win64)
IF "%PROCESSOR_ARCHITECTURE%" == "AMD64" call rebuild-install-win64.bat "%PACKAGE_DIR%\%BUILD_DIR%\src\cpp\session" %1

REM create packages
cd "%BUILD_DIR%"

REM creating the NSIS installer takes a long time, so we just make a zip
cpack -G ZIP
cd ..

REM unzip to devel directory
rmdir /S /Q "C:\RStudio-devel"
cd "%BUILD_DIR%"
7z x RStudio-99.9.9.zip -y -o"%USERPROFILE%\RStudio-devel"
cd ..

REM reset modified environment variables (PATH)
endlocal





