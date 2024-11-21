@REM @echo off

pushd ..\..\..\src\gwt\lib


:: IMPORTANT: When changing which branch this pulls from below, also update the Dockerfiles'
::            "panmirror check for changes" command to use the equivalent.

if not exist quarto (
  echo "Cloning quarto repo"
  git clone https://github.com/quarto-dev/quarto.git ..\..\..\src\gwt\lib\quarto
  REM git clone --branch release/rstudio-mariposa-orchid https://github.com/quarto-dev/quarto.git ..\..\..\src\gwt\lib\quarto
  pushd ..\..\..\src\gwt\lib\quarto
  git rev-parse HEAD
  popd
) else (
  echo "quarto repo already cloned"

  pushd quarto
  git fetch
  git reset --hard
  git clean -dfx
  git checkout main
  REM git checkout release/rstudio-mariposa-orchid
  git pull
  git rev-parse HEAD
  popd
)

popd
