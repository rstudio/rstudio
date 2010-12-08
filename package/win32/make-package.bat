
REM perform 32-bit build 
if "%1" == "clean" rmdir /s /q build
mkdir build
cd build
del CMakeCache.txt
rmdir /s /q build\_CPack_Packages
cmake -G"MinGW Makefiles" -DRSTUDIO_TARGET=Desktop -DCMAKE_BUILD_TYPE=Release ..\..\..
mingw32-make 
cd ..

REM perform 64-bit build and install it into the 32-bit tree
REM (but only do this if we are on win64)
if "%1" == "clean" rmdir /s /q build64
IF DEFINED PROGRAMW6432 call make-install-win64.bat %CD%\build\src\cpp\session

REM create packages
cd build
cpack -G NSIS
REM cpack -G ZIP
cd ..






