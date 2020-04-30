@echo off
setlocal enableextensions enabledelayedexpansion

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

set IMAGE=windows
set FLAVOR=desktop

REM set destination folder
set PKG_DIR="%cd%"\package
if not exist %PKG_DIR% (
    md %PKG_DIR%
)

REM move to the repo root (script's parent directory)
cd %~dp0\..

REM determine repo name
set CURRENTDIR="%cd%"
for /F "delims=" %%i in ("%cd%") do set REPO=%%~nxi

REM check to see if there's already a built image
set TEMPFILE=%TEMP%\docker-compile~%RANDOM%.txt
docker images %REPO%:%IMAGE% --format "{{.ID}}" > %TEMPFILE%
set /p IMAGEID= < %TEMPFILE%
del %TEMPFILE%
if DEFINED IMAGEID (
    echo Found image %IMAGEID% for %REPO%:%IMAGE%.
) else (
    echo No image found for %REPO%:%IMAGE%.
)

REM get build arg env vars, if any
if defined DOCKER_GITHUB_LOGIN (
    set "BUILD_ARGS=--build-arg GITHUB_LOGIN=%DOCKER_GITHUB_LOGIN%"
    echo "!BUILD_ARGS!"
)

REM rebuild the image if necessary
docker build --tag %REPO%:%IMAGE% --file docker\jenkins\Dockerfile.%IMAGE% %BUILD_ARGS% -m 4GB .\docker\jenkins

REM set up build flags
git rev-parse HEAD > %TEMPFILE%
set /p GIT_COMMIT= < %TEMPFILE%
del %TEMPFILE%
set BUILD_ID=local

REM infer make parallelism
set "MAKEFLAGS=-j%NUMBER_OF_PROCESSORS%"

REM remove previous image if it exists
set CONTAINER_ID=build-%REPO%-%IMAGE%
echo Cleaning up container %CONTAINER_ID% if it exists...
docker rm %CONTAINER_ID%

for %%A in ("%cd%") do set HOSTPATH=%%~sA

echo Creating container %CONTAINER_ID%...
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
docker cp %HOSTPATH% %CONTAINER_ID%:/src

echo Starting container...
docker start %CONTAINER_ID%

echo Installing dependencies...
docker exec %CONTAINER_ID% cmd.exe /C "cd \src\dependencies\windows && set RSTUDIO_SKIP_QT=1 && install-dependencies.cmd"

echo Building RStudio...
docker exec %CONTAINER_ID% cmd.exe /C "cd \src\package\win32 && make-package.bat clean"

echo Stopping container...
docker stop %CONTAINER_ID%

if "%REPO%" == "rstudio-pro" (
    set PKG_FILENAME=RStudio-pro-99.9.9-RelWithDebInfo
) ELSE (
    set PKG_FILENAME=RStudio-99.9.9-RelWithDebInfo
)
echo Copying build result (%PKG_FILENAME%.zip) to %HOSTPATH%/docker/package
docker cp %CONTAINER_ID%:/src/package/win32/build/%PKG_FILENAME%.zip %HOSTPATH%/docker/package/%PKG_FILENAME%.zip

echo Copying build result (%PKG_FILENAME%.exe) to %HOSTPATH%/docker/package
docker cp %CONTAINER_ID%:/src/package/win32/build/%PKG_FILENAME%.exe %HOSTPATH%/docker/package/%PKG_FILENAME%.exe
