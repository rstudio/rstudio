
set PACKAGE_DIR="%CD%"

REM clean if requested
if "%1" == "clean" call clean-build.bat

REM Prepend Qt 5.2 SDK Mingw to path
setlocal
set PATH=C:\Qt\Qt5.2.0\Tools\mingw48_32\bin;%PATH%


REM Establish build dir
set BUILD_DIR=build
IF "%CMAKE_BUILD_TYPE%" == "" set CMAKE_BUILD_TYPE=Release
IF "%CMAKE_BUILD_TYPE%" == "Debug" set BUILD_DIR=build-debug

REM perform 32-bit build 
cd "%PACKAGE_DIR%"
mkdir "%BUILD_DIR%"
cd "%BUILD_DIR%"
del CMakeCache.txt
rmdir /s /q "%BUILD_DIR%\_CPack_Packages"
cmake -G"MinGW Makefiles" -DRSTUDIO_TARGET=Desktop -DCMAKE_BUILD_TYPE=%CMAKE_BUILD_TYPE% -DRSTUDIO_PACKAGE_BUILD=1 ..\..\..
mingw32-make 
cd ..

REM perform 64-bit build and install it into the 32-bit tree
REM (but only do this if we are on win64)
IF "%PROCESSOR_ARCHITECTURE%" == "AMD64" call make-install-win64.bat "%PACKAGE_DIR%\%BUILD_DIR%\src\cpp\session" %1

REM create packages
cd "%BUILD_DIR%"
cpack -G NSIS
IF "%CMAKE_BUILD_TYPE%" == "Release" cpack -G ZIP
cd ..

REM reset modified environment variables (PATH)
endlocal





