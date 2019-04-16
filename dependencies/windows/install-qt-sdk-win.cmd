@echo off

setlocal EnableDelayedExpansion

@rem When updating to a new Qt version, be sure to also update the 
@rem component versions in qt-noninteractive-install-win.qs
set QT_VERSION=5.12.1
set QT_SDK_BINARY=qt-opensource-windows-x86-%QT_VERSION%.exe
set QT_SDK_URL=https://s3.amazonaws.com/rstudio-buildtools/%QT_SDK_BINARY%
set QT_SCRIPT=qt-noninteractive-install-win.qs

call :DetectQt foundQt
if "!foundQt!" == "false" (
  wget -c --no-check-certificate %QT_SDK_URL%
  echo "Installing Qt %QT_VERSION%, this will take a while."
  echo "Ignore warnings about QtAccount credentials."
  echo "Do not click on the setup interface, it is controlled by a script."
  %QT_SDK_BINARY% --script %QT_SCRIPT%
  del %QT_SDK_BINARY%
)
call :DetectQt foundQt
if "!foundQt!" == "false" (
  echo Qt installation failed, please re-run this script to try again.
  echo Or you can manually install with the Qt online installer and select
  echo the 64-bit Visual Studio 2017 and QtWebEngine components of
  echo %QT_VERSION%.
  exit /b 1
) else (
  echo Qt %QT_VERSION% installed.
)
exit /b 0

:DetectQt
set "%~1=true"
set QT_SDK_DIR=C:\Qt%QT_VERSION%
set QT_SDK_DIR2=C:\Qt\Qt%QT_VERSION%
set QT_SDK_DIR3=C:\Qt\%QT_VERSION%
if not exist %QT_SDK_DIR% (
  if not exist %QT_SDK_DIR2% (
    if not exist %QT_SDK_DIR3% (
      set "%~1=false"
    )
  )
)
exit /b 0

