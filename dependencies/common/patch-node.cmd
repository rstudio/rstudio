@echo off

setlocal

call ..\tools\rstudio-tools.cmd

if "%NODE_VERSION%" == "18.19.1" (
	echo Applying patches to node %NODE_VERSION%

	:: remove npm from the node installation
	rd /s /q "%NODE_SUBDIR%\node_modules\npm"
	del "%NODE_SUBDIR%\npm"
	del "%NODE_SUBDIR%\npm.cmd"
	del "%NODE_SUBDIR%\npx"
	del "%NODE_SUBDIR%\npx.cmd"
)

exit /b 0
