@echo off
setlocal EnableDelayedExpansion

::
:: Script to build docker image for building RStudio Desktop for Windows
::

set IMAGE=windows

REM move to the repo root (script's parent directory)
cd %~dp0\..
call dependencies\tools\rstudio-tools.cmd

REM determine repo name
for /F "delims=" %%i in ("%CD%") do set REPO=%%~nxi

REM rebuild the image if necessary
%RUN% with-echo docker build ^
    --tag %REPO%:%IMAGE% ^
    --file docker\jenkins\Dockerfile.%IMAGE% ^
    --memory 16GB ^
    --build-arg JENKINS_URL=development-build ^
    --build-arg GITHUB_LOGIN=%DOCKER_GITHUB_LOGIN% ^
    .

echo -- Created image %REPO%:%IMAGE%
