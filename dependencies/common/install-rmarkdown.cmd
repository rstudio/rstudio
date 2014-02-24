@echo off

setlocal

set PATH=%PATH%;%CD%\tools

REM git clone if necessary
set RMARKDOWN_DIR=rmarkdown
if not exist "%RMARKDOWN_DIR%" (
   git clone https://github.com/rstudio/rmarkdown.git
)

REM clean and checkout target branch
set RMARKDOWN_VERSION=master
pushd "%RMARKDOWN_DIR%"
git checkout .
git clean -df .
git pull
git checkout "%RMARKDOWN_VERSION%"


REM append GitHub fields to DESCRIPTION
git rev-parse "%RMARKDOWN_VERSION%" > RMARKDOWN_SHA1
set /p RMARKDOWN_SHA1= < RMARKDOWN_SHA1
del RMARKDOWN_SHA1
echo GithubRepo: rmarkdown >> DESCRIPTION
echo GithubUsername: rstudio >> DESCRIPTION
echo GithubRef: %RMARKDOWN_VERSION% >> DESCRIPTION
echo GithubSHA1: %RMARKDOWN_SHA1% >> DESCRIPTION

REM create source package
popd
del /s /q rmarkdown*.tar.gz
R CMD build rmarkdown

