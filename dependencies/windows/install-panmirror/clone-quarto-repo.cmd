@REM @echo off

:: This folder won't exist when building the docker image
if not exist ..\..\..\src\gwt\lib (
    echo Creating directory ..\..\..\src\gwt\lib
    mkdir ..\..\..\src\gwt\lib
)
pushd ..\..\..\src\gwt\lib


:: IMPORTANT: When changing which branch this pulls from below, also update the Dockerfiles'
::            "panmirror check for changes" command to use the equivalent.

if not exist quarto (
  git clone https://github.com/quarto-dev/quarto.git ..\..\..\src\gwt\lib\quarto
  REM git clone --branch release/rstudio-cucumberleaf-sunflower https://github.com/quarto-dev/quarto.git ..\..\..\src\gwt\lib\quarto
  pushd ..\..\..\src\gwt\lib\quarto
  git rev-parse HEAD
  popd
) else (
  pushd quarto
  git fetch
  git reset --hard
  git clean -dfx
  git checkout main
  REM git checkout release/rstudio-cucumberleaf-sunflower
  git pull
  git rev-parse HEAD
  popd
)

popd
