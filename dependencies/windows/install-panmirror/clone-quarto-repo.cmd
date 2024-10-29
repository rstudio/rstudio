@REM @echo off

pushd ..\..\..\src\gwt\lib


:: IMPORTANT: When changing which branch this pulls from below, also update the Dockerfiles'
::            "panmirror check for changes" command to use the equivalent.

if not exist quarto (
  echo "Cloning quarto repo"
  REM git clone https://github.com/quarto-dev/quarto.git ..\..\..\src\gwt\lib\quarto
  git clone --branch release/rstudio-kousa-dogwood https://github.com/quarto-dev/quarto.git ..\..\..\src\gwt\lib\quarto
  pushd ..\..\..\src\gwt\lib\quarto
  git rev-parse HEAD
  popd
) else (
  echo "quarto repo already cloned"

  pushd quarto
  git fetch
  git reset --hard
  git clean -dfx
  REM git checkout main
  git checkout release/rstudio-kousa-dogwood
  git pull
  git rev-parse HEAD
  popd
)

popd
