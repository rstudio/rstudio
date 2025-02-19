@if not defined _echo echo off
setlocal enabledelayedexpansion

:: Helper script used when installing Visual C++ build tools into docker image.
:: See: https://learn.microsoft.com/en-us/visualstudio/install/advanced-build-tools-container

call %*
if "%ERRORLEVEL%"=="3010" (
    exit /b 0
) else (
    if not "%ERRORLEVEL%"=="0" (
        set ERR=%ERRORLEVEL%
        call dependencies\windows\vscollect.exe -zip:C:\vslogs.zip

        exit /b !ERR!
    )
)

exit /b 0
