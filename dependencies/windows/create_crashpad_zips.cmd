@echo off
setlocal

set PATH=%CD%\tools;%PATH%

REM Build the Crashpad archives for upload to S3.
REM Use this after making changes to the crashpad-debug and crashpad-release
REM folders.
echo --^> Packaging Crashpad debug...
zip -r -q -9 crashpad-debug.zip crashpad-debug
echo --^> Done!

echo --^> Packaging Crashpad release...
zip -r -q -9 crashpad-release.zip crashpad-release
echo --^> Done!
