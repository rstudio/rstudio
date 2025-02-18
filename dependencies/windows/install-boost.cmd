@echo off
setlocal

set PATH=%CD%\tools;%PATH%


REM Build Boost.
cd install-boost
R --vanilla -s -f install-boost.R --args debug static
R --vanilla -s -f install-boost.R --args release static
cd ..

REM Build the Boost archive for upload to S3.
echo --^> Packaging Boost 1.83.0 ...
zip -r -q -9 boost-1.83.0-win-msvc142.zip ^
	boost-1.83.0-win-msvc142-debug-static ^
	boost-1.83.0-win-msvc142-release-static
echo --^> Done!
