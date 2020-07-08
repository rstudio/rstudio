@echo off
set PACKAGE_DIR="%CD%"

REM clean if requested
if "%1" == "clean" (
      call clean-build.bat
      if exist CMakeCache.txt del CMakeCache.txt
)

REM check for required programs on PATH
for %%F in (ant cmake) do (
      where /q %%F
      if ERRORLEVEL 1 (
            echo '%%F' not found on PATH; exiting
            exit /b 1
      )
)

setlocal

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
if not exist "%BUILD_DIR%" mkdir "%BUILD_DIR%"
cd "%BUILD_DIR%"
if exist "%BUILD_DIR%/_CPack_Packages" rmdir /s /q "%BUILD_DIR%\_CPack_Packages"

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
      -DRSTUDIO_TARGET=Desktop ^
      -DCMAKE_BUILD_TYPE=%CMAKE_BUILD_TYPE% ^
      -DRSTUDIO_PACKAGE_BUILD=1 ^
      -DCMAKE_C_COMPILER=cl ^
      -DCMAKE_CXX_COMPILER=cl ^
      ..\..\.. || goto :error
cmake --build . --config %CMAKE_BUILD_TYPE% -- %MAKEFLAGS% || goto :error
cd ..

REM perform 32-bit build and install it into the 64-bit tree
call make-install-win32.bat "%PACKAGE_DIR%\%BUILD_DIR%\src\cpp\session" %1 || goto :error

REM create packages
cd "%BUILD_DIR%"
if not "%1" == "quick" cpack -C "%CMAKE_BUILD_TYPE%" -G NSIS
if "%CMAKE_BUILD_TYPE%" == "RelWithDebInfo" cpack -C "%CMAKE_BUILD_TYPE%" -G ZIP
cd ..

REM reset modified environment variables (PATH)
endlocal
goto :EOF

:error
echo Failed to build RStudio! Error: %ERRORLEVEL%
exit /b %ERRORLEVEL%
