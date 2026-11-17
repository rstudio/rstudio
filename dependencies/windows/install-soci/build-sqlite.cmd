if /I not "%SOCI_ARCH%"=="arm64" goto x64
rem Native compiler on an ARM64 machine; elsewhere (or with RSTUDIO_ARM64_CROSS=1)
rem the x64-hosted cross compiler, as in make-install-winarm64.bat
set VCVARS_ARCH=x64_arm64
if /I "%PROCESSOR_ARCHITECTURE%"=="ARM64" set VCVARS_ARCH=arm64
if /I "%PROCESSOR_ARCHITEW6432%"=="ARM64" set VCVARS_ARCH=arm64
if "%RSTUDIO_ARM64_CROSS%"=="1" set VCVARS_ARCH=x64_arm64
call vcvarsall.bat %VCVARS_ARCH%
cd sqlite
cd sqlite-*
cl /c /EHsc /MDd sqlite3.c
lib /OUT:sqlite3-debug-arm64.lib sqlite3.obj
move sqlite3-debug-arm64.lib ..
cl /c /EHsc /MD sqlite3.c
lib /OUT:sqlite3-release-arm64.lib sqlite3.obj
move sqlite3-release-arm64.lib ..
exit /b 0

:x64
call vcvarsall.bat amd64
cd sqlite
cd sqlite-*
cl /c /EHsc /MDd sqlite3.c
lib /OUT:sqlite3-debug-x64.lib sqlite3.obj
move sqlite3-debug-x64.lib ..
cl /c /EHsc /MD sqlite3.c
lib /OUT:sqlite3-release-x64.lib sqlite3.obj
move sqlite3-release-x64.lib ..
call vcvarsall.bat x86
cl /c /EHsc /MDd sqlite3.c
lib /OUT:sqlite3-debug-x86.lib sqlite3.obj
move sqlite3-debug-x86.lib ..
cl /c /EHsc /MD sqlite3.c
lib /OUT:sqlite3-release-x86.lib sqlite3.obj
move sqlite3-release-x86.lib ..
