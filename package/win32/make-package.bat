::
:: make-packages.bat
::
:: Copyright (C) 2022 by Posit Software, PBC
::
:: Unless you have received this program directly from Posit Software pursuant
:: to the terms of a commercial license agreement with Posit Software, then
:: this program is licensed to you under the terms of version 3 of the
:: GNU Affero General Public License. This program is distributed WITHOUT
:: ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
:: MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
:: AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
::
@echo off

setlocal EnableDelayedExpansion
set "PACKAGE_DIR=%CD%"

if not exist c:\rstudio-tools\dependencies (
    set RSTUDIO_DEPENDENCIES=%PACKAGE_DIR%\..\..\dependencies
) else (
    set RSTUDIO_DEPENDENCIES=c:\rstudio-tools\dependencies
)

call %RSTUDIO_DEPENDENCIES%\tools\rstudio-tools.cmd

%RUN% normalize-path "%PACKAGE_DIR%\..\..\src\node\desktop" ELECTRON_SOURCE_DIR
%RUN% normalize-path "%RSTUDIO_DEPENDENCIES%" RSTUDIO_DEPENDENCIES

set BUILD_GWT=1
set QUICK=
set NOZIP=
set CLEANBUILD=
set RSTUDIO_TARGET=Electron
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

setlocal EnableDelayedExpansion
for %%A in (%*) do (
    set KNOWN_ARG=0
    if /I "%%A" == "clean" (
        set CLEANBUILD=1
        set KNOWN_ARG=1
    )
    if /I "%%A" == "debug" (
        set DEBUG_BUILD=1
        set KNOWN_ARG=1
    )
    if /I "%%A" == "electron" (
        set RSTUDIO_TARGET=Electron
        set KNOWN_ARG=1
    )
    if /I "%%A" == "multiarch" (
        set MULTIARCH=1
        set KNOWN_ARG=1
    )
    if /I "%%A" == "nogwt" (
        set BUILD_GWT=0
        set KNOWN_ARG=1
    )
    if /I "%%A" == "nozip" (
        set NOZIP=1
        set KNOWN_ARG=1
    )
    if /I "%%A" == "quick" (
        set QUICK=1
        set KNOWN_ARG=1
    )
    if "!KNOWN_ARG!" == "0" goto :showhelp
)

REM Check for debug build.
if defined DEBUG_BUILD (
    set CMAKE_BUILD_TYPE=Debug
)

REM Clean if requested.
if defined CLEANBUILD (
    call clean-build.bat
    if exist CMakeCache.txt del CMakeCache.txt
)

REM Check for required programs on the PATH.
for %%F in (ant cmake) do (
    where /q %%F
    if ERRORLEVEL 1 (
        echo '%%F' not found on PATH; exiting
        endlocal
        exit /b 1
    )
)

REM Find node tools.
set NODE_DIR=%RSTUDIO_DEPENDENCIES%\common\node\%RSTUDIO_NODE_VERSION%
set NODE=%NODE_DIR%\node.exe
if not exist %NODE% (
    echo node.exe not found at %NODE_DIR%; exiting
    endlocal
    exit /b 1
)
echo Using node: %NODE%

set NPM=%NODE_DIR%\npm
if not exist %NPM% (
    echo npm not found at %NODE_DIR%; exiting
    endlocal
    exit /b 1
)
echo Using npm: %NPM%

set NPX=%NODE_DIR%\npx
if not exist %NPX% (
    echo npx not found at %NODE_DIR%; exiting
    endlocal
    exit /b 1
)
echo Using npx: %NPX%

REM Put node on the path
set PATH=%NODE_DIR%;%PATH%

REM Set up resource-hacker utility.
set REZH=%RSTUDIO_DEPENDENCIES%\windows\resource-hacker\ResourceHacker.exe
if not exist %REZH% (
    echo ResourceHacker.exe not found; re-run install-dependencies.cmd and try again; exiting
    endlocal
    exit /b 1
)

REM Build GWT desktop module.
set GWT_MAIN_MODULE=RStudioDesktop

REM Remove system Rtools from PATH (can confuse CMake and other tools)
call set PATH=%PATH:C:\Rtools\bin=%
call set PATH=%PATH:C:\Rtools\gcc-4.6.3\bin=%

REM Remove Git from PATH (otherwise CMake complains about 'sh.exe')
call set PATH=%PATH:C:\Program Files (x86)\Git\bin=%
call set PATH=%PATH:C:\Program Files\Git\bin=%

REM Build RStudio version suffix.
if not defined RSTUDIO_VERSION_MAJOR set RSTUDIO_VERSION_MAJOR=99
if not defined RSTUDIO_VERSION_MINOR set RSTUDIO_VERSION_MINOR=9
if not defined RSTUDIO_VERSION_PATCH set RSTUDIO_VERSION_PATCH=9
if not defined RSTUDIO_VERSION_SUFFIX set RSTUDIO_VERSION_SUFFIX=-dev+999
set RSTUDIO_VERSION_FULL=%RSTUDIO_VERSION_MAJOR%.%RSTUDIO_VERSION_MINOR%.%RSTUDIO_VERSION_PATCH%%RSTUDIO_VERSION_SUFFIX%

REM Set default CMake build type.
if "%CMAKE_BUILD_TYPE%" == "" set CMAKE_BUILD_TYPE=RelWithDebInfo

REM Work in package directory.
cd "%PACKAGE_DIR%"

REM Compute build directory.
if not defined BUILD_DIR (
    if "%CMAKE_BUILD_TYPE%" == "Debug" (
        set BUILD_DIR=build-debug
    ) else (
        set BUILD_DIR=build
    )
)

