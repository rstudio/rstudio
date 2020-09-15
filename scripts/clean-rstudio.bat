@echo off
echo About to delete all per-user RStudio state and settings, proceed with caution!
echo (Does not delete Project-specific state in .Rproj.user, global machine state, or .Renviron)
set /p=Press [enter] to continue or Ctrl+C

if EXIST "%localappdata%\RStudio-Desktop\" (
    rd /q /s "%localappdata%\RStudio-Desktop"
)
if EXIST "%localappdata%\RStudio\" (
    rd /q /s "%localappdata%\RStudio"
)
if EXIST "%appdata%\RStudio\" (
    rd /q /s "%appdata%\RStudio"
)
if EXIST "%localappdata%\R\crash-handler-permission" (
    del "%localappdata%\R\crash-handler-permission"
)
if EXIST "%localappdata%\R\crash-handler.conf" (
    del "%localappdata%\R\crash-handler.conf"
)

for /f "tokens=*" %%i in ('powershell /command "[System.Environment]::GetFolderPath([Environment+SpecialFolder]::MyDocuments)"') do set MyDocsDir=%%i
if EXIST "%MyDocsDir%\.RData" (
    del "%MyDocsDir%\.RData"
)
if EXIST "%MyDocsDir%\.Rhistory" (
    del "%MyDocsDir%\.Rhistory"
)
if EXIST "%MyDocsDir%\.R\rstudio\" (
    rd /q /s "%MyDocsDir%\.R\rstudio"
)
if EXIST "%MyDocsDir%\.Renviron" (
    echo Note: "%MyDocsDir%\.Renviron" found, not deleting
)
echo Done cleaning RStudio settings and state
