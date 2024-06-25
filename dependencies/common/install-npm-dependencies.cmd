@echo off

setlocal

call ..\tools\rstudio-tools.cmd
set PATH=%CD%\tools;%PATH%

:: the version of node.js that gets installed with the product
call :install-node %RSTUDIO_INSTALLED_NODE_VERSION% apply-patches

:: the version we use for building product components
call :install-node %RSTUDIO_NODE_VERSION%

:: install yarn for use during builds
set YARN_DIR=%NODE_SUBDIR%\node_modules\yarn\bin
if not exist %YARN_DIR%\yarn (
	echo "Installing yarn into '%YARN_DIR%'"
	call %NODE_SUBDIR%\npm install --global yarn
)
goto :EOF

:install-node :: version [apply-patches]

	set NODE_VERSION=%~1
	if "%~2"=="apply-patches" (
		set NODE_FOLDER=%NODE_VERSION%-patched
	) else (
		set NODE_FOLDER=%NODE_VERSION%
	)
	set NODE_ROOT=node
	set NODE_SUBDIR=%NODE_ROOT%\%NODE_FOLDER%
	set NODE_BASE_URL=%BASEURL%node/v%NODE_VERSION%/
	set NODE_ARCHIVE_DIR=node-v%NODE_VERSION%-win-x64
	set NODE_ARCHIVE_FILE=%NODE_ARCHIVE_DIR%.zip

	if "%~2"=="apply-patches" (
		call install-node.cmd reinstall
		call patch-node.cmd
	) else (
		call install-node.cmd
	)

goto :EOF :: end install-node

exit /b 0