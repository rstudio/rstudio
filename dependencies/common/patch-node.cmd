@echo off

setlocal

call ..\tools\rstudio-tools.cmd

if "%NODE_VERSION%" == "%RSTUDIO_INSTALLED_NODE_VERSION%" (
	echo Applying patches to node %NODE_VERSION%

	:: remove npm from the node installation
	rd /s /q "%NODE_SUBDIR%\node_modules\npm"
	del "%NODE_SUBDIR%\node_modules\corepack\shims\nodewin\npm"
	del "%NODE_SUBDIR%\node_modules\corepack\shims\nodewin\npm.cmd"
	del "%NODE_SUBDIR%\node_modules\corepack\shims\nodewin\npm.ps1"
	del "%NODE_SUBDIR%\node_modules\corepack\shims\nodewin\npx"
	del "%NODE_SUBDIR%\node_modules\corepack\shims\nodewin\npx.cmd"
	del "%NODE_SUBDIR%\node_modules\corepack\shims\nodewin\npx.ps1"
	del "%NODE_SUBDIR%\node_modules\corepack\shims\npm"
	del "%NODE_SUBDIR%\node_modules\corepack\shims\npm.cmd"
	del "%NODE_SUBDIR%\node_modules\corepack\shims\npm.ps1"
	del "%NODE_SUBDIR%\node_modules\corepack\shims\npx"
	del "%NODE_SUBDIR%\node_modules\corepack\shims\npx.cmd"
	del "%NODE_SUBDIR%\node_modules\corepack\shims\npx.ps1"
	del "%NODE_SUBDIR%\npm"
	del "%NODE_SUBDIR%\npm.cmd"
	del "%NODE_SUBDIR%\npx"
	del "%NODE_SUBDIR%\npx.cmd"
)

exit /b 0
