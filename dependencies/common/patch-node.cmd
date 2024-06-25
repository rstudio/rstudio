@echo off

setlocal

call ..\tools\rstudio-tools.cmd

echo Applying patches to node %NODE_VERSION%

rd /s /q "%NODE_SUBDIR%\node_modules"
del "%NODE_SUBDIR%\corepack"
del "%NODE_SUBDIR%\corepack.cmd"
del "%NODE_SUBDIR%\install_tools.bat"
del "%NODE_SUBDIR%\npm"
del "%NODE_SUBDIR%\npm.cmd"
del "%NODE_SUBDIR%\npx"
del "%NODE_SUBDIR%\npx.cmd"
del "%NODE_SUBDIR%\CHANGELOG.md"
del "%NODE_SUBDIR%\README.md"
del "%NODE_SUBDIR%\nodevars.bat"

exit /b 0