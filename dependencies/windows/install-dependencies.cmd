@echo off

setlocal

call ..\tools\rstudio-tools.cmd

set PATH=%CD%\tools;%PATH%

REM Check for required tools on the PATH.
for %%X in (R.exe 7z.exe cmake.exe) do (
  where /q %%X
  if ERRORLEVEL 1 (
    echo ERROR: %%X is not available on the PATH; cannot proceed.
    exit /b
  )
)

set WGET_ARGS=-c --no-check-certificate --no-hsts
set UNZIP_ARGS=-q

set BASEURL=https://s3.amazonaws.com/rstudio-buildtools/
set GIN_FILE=gin-2.1.2.zip
set JUNIT_FILE=junit-4.9b3.jar
set GNUDIFF_FILE=gnudiff.zip

set GNUGREP_VERSION=3.0
set GNUGREP_NAME=gnugrep-%GNUGREP_VERSION%
set GNUGREP_FILE=%GNUGREP_NAME%.zip

set MSYS_SSH_FILE=msys-ssh-1000-18.zip
set SUMATRA_PDF_FILE=SumatraPDF-3.1.2-64.zip
set WINUTILS_FILE=winutils-1.0.zip
set WINPTY_FILES=winpty-0.4.3-msys2-2.7.0.zip
set OPENSSL_FILES=openssl-1.1.1t.zip
set BOOST_FILES=boost-1.78.0-win-msvc142.zip
set YAML_CPP_FILES=yaml-cpp-0.6.3.zip
set RESOURCE_HACKER=resource_hacker.zip

set NSIS_NSPROCESS_VERSION=1.6
set NSIS_NSPROCESS_FILE=NsProcess.zip

set PANDOC_VERSION=2.18
set PANDOC_NAME=pandoc-%PANDOC_VERSION%
set PANDOC_FILE=%PANDOC_NAME%-windows-x86_64.zip

REM Pin to specific Quarto version for releases
REM set QUARTO_VERSION=1.2.335

REM Get latest Quarto release version
cd install-quarto
for /F "delims=" %%L in ('powershell.exe -File get-quarto-version.ps1') do (set "QUARTO_VERSION=%%L")
cd ..

set QUARTO_FILE=quarto-%QUARTO_VERSION%-win.zip

set LIBCLANG_VERSION=13.0.1
set LIBCLANG_NAME=libclang-windows-%LIBCLANG_VERSION%
set LIBCLANG_FILE=%LIBCLANG_NAME%.zip

set NODE_VERSION=%RSTUDIO_NODE_VERSION%
set NODE_ROOT=node
set NODE_SUBDIR=%NODE_ROOT%\%NODE_VERSION%
set NODE_BASE_URL=https://nodejs.org/dist/v%NODE_VERSION%/
set NODE_ARCHIVE_DIR=node-v%NODE_VERSION%-win-x64
set NODE_ARCHIVE_FILE=%NODE_ARCHIVE_DIR%.zip

if not exist gnudiff (
  wget %WGET_ARGS% "%BASEURL%%GNUDIFF_FILE%"
  mkdir gnudiff
  echo Unzipping %GNUDIFF_FILE%
  unzip %UNZIP_ARGS% "%GNUDIFF_FILE%" -d gnudiff
  del "%GNUDIFF_FILE%"
)

if not exist gnugrep\%GNUGREP_VERSION% (
  wget %WGET_ARGS% "%BASEURL%%GNUGREP_FILE%"
  mkdir gnugrep\%GNUGREP_VERSION%
  echo Unzipping %GNUGREP_FILE%
  unzip %UNZIP_ARGS% "%GNUGREP_FILE%" -d gnugrep\%GNUGREP_VERSION%
  del "%GNUGREP_FILE%"
)

if not exist msys-ssh-1000-18 (
  wget %WGET_ARGS% "%BASEURL%%MSYS_SSH_FILE%"
  mkdir msys-ssh-1000-18
  echo Unzipping %MSYS_SSH_FILE%
  unzip %UNZIP_ARGS% "%MSYS_SSH_FILE%" -d msys-ssh-1000-18
  del "%MSYS_SSH_FILE%"
)

if not exist sumatra\3.1.2 (
  wget %WGET_ARGS% "%BASEURL%sumatrapdf/%SUMATRA_PDF_FILE%"
  mkdir sumatra\3.1.2
  echo Unzipping %SUMATRA_PDF_FILE%
  unzip %UNZIP_ARGS% "%SUMATRA_PDF_FILE%" -d sumatra\3.1.2
  del "%SUMATRA_PDF_FILE%"
)

if not exist winutils\1.0 (
  wget %WGET_ARGS% "%BASEURL%%WINUTILS_FILE%"
  mkdir winutils\1.0
  echo Unzipping %WINUTILS_FILE%
  unzip %UNZIP_ARGS% "%WINUTILS_FILE%" -d winutils\1.0
  del "%WINUTILS_FILE%"
)

if not exist winpty-0.4.3-msys2-2.7.0 (
  wget %WGET_ARGS% "%BASEURL%%WINPTY_FILES%"
  echo Unzipping %WINPTY_FILES%
  unzip %UNZIP_ARGS% "%WINPTY_FILES%"
  del %WINPTY_FILES%
)

if not exist %OPENSSL_FILES:~0,-4% (
  wget %WGET_ARGS% "%BASEURL%%OPENSSL_FILES%"
  echo Unzipping %OPENSSL_FILES%
  unzip %UNZIP_ARGS% "%OPENSSL_FILES%"
  del %OPENSSL_FILES%
)

