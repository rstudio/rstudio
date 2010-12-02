@echo off
setlocal
echo.
::check if correct folder
::check if devenv is in the path
echo.
echo building 32 bits
echo ~~~~~~~~~~~~~~~~
::pushd oophm\oophm
devenv /rebuild "Release|Win32" %~dp0\oophm\oophm\oophm.vcproj /out build_win32.log
echo.
echo building 64 bits
echo ~~~~~~~~~~~~~~~~
devenv /rebuild "Release|x64" %~dp0\oophm\oophm\oophm.vcproj /out build_win64.log
echo.
echo Done.
echo.
endlocal
