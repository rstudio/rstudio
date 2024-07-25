@REM @echo off

pushd ..\..\..\src\gwt\lib

if not exist quarto (
  echo "Cloning quarto repo"
  REM git clone https://github.com/quarto-dev/quarto.git ..\..\..\src\gwt\lib\quarto
  git clone --branch release/rstudio-chocolate-cosmos https://github.com/quarto-dev/quarto.git ..\..\..\src\gwt\lib\quarto
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
  git checkout release/rstudio-chocolate-cosmos
  git pull
  git rev-parse HEAD
  popd
)

popd