REM Enter build directory.
if not exist "%BUILD_DIR%" mkdir "%BUILD_DIR%"
pushd "%BUILD_DIR%"

REM Package these files into a shorter path to workaround windows max path of 260
REM Must match CPACK_PACKAGE_DIRECTORY set in rstudio/package/win32/CMakeLists.txt
set PKG_TEMP_DIR=C:\rsbuild
if exist "%PKG_TEMP_DIR%/_CPack_Packages" rmdir /s /q "%PKG_TEMP_DIR%\_CPack_Packages"

REM Configure the main build.
call vcvarsall.bat amd64
cmake -G Ninja ^
    -DCMAKE_BUILD_TYPE=%CMAKE_BUILD_TYPE% ^
    -DCMAKE_C_COMPILER=cl.exe ^
    -DCMAKE_CXX_COMPILER=cl.exe ^
    -DRSTUDIO_TARGET=%RSTUDIO_TARGET% ^
    -DRSTUDIO_PACKAGE_BUILD=1 ^
    -DLIBR_HOME=C:\R\R-3.6.3 ^
    -DGWT_BUILD=%BUILD_GWT% ^
    %RSTUDIO_PROJECT_ROOT% || (
        echo.!! ERROR: configure failed
        exit /b 1
    )

REM Perform the build.
cmake --build . --config %CMAKE_BUILD_TYPE% -- %MAKEFLAGS% || (
    echo.!! ERROR: build failed
    exit /b 1
)

REM Add icons for supported file types.
if "%RSTUDIO_TARGET%" == "Electron" (
    pushd %ELECTRON_SOURCE_DIR%
    cd out\RStudio-win32-x64
    %REZH% -open rstudio.exe -save rstudio.exe -action addoverwrite -resource ..\..\resources\icons\RProject.ico -mask ICONGROUP,2,1033
    %REZH% -open rstudio.exe -save rstudio.exe -action addoverwrite -resource ..\..\resources\icons\RSource.ico -mask ICONGROUP,3,1033
    %REZH% -open rstudio.exe -save rstudio.exe -action addoverwrite -resource ..\..\resources\icons\CSS.ico -mask ICONGROUP,4,1033
    %REZH% -open rstudio.exe -save rstudio.exe -action addoverwrite -resource ..\..\resources\icons\HTML.ico -mask ICONGROUP,5,1033
    %REZH% -open rstudio.exe -save rstudio.exe -action addoverwrite -resource ..\..\resources\icons\JS.ico -mask ICONGROUP,6,1033
    %REZH% -open rstudio.exe -save rstudio.exe -action addoverwrite -resource ..\..\resources\icons\Markdown.ico -mask ICONGROUP,7,1033
    %REZH% -open rstudio.exe -save rstudio.exe -action addoverwrite -resource ..\..\resources\icons\QuartoMarkdown.ico -mask ICONGROUP,8,1033
    %REZH% -open rstudio.exe -save rstudio.exe -action addoverwrite -resource ..\..\resources\icons\RData.ico -mask ICONGROUP,9,1033
    %REZH% -open rstudio.exe -save rstudio.exe -action addoverwrite -resource ..\..\resources\icons\RDoc.ico -mask ICONGROUP,10,1033
    %REZH% -open rstudio.exe -save rstudio.exe -action addoverwrite -resource ..\..\resources\icons\RHTML.ico -mask ICONGROUP,11,1033
    %REZH% -open rstudio.exe -save rstudio.exe -action addoverwrite -resource ..\..\resources\icons\RMarkdown.ico -mask ICONGROUP,12,1033
    %REZH% -open rstudio.exe -save rstudio.exe -action addoverwrite -resource ..\..\resources\icons\RPresentation.ico -mask ICONGROUP,13,1033
    %REZH% -open rstudio.exe -save rstudio.exe -action addoverwrite -resource ..\..\resources\icons\RSweave.ico -mask ICONGROUP,14,1033
    %REZH% -open rstudio.exe -save rstudio.exe -action addoverwrite -resource ..\..\resources\icons\RTex.ico -mask ICONGROUP,15,1033
    popd
)

REM Return to package directory.
popd

REM Perform 32-bit build and install it into the 64-bit tree.
if "%MULTIARCH%" == "1" (
    call make-install-win32.bat "%PACKAGE_DIR%\%BUILD_DIR%\src\cpp\session" %* || (
        echo.!! ERROR: 32-bit session components build failed
        exit /b 1
    )
)

REM Create packages for dev build.
REM The official build invokes this from Jenkinsfile after signing binaries.
if not defined JENKINS_URL (
    call make-dist-packages.bat || (
        echo.!! ERROR: package creation failed
        exit /b 1
    )
)

echo RStudio was built successfully.
goto :eof


:showhelp
echo.
echo make-package [clean] [debug] [electron] [multiarch] [nogwt] [nozip] [quick]
echo.
echo     clean:      perform full rebuild
echo     debug:      perform a debug build
echo     electron:   build Electron desktop (default)
echo     multiarch:  produce both 32-bit and 64-bit rsession executables
echo     nogwt:      skip GWT build (use previous GWT build)
echo     nozip:      skip creation of ZIP file
echo     quick:      skip creation of setup package
echo.
echo     Environment variables specify the product's build version (default is 99.9.9).
echo.
echo     Example:
echo.
echo     set RSTUDIO_VERSION_MAJOR=2023
echo     set RSTUDIO_VERSION_MINOR=8
echo     set RSTUDIO_VERSION_PATCH=1
echo     set RSTUDIO_VERSION_SUFFIX=-daily+321
echo     make-package clean electron
echo.
exit /b 0
