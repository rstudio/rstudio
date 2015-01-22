REM This script installs R-devel from source on Windows.
REM Update the variables up here as new versions of R are released;
REM we attempt to build from tarballs.
REM
REM This script assumes you have 'wget' on your path, as it
REM is used to download. You can find a binary here:
REM
REM     https://eternallybored.org/misc/wget/wget64.exe 
REM
REM

REM ---------------------------------
REM - BEGIN CONFIGURATION VARIABLES -
REM ---------------------------------

SET ROOT_DIR=C:\R-src
SET R_MAJOR_VERSION=3
SET R_FULL_VERSION=3.1.2
SET RTOOLS_BIN_DIR=C:\Rtools\bin
SET TMPDIR=C:\tmp

REM Note: Tool path must use forward slashes, and
REM should end with a trailing slash.
SET TOOL_PATH=C:/Rtools/gcc-4.6.3/bin/

REM -------------------------------
REM - END CONFIGURATION VARIABLES -
REM -------------------------------

REM Unix-style newline. The newline hack!
REM Creating a Newline variable (the two blank lines are required!)
set NLM=^


set NL=^^^%NLM%%NLM%^%NLM%%NLM%

REM Set the current directory.
if not exist "%ROOT_DIR%" mkdir "%ROOT_DIR%"
cd "%ROOT_DIR%"

SET OLDPATH=%PATH%

REM Download the R sources.
if not exist R-%R_FULL_VERSION%.tar.gz (
	wget http://cran.us.r-project.org/src/base/R-%R_MAJOR_VERSION%/R-%R_FULL_VERSION%.tar.gz
)

REM Put Rtools on the path.
SET PATH=C:\Rtools\bin;%PATH%

if exist R-%R_FULL_VERSION% rmdir /S /Q R-%R_FULL_VERSION%
tar -xf R-%R_FULL_VERSION%.tar.gz

REM Copy in the 'extras' for a 64bit build. This includes tcltk
REM plus some other libraries.
xcopy /E C:\R64 %ROOT_DIR%\R-%R_FULL_VERSION%

REM Ensure the temporary directory exists.
if not exist "%TMPDIR%" mkdir "%TMPDIR%"

cd R-%R_FULL_VERSION%\src\gnuwin32

REM Look at MkRules.dist and if settings need to be altered, copy it to
REM MkRules.local and edit the settings there. In particular, this is where a
REM 64-bit build is selected.
if exist MkRules.local rm MkRules.local
cp MkRules.dist MkRules.local

REM Set the MULTI and TOOL_PATH variables for the
REM R build. We focus on producing a 64bit build here.
sed -i 's/^MULTI =/MULTI = 64/g' MkRules.local
sed -i 's/^TOOL_PATH =//g' MkRules.local
echo | set /p=TOOL_PATH = %TOOL_PATH%%NL%>> MkRules.local

REM Don't use MIKTEX
sed -i 's/^MIKTEX = TRUE//g' MkRules.local

REM Make it!
REM For this part, we ensure only Rtools is on the PATH.
SET PATH=C:\Rtools\bin
make -j10 all recommended

REM Clean up
SET PATH=%OLDPATH%
