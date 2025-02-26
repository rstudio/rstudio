
@echo off
setlocal EnableDelayedExpansion

for %%F in ("%CD%\..\tools\rstudio-tools.cmd") do (
  set "RSTUDIO_TOOLS=%%~fF"
)

set X=1
echo %X%

call %RSTUDIO_TOOLS%
set PATH=%CD%\tools;%PATH%

set BOOST_VERSION=1.87.0
set GNUGREP_VERSION=3.0
set LIBCLANG_VERSION=13.0.1
set MATHJAX_VERSION=2.7.9
set MSVC_VERSION=vc142
set NSPROCESS_VERSION=1.6
set OPENSSL_VERSION=3.1.4
set PANDOC_VERSION=3.2
set QUARTO_VERSION=1.6.42
set SUMATRA_VERSION=3.1.2
set WINPTY_VERSION=0.4.3-msys2-2.7.0
set WINUTILS_VERSION=1.0

set CLEAN=0
set QUIET=0
for %%A in (%*) do (

  if /I "%%A" == "clean" (
    set CLEAN=1
  )

  if /I "%%A" == "quiet" (
    set QUIET=1
  )

)


REM Check for required tools on the PATH.
for %%X in (R.exe 7z.exe cmake.exe curl.exe) do (
  where /q %%X
  if ERRORLEVEL 1 (
    echo ERROR: %%X is not available on the PATH; cannot proceed.
    exit /b
  )
)


REM Get latest Quarto release version
REM cd install-quarto
REM for /F "delims=" %%L in ('powershell.exe -File get-quarto-version.ps1') do (set "QUARTO_VERSION=%%L")
REM cd ..

REM Check for errors.
if not "%QUARTO_VERSION%" == "%QUARTO_VERSION:ERROR=%" (
	echo ERROR: Failed to determine Quarto version; cannot proceed.
	echo Did you set the Powershell execution policy?
	echo Try running 'Set-ExecutionPolicy Unrestricted'.
	exit /b
)

set QUARTO_URL=https://github.com/quarto-dev/quarto-cli/releases/download/v%QUARTO_VERSION%/quarto-%QUARTO_VERSION%-win.zip
set QUARTO_FOLDER=quarto
set QUARTO_OUTPUT=quarto

set GNUDIFF_URL=gnudiff.zip
set GNUDIFF_FOLDER=gnudiff
set GNUDIFF_OUTPUT=gnudiff

set GNUGREP_URL=gnugrep-%GNUGREP_VERSION%.zip
set GNUGREP_FOLDER=gnugrep\%GNUGREP_VERSION%
set GNUGREP_OUTPUT=gnugrep\%GNUGREP_VERSION%

set SUMATRA_URL=sumatrapdf/SumatraPDF-%SUMATRA_VERSION%-64.zip
set SUMATRA_FOLDER=sumatra\%SUMATRA_VERSION%
set SUMATRA_OUTPUT=sumatra\%SUMATRA_VERSION%


set WINUTILS_URL=winutils-%WINUTILS_VERSION%.zip
set WINUTILS_FOLDER=winutils\%WINUTILS_VERSION%
set WINUTILS_OUTPUT=winutils\%WINUTILS_VERSION%


set WINPTY_URL=winpty-%WINPTY_VERSION%.zip
set WINPTY_FOLDER=winpty-%WINPTY_VERSION%
set WINPTY_OUTPUT=


set OPENSSL_URL=openssl-%OPENSSL_VERSION%.zip
set OPENSSL_FOLDER=openssl-%OPENSSL_VERSION%
set OPENSSL_OUTPUT=


set BOOST_URL=Boost/boost-%BOOST_VERSION%-win-ms%MSVC_VERSION%.zip
set BOOST_FOLDER=boost-%BOOST_VERSION%-win-ms%MSVC_VERSION%
set BOOST_OUTPUT=


set RESHACKER_URL=resource-hacker/resource_hacker.zip
set RESHACKER_FOLDER=resource-hacker
set RESHACKER_OUTPUT=resource-hacker


set NSPROCESS_URL=nsprocess/NsProcess.zip
set NSPROCESS_FOLDER=nsprocess\%NSPROCESS_VERSION%
set NSPROCESS_OUTPUT=nsprocess\%NSPROCESS_VERSION%


set DICTIONARIES_URL=dictionaries/core-dictionaries.zip
set DICTIONARIES_FOLDER=dictionaries
set DICTIONARIES_OUTPUT=dictionaries


set MATHJAX_URL=mathjax-%MATHJAX_VERSION%.zip
set MATHJAX_FOLDER=mathjax-27
set MATHJAX_OUTPUT=


set PANDOC_URL=pandoc/%PANDOC_VERSION%/pandoc-%PANDOC_VERSION%-windows-x86_64.zip
set PANDOC_FOLDER=pandoc
set PANDOC_OUTPUT=pandoc


set LIBCLANG_URL=libclang-windows-%LIBCLANG_VERSION%.zip
set LIBCLANG_FOLDER=libclang\%LIBCLANG_VERSION%
set LIBCLANG_OUTPUT=


set BREAKPAD_URL=https://s3.amazonaws.com/getsentry-builds/getsentry/breakpad-tools/windows/breakpad-tools-windows.zip
set BREAKPAD_FOLDER=breakpad-tools-windows
set BREAKPAD_OUTPUT=breakpad-tools-windows


%RUN% install GNUDIFF
%RUN% install GNUGREP
%RUN% install SUMATRA
%RUN% install WINUTILS
%RUN% install WINPTY
%RUN% install OPENSSL
%RUN% install BOOST
%RUN% install RESHACKER
%RUN% install NSPROCESS
%RUN% install BREAKPAD

echo -- Installing crashpad
call install-crashpad.cmd

echo -- Installing SOCI
call install-soci.cmd

if not exist sentry-cli.exe (
  REM specify a version to install
  set SENTRY_CLI_VERSION=2.9.0
  echo Installing sentry-cli
  powershell.exe "[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12 ; Invoke-WebRequest -Uri https://github.com/getsentry/sentry-cli/releases/download/2.9.0/sentry-cli-Windows-x86_64.exe -OutFile sentry-cli.exe"
  %RUN% command "sentry-cli.exe --version" SENTRY_CLI_INSTALLED_VERSION
  echo Installed Sentry CLI version: %SENTRY_CLI_INSTALLED_VERSION%
)

pushd ..\common

%RUN% install DICTIONARIES
%RUN% install MATHJAX
%RUN% install LIBCLANG
%RUN% install QUARTO
%RUN% install PANDOC

if exist pandoc\pandoc-%PANDOC_VERSION% (
  move pandoc\pandoc-%PANDOC_VERSION% pandoc\%PANDOC_VERSION%
)

echo -- Installing NPM dependencies
call install-npm-dependencies.cmd

echo -- Installing packages
call install-packages.cmd

echo -- Installing panmirror (Visual Editor)
pushd ..\windows\install-panmirror
call clone-quarto-repo.cmd

popd


if not defined JENKINS_URL (
  if exist C:\Windows\py.exe (
    pushd ..\..\src\gwt\tools\i18n-helpers\
    py -3 -m venv VENV
    VENV\Scripts\pip install --disable-pip-version-check -r commands.cmd.xml\requirements.txt
    popd
  )
)

popd

endlocal
