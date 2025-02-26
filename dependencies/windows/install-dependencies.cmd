@echo off
setlocal EnableDelayedExpansion

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


call ..\tools\rstudio-tools.cmd
set PATH=%CD%\tools;%PATH%
set BASEURL=%RSTUDIO_BUILDTOOLS%/


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

set QUARTO_FILE=quarto-%QUARTO_VERSION%-win.zip
set QUARTO_URL=https://github.com/quarto-dev/quarto-cli/releases/download/v%QUARTO_VERSION%/%QUARTO_FILE%
set QUARTO_FOLDER=quarto
set QUARTO_OUTPUT=quarto


set GNUDIFF_FILE=gnudiff.zip
set GNUDIFF_URL=%GNUDIFF_FILE%
set GNUDIFF_FOLDER=gnudiff
set GNUDIFF_OUTPUT=gnudiff


set GNUGREP_FILE=gnugrep-%GNUGREP_VERSION%.zip
set GNUGREP_URL=%GNUGREP_FILE%
set GNUGREP_FOLDER=gnugrep\%GNUGREP_VERSION%
set GNUGREP_OUTPUT=gnugrep\%GNUGREP_VERSION%


set SUMATRA_FILE=SumatraPDF-%SUMATRA_VERSION%-64.zip
set SUMATRA_URL=sumatrapdf/%SUMATRA_FILE%
set SUMATRA_FOLDER=sumatra\%SUMATRA_VERSION%
set SUMATRA_OUTPUT=sumatra\%SUMATRA_VERSION%


set WINUTILS_FILE=winutils-%WINUTILS_VERSION%.zip
set WINUTILS_URL=%WINUTILS_FILE%
set WINUTILS_FOLDER=winutils\%WINUTILS_VERSION%
set WINUTILS_OUTPUT=winutils\%WINUTILS_VERSION%


set WINPTY_FILE=winpty-%WINPTY_VERSION%.zip
set WINPTY_URL=%WINPTY_FILE%
set WINPTY_FOLDER=winpty-%WINPTY_VERSION%
set WINPTY_OUTPUT=


set OPENSSL_FILE=openssl-%OPENSSL_VERSION%.zip
set OPENSSL_URL=%OPENSSL_FILE%
set OPENSSL_FOLDER=openssl-%OPENSSL_VERSION%
set OPENSSL_OUTPUT=


set BOOST_FILE=boost-%BOOST_VERSION%-win-ms%MSVC_VERSION%.zip
set BOOST_URL=Boost/%BOOST_FILE%
set BOOST_FOLDER=boost-%BOOST_VERSION%-win-ms%MSVC_VERSION%
set BOOST_OUTPUT=


set RESHACKER_FILE=resource_hacker.zip
set RESHACKER_URL=resource-hacker/%RESHACKER_FILE%
set RESHACKER_FOLDER=resource-hacker
set RESHACKER_OUTPUT=resource-hacker


set NSPROCESS_FILE=NsProcess.zip
set NSPROCESS_URL=nsprocess/%NSPROCESS_FILE%
set NSPROCESS_FOLDER=nsprocess\%NSPROCESS_VERSION%
set NSPROCESS_OUTPUT=nsprocess\%NSPROCESS_VERSION%


set DICTIONARIES_FILE=core-dictionaries.zip
set DICTIONARIES_URL=dictionaries/%DICTIONARIES_FILE%
set DICTIONARIES_FOLDER=dictionaries
set DICTIONARIES_OUTPUT=dictionaries


set MATHJAX_FILE=mathjax-%MATHJAX_VERSION%.zip
set MATHJAX_URL=%MATHJAX_FILE%
set MATHJAX_FOLDER=mathjax-27
set MATHJAX_OUTPUT=


set PANDOC_FILE=pandoc-%PANDOC_VERSION%-windows-x86_64.zip
set PANDOC_URL=pandoc/%PANDOC_VERSION%/%PANDOC_FILE%
set PANDOC_FOLDER=pandoc
set PANDOC_OUTPUT=pandoc


