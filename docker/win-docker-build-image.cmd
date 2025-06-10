@echo off
setlocal EnableDelayedExpansion

::
:: Script to build docker image for building RStudio Desktop for Windows
::

for %%A in (%*) do (
    if /I "%%A" == "clean" (
        set "NO_CACHE=--no-cache"
    )
)

set IMAGE=windows

REM move to the repo root (script's parent directory)
cd %~dp0\..
call dependencies\tools\rstudio-tools.cmd

REM Determine the repository name.
REM Used to differentiate 'rstudio' from 'rstudio-pro' when tagging the image.
for /F "delims=" %%i in ("%CD%") do set REPO=%%~nxi

REM Build the image.
%RUN% with-echo docker build ^
    --file docker\jenkins\Dockerfile.%IMAGE% ^
    --tag %REPO%:%IMAGE% ^
    --memory 16GB ^
    %NO_CACHE% ^
    --build-arg JENKINS_URL=development-build ^
    --build-arg GITHUB_LOGIN=%DOCKER_GITHUB_LOGIN% ^
    .

if %ERRORLEVEL% NEQ 0 (
    echo -- Error creating image %REPO%:%IMAGE% [error code %ERRORLEVEL%]
    exit /b 1
)

echo -- Created image %REPO%:%IMAGE%
