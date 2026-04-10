@echo off
setlocal EnableDelayedExpansion

echo About to delete all per-user RStudio state and settings, proceed with caution!
echo (Does not delete Project-specific state in .Rproj.user, global machine state, or .Renviron)
pause

set ERRORS=0
set CLEANED=0

REM --- Helper: remove a directory if it exists ---
REM   Uses a subroutine so each call reports what it's doing and whether it succeeded.
goto :start

:rmdir_if_exists
if not exist "%~1\" exit /b 0
echo Removing directory: %~1
rd /q /s "%~1" 2>nul
if !ERRORLEVEL! neq 0 (
    echo ERROR: Failed to remove "%~1" -- is something holding it open?
    echo        Try: handle.exe "%~1" ^(Sysinternals^) to find the locking process.
    set /a ERRORS+=1
) else (
    set /a CLEANED+=1
)
exit /b 0

:del_if_exists
if not exist "%~1" exit /b 0
echo Deleting file: %~1
del /f "%~1" 2>nul
if !ERRORLEVEL! neq 0 (
    echo ERROR: Failed to delete "%~1" -- is something holding it open?
    echo        Try: handle.exe "%~1" ^(Sysinternals^) to find the locking process.
    set /a ERRORS+=1
) else (
    set /a CLEANED+=1
)
exit /b 0

:start

call :rmdir_if_exists "%localappdata%\RStudio-Desktop"
call :rmdir_if_exists "%localappdata%\RStudio"
call :rmdir_if_exists "%appdata%\RStudio"
call :del_if_exists "%localappdata%\R\crash-handler-permission"
call :del_if_exists "%localappdata%\R\crash-handler.conf"
call :rmdir_if_exists "%appdata%\R\rsconnect"
call :rmdir_if_exists "%appdata%\R\config\R\rsconnect"
call :rmdir_if_exists "%USERPROFILE%\.positai"

for /f "tokens=*" %%i in ('powershell -NoProfile -Command "[System.Environment]::GetFolderPath([Environment+SpecialFolder]::MyDocuments)"') do set MyDocsDir=%%i

call :del_if_exists "%MyDocsDir%\.RData"
call :del_if_exists "%MyDocsDir%\.Rhistory"
call :rmdir_if_exists "%MyDocsDir%\.R\rstudio"

if exist "%MyDocsDir%\.Renviron" (
    echo Note: "%MyDocsDir%\.Renviron" found, not deleting
)

echo.
if %ERRORS% equ 0 (
    echo Done. Cleaned %CLEANED% item^(s^), no errors.
) else (
    echo Done. Cleaned %CLEANED% item^(s^), %ERRORS% FAILED.
    echo Re-run after closing RStudio and any R sessions.
    exit /b 1
)

endlocal
