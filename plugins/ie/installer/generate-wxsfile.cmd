@echo OFF
REM harvest COM settings from COM binary
setlocal
echo.
if .%1==. goto :usage
echo IMPORTANT: Make sure "%~dp0oophm.wsx" is checked out and writable!
echo.
echo 'heating' binary %~nx1 under %~dp1 ...
echo ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
pushd %~dp1
%~dp0wix\heat.exe file .\%~nx1 -v -nologo -gg -g1 -dr INSTALLDIR -generate components -directoryid ff -cg oophmDll -out %~dp0oophm.wxs -var var.binDir
popd
%~dp0wix\sed.exe --in-place=xml "s/Source=\"\$(var\.binDir)\\Win32\\oophm.dll\"/Source=\"\$(var\.binDir)\\oophm.dll\"/g" %~dp0oophm.wxs
%~dp0wix\sed.exe --in-place=xml "s/Directory Id=\"dir315E0C50682DFB472927FE1254A22F6A\" Name=\"Win32\"/Directory Id=\"dir315E0C50682DFB472927FE1254A22F6A\" Name=\"$(var.platform)\"/g" %~dp0oophm.wxs
%~dp0wix\sed.exe --in-place=xml "s/<Component /<Component Win64=\"$(var.win64Flag)\" /g" %~dp0oophm.wxs
%~dp0wix\sed.exe --in-place=xml "s/<Wix xmlns=\"http:\/\/schemas.microsoft.com\/wix\/2006\/wi\">/<Wix xmlns=\"http:\/\/schemas.microsoft.com\/wix\/2006\/wi\">\n\t<\?if $(var.platform)=x64 \?>\n\t<\?define win64Flag=\"yes\" \?>\n\t<\?else \?>\n\t<\?define win64Flag=\"no\" \?>\n\t<\?endif \?>/g" %~dp0oophm.wxs
%~dp0wix\sed.exe --in-place=xml "s/Root=\"HKCR\"/Root=\"HKMU\"/g" %~dp0oophm.wxs

echo ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
echo Done.
echo.
goto :eof
:usage
echo.
echo   usage: generate-wsxfile.cmd ^<binary-file-path^>
echo.
echo        example: generate-wsxfile.cmd ..\prebuilt\Win32\oophm.dll
echo.
endlocal

