@echo off

:: Configure current shell for configuring and building via cmake on Windows.
set VS_TOOLS="%ProgramFiles(x86)%\Microsoft Visual Studio\2019\BuildTools\Common7\Tools"
if not exist %VS_TOOLS% set VS_TOOLS="%ProgramFiles(x86)%\Microsoft Visual Studio\2019\Community\Common7\Tools"
if not exist %VS_TOOLS% echo "Could not find VsDevCmd.bat. Please ensure Microsoft Visual Studio 2019 Build tools are installed." && exit /b 1

pushd %VS_TOOLS%
call VsDevCmd.bat -clean_env -no_logo || goto :error
call VsDevCmd.bat -arch=amd64 -startdir=none -host_arch=amd64 -winsdk=10.0.19041.0 -no_logo || goto :error
popd

echo Microsoft toolchain configured, can now use cmake. For example:
echo mkdir build
echo cd build
echo cmake ..\cpp -GNinja
echo ninja