@REM @echo off

pushd ..\..\..\src\gwt\lib

if not exist quarto (
  echo "Cloning quarto repo"
  REM git clone https://github.com/quarto-dev/quarto.git ..\..\..\src\gwt\lib\quarto
  git clone --branch release/rstudio-mountain-hydrangea https://github.com/quarto-dev/quarto.git ..\..\..\src\gwt\lib\quarto
) else (
  echo "quarto repo already cloned"

  pushd quarto
  git fetch
  git reset --hard
  git clean -dfx
  REM git checkout main
  git checkout release/rstudio-mountain-hydrangea
  git pull
  popd
)

popd
