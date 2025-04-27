@echo off
setlocal enableextensions enabledelayedexpansion

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
set "MAKEFLAGS=-j%NUMBER_OF_PROCESSORS%"
for %%A in ("%cd%") do set HOSTPATH=%%~sA


REM remove previous image if it exists
%RUN% with-echo ^
    docker rm %CONTAINER_ID%

%RUN% with-echo ^
    docker create -m 6GB -i --name %CONTAINER_ID% %REPO%:%IMAGE% cmd.exe


:: Copy sources into the container; a volume mount doesn't work due to problems with the
:: MSVC toolchain used by RStudio: https://github.com/docker/for-win/issues/829
::
:: This issue is apparently fixed in latest MSVC 2019 so can reevaluate this approach when
:: we update to newer toolchain and use -v %HOSTPATH%:c:/src instead of copying repo.
::
:: A volume mount does work when using "--isolation process" but this mode of operation
:: requires a close Windows version match between the base image and the host operating
:: system (largely defeating the whole point of containerization).
::
:: https://docs.microsoft.com/en-us/virtualization/windowscontainers/deploy-containers/version-compatibility
::
echo Copying repo into container...
%RUN% with-echo ^
    docker cp %HOSTPATH% %CONTAINER_ID%:/src

echo Starting container...
%RUN% with-echo ^
    docker start %CONTAINER_ID%

echo Installing dependencies...
%RUN% with-echo ^
    docker exec %CONTAINER_ID% cmd.exe /C "cd \src\dependencies\windows && set RSTUDIO_SKIP_QT=1 && install-dependencies.cmd"

echo Building RStudio...
%RUN% with-echo ^
    docker exec %CONTAINER_ID% cmd.exe /C "cd \src\package\win32 && make-package.bat clean"

echo Stopping container...
%RUN% with-echo
    docker stop %CONTAINER_ID%

if "%REPO%" == "rstudio-pro" (
    set PKG_FILENAME=RStudio-pro-99.9.9-RelWithDebInfo
) ELSE (
    set PKG_FILENAME=RStudio-99.9.9-RelWithDebInfo
)

echo Copying build result (%PKG_FILENAME%.zip) to %HOSTPATH%/docker/package
%RUN% with-echo
    docker cp %CONTAINER_ID%:/src/package/win32/build/%PKG_FILENAME%.zip %HOSTPATH%/docker/package/%PKG_FILENAME%.zip

echo Copying build result (%PKG_FILENAME%.exe) to %HOSTPATH%/docker/package
%RUN% with-echo
    docker cp %CONTAINER_ID%:/src/package/win32/build/%PKG_FILENAME%.exe %HOSTPATH%/docker/package/%PKG_FILENAME%.exe