set LIBCLANG_FILE=libclang-windows-%LIBCLANG_VERSION%.zip
set LIBCLANG_URL=%LIBCLANG_FILE%
set LIBCLANG_FOLDER=libclang\%LIBCLANG_VERSION%
set LIBCLANG_OUTPUT=


goto :main


REM Helper sub-routines.

:download

  set _URLARG=%~1
  if "%_URLARG:~0,4%" == "http" (
    set _URL=%_URLARG%
  ) else (
    set _URL=%BASEURL%%~1%
  )

  echo -- Downloading %_URL%
  curl -L -f -C - -O "%_URL%"
  if %ERRORLEVEL% neq 0 (
    echo Error downloading %_URL% [exit code %ERRORLEVEL%]
    goto :error
  )

  goto :eof


:extract

  set _ARCHIVE=%~1
  set _OUTPUT=%~2

  if defined _OUTPUT (
    echo -- Extracting %_ARCHIVE% to %_OUTPUT%
    7z x -y %_ARCHIVE% -o%_OUTPUT%
  ) else (
    echo -- Extracting %_ARCHIVE%
    7z x -y %_ARCHIVE%
  )

  if %ERRORLEVEL% neq 0 (
    echo Error extracting %_ARCHIVE% [exit code %ERRORLEVEL%]
    goto :error
  )

  goto :eof


:install

  set _NAME=%~1

  set _FILE=!%_NAME%_FILE!
  set _URL=!%_NAME%_URL!
  set _FOLDER=!%_NAME%_FOLDER!
  set _OUTPUT=!%_NAME%_OUTPUT!

  if "%CLEAN%" == "1" (
    rmdir /s /q %_FOLDER%
  )

  if exist %_FOLDER% (
    echo -- %_NAME% is already installed.
    goto :eof
  )

  mkdir %_FOLDER%
  call :download "%_URL%"
  call :extract "%_FILE%" "%_OUTPUT%"

  goto :eof


:main

call :install GNUDIFF
call :install GNUGREP
call :install SUMATRA
call :install WINUTILS
call :install WINPTY
call :install OPENSSL
call :install BOOST
call :install RESHACKER
call :install NSPROCESS

call install-crashpad.cmd
call install-soci.cmd

if not exist sentry-cli.exe (
  REM specify a version to install
  set SENTRY_CLI_VERSION=2.9.0
  echo Installing sentry-cli
  powershell.exe "[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12 ; Invoke-WebRequest -Uri https://github.com/getsentry/sentry-cli/releases/download/2.9.0/sentry-cli-Windows-x86_64.exe -OutFile sentry-cli.exe"
  for /F "delims=" %%G in ('sentry-cli.exe --version') do (set "SENTRY_CLI_INSTALLED_VERSION=%%G")
  echo Installed Sentry CLI version: %SENTRY_CLI_INSTALLED_VERSION%
)

if not exist breakpad-tools-windows (
  echo Installing breakpad tools for Windows
  wget %WGET_ARGS% "https://s3.amazonaws.com/getsentry-builds/getsentry/breakpad-tools/windows/breakpad-tools-windows.zip"
  echo Unzipping breakpad tools
  unzip %UNZIP_ARGS% breakpad-tools-windows.zip -d breakpad-tools-windows
  del breakpad-tools-windows.zip
)

pushd ..\common

call :install DICTIONARIES
call :install MATHJAX
call :install LIBCLANG
call :install QUARTO
call :install PANDOC
move pandoc\pandoc-%PANDOC_VERSION% pandoc\%PANDOC_VERSION%

call install-npm-dependencies.cmd
call install-packages.cmd

if not defined JENKINS_URL (
  if exist C:\Windows\py.exe (
    pushd ..\..\src\gwt\tools\i18n-helpers\
    py -3 -m venv VENV
    VENV\Scripts\pip install --disable-pip-version-check -r commands.cmd.xml\requirements.txt
    popd
  )
)

echo "Installing panmirror (visual editor)"
pushd ..\windows\install-panmirror
call clone-quarto-repo.cmd
popd

popd

endlocal
exit /b 0

:error
  exit /b 1
