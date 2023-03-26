@echo off

REM Run this script with 'call envvars.bat' to update the PATH in your
REM cmd.exe shell, so that the appropriate versions of node are found
set NODE_PATHS=^
	..\..\..\dependencies\common\node\16.14.0^
	c:\rstudio-tools\dependencies\common\node\16.14.0

call :SetNodePath
if not exist "%NODE_PATH%" (
    echo "ERROR: node installation not found"
    exit /b
)

echo Using node: %NODE_PATH%
set PATH=%NODE_PATH%;%PATH%

call :SetVar NODE_VERSION node --version
echo node: %NODE_VERSION%

call :SetVar NPM_VERSION npm --version
echo npm: %NPM_VERSION%

exit /b

:SetVar
    for /f "tokens=* delims=" %%A in ('%2 %3 %4 %5 %6') do set "%1=%%A"
    exit /b

:SetNodePath
    for %%a in (%NODE_PATHS%) do (
		if exist %%a (
			set NODE_PATH=%%~fa
		)
    )

    exit /b
