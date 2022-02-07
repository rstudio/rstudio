@echo off
set PACKAGE_DIR="%CD%"

setlocal

set BUILD_GWT=1
set RSTUDIO_TARGET=Desktop

if "%1" == "--help" goto :showhelp
if "%1" == "-h" goto :showhelp
if "%1" == "/?" goto :showhelp

for %%A in (%*) do (
      if /I "%%A" == "clean" set CLEANBUILD=1
      if /I "%%A" == "quick" set QUICK=1
      if /I "%%A" == "nozip" set NOZIP=1
      if /I "%%A" == "electron" set RSTUDIO_TARGET=Electron
      if /I "%%A" == "nogwt" set BUILD_GWT=0
)

REM clean if requested
if defined CLEANBUILD (
      call clean-build.bat
      if exist CMakeCache.txt del CMakeCache.txt
)

REM check for required programs on PATH
for %%F in (ant cmake) do (
      where /q %%F
      if ERRORLEVEL 1 (
            echo '%%F' not found on PATH; exiting
            endlocal
            exit /b 1
      )
)

REM Build for desktop
set GWT_MAIN_MODULE=RStudioDesktop

REM Remove system Rtools from PATH
call set PATH=%PATH:C:\Rtools\bin=%
call set PATH=%PATH:C:\Rtools\gcc-4.6.3\bin=%

REM Remove Git from PATH (otherwise cmake complains about 'sh.exe')
call set PATH=%PATH:C:\Program Files (x86)\Git\bin=%
call set PATH=%PATH:C:\Program Files\Git\bin=%

REM Establish build dir
set BUILD_DIR=build
if "%CMAKE_BUILD_TYPE%" == "" set CMAKE_BUILD_TYPE=RelWithDebInfo
if "%CMAKE_BUILD_TYPE%" == "Debug" set BUILD_DIR=build-debug

REM perform 64-bit build
cd "%PACKAGE_DIR%"

REM Select the appropriate NSIS template
if "%RSTUDIO_TARGET%" == "Electron" (
      copy cmake\modules\NSIS.template.in.electron cmake\modules\NSIS.template.in
) else (
      copy cmake\modules\NSIS.template.in.qt cmake\modules\NSIS.template.in
)

if not exist "%BUILD_DIR%" mkdir "%BUILD_DIR%"
cd "%BUILD_DIR%"

REM Package these files into a shorter path to workaround windows max path of 260
REM Must match CPACK_PACKAGE_DIRECTORY set in rstudio/package/win32/CMakeLists.txt
set PKG_TEMP_DIR=c:\temp\ide-build
if exist "%PKG_TEMP_DIR%/_CPack_Packages" rmdir /s /q "%PKG_TEMP_DIR%\_CPack_Packages"



REM Configure and build the project. (Note that Windows / MSVC builds require
REM that we specify the build type both at configure time and at build time)
set VS_TOOLS="%ProgramFiles(x86)%\Microsoft Visual Studio\2017\BuildTools\Common7\Tools"
if not exist %VS_TOOLS% set VS_TOOLS="%ProgramFiles(x86)%\Microsoft Visual Studio\2017\Community\Common7\Tools"
if not exist %VS_TOOLS% echo "Could not find VsDevCmd.bat. Please ensure Microsoft Visual Studio 2017 Build tools are installed." && exit /b 1

pushd %VS_TOOLS%
call VsDevCmd.bat -clean_env -no_logo || goto :error
call VsDevCmd.bat -arch=amd64 -startdir=none -host_arch=amd64 -winsdk=10.0.17134.0 -no_logo || goto :error
popd

cmake -G "Ninja" ^
      -DRSTUDIO_TARGET=%RSTUDIO_TARGET% ^
      -DCMAKE_BUILD_TYPE=%CMAKE_BUILD_TYPE% ^
      -DRSTUDIO_PACKAGE_BUILD=1 ^
      -DCMAKE_C_COMPILER=cl ^
      -DCMAKE_CXX_COMPILER=cl ^
      -DGWT_BUILD=%BUILD_GWT% ^
      ..\..\.. || goto :error
cmake --build . --config %CMAKE_BUILD_TYPE% -- %MAKEFLAGS% || goto :error

cd ..

REM perform 32-bit build and install it into the 64-bit tree
call make-install-win32.bat "%PACKAGE_DIR%\%BUILD_DIR%\src\cpp\session" %* || goto :error

REM create packages
cd "%BUILD_DIR%"
if not defined QUICK cpack -C "%CMAKE_BUILD_TYPE%" -G NSIS
if not defined NOZIP (
      if "%CMAKE_BUILD_TYPE%" == "RelWithDebInfo" cpack -C "%CMAKE_BUILD_TYPE%" -G ZIP
)
cd ..

move "%PKG_TEMP_DIR%\*.exe" "%PACKAGE_DIR%\%BUILD_DIR%"
move "%PKG_TEMP_DIR%\*.zip" "%PACKAGE_DIR%\%BUILD_DIR%"

REM emit NSIS error output if present
if not defined QUICK (
      if exist "%PKG_TEMP_DIR%\_CPack_Packages\win64\NSIS\NSISOutput.log" type "%PKG_TEMP_DIR%\_CPack_Packages\win64\NSIS\NSISOutput.log"
)

REM reset modified environment variables (PATH)
endlocal
goto :EOF

:error
echo Failed to build RStudio! Error: %ERRORLEVEL%
exit /b %ERRORLEVEL%

:showhelp
echo make-package [clean] [quick] [nozip] [electron] [nogwt]
echo     clean: full rebuild
echo     quick: skip creation of setup package
echo     nozip: skip creation of ZIP file
echo     electron: build Electron instead of Qt desktop (NYI)
echo     nogwt: use results of last GWT build
exit /b 0
