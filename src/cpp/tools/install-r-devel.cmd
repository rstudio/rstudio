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

SET R_HOME=C:\R-src
SET RTOOLS_BIN_DIR=C:\Rtools\bin
SET TMPDIR=C:\tmp

REM -------------------------------
REM - END CONFIGURATION VARIABLES -
REM -------------------------------

REM Unix-style newline. The newline hack!
REM Creating a Newline variable (the two blank lines are required!)
SET NLM=^


SET NL=^^^%NLM%%NLM%^%NLM%%NLM%

REM Set the current directory.
if not exist R_HOME%" mkdir "%R_HOME%"
cd "%R_HOME%"
SET OLDPATH=%PATH%

REM Download the R sources. Get the latest R-devel sources using SVN.
REM
REM If you need a Windows SVN client, you can download SilkSVN here:
REM
REM     https://sliksvn.com/download/
REM
REM Be sure to place the installed binary directory on your PATH.
svn checkout https://svn.r-project.org/R/trunk/

REM Put Rtools on the path.
SET PATH=C:\Rtools\bin;%PATH%

REM Copy in the 'extras' for a 64bit build. This includes tcltk
REM plus some other libraries. Note that the R64 directory should
REM have been populated by the RTools installation.
xcopy /E C:\R64 %R_HOME%\trunk

REM Ensure the temporary directory exists.
if not exist "%TMPDIR%" mkdir "%TMPDIR%"

cd trunk\src\gnuwin32

REM Since we're building from source, we need to get Recommended packages.
make rsync-recommended

REM Download external software -- libpng, libgsl, and so on.
make rsync-extsoft

REM Look at MkRules.dist and if settings need to be altered, copy it to
REM MkRules.local and edit the settings there.
if exist MkRules.local (
	rm MkRules.local
)
cp MkRules.dist MkRules.local

REM Don't use MIKTEX.
sed -i 's/^MIKTEX = TRUE//g' MkRules.local


REM Make it!
REM For this part, we ensure only Rtools is on the PATH.
SET PATH=C:\Rtools\bin
make -j10 all recommended

REM Clean up
SET PATH=%OLDPATH%
