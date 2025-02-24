@echo off
setlocal

REM Variables defining the build. Update as appropriate.
set BOOST_VERSION=1.87.0
set MSVC_VERSION=vc142

REM The file name prefix used for Boost build folders.
set BOOST_PREFIX=boost-%BOOST_VERSION%-win-ms%MSVC_VERSION%

REM Make sure our tools are on the PATH.
set PATH=%CD%\tools;%PATH%

REM Build Boost.
cd install-boost
R --vanilla -s -f install-boost.R --args debug static
R --vanilla -s -f install-boost.R --args release static
cd ..

REM Build the Boost archive for upload to S3.
echo --^> Packaging Boost %BOOST_VERSION% ...
zip -r -q -9 ^
   %BOOST_PREFIX%.zip ^
   %BOOST_PREFIX%-debug-static ^
   %BOOST_PREFIX%-release-static
echo --^> Done!
