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
set ELECTRON_SRC_DIR=%PACKAGE_DIR%\..\..\src\node\desktop

REM perform 64-bit build
cd "%PACKAGE_DIR%"
if not exist "%BUILD_DIR%" mkdir "%BUILD_DIR%"
cd "%BUILD_DIR%"

REM Package these files into a shorter path to workaround windows max path of 260
REM Must match CPACK_PACKAGE_DIRECTORY set in rstudio/package/win32/CMakeLists.txt
set PKG_TEMP_DIR=c:\temp\ide-build
if exist "%PKG_TEMP_DIR%/_CPack_Packages" rmdir /s /q "%PKG_TEMP_DIR%\_CPack_Packages"
set INSTALL_DIR=%PKG_TEMP_DIR%\install

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
      -DCMAKE_INSTALL_PREFIX="%INSTALL_DIR%" ^
      -DCMAKE_C_COMPILER=cl ^
      -DCMAKE_CXX_COMPILER=cl ^
      -DGWT_BUILD=%BUILD_GWT% ^
      ..\..\.. || goto :error
cmake --build . --config %CMAKE_BUILD_TYPE% -- %MAKEFLAGS% || goto :error

cd ..

if "%RSTUDIO_TARGET%" == "Electron" (
      rmdir /s /q "%INSTALL_DIR%"
      mkdir "%INSTALL_DIR%"

      REM perform install of 64-bit build 
      cd "%PACKAGE_DIR%\%BUILD_DIR%"
      REM cmake --build . --target install -- %MAKEFLAGS% || goto :error

      REM perform 32-bit build and install it into the 64-bit tree
      cd "%PACKAGE_DIR%"
      call make-install-win32.bat "%INSTALL_DIR%\bin" %* || goto :error

      REM Build and package the Electron app.
      cd %ELECTRON_SRC_DIR%
      REM TODO versioning: ./scripts/update-json-version.sh "$RSTUDIO_VERSION_FULL"
      yarn package
      REM TODO Switch above to "yarn make" once implemented
) else (
      REM perform 32-bit build and install it into the 64-bit tree
      call make-install-win32.bat "%PACKAGE_DIR%\%BUILD_DIR%\src\cpp\session" %* || goto :error

      REM create packages
      cd "%BUILD_DIR%"
      if NOT defined QUICK cpack -C "%CMAKE_BUILD_TYPE%" -G NSIS
      if "%CMAKE_BUILD_TYPE%" == "RelWithDebInfo" cpack -C "%CMAKE_BUILD_TYPE%" -G ZIP
      cd ..

      echo "Before moving files in %PKG_TEMP_DIR%:"
      dir "%PKG_TEMP_DIR%"
      move "%PKG_TEMP_DIR%\*.exe" "%PACKAGE_DIR%\%BUILD_DIR%"
      move "%PKG_TEMP_DIR%\*.zip" "%PACKAGE_DIR%\%BUILD_DIR%"
      echo "After moving files to %PACKAGE_DIR%\%BUILD_DIR%:"
      dir "%PACKAGE_DIR%\%BUILD_DIR%"

      REM emit NSIS error output if present
      if exist "%PKG_TEMP_DIR%\_CPack_Packages\win64\NSIS\NSISOutput.log" type "%PKG_TEMP_DIR%\_CPack_Packages\win64\NSIS\NSISOutput.log"
)

REM reset modified environment variables (PATH)
endlocal
goto :EOF

:error
echo Failed to build RStudio! Error: %ERRORLEVEL%
exit /b %ERRORLEVEL%

:showhelp
echo make-package [clean] [quick] [electron] [nogwt]
exit /b 0