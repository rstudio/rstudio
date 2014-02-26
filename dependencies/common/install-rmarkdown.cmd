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
echo Repository: RStudioIDE >> DESCRIPTION

REM create source package
popd
set RMARKDOWN_ARCHIVE_PATTERN=rmarkdown*.tar.gz
del /s /q %RMARKDOWN_ARCHIVE_PATTERN%
R CMD build rmarkdown

REM modify filename to include SHA1
for %%f in (%RMARKDOWN_ARCHIVE_PATTERN%) do set RMARKDOWN_ARCHIVE=%%f
set RMARKDOWN_ARCHIVE_STEM=%RMARKDOWN_ARCHIVE:~0,-7%
set RMARKDOWN_ARCHIVE_SHA1=%RMARKDOWN_ARCHIVE_STEM%_%RMARKDOWN_SHA1%.tar.gz
move %RMARKDOWN_ARCHIVE% %RMARKDOWN_ARCHIVE_SHA1%

