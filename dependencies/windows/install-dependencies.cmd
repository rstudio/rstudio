@echo off

setlocal

set PATH=%PATH%;%CD%\tools

set WGET_ARGS=--no-check-certificate
set UNZIP_ARGS=-q

set BASEURL=https://s3.amazonaws.com/rstudio-buildtools/
set BOOST_FILE=boost-1.44-win.zip
set MINGW_FILE=mingw64-2010-10-03.zip
set GIN_FILE=gin-1.5.zip
set GWT_FILE=gwt-2.3.0.m1.zip

if not exist boost-win (
  wget %WGET_ARGS% "%BASEURL%%BOOST_FILE%"
  unzip %UNZIP_ARGS% "%BOOST_FILE%"
  del "BOOST_FILE%"
)

if not exist mingw64 (
  wget %WGET_ARGS% "%BASEURL%%MINGW_FILE%"
  unzip %UNZIP_ARGS% "%MINGW_FILE%"
  del "%MINGW_FILE%"
)

pushd ..\..\src\gwt\lib

if not exist gin\1.5 (
  wget %WGET_ARGS% "%BASEURL%%GIN_FILE%"
  mkdir gin\1.5
  unzip %UNZIP_ARGS% "%GIN_FILE%" -d gin\1.5
  del "%GIN_FILE%"
)

if not exist gwt\2.3.0-m1 (
  wget %WGET_ARGS% "%BASEURL%%GWT_FILE%"
  unzip %UNZIP_ARGS% "%GWT_FILE%"
  mkdir gwt
  move gwt-2.3.0-m1 gwt\2.3.0-m1
  del "%GWT_FILE%"
)
