@echo off
setlocal enableextensions enabledelayedexpansion

:: Script to build docker image for buildng RStudio Desktop for Windows

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

echo Created image %REPO%:%IMAGE%
