@echo off

setlocal

set PATH=%PATH%;%CD%\tools

set WGET_ARGS=--no-check-certificate
set UNZIP_ARGS=-q

set BASEURL=https://s3.amazonaws.com/rstudio-buildtools/
set BOOST_FILE=boost-1.53-win.zip
set MINGW_FILE=mingw64-2010-10-03.zip
set GIN_FILE=gin-1.5.zip
set GWT_FILE=gwt-2.5.0.rc1.zip
set JUNIT_FILE=junit-4.9b3.jar
set GNUDIFF_FILE=gnudiff.zip
set GNUGREP_FILE=gnugrep-2.5.4.zip
set MSYS_SSH_FILE=msys_ssh.zip
set SUMATRA_PDF_FILE=SumatraPDF-2.1.1.zip

if not exist boost-1.53-win (
  wget %WGET_ARGS% "%BASEURL%%BOOST_FILE%"
  echo Unzipping %BOOST_FILE%
  unzip %UNZIP_ARGS% "%BOOST_FILE%"
  del "%BOOST_FILE%"
)

if not exist mingw64 (
  wget %WGET_ARGS% "%BASEURL%%MINGW_FILE%"
  echo Unzipping %MINGW_FILE%
  unzip %UNZIP_ARGS% "%MINGW_FILE%"
  del "%MINGW_FILE%"
)

if not exist gnudiff (
  wget %WGET_ARGS% "%BASEURL%%GNUDIFF_FILE%"
  mkdir gnudiff
  echo Unzipping %GNUDIFF_FILE%
  unzip %UNZIP_ARGS% "%GNUDIFF_FILE%" -d gnudiff
  del "%GNUDIFF_FILE%"
)

if not exist gnugrep (
  wget %WGET_ARGS% "%BASEURL%%GNUGREP_FILE%"
  mkdir gnugrep
  echo Unzipping %GNUGREP_FILE%
  unzip %UNZIP_ARGS% "%GNUGREP_FILE%" -d gnugrep
  del "%GNUGREP_FILE%"
)

if not exist msys_ssh (
  wget %WGET_ARGS% "%BASEURL%%MSYS_SSH_FILE%"
  mkdir msys_ssh
  echo Unzipping %MSYS_SSH_FILE%
  unzip %UNZIP_ARGS% "%MSYS_SSH_FILE%" -d msys_ssh
  del "%MSYS_SSH_FILE%"
)

if not exist sumatra\2.1.1 (
  wget %WGET_ARGS% "%BASEURL%%SUMATRA_PDF_FILE%"
  mkdir sumatra\2.1.1
  echo Unzipping %SUMATRA_PDF_FILE%
  unzip %UNZIP_ARGS% "%SUMATRA_PDF_FILE%" -d sumatra\2.1.1
  del "%SUMATRA_PDF_FILE%"
)

if not exist ..\..\src\gwt\lib (
  mkdir ..\..\src\gwt\lib
)
pushd ..\..\src\gwt\lib

if not exist gin\1.5 (
  wget %WGET_ARGS% "%BASEURL%%GIN_FILE%"
  mkdir gin\1.5
  echo Unzipping %GIN_FILE%
  unzip %UNZIP_ARGS% "%GIN_FILE%" -d gin\1.5
  del "%GIN_FILE%"
)

if not exist gwt\2.5.0.rc1 (
  wget %WGET_ARGS% "%BASEURL%%GWT_FILE%"
  echo Unzipping %GWT_FILE%
  unzip %UNZIP_ARGS% "%GWT_FILE%"
  mkdir gwt
  move gwt-2.5.0.rc1 gwt\2.5.0.rc1
  del "%GWT_FILE%"
)

if not exist %JUNIT_FILE% (
  wget %WGET_ARGS% "%BASEURL%%JUNIT_FILE%"
)

popd

pushd ..\common
set CORE_DICTIONARIES=core-dictionaries.zip
if not exist "dictionaries\en_US.dic" (
  wget %WGET_ARGS% "https://s3.amazonaws.com/rstudio-dictionaries/%CORE_DICTIONARIES%"
  if exist "%CORE_DICTIONARIES%" (
     mkdir dictionaries
     echo Unzipping %CORE_DICTIONARIES%
     unzip %UNZIP_ARGS% "%CORE_DICTIONARIES%" -d dictionaries
     del "%CORE_DICTIONARIES%"
  )
)

set MATHJAX=mathjax-20.zip
if not exist "mathjax" (
  wget %WGET_ARGS% "https://s3.amazonaws.com/rstudio-buildtools/%MATHJAX%"
  if exist "%MATHJAX%" (
     mkdir mathjax
     echo Unzipping %MATHJAX%
     unzip %UNZIP_ARGS% "%MATHJAX%"
     del "%MATHJAX%"
  )
)

