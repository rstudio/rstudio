@echo off

call %~dp0\..\..\..\dependencies\tools\rstudio-tools.cmd


pushd "%_VCTOOLSDIR%"
call VsDevCmd.bat -clean_env -no_logo || goto :error
call VsDevCmd.bat -arch=amd64 -startdir=none -host_arch=amd64 -winsdk=%WIN32_SDK_VERSION% -no_logo || goto :error
popd


echo Microsoft toolchain configured, can now use cmake. For example:
echo mkdir rstudio\src\build
echo cd rstudio\src\build
echo cmake ..\cpp -GNinja
echo ninja
