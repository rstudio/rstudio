@REM @echo off

pushd ..\..\..\src\gwt\lib

if not exist quarto (
  echo "Cloning quarto repo"
  git clone https://github.com/quarto-dev/quarto.git ..\..\..\src\gwt\lib\quarto
) else (
  echo "quarto repo already cloned"

  pushd quarto
  git reset --hard
  git clean -dfx
  git pull
  popd
)

popd
