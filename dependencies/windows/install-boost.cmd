@echo off
setlocal

set PATH=%CD%\tools;%PATH%

cd install-boost
R --vanilla --slave -f install-boost.R --args debug static
R --vanilla --slave -f install-boost.R --args release static
cd ..

REM Build the Boost archive for upload to S3.
echo --^> Packaging Boost 1.65.1 ...
zip -r -q -9 boost-1.65.1-win-msvc141.zip ^
        boost-1.65.1-win-msvc141-debug-static ^
        boost-1.65.1-win-msvc141-release-static
echo --^> Done!
