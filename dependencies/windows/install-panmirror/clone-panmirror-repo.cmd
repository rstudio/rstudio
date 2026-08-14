@REM @echo off

:: This folder won't exist when building the docker image
if not exist ..\..\..\src\gwt\lib (
    echo Creating directory ..\..\..\src\gwt\lib
    mkdir ..\..\..\src\gwt\lib
)

:: The clone below is addressed relative to this directory, so stop if we are not in
:: it -- otherwise quarto lands next to this script and the build never sees it. A
:: failed mkdir above arrives here too, having already reported why. Nothing has been
:: pushed yet, so exit without the popd that :failed does.
pushd ..\..\..\src\gwt\lib
if errorlevel 1 (
  echo ERROR: Could not enter ..\..\..\src\gwt\lib
  exit /b 1
)


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
:: say what to do about a missing branch. Match the full ref path rather than passing
:: --heads with a bare name: --heads matches any tail of a ref at a path boundary, so a
:: name missing the 'release/' prefix would match the real branch and pass the check.
:: Never fall back to another branch -- that would silently ship the wrong panmirror.
git ls-remote --exit-code %PANMIRROR_REPO_URL% refs/heads/%PANMIRROR_BRANCH% >nul

:: Only --exit-code's 2 means "no such ref". Anything else -- 128 for network, DNS or
:: TLS trouble -- must not be reported as a missing branch. 'if errorlevel N' means "N
:: or greater", so the cases are tested from the top down.
if errorlevel 3 goto :unreachable
if errorlevel 2 goto :nobranch
if errorlevel 1 goto :unreachable

:: Every git step below is checked. cmd has no 'set -e', and a later command that
:: succeeds clears ERRORLEVEL, so an unchecked failure would leave a missing or
:: stale clone for the build to pick up instead of stopping it.
if not exist quarto (
  git clone --branch %PANMIRROR_BRANCH% %PANMIRROR_REPO_URL% quarto
  if errorlevel 1 goto :failed
) else (
  git -C quarto fetch
  if errorlevel 1 goto :failed
  git -C quarto reset --hard
  if errorlevel 1 goto :failed
  git -C quarto clean -dfx
  if errorlevel 1 goto :failed
  REM Force the branch to the fetched tip rather than checkout + pull. A local branch
  REM left ahead of origin -- an interrupted earlier run, or an upstream force-push --
  REM makes pull report "Already up to date" and exit 0 while HEAD sits on a commit
  REM that is not the branch tip, the divergence this whole scheme exists to avoid.
  REM Uses REM, not ::, since a label inside a parenthesized block is a cmd error.
  git -C quarto checkout -B %PANMIRROR_BRANCH% origin/%PANMIRROR_BRANCH%
  if errorlevel 1 goto :failed
)

git -C quarto rev-parse HEAD
if errorlevel 1 goto :failed

popd
exit /b 0

:nobranch
echo ERROR: Branch %PANMIRROR_BRANCH% does not exist in %PANMIRROR_REPO_URL%.
echo Create it from quarto's 'main', or edit PANMIRROR_BRANCH in this script.
goto :failed

:unreachable
echo ERROR: Could not reach %PANMIRROR_REPO_URL% to look for %PANMIRROR_BRANCH%.
goto :failed

:failed
echo ERROR: panmirror (quarto) checkout failed.
popd
exit /b 1
