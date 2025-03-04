
@echo off

for %%F in ("%~dp0\..\..\dependencies\tools\rstudio-tools.cmd") do (
  set "RSTUDIO_TOOLS=%%~fF"
)

call %RSTUDIO_TOOLS%

%RUN% add-first-to-path ^
	%RSTUDIO_PROJECT_ROOT%\dependencies\common\node\%RSTUDIO_NODE_VERSION% ^
	c:\rstudio-tools\dependencies\common\node\%RSTUDIO_NODE_VERSION%

%RUN% subprocess "node --version" NODE_VERSION
echo node: %NODE_VERSION%

%RUN% subprocess "npm --version" NPM_VERSION
echo npm: %NPM_VERSION%

goto :eof
