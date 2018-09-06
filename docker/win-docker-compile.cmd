@echo off
setlocal enableextensions enabledelayedexpansion

REM **************************************************************************
REM Build Windows docker image containing tools to build RStudio IDE
REM **************************************************************************
REM Currently this goes as far as creating an image, but doesn't 
REM run the build itself. See TODO's at end of this file.
REM **************************************************************************
REM Build this image and run the build using it on Windows Server 2016,
REM where Docker can be run in process-isolation mode. Building RStudio in
REM a container using Hyper-V will not work due to:
REM     https://github.com/docker/for-win/issues/829
REM **************************************************************************

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
docker build --tag %REPO%:%IMAGE% --file docker\jenkins\Dockerfile.%IMAGE% %BUILD_ARGS% -m 2GB .\docker\jenkins

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

REM Startup the container. To build, run "install-dependencies.cmd" then "make-package" inside 
REM it as you would on a Windows dev box.
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


