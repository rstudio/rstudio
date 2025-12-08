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

set "_CLEAN="
set "_BUILD="

for %%A in (%*) do (
    if /I "%%A" == "clean" (
        set _CLEAN=1
    )
)

set IMAGE=windows
set FLAVOR=electron

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

if defined _CLEAN (
    docker stop %CONTAINER_ID%
    docker rm %CONTAINER_ID%
)

REM Create the container if it doesn't already exist.
docker inspect %CONTAINER_ID% >NUL 2>NUL
if ERRORLEVEL 1 (

    docker create ^
        --name %CONTAINER_ID% ^
        --volume %HOSTPATH%:C:/rstudio:rw ^
        --cpu-count %CPUS% ^
        --memory 16GB ^
        --interactive ^
        %REPO%:%IMAGE% ^
        cmd.exe

    if ERRORLEVEL 1 (
        echo.!! ERROR: docker container creation failed.
        exit /b 1
    )

)

docker start %CONTAINER_ID%

docker exec %CONTAINER_ID% cmd.exe /C ^
    "git config --global --add safe.directory C:/rstudio/"

docker exec %CONTAINER_ID% cmd.exe /C ^
    "cd C:\rstudio-tools\dependencies\windows && C:\rstudio\dependencies\windows\install-dependencies.cmd"

docker exec %CONTAINER_ID% cmd.exe /C setx /M BUILD_DIR C:/build
docker exec %CONTAINER_ID% cmd.exe /C setx /M WIN32_BUILD_PATH C:/build32
docker exec %CONTAINER_ID% cmd.exe /C setx /M RSTUDIO_PROJECT_ROOT C:/rstudio
docker exec %CONTAINER_ID% cmd.exe /C setx /M RSTUDIO_DOCKER_DEVELOPMENT_BUILD 1

docker exec %CONTAINER_ID% cmd.exe /C ^
    "cd C:\rstudio\src\gwt && ant clean"

docker exec %CONTAINER_ID% cmd.exe /C ^
    "cd C:\rstudio\package\win32 && make-package.bat"

docker exec %CONTAINER_ID% cmd.exe /C "mkdir C:\package"
docker exec %CONTAINER_ID% cmd.exe /C "move C:\rsbuild\*.exe C:\package"
docker exec %CONTAINER_ID% cmd.exe /C "move C:\rsbuild\*.zip C:\package"

docker stop %CONTAINER_ID%

@echo off

if "%REPO%" == "rstudio-pro" (
    set PKG_FILENAME=RStudio-pro-99.9.9-RelWithDebInfo
) ELSE (
    set PKG_FILENAME=RStudio-99.9.9-RelWithDebInfo
)

@echo on
mkdir %HOSTPATH%\docker\package 2>NUL
docker cp %CONTAINER_ID%:C:/package %HOSTPATH%/docker
@echo off