if not exist %BOOST_FILES:~0,-4%* (
  wget %WGET_ARGS% "%BASEURL%Boost/%BOOST_FILES%"
  echo Unzipping %BOOST_FILES%
  unzip %UNZIP_ARGS% "%BOOST_FILES%"
  del %BOOST_FILES%
)

if not exist %YAML_CPP_FILES:~0,-4%* (
  wget %WGET_ARGS% "%BASEURL%yaml-cpp/%YAML_CPP_FILES%
  unzip %UNZIP_ARGS% "%YAML_CPP_FILES%"
  del %YAML_CPP_FILES%
)

if not exist resource-hacker (
  mkdir resource-hacker
  wget %WGET_ARGS% "%BASEURL%resource-hacker/%RESOURCE_HACKER%
  unzip %UNZIP_ARGS% "%RESOURCE_HACKER%" -d resource-hacker
  del %RESOURCE_HACKER%
)

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

if not exist "nsprocess/%NSIS_NSPROCESS_VERSION%" (
  wget %WGET_ARGS% "%BASEURL%nsprocess/%NSIS_NSPROCESS_FILE%"
  echo Unzipping NSIS NsProcess plugin
  mkdir nsprocess\%NSIS_NSPROCESS_VERSION%
  unzip %UNZIP_ARGS% "%NSIS_NSPROCESS_FILE%" -d nsprocess\1.6
  del %NSIS_NSPROCESS_FILE%
)

pushd ..\common
set CORE_DICTIONARIES=core-dictionaries.zip
if not exist "dictionaries\en_US.dic" (
  wget %WGET_ARGS% "https://s3.amazonaws.com/rstudio-buildtools/dictionaries/%CORE_DICTIONARIES%"
  if exist "%CORE_DICTIONARIES%" (
     mkdir dictionaries
     echo Unzipping %CORE_DICTIONARIES%
     unzip %UNZIP_ARGS% "%CORE_DICTIONARIES%" -d dictionaries
     del "%CORE_DICTIONARIES%"
  )
)

set MATHJAX=mathjax-27.zip
if not exist "mathjax-27" (
  wget %WGET_ARGS% "https://s3.amazonaws.com/rstudio-buildtools/%MATHJAX%"
  if exist "%MATHJAX%" (
     mkdir mathjax-27
     echo Unzipping %MATHJAX%
     unzip %UNZIP_ARGS% "%MATHJAX%"
     del "%MATHJAX%"
  )
)

if not exist pandoc\%PANDOC_VERSION% (
  wget %WGET_ARGS% "%BASEURL%pandoc/%PANDOC_VERSION%/%PANDOC_FILE%"
  echo Unzipping %PANDOC_FILE%
  unzip %UNZIP_ARGS% "%PANDOC_FILE%"
  mkdir pandoc\%PANDOC_VERSION%
  copy "%PANDOC_NAME%\pandoc*" "pandoc\%PANDOC_VERSION%""
  del %PANDOC_FILE%
  rmdir /s /q %PANDOC_NAME%
)



REM wget %WGET_ARGS% https://s3.amazonaws.com/rstudio-buildtools/quarto/%QUARTO_VERSION%/%QUARTO_FILE%
wget %WGET_ARGS% https://github.com/quarto-dev/quarto-cli/releases/download/v%QUARTO_VERSION%/%QUARTO_FILE%
echo Unzipping Quarto %QUARTO_FILE%
rmdir /s /q quarto
mkdir quarto
pushd quarto
unzip %UNZIP_ARGS% ..\%QUARTO_FILE%
popd
del %QUARTO_FILE%


if not exist libclang\%LIBCLANG_VERSION% (
  wget %WGET_ARGS% "%BASEURL%%LIBCLANG_FILE%"
  echo Unzipping %LIBCLANG_FILE%
  unzip %UNZIP_ARGS% "%LIBCLANG_FILE%"
  del %LIBCLANG_FILE%
)

if not exist %NODE_SUBDIR% (
  wget %WGET_ARGS% %NODE_BASE_URL%%NODE_ARCHIVE_FILE%
  echo Unzipping node %NODE_VERSION%
  mkdir %NODE_ROOT%
  unzip %UNZIP_ARGS% %NODE_ARCHIVE_FILE%
  move %NODE_ARCHIVE_DIR% %NODE_SUBDIR%
  del %NODE_ARCHIVE_FILE%
)

set YARN_DIR=%NODE_SUBDIR%\node_modules\yarn\bin
if not exist %YARN_DIR%\yarn (
  echo "Installing yarn"
  call %NODE_SUBDIR%\npm install --global yarn
)

if not defined JENKINS_URL (
  if exist C:\Windows\py.exe (
    pushd ..\..\src\gwt\tools\i18n-helpers\
    py -3 -m venv VENV
    VENV\Scripts\pip install --disable-pip-version-check -r commands.cmd.xml\requirements.txt
    popd
  )
)

cd
echo "Installing panmirror (visual editor)"
pushd ..\windows\install-panmirror
call clone-quarto-repo.cmd
popd
cd

call install-packages.cmd

popd

regsvr32 /s "C:\Program Files (x86)\Microsoft Visual Studio\2019\BuildTools\DIA SDK\bin\msdia140.dll"

call install-crashpad.cmd
call install-soci.cmd
