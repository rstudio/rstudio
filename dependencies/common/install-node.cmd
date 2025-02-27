
@echo off
setlocal EnableDelayedExpansion

for %%F in ("%CD%\..\tools\rstudio-tools.cmd") do (
  set "RSTUDIO_TOOLS=%%~fF"
)

call %RSTUDIO_TOOLS%
set PATH=%CD%\tools;%PATH%

set ACTION=%~1

if not defined NODE_BASE_URL (
	echo ERROR: NODE_BASE_URL is not defined.
	exit /b 1
)

if exist "%NODE_SUBDIR%" (
	if "%ACTION%"=="reinstall" (
		echo removing previous node %NODE_VERSION% from '%NODE_SUBDIR%'
		rd /s /q "%NODE_SUBDIR%"
	) else (
		echo node %NODE_VERSION% is already installed at '%NODE_SUBDIR%'
		exit /b 0
	)
)

set _URL=%NODE_BASE_URL%
%RUN% download
curl -L -f -C - -O %NODE_BASE_URL%%NODE_ARCHIVE_FILE%
echo Unzipping node %NODE_VERSION%
if not exist "%NODE_ROOT%" mkdir "%NODE_ROOT%"
unzip %UNZIP_ARGS% %NODE_ARCHIVE_FILE%
move %NODE_ARCHIVE_DIR% %NODE_SUBDIR%


exit /b 0
