@echo off

setlocal
set PACKAGE_DIR="%CD%"
set ELECTRON_SOURCE_DIR=%PACKAGE_DIR%\..\..\src\node\desktop

if not exist c:\rstudio-tools\dependencies (
      set RSTUDIO_DEPENDENCIES=%PACKAGE_DIR%\..\..\dependencies
) else (
      set RSTUDIO_DEPENDENCIES=c:\rstudio-tools\dependencies
)

call %RSTUDIO_DEPENDENCIES%\tools\rstudio-tools.cmd

set BUILD_GWT=1
set QUICK=
set NOZIP=
set CLEANBUILD=
set RSTUDIO_TARGET=Desktop
set PACKAGE_VERSION_SET=
set DEBUG_BUILD=

REM Produce multiarch builds by default on Jenkins.
if defined JENKINS_URL (
	set MULTIARCH=1
) else (
	set MULTIARCH=0
)

if "%1" == "--help" goto :showhelp
if "%1" == "-h" goto :showhelp
if "%1" == "help" goto :showhelp
if "%1" == "/?" goto :showhelp

SETLOCAL ENABLEDELAYEDEXPANSION
for %%A in (%*) do (
      set KNOWN_ARG=0
      if /I "%%A" == "clean" set CLEANBUILD=1 && set KNOWN_ARG=1
      if /I "%%A" == "debug" set DEBUG_BUILD=1 && set KNOWN_ARG=1
      if /I "%%A" == "desktop" set RSTUDIO_TARGET=Desktop && set KNOWN_ARG=1
      if /I "%%A" == "electron" set RSTUDIO_TARGET=Electron && set KNOWN_ARG=1
      if /I "%%A" == "multiarch" set MULTIARCH=1 && set KNOWN_ARG=1
      if /I "%%A" == "nogwt" set BUILD_GWT=0 && set KNOWN_ARG=1
      if /I "%%A" == "nozip" set NOZIP=1 && set KNOWN_ARG=1
      if /I "%%A" == "quick" set QUICK=1 && set KNOWN_ARG=1

      if "!KNOWN_ARG!" == "0" goto :showhelp
)

REM check for debug build
if defined DEBUG_BUILD (
      set CMAKE_BUILD_TYPE=Debug
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
set NODE_DIR=%RSTUDIO_DEPENDENCIES%\common\node\%RSTUDIO_NODE_VERSION%
set NODE=%NODE_DIR%\node.exe
if not exist %NODE% (
      echo node.exe not found at %NODE_DIR%; exiting
      endlocal
      exit /b 1
)
echo Using node: %NODE%

REM find npm
set NPM=%NODE_DIR%\npm
if not exist %NPM% (
      echo npm not found at %NODE_DIR%; exiting
      endlocal
      exit /b 1
)
echo Using npm: %NPM%

REM find npx
set NPX=%NODE_DIR%\npx
if not exist %NPX% (
      echo npx not found at %NODE_DIR%; exiting
      endlocal
      exit /b 1
)
echo Using npx: %NPX%

REM Put node on the path
set PATH=%NODE_DIR%;%PATH%

set REZH=%RSTUDIO_DEPENDENCIES%\windows\resource-hacker\ResourceHacker.exe
if not exist %REZH% (
      echo ResourceHacker.exe not found; re-run install-dependencies.cmd and try again; exiting
      endlocal
      exit /b 1
)

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

REM put version and product name into package.json
call :set-version %RSTUDIO_VERSION_FULL% RStudio

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
set VS_TOOLS="%ProgramFiles(x86)%\Microsoft Visual Studio\2019\BuildTools\Common7\Tools"
if not exist %VS_TOOLS% set VS_TOOLS="%ProgramFiles(x86)%\Microsoft Visual Studio\2019\Community\Common7\Tools"
if not exist %VS_TOOLS% echo "Could not find VsDevCmd.bat. Please ensure Microsoft Visual Studio 2019 Build tools are installed." && exit /b 1

pushd %VS_TOOLS%
call VsDevCmd.bat -clean_env -no_logo || goto :error
call VsDevCmd.bat -arch=amd64 -startdir=none -host_arch=amd64 -winsdk=10.0.19041.0 -no_logo || goto :error
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

REM add icon for .rproj file extension
if "%RSTUDIO_TARGET%" == "Electron" (
      pushd %ELECTRON_SOURCE_DIR%
      cd out\RStudio-win32-x64
      %REZH% -open rstudio.exe -save rstudio.exe.new -action add -resource ..\..\resources\icons\RProject.ico -mask ICONGROUP,2,1033
      del rstudio.exe
      rename rstudio.exe.new rstudio.exe
      popd
)
cd ..

REM perform 32-bit build and install it into the 64-bit tree
if "%MULTIARCH%" == "1" (
	call make-install-win32.bat "%PACKAGE_DIR%\%BUILD_DIR%\src\cpp\session" %* || goto :error
)

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
echo.
echo make-package [clean] [debug] [desktop] [electron] [multiarch] [nogwt] [nozip] [quick]
echo.
echo     clean:      perform full rebuild
echo     debug:      perform a debug build
echo     desktop:    build Qt desktop (default)
echo     electron:   build Electron instead of Qt desktop
echo     multiarch:  produce both 32-bit and 64-bit rsession executables
echo     nogwt:      skip GWT build (use previous GWT build)
echo     nozip:      skip creation of ZIP file
echo     quick:      skip creation of setup package
echo.
exit /b 0

REM For a full package build the package.json file gets modified with the 
REM desired build version and product name, and the build-info.ts source file 
REM gets modified with details on the build (date, git-commit, etc). We try to 
REM put these back to their original state at the end of the package build.
:set-version
if "%RSTUDIO_TARGET%" == "Electron" (
      pushd %ELECTRON_SOURCE_DIR%

      echo ensure msvs_version=2019
      call %NPM% set msvs_version 2019

      echo ensure node-gyp installed for node %RSTUDIO_NODE_VERSION%
      call %NPX% node-gyp install %RSTUDIO_NODE_VERSION%
      echo %LOCALAPPDATA%\node-gyp\Cache\%RSTUDIO_NODE_VERSION%\include\node
      dir %LOCALAPPDATA%\node-gyp\Cache\%RSTUDIO_NODE_VERSION%\include\node

      call %NPM% ci

      REM Set package.json info
      call :save-original-file package.json
      call %NPX% json -I -f package.json -e "this.version=\"%~1\""
      call %NPX% json -I -f package.json -e "this.productName=\"%~2\""

      REM Keep a backup of build-info.ts so we can restore it
      call :save-original-file src\main\build-info.ts

      set PACKAGE_VERSION_SET=1
      popd
)
exit /b 0

:restore-package-version
if defined PACKAGE_VERSION_SET (
      pushd %ELECTRON_SOURCE_DIR%
      call :restore-original-file package.json
      call :restore-original-file src\main\build-info.ts
      set PACKAGE_VERSION_SET=
      popd
)
exit /b 0

REM create a copy of a file in the same folder with .original extension
:save-original-file
copy %~1 %~1.original
exit /b 0

REM restore a file previously saved with save-original-file
:restore-original-file
if exist %~1.modified del %~1.modified
move %~1 %~1.modified
move %~1.original %~1
del %~1.modified
exit /b 0

