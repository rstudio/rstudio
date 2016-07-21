@echo off
set PACKAGE_DIR="%CD%"

REM clean if requested
if "%1" == "clean" (
      call clean-build.bat
      if exist CMakeCache.txt del CMakeCache.txt
)

setlocal

REM Put our MinGW toolchain on the PATH
set MINGW64_32BIT_PATH=%CD%\..\..\dependencies\windows\Rtools33\mingw_32\bin
set PATH=%MINGW64_32BIT_PATH%;%PATH%

REM Remove system Rtools from PATH
call set PATH=%PATH:C:\Rtools\bin=%
call set PATH=%PATH:C:\Rtools\gcc-4.6.3\bin=%

REM Remove Git from PATH (otherwise cmake complains about 'sh.exe')
call set PATH=%PATH:C:\Program Files (x86)\Git\bin=%
call set PATH=%PATH:C:\Program Files\Git\bin=%

REM Establish build dir
set BUILD_DIR=build
if "%CMAKE_BUILD_TYPE%" == "" set CMAKE_BUILD_TYPE=Release
if "%CMAKE_BUILD_TYPE%" == "Debug" set BUILD_DIR=build-debug

REM perform 32-bit build 
cd "%PACKAGE_DIR%"
if not exist "%BUILD_DIR%" mkdir "%BUILD_DIR%"
cd "%BUILD_DIR%"
if exist "%BUILD_DIR%/_CPack_Packages" rmdir /s /q "%BUILD_DIR%\_CPack_Packages"
cmake -G"MinGW Makefiles" ^
      -DRSTUDIO_TARGET=Desktop ^
      -DCMAKE_BUILD_TYPE=%CMAKE_BUILD_TYPE% ^
      -DRSTUDIO_PACKAGE_BUILD=1 ^
      ..\..\.. || goto :error
mingw32-make %MAKEFLAGS% || goto :error
cd ..

REM perform 64-bit build and install it into the 32-bit tree
REM (but only do this if we are on win64)
if "%PROCESSOR_ARCHITECTURE%" == "AMD64" (
      call make-install-win64.bat "%PACKAGE_DIR%\%BUILD_DIR%\src\cpp\session" %1 || goto :error
)

REM create packages
cd "%BUILD_DIR%"
if not "%1" == "quick" cpack -G NSIS
if "%CMAKE_BUILD_TYPE%" == "Release" cpack -G ZIP
cd ..

REM reset modified environment variables (PATH)
endlocal
goto :EOF

:error
echo Failed to build RStudio! Error: %ERRORLEVEL%
exit /b %ERRORLEVEL%

