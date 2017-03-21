@echo off

setlocal

set PATH=%CD%\tools;%PATH%

set WGET_ARGS=-c --no-check-certificate
set UNZIP_ARGS=-q

set BASEURL=https://s3.amazonaws.com/rstudio-buildtools/
set BOOST_GCC493_FILE=boost-1.63-win-rtools33-gcc493.zip
set RTOOLS_FILE=Rtools33.exe
set GIN_FILE=gin-1.5.zip
set GWT_FILE=gwt-2.7.0.zip
set SELENIUM_FILE=selenium-java-2.37.0.zip
set SELENIUM_SERVER_FILE=selenium-server-standalone-2.37.0.jar
set JUNIT_FILE=junit-4.9b3.jar
set GNUDIFF_FILE=gnudiff.zip
set GNUGREP_FILE=gnugrep-2.5.4.zip
set MSYS_SSH_FILE=msys-ssh-1000-18.zip
set SUMATRA_PDF_FILE=SumatraPDF-3.1.1.zip
set WINUTILS_FILE=winutils-1.0.zip
set WINPTY_FILES=winpty-0.4.2-msys2-2.6.0.zip

set PANDOC_VERSION=1.17.2
set PANDOC_NAME=pandoc-%PANDOC_VERSION%
set PANDOC_FILE=%PANDOC_NAME%.zip

set LIBCLANG_VERSION=3.4
set LIBCLANG_NAME=libclang-%LIBCLANG_VERSION%
set LIBCLANG_FILE=%LIBCLANG_NAME%.zip
set LIBCLANG_HEADERS=builtin-headers
set LIBCLANG_HEADERS_FILE=libclang-%LIBCLANG_HEADERS%.zip

set RTOOLS_DIR=%RTOOLS_FILE:.exe=%
if not exist %RTOOLS_DIR% (
  wget %WGET_ARGS% "%BASEURL%%RTOOLS_FILE%"
  echo Installing '%RTOOLS_FILE%' -- please wait...
  start /wait Rtools33.exe /verysilent /dir=%CD%\%RTOOLS_DIR%
  if %ERRORLEVEL% EQU 0 (
    echo '%RTOOLS_FILE%' successfully installed!
    del %RTOOLS_FILE%
  ) else (
    echo Failed to install '%RTOOLS_FILE%'!
  )
)

if not exist boost-1.63-win-rtools33-gcc493 (
  wget %WGET_ARGS% "%BASEURL%%BOOST_GCC493_FILE%"
  echo Unzipping %BOOST_GCC493_FILE%
  unzip %UNZIP_ARGS% "%BOOST_GCC493_FILE%" -d boost-1.63-win-rtools33-gcc493
  del "%BOOST_GCC493_FILE%"
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

if not exist msys-ssh-1000-18 (
  wget %WGET_ARGS% "%BASEURL%%MSYS_SSH_FILE%"
  mkdir msys-ssh-1000-18
  echo Unzipping %MSYS_SSH_FILE%
  unzip %UNZIP_ARGS% "%MSYS_SSH_FILE%" -d msys-ssh-1000-18
  del "%MSYS_SSH_FILE%"
)

if not exist sumatra\3.1.1 (
  wget %WGET_ARGS% "%BASEURL%%SUMATRA_PDF_FILE%"
  mkdir sumatra\3.1.1
  echo Unzipping %SUMATRA_PDF_FILE%
  unzip %UNZIP_ARGS% "%SUMATRA_PDF_FILE%" -d sumatra\3.1.1
  del "%SUMATRA_PDF_FILE%"
)

if not exist winutils\1.0 (
  wget %WGET_ARGS% "%BASEURL%%WINUTILS_FILE%"
  mkdir winutils\1.0
  echo Unzipping %WINUTILS_FILE%
  unzip %UNZIP_ARGS% "%WINUTILS_FILE%" -d winutils\1.0
  del "%WINUTILS_FILE%"
)

if not exist winpty-0.4.2-msys2-2.6.0 (
  wget %WGET_ARGS% "%BASEURL%%WINPTY_FILES%"
  echo Unzipping %WINPTY_FILES%
  unzip %UNZIP_ARGS% "%WINPTY_FILES%"
  del %WINPTY_FILES%
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

if not exist gwt\2.7.0 (
  wget %WGET_ARGS% "%BASEURL%%GWT_FILE%"
  echo Unzipping %GWT_FILE%
  unzip %UNZIP_ARGS% "%GWT_FILE%"
  mkdir gwt
  move gwt-2.7.0 gwt\2.7.0
  del "%GWT_FILE%"
)

if not exist selenium\2.37.0 (
  wget %WGET_ARGS% "%BASEURL%%SELENIUM_FILE%"
  echo Unzipping %SELENIUM_FILE%
  unzip %UNZIP_ARGS% "%SELENIUM_FILE%"
  mkdir selenium
  move selenium-2.37.0 selenium\2.37.0
  del "%SELENIUM_FILE%"
)

if not exist selenium\2.37.0\%SELENIUM_SERVER_FILE% (
  wget %WGET_ARGS% "%BASEURL%%SELENIUM_SERVER_FILE%"
  move %SELENIUM_SERVER_FILE% selenium\2.37.0
)

if not exist selenium\chromedriver\2.7 (
  wget %WGET_ARGS% "%BASEURL%chromedriver-win.exe"
  mkdir selenium\chromedriver\2.7
  move chromedriver-win.exe selenium\chromedriver\2.7
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

set MATHJAX=mathjax-26.zip
if not exist "mathjax-26" (
  wget %WGET_ARGS% "https://s3.amazonaws.com/rstudio-buildtools/%MATHJAX%"
  if exist "%MATHJAX%" (
     mkdir mathjax-26
     echo Unzipping %MATHJAX%
     unzip %UNZIP_ARGS% "%MATHJAX%"
     del "%MATHJAX%"
  )
)

if not exist pandoc\%PANDOC_VERSION% (
  wget %WGET_ARGS% "%BASEURL%%PANDOC_FILE%"
  echo Unzipping %PANDOC_FILE%
  unzip %UNZIP_ARGS% "%PANDOC_FILE%"
  mkdir pandoc\%PANDOC_VERSION%
  copy "%PANDOC_NAME%\windows\pandoc*" "pandoc\%PANDOC_VERSION%""
  del %PANDOC_FILE%
  rmdir /s /q %PANDOC_NAME%
)

if not exist libclang\%LIBCLANG_VERSION% (
  wget %WGET_ARGS% "%BASEURL%%LIBCLANG_FILE%"
  echo Unzipping %LIBCLANG_FILE%
  unzip %UNZIP_ARGS% "%LIBCLANG_FILE%"
  mkdir libclang\%LIBCLANG_VERSION%
  xcopy /s "%LIBCLANG_NAME%\windows\mingw" "libclang\%LIBCLANG_VERSION%"
  del %LIBCLANG_FILE%
  rmdir /s /q %LIBCLANG_NAME%
)

if not exist libclang\%LIBCLANG_HEADERS% (
  wget %WGET_ARGS% "%BASEURL%%LIBCLANG_HEADERS_FILE%"
  echo Unzipping %LIBCLANG_HEADERS%
  unzip %UNZIP_ARGS% "%LIBCLANG_HEADERS_FILE%" -d libclang
  del %LIBCLANG_HEADERS_FILE%
)



install-packages.cmd

popd




