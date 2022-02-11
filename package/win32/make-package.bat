@echo off

setlocal
set PACKAGE_DIR="%CD%"
set ELECTRON_SOURCE_DIR=%PACKAGE_DIR%\..\..\src\node\desktop

call %PACKAGE_DIR%\..\..\dependencies\tools\rstudio-tools.cmd

set BUILD_GWT=1
set QUICK=
set NOZIP=
set CLEANBUILD=
set RSTUDIO_TARGET=Desktop

if "%1" == "--help" goto :showhelp
if "%1" == "-h" goto :showhelp
if "%1" == "/?" goto :showhelp

for %%A in (%*) do (
      if /I "%%A" == "clean" set CLEANBUILD=1
      if /I "%%A" == "quick" set QUICK=1
      if /I "%%A" == "nozip" set NOZIP=1
      if /I "%%A" == "electron" set RSTUDIO_TARGET=Electron
      if /I "%%A" == "desktop" set RSTUDIO_TARGET=Desktop
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

REM find node
set NODE_DIR=%PACKAGE_DIR%\..\..\dependencies\common\node\%RSTUDIO_NODE_VERSION%
set NODE=%NODE_DIR%\node.exe
if not exist %NODE% (
      echo node.exe not found at %NODE_DIR%; exiting
      endlocal
      exit /b 1
)
echo Using node: %NODE%

REM find yarn
set YARN=%NODE_DIR%\yarn
if not exist %YARN% (
      echo yarn not found at %NODE_DIR%; exiting
      endlocal
      exit /b 1
)
echo Using yarn: %YARN%

REM Build for desktop
set GWT_MAIN_MODULE=RStudioDesktop

REM Remove system Rtools from PATH
call set PATH=%PATH:C:\Rtools\bin=%
call set PATH=%PATH:C:\Rtools\gcc-4.6.3\bin=%

REM Remove Git from PATH (otherwise cmake complains about 'sh.exe')
call set PATH=%PATH:C:\Program Files (x86)\Git\bin=%
call set PATH=%PATH:C:\Program Files\Git\bin=%

REM Build RStudio version suffix
if not defined RSTUDIO_VERSION_MAJOR set RSTUDIO_VERSION_MAJOR=99
if not defined RSTUDIO_VERSION_MINOR set RSTUDIO_VERSION_MINOR=9
if not defined RSTUDIO_VERSION_PATCH set RSTUDIO_VERSION_PATCH=9
if not defined RSTUDIO_VERSION_SUFFIX set RSTUDIO_VERSION_SUFFIX=-dev+999
set RSTUDIO_VERSION_FULL=%RSTUDIO_VERSION_MAJOR%.%RSTUDIO_VERSION_MINOR%.%RSTUDIO_VERSION_PATCH%%RSTUDIO_VERSION_SUFFIX%

REM put version into package.json
call :set-version %RSTUDIO_VERSION_FULL%

REM Establish build dir
set BUILD_DIR=build
if "%CMAKE_BUILD_TYPE%" == "" set CMAKE_BUILD_TYPE=RelWithDebInfo
if "%CMAKE_BUILD_TYPE%" == "Debug" set BUILD_DIR=build-debug

REM perform 64-bit build
cd "%PACKAGE_DIR%"

REM Select the appropriate NSIS template
if "%RSTUDIO_TARGET%" == "Electron" (
      copy cmake\modules\NSIS.template.in.electron cmake\modules\NSIS.template.in >nul
) else (
      copy cmake\modules\NSIS.template.in.qt cmake\modules\NSIS.template.in >nul
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
if not defined QUICK (
      echo Creating NSIS setup package...
      cpack -C "%CMAKE_BUILD_TYPE%" -G NSIS
      REM emit NSIS error output if present
      if exist "%PKG_TEMP_DIR%\_CPack_Packages\win64\NSIS\NSISOutput.log" type "%PKG_TEMP_DIR%\_CPack_Packages\win64\NSIS\NSISOutput.log"
      move "%PKG_TEMP_DIR%\*.exe" "%PACKAGE_DIR%\%BUILD_DIR%"
)

if not defined NOZIP (
      if "%CMAKE_BUILD_TYPE%" == "RelWithDebInfo" (
            echo Creating ZIP package...
            cpack -C "%CMAKE_BUILD_TYPE%" -G ZIP
            move "%PKG_TEMP_DIR%\*.zip" "%PACKAGE_DIR%\%BUILD_DIR%"
      )
)
cd ..

REM reset modified environment variables (PATH)
call :restore-package-version
endlocal
goto :EOF

:error
echo Failed to build RStudio! Error: %ERRORLEVEL%
exit /b %ERRORLEVEL%

:showhelp
echo make-package [clean] [quick] [nozip] [electron] [desktop] [nogwt]
echo     clean: full rebuild
echo     quick: skip creation of setup package
echo     nozip: skip creation of ZIP file
echo     electron: build Electron instead of Qt desktop
echo     desktop: build Qt desktop (default)
echo     nogwt: use results of last GWT build
exit /b 0

:set-version
if "%RSTUDIO_TARGET%" == "Electron" (
      pushd %ELECTRON_SOURCE_DIR%
      call %YARN%
      call %YARN% json -I -f package.json -e "this.version=\"%~1\""
      popd
)
exit /b 0

:restore-package-version
call :set-version 99.9.9
exit /b 0

