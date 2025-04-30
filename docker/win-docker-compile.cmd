@echo off
setlocal EnableDelayedExpansion

::
:: Script to build RStudio Desktop for Windows using Docker
::
:: Requires a Windows machine, with Docker installed and set to use Windows
:: containers, and the RStudio repo cloned onto the machine. Then simply execute
:: this script from a command prompt in rstudio\docker directory.
::
:: This script will mount the RStudio project directory (from the host) into
:: the container for building, but the build will happen on the container's
:: local filesystem.
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

REM figure out how many CPUs we want to use
set /A "CPUS=%NUMBER_OF_PROCESSORS%/2"
if %CPUS% gtr 8 set "CPUS=8"

REM set up build flags
set "CONTAINER_ID=build-%REPO%-%IMAGE%"
set "HOSTPATH=%CD:\=/%"
set "MAKEFLAGS=-j%NUMBER_OF_PROCESSORS%"

set "PROMPT=> "
@echo on

docker stop %CONTAINER_ID%
docker rm %CONTAINER_ID%

docker create ^
    --name %CONTAINER_ID% ^
    --volume %HOSTPATH%:C:/rstudio:rw ^
    --cpu-count %CPUS% ^
    --memory 16GB ^
    --interactive ^
    %REPO%:%IMAGE% ^
    cmd.exe

docker start %CONTAINER_ID%

docker exec %CONTAINER_ID% cmd.exe /C ^
    "git config --global --add safe.directory C:/rstudio/"

docker exec %CONTAINER_ID% cmd.exe /C ^
    "cd C:\rstudio-tools\dependencies\windows && C:\rstudio\dependencies\windows\install-dependencies.cmd"

docker exec %CONTAINER_ID% cmd.exe /C setx /M BUILD_DIR C:/build
docker exec %CONTAINER_ID% cmd.exe /C setx /M WIN32_BUILD_PATH C:/build32

docker exec %CONTAINER_ID% cmd.exe /C ^
    "cd C:\rstudio\package\win32 && make-package.bat clean"

docker stop %CONTAINER_ID%

@echo off

if "%REPO%" == "rstudio-pro" (
    set PKG_FILENAME=RStudio-pro-99.9.9-RelWithDebInfo
) ELSE (
    set PKG_FILENAME=RStudio-99.9.9-RelWithDebInfo
)

@echo on
docker cp %CONTAINER_ID%:/rstudio/package/win32/build/%PKG_FILENAME%.zip %HOSTPATH%/docker/package/%PKG_FILENAME%.zip
docker cp %CONTAINER_ID%:/rstudio/package/win32/build/%PKG_FILENAME%.exe %HOSTPATH%/docker/package/%PKG_FILENAME%.exe
@echo off
