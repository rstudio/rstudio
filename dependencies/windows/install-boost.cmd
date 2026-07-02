@echo off
setlocal

REM Variables defining the build. Update as appropriate, or set in the
REM environment to override (e.g. when building for a newer MSVC toolset).
if not defined BOOST_VERSION set BOOST_VERSION=1.91.0
if not defined MSVC_TOOLSET_VERSION set MSVC_TOOLSET_VERSION=143

REM The file name prefix used for Boost build folders.
set BOOST_PREFIX=boost-%BOOST_VERSION%-win-msvc%MSVC_TOOLSET_VERSION%

REM Make sure our tools are on the PATH.
set PATH=%CD%\tools;%PATH%

REM Build Boost.
cd install-boost

cmd.exe /C R --vanilla -s -f install-boost.R --args debug static
if ERRORLEVEL 1 (
  echo !! ERROR: Boost debug build failed.
  exit /b 1
)

cmd.exe /C R --vanilla -s -f install-boost.R --args release static
if ERRORLEVEL 1 (
  echo !! ERROR: Boost release build failed.
  exit /b 1
)

cd ..

REM Build the Boost archive for upload to S3.
echo -- Packaging Boost %BOOST_VERSION% ...
7z a -mmt8 -mx9 ^
   %BOOST_PREFIX%.zip ^
   %BOOST_PREFIX%-debug-static ^
   %BOOST_PREFIX%-release-static
if ERRORLEVEL 1 (
  echo !! ERROR: Failed to package Boost archive.
  exit /b 1
)
echo -- Done!
