@echo off
setlocal enableextensions enabledelayedexpansion

REM Build Windows docker image containing tools to build RStudio IDE
REM **************************************************************************
REM Currently this goes as far as creating an image, but doesn't 
REM run the build itself. See TODO's at end of this file.
REM **************************************************************************
REM Dependencies installed via install-dependencies.cmd must be present
REM in rstudio\dependencies\windows for the container to build RStudio.
REM 
REM The dockerfile does not currently handle this, so the machine hosting 
REM the container and the source tree must have install-dependencies.cmd 
REM run first. This needs to be cleaned up to work like the Linux builds, 
REM where everything needed is installed via the dockerfile and the cmake
REM scripts can discover them in the alternative location.
REM **************************************************************************
REM Ideally, build this image and run the build using it on Windows Server 2016,
REM using a physical server, not a VM.
REM **************************************************************************
REM It is possible to create the image on Windows-10 Pro VM using Hyper-V, but you
REM cannot currently build RStudio using the image on Win-10 due to:
REM https://github.com/docker/for-win/issues/829
REM **************************************************************************
REM Running Docker in Windows Server in some configurations (such as a virtual
REM machine on Google Compute) causes Chocolately to fail to install. See
REM https://stackoverflow.com/questions/50689574/diagnosing-download-timeout-from-chocolatey-org-in-a-windows-docker-build

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
docker build --tag %REPO%:%IMAGE% --file docker\jenkins\Dockerfile.%IMAGE% %BUILD_ARGS% -m 2GB .

echo Produced image.
exit /b 0

REM Everything below here is a work-in-progress. Up to here we've produced the image.
REM Below here, we need to trigger the build, and report the results.

REM set up build flags
git rev-parse HEAD > %TEMPFILE%
set /p GIT_COMMIT= < %TEMPFILE%
del %TEMPFILE%
set BUILD_ID=local

REM infer make parallelism
set "MAKEFLAGS=-j%NUMBER_OF_PROCESSORS%

REM remove previous image if it exists
set CONTAINER_ID=build-%REPO%-%IMAGE%
echo Cleaning up container %CONTAINER_ID% if it exists...
docker rm %CONTAINER_ID%

REM Startup the container, to build, run "make-package" inside it as you would on a
REM Windows dev box.
for %%A in ("%cd%") do set HOSTPATH=%%~sA
docker run -it --isolation process --name %CONTAINER_ID% -v %HOSTPATH%:c:/src %REPO%:%IMAGE%

REM extract logs to get filename (should be on the last line)
REM TODO
REM PKG_FILENAME=$(docker logs --tail 1 "$CONTAINER_ID")

REM report name of produced package
REM TODO

REM stop the container
REM TODO
REM docker stop %CONTAINER_ID%
REM echo Container image saved in %CONTAINER_ID%.


