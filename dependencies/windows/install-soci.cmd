@echo off
setlocal

REM Run from the script directory; install-soci.R places its build artifacts
REM under RSTUDIO_TOOLS_ROOT when that is set in the environment.
pushd "%~dp0install-soci"
R --vanilla -s -f install-soci.R
if ERRORLEVEL 1 (
  echo !! ERROR: SOCI build failed.
  popd
  exit /b 1
)
popd
