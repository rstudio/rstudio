@echo off

REM Run this script with 'call envvars.bat' to update the PATH in your
REM cmd.exe shell, so that the appropriate versions of node are found
set node_paths="..\..\..\dependencies\common\node\16.14.0" "c:\rstudio-tools\dependencies\common\node\16.14.0"
call :SetNodePath
if not exist "%NODE_PATH%" (
    echo "Error: node path not found"
    exit /b
)

set PATH=%NODE_PATH%;%PATH%

call :SetVar NODE_VERSION node --version
echo Using node: %NODE_VERSION%

call :SetVar NPM_VERSION npm --version
echo Using npm: %NPM_VERSION%

exit /b

:NormalizePath
    set "%1=%~f2"
    exit /b

:SetVar
    for /f "tokens=* delims=" %%A in ('%2 %3 %4 %5 %6') do set "%1=%%A"
    exit /b

:SetNodePath
    for %%a in (%node_paths%) do (
        call :NormalizePath %%a %%a
        if not exist "%%a" (
            echo %%a does not exist
        ) else (
            echo Using %%a
            set NODE_PATH="%%a"
        )
    )

    exit /b
