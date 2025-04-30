@echo off
setlocal EnableExtensions EnableDelayedExpansion

::
:: Script to build RStudio Desktop for Windows using Docker
::
:: Requires a Windows machine, with Docker installed and set to use Windows
:: containers, and the RStudio repo cloned onto the machine. Then simply execute
:: this script from a command prompt in rstudio\docker directory.
::
:: For best reproduction of an official build use a pristine repo containing no
:: previous local builds or installed dependencies. For example you could use
:: git clean -ffdx.
::
:: Reason: The entire repo is copied into the container on each run (see comment
:: later in this script on why this is).
::

set IMAGE=windows
set FLAVOR=electron

REM make sure package directory exists
mkdir package 2> NUL

REM call the docker build image helper
cmd /C win-docker-build-image.cmd

REM move to the repo root (script's parent directory)
cd %~dp0\..
call dependencies\tools\rstudio-tools.cmd


REM determine repo name
for /F "delims=" %%i in ("%CD%") do set REPO=%%~nxi


REM set up build flags
set "CONTAINER_ID=build-%REPO%-%IMAGE%"
set "HOSTPATH=%CD:\=/%"
set "MAKEFLAGS=-j%NUMBER_OF_PROCESSORS%"


echo Ensuring a previously-running container is stopped / deleted...
%RUN% with-echo docker stop %CONTAINER_ID%
%RUN% with-echo docker rm %CONTAINER_ID%
echo.


echo Creating a new container...
%RUN% with-echo docker create ^
    --name %CONTAINER_ID% ^
    --volume %HOSTPATH%:C:/rstudio ^
    --memory 6GB ^
    --interactive ^
    %REPO%:%IMAGE% ^
    cmd.exe
echo.

echo Starting container...
%RUN% with-echo ^
    docker start %CONTAINER_ID%
echo.

echo Marking directory as safe for git...
%RUN% with-echo ^
    docker exec %CONTAINER_ID% cmd.exe /C ^
    "git config --global --add safe.directory C:/rstudio/"
echo.

echo Installing dependencies...
%RUN% with-echo ^
    docker exec %CONTAINER_ID% cmd.exe /C ^
    "cd C:\rstudio-tools\dependencies\windows && C:\rstudio\dependencies\windows\install-dependencies.cmd"
echo.

echo Building RStudio...
%RUN% with-echo ^
    docker exec %CONTAINER_ID% cmd.exe /C "cd C:\rstudio\package\win32 && make-package.bat clean"
echo.

echo Stopping container...
%RUN% with-echo docker stop %CONTAINER_ID%
echo.

if "%REPO%" == "rstudio-pro" (
    set PKG_FILENAME=RStudio-pro-99.9.9-RelWithDebInfo
) ELSE (
    set PKG_FILENAME=RStudio-99.9.9-RelWithDebInfo
)

echo Copying build result (%PKG_FILENAME%.zip) to %HOSTPATH%/docker/package
%RUN% with-echo
    docker cp %CONTAINER_ID%:/rstudio/package/win32/build/%PKG_FILENAME%.zip %HOSTPATH%/docker/package/%PKG_FILENAME%.zip

echo Copying build result (%PKG_FILENAME%.exe) to %HOSTPATH%/docker/package
%RUN% with-echo
    docker cp %CONTAINER_ID%:/rstudio/package/win32/build/%PKG_FILENAME%.exe %HOSTPATH%/docker/package/%PKG_FILENAME%.exe
