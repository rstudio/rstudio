@REM @echo off

pushd ..\..\..\src\gwt\lib

if not exist quarto (
  echo "Cloning quarto repo"
  REM git clone https://github.com/quarto-dev/quarto.git ..\..\..\src\gwt\lib\quarto
  git clone --branch release/rstudio-cranberry-hibiscus https://github.com/quarto-dev/quarto.git ..\..\..\src\gwt\lib\quarto
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
  git checkout release/rstudio-cranberry-hibiscus
  git pull
  git rev-parse HEAD
  popd
)

popd

:: copy custom scripts into resources folder
PANMIRROR_SCRIPTS_DIR="..\..\..\src\cpp\session\resources\panmirror-scripts"
if not exist %PANMIRROR_SCRIPTS_DIR% (
  md %PANMIRROR_SCRIPTS_DIR%
)
copy ..\..\..\src\gwt\lib\quarto\packages\editor-server\src\resources\md-writer.lua %PANMIRROR_SCRIPTS_DIR%
