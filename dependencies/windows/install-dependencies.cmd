
@echo off
setlocal EnableDelayedExpansion

for %%F in ("%CD%\..\tools\rstudio-tools.cmd") do (
  set "RSTUDIO_TOOLS=%%~fF"
)

echo -- Using RStudio tools: %RSTUDIO_TOOLS%
call %RSTUDIO_TOOLS%

set PATH=%CD%\tools;%PATH%

set BOOST_VERSION=1.87.0
set GNUGREP_VERSION=3.0
set GWT_VERSION=2.12.2
set LIBCLANG_VERSION=13.0.1
set MATHJAX_VERSION=2.7.9
set MSVC_TOOLSET_VERSION=143
set NSPROCESS_VERSION=1.6
set OPENSSL_VERSION=3.1.4
set PANDOC_VERSION=3.2
set QUARTO_VERSION=1.7.31
set COPILOT_VERSION=1.323.0
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
    echo ^^!^^! ERROR: %%X is not available on the PATH; cannot proceed.
    exit /b
  )
)

set QUARTO_URL=https://github.com/quarto-dev/quarto-cli/releases/download/v%QUARTO_VERSION%/quarto-%QUARTO_VERSION%-win.zip
set QUARTO_FOLDER=quarto
set QUARTO_OUTPUT=quarto


set COPILOT_URL=copilot-language-server/%COPILOT_VERSION%/copilot-language-server-win32-x64-%COPILOT_VERSION%.zip
set COPILOT_FOLDER=copilot-language-server
set COPILOT_OUTPUT=copilot-language-server


set GNUDIFF_URL=gnudiff.zip
set GNUDIFF_FOLDER=gnudiff
set GNUDIFF_OUTPUT=gnudiff


set GNUGREP_URL=gnugrep-%GNUGREP_VERSION%.zip
set GNUGREP_FOLDER=gnugrep\%GNUGREP_VERSION%
set GNUGREP_OUTPUT=gnugrep\%GNUGREP_VERSION%


set GWT_URL=gwt/gwt-%GWT_VERSION%.tar.gz
set GWT_FOLDER=%RSTUDIO_PROJECT_ROOT%/src/gwt/lib/gwt
set GWT_OUTPUT=%RSTUDIO_PROJECT_ROOT%/src/gwt/lib


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


set BOOST_URL=Boost/boost-%BOOST_VERSION%-win-msvc%MSVC_TOOLSET_VERSION%.zip
set BOOST_FOLDER=boost-%BOOST_VERSION%-win-msvc%MSVC_TOOLSET_VERSION%
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


set HUNSPELL_URL=hunspell/hunspell-v1.7.2.7z
set HUNSPELL_FOLDER=hunspell-v1.7.2
set HUNSPELL_OUTPUT=


set NODEBUILD_VERSION=%RSTUDIO_NODE_VERSION%
set NODEBUILD_LABEL=node (%NODEBUILD_VERSION%; build)
set NODEBUILD_FILE=node-v%NODEBUILD_VERSION%-win-x64
set NODEBUILD_URL=%RSTUDIO_BUILDTOOLS%/node/v%NODEBUILD_VERSION%/%NODEBUILD_FILE%.zip
set NODEBUILD_FOLDER=node\%NODEBUILD_VERSION%
set NODEBUILD_OUTPUT=node


:: Install dependencies within 'common' first.
cd ..\common

%RUN% install DICTIONARIES
%RUN% install MATHJAX
%RUN% install LIBCLANG

REM Determine if we have the correct version of quarto.exe already installed
if exist quarto\bin\quarto.exe (
  for /f "usebackq" %%v in (`quarto\bin\quarto.exe --version`) do (
    if not "%%v" == "%QUARTO_VERSION%" (
      echo -- Quarto version mismatch: found %%v, expected %QUARTO_VERSION%
      rmdir /s /q quarto
    )
  )
)
%RUN% install QUARTO

REM Determine if we have the correct version of copilot-language-server.exe already installed
if exist copilot-language-server\copilot-language-server.exe (
  for /f "usebackq" %%v in (`copilot-language-server\copilot-language-server.exe --version`) do (
    if not "%%v" == "%COPILOT_VERSION%" (
      echo -- Copilot version mismatch: found %%v, expected %COPILOT_VERSION%
      rmdir /s /q copilot-language-server
    )
  )
)
%RUN% install COPILOT

%RUN% install PANDOC
if exist pandoc\pandoc-%PANDOC_VERSION% (
  rmdir /s /q pandoc\%PANDOC_VERSION% 2>NUL
  %RUN% move pandoc\pandoc-%PANDOC_VERSION% pandoc\%PANDOC_VERSION%
  if ERRORLEVEL 1 (
    echo ^^!^^! ERROR: Could not move pandoc installation to pandoc\%PANDOC_VERSION%.
    exit /b
  )
)

%RUN% install NODEBUILD
if exist node\%NODEBUILD_FILE% (
  rmdir /s /q node\%NODEBUILD_VERSION% 2>NUL
  %RUN% move node\%NODEBUILD_FILE% node\%NODEBUILD_VERSION%
  if ERRORLEVEL 1 (
    echo ^^!^^! ERROR: Could not move node installation to node\%NODEBUILD_VERSION%.
    exit /b
  )
)

pushd node\%NODEBUILD_VERSION%
if not exist yarn.cmd (
  echo -- Installing yarn
  call npm install --global yarn
)
popd

echo -- Installing packages
call install-packages.cmd

:: Install the rest of our dependencies in the 'windows' folder.
cd ..\windows

%RUN% install HUNSPELL
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


echo -- Installing panmirror (Visual Editor)
pushd install-panmirror
call clone-quarto-repo.cmd
popd

echo -- Installing crashpad
call install-crashpad.cmd

echo -- Installing SOCI
call install-soci.cmd

if not exist sentry-cli.exe (
  set SENTRY_CLI_VERSION=2.9.0
  echo -- Installing sentry-cli
  %RUN% download "https://github.com/getsentry/sentry-cli/releases/download/2.9.0/sentry-cli-Windows-x86_64.exe" sentry-cli.exe
  sentry-cli.exe --version
)

%RUN% install-i18n-dependencies
