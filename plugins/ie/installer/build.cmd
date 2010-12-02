@echo off
setlocal
if .%1==. goto :usage
echo.
echo.

echo Checking if output files are writeable...
SET WRITEABLE_FILE_ERROR=0
CALL :CheckWritable "%~dp0..\prebuilt\gwt-dev-plugin-x86.msi"
CALL :CheckWritable "%~dp0..\prebuilt\gwt-dev-plugin-x64.msi"
CALL :CheckWritable "%~dp0oophm.wxs"
IF "%WRITEABLE_FILE_ERROR%"=="1" GOTO :DONE

SET BINARY_DIR=%~dp0..\prebuilt\Win32
SET BINARY_FILE=oophm.dll

IF NOT EXIST %BINARY_DIR%\%BINARY_FILE% (
    echo.
	echo ERROR - Could not find oophm binary under %BINARY_DIR%
	echo         Verify that the build succeeded before trying to create the installer.
	echo.
	goto :eof
)

echo.
echo 'heating' binary %BINARY_FILE% under %BINARY_DIR% ...
echo ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
%~dp0wix\heat.exe file %BINARY_DIR%\%BINARY_FILE% -v -nologo -gg -g1 -dr INSTALLDIR -generate components -directoryid ff -cg oophmDll -out %~dp0oophm.wxs -var var.binDir
%~dp0wix\sed.exe --in-place=xml "s/Source=\"\$(var\.binDir)\\Win32\\oophm.dll\"/Source=\"\$(var\.binDir)\\oophm.dll\"/g" %~dp0oophm.wxs
%~dp0wix\sed.exe --in-place=xml "s/Directory Id=\"dir315E0C50682DFB472927FE1254A22F6A\" Name=\"Win32\"/Directory Id=\"dir315E0C50682DFB472927FE1254A22F6A\" Name=\"$(var.platform)\"/g" %~dp0oophm.wxs
%~dp0wix\sed.exe --in-place=xml "s/<Component /<Component Win64=\"$(var.win64Flag)\" /g" %~dp0oophm.wxs
%~dp0wix\sed.exe --in-place=xml "s/<Wix xmlns=\"http:\/\/schemas.microsoft.com\/wix\/2006\/wi\">/<Wix xmlns=\"http:\/\/schemas.microsoft.com\/wix\/2006\/wi\">\n\t<\?if $(var.platform)=x64 \?>\n\t<\?define win64Flag=\"yes\" \?>\n\t<\?else \?>\n\t<\?define win64Flag=\"no\" \?>\n\t<\?endif \?>/g" %~dp0oophm.wxs
%~dp0wix\sed.exe --in-place=xml "s/Root=\"HKCR\"/Root=\"HKMU\"/g" %~dp0oophm.wxs

echo.
echo building 32 bit installer...
echo ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
echo.
%~dp0wix\candle -nologo -arch x86 -dversion=%1 -dplatform=Win32 -dbinDir=%~dp0\..\prebuilt\Win32\ -dbinary=oophm.dll %~dp0\installer.wxs.xml %~dp0\oophm.wxs
%~dp0wix\light.exe -nologo oophm.wixobj installer.wxs.wixobj -o ..\prebuilt\gwt-dev-plugin-x86.msi -spdb

echo.
echo building 64 bit installer...
echo ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
echo.
%~dp0wix\candle -nologo -arch x64 -dversion=%1 -dplatform=x64 -dbinDir=%~dp0\..\prebuilt\x64\  -dbinary=oophm.dll %~dp0\installer.wxs.xml %~dp0\oophm.wxs
%~dp0wix\light.exe -nologo oophm.wixobj installer.wxs.wixobj -o ..\prebuilt\gwt-dev-plugin-x64.msi -spdb

echo.
echo Done.
echo.
goto :eof

:CheckWritable
ECHO Checking if %1 is writable...
SET RW=
DIR /A:-R %1 1>NUL 2>NUL
if %ERRORLEVEL% LEQ 0 GOTO :EOF
ECHO.
ECHO     ERROR: file %1 must be writeable before executing this script.
ECHO            make sure you checked the file for editing.
ECHO.
SET WRITEABLE_FILE_ERROR=1
ECHO %WRITEABLE_FILE_ERROR%
GOTO :eof

:usage
echo.
echo   usage: build ^<version^> 
echo      where: ^<version^> has the syntax major.minor.build 
echo.
echo        example: build 1.1.3123

:done
echo.
echo.

endlocal

