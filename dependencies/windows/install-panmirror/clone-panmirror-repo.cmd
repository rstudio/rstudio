@REM @echo off

:: This folder won't exist when building the docker image
if not exist ..\..\..\src\gwt\lib (
    echo Creating directory ..\..\..\src\gwt\lib
    mkdir ..\..\..\src\gwt\lib
)
pushd ..\..\..\src\gwt\lib


:: panmirror is taken from a branch of the quarto repo named for the RStudio release
:: cycle. Open a cycle by branching quarto's 'main' as release/rstudio-<flower>; to
:: take panmirror changes mid-cycle, create a NEW branch with a numeric suffix
:: (release/rstudio-<flower>-01, -02, ...) and point PANMIRROR_BRANCH at it.
::
:: IMPORTANT: treat a branch named here as immutable -- never push to it. Editing
:: this script is what invalidates the Docker layer that bakes panmirror into the
:: Jenkins builder images, so nothing detects a branch tip that moves: images cached
:: before the push keep the old panmirror while freshly built ones get the new code,
:: and the divergence is silent.
set PANMIRROR_REPO_URL=https://github.com/quarto-dev/quarto.git
set PANMIRROR_BRANCH=release/rstudio-autumn-hawkbit

echo -- panmirror branch: %PANMIRROR_BRANCH%

:: Check the branch up front; the git errors from the clone and checkout below don't
:: say what to do about a missing branch. Never fall back to another branch -- that
:: would silently ship the wrong panmirror.
git ls-remote --exit-code --heads %PANMIRROR_REPO_URL% %PANMIRROR_BRANCH% >nul
if errorlevel 1 (
  echo ERROR: Branch %PANMIRROR_BRANCH% does not exist in %PANMIRROR_REPO_URL%.
  echo Create it from quarto's 'main', or set PANMIRROR_BRANCH to a branch that exists.
  popd
  exit /b 1
)

if not exist quarto (
  git clone --branch %PANMIRROR_BRANCH% %PANMIRROR_REPO_URL% ..\..\..\src\gwt\lib\quarto
  pushd ..\..\..\src\gwt\lib\quarto
  git rev-parse HEAD
  popd
) else (
  pushd quarto
  git fetch
  git reset --hard
  git clean -dfx
  git checkout %PANMIRROR_BRANCH%
  git pull
  git rev-parse HEAD
  popd
)

popd
