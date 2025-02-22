@echo off
setlocal

set PATH=%CD%\tools;%PATH%
set BOOST_VERSION=1.87.0

REM Build Boost.
cd install-boost
R --vanilla -s -f install-boost.R --args debug static
R --vanilla -s -f install-boost.R --args release static
cd ..

REM Build the Boost archive for upload to S3.
echo --^> Packaging Boost %BOOST_VERSION% ...
zip -r -q -9 boost-%BOOST_VERSION%-win-msvc142.zip ^
	boost-%BOOST_VERSION%-win-msvc142-debug-static ^
	boost-%BOOST_VERSION%-win-msvc142-release-static
echo --^> Done!
