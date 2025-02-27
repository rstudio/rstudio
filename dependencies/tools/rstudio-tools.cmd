
::
:: rstudio-tools.cmd -- Batch file toolkit used in dependency scripts
::
:: Copyright (C) 2022 by Posit Software, PBC
::
:: Unless you have received this program directly from Posit Software pursuant
:: to the terms of a commercial license agreement with Posit Software, then
:: this program is licensed to you under the terms of version 3 of the
:: GNU Affero General Public License. This program is distributed WITHOUT
:: ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
:: MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
:: AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
::

::
:: This file provides helpers and subroutines that can be used in other
:: dependency installer scripts. The subroutines in this file can be invoked
:: by using something like:
::
::   call rstudio-tools.cmd <routine> <arguments>
::
:: Alternatively, using a helper variable defined in this script:
::
::  %RUN% <routine> <arguments>
::
:: When called without arguments, the 'initialize' subroutine is invoked,
:: which defines commonly-used variables.
::
:: When using subroutines that have output variables, make sure those usages
:: are expanded with !FOO! instead of %FOO%. %FOO% is expanded at parse time,
:: not use time, and so it's possible that usages of %FOO% in a script could
:: be expanded before the command populating FOO is called.
::
set "RUN=call %0"

if "%1" == "" goto initialize
call :%*
exit /b %ERRORLEVEL%


:initialize

:: Node version used when building the product
set RSTUDIO_NODE_VERSION=22.13.1

:: Node version installed with the product
set RSTUDIO_INSTALLED_NODE_VERSION=20.15.1

:: The base URL where RStudio build tools are available for download.
set RSTUDIO_BUILDTOOLS=https://rstudio-buildtools.s3.amazonaws.com

goto :eof


::
:: Get the basename from a path.
::
:: The second parameter is expected to be an output variable.
::
:basename

setlocal EnableDelayedExpansion

set "_BASENAME=%~nx1"
set "_OUTPUT=%~2"

set "_RESULT=%_BASENAME%"

endlocal & set "%_OUTPUT%=%_RESULT%"

goto :eof


::
:: Get the dirname from a path.
::
:: The second parameter is expected to be an output variable.
::
:dirname

setlocal EnableDelayedExpansion

set "_PATH=%~1"
set "_OUTPUT=%~2"

for %%F in ("%_PATH%") do set _RESULT=%%~dpF

endlocal & set "%_OUTPUT%=%_RESULT:~0,-1%"

goto :eof


::
:: Download a file.
::
:: If the URL passed does not include 'http' as a prefix, then the
:: RSTUDIO_BUILDTOOLS URL above will be prepended as the base URL.
::
:download

setlocal EnableDelayedExpansion

set _URLARG=%~1
if "%_URLARG:~0,4%" == "http" (
  set _URL=%_URLARG%
) else (
  set _URL=%RSTUDIO_BUILDTOOLS%/%_URLARG%
)

echo -- Downloading %_URL%
curl -L -f -C - -O "%_URL%"
if %ERRORLEVEL% neq 0 (
  echo Error downloading %_URL% [exit code %ERRORLEVEL%]
  goto :error
)

endlocal

goto :eof


::
:: Extract an archive.
::
:: Extract an archive (usually a .zip) using 7z.
:: The second parameter is optional; if provided, then the archive
:: contents will be unpacked into that directory.
::
:extract

setlocal EnableDelayedExpansion

set _ARCHIVE=%~1
set _OUTPUT=%~2

if "%QUIET%" == "1" (
  set _QUIET=-bb0
)

if defined _OUTPUT (
  echo -- Extracting %_ARCHIVE% to %_OUTPUT%
  7z x -y %_QUIET% %_ARCHIVE% -o%_OUTPUT%
) else (
  echo -- Extracting %_ARCHIVE%
  7z x -y %_QUIET% %_ARCHIVE%
)

if %ERRORLEVEL% neq 0 (
  echo Error extracting %_ARCHIVE% [exit code %ERRORLEVEL%]
  goto :error
)

endlocal

goto :eof


::
:: Install a dependency.
::
:: This subroutine expects a number of variables to be defined, of the form
::
:: - <dep>_URL    --  The URL from which the dependency can be downloaded.
:: - <dep>_FOLDER --  The name of the folder wherein the dependencies will be unpacked.
:: - <dep>_OUTPUT --  The directory in which the downloaded archive should be unpacked.
::
:: Note that <dep>_OUTPUT is optional; it's provided because some of the archives we
:: download and unpack have their archive contents at the 'root' of the archive, whereas
:: others will have their contents within a sub-folder.
::
:install

setlocal EnableDelayedExpansion

set _NAME=%~1
set _LABEL=!%_NAME%_LABEL!
set _VERSION=!%_NAME%_VERSION!
set _URL=!%_NAME%_URL!
set _FOLDER=!%_NAME%_FOLDER!
set _OUTPUT=!%_NAME%_OUTPUT!

if "%CLEAN%" == "1" (
  rmdir /s /q %_FOLDER%
)

if not defined _LABEL (
  call :tolower "%_NAME%" _NAME_LOWER
  if defined _VERSION (
    set _LABEL=!_NAME_LOWER! !_VERSION!
  ) else (
    set _LABEL=!_NAME_LOWER!
  )
)

if exist %_FOLDER% (
  echo -- !_LABEL! is already installed.
  goto :eof
)

mkdir %_FOLDER%
call :download "%_URL%"
call :basename "%_URL%" _FILE
call :extract "!_FILE!" "!_OUTPUT!"

endlocal

goto :eof


::
:: Convert a string to lowercase.
::
:tolower

setlocal EnableDelayedExpansion

set _STRING=%~1
set _OUTPUT=%~2

set "_STRING=%_STRING:A=a%"
set "_STRING=%_STRING:B=b%"
set "_STRING=%_STRING:C=c%"
set "_STRING=%_STRING:D=d%"
set "_STRING=%_STRING:E=e%"
set "_STRING=%_STRING:F=f%"
set "_STRING=%_STRING:G=g%"
set "_STRING=%_STRING:H=h%"
set "_STRING=%_STRING:I=i%"
set "_STRING=%_STRING:J=j%"
set "_STRING=%_STRING:K=k%"
set "_STRING=%_STRING:L=l%"
set "_STRING=%_STRING:M=m%"
set "_STRING=%_STRING:N=n%"
set "_STRING=%_STRING:O=o%"
set "_STRING=%_STRING:P=p%"
set "_STRING=%_STRING:Q=q%"
set "_STRING=%_STRING:R=r%"
set "_STRING=%_STRING:S=s%"
set "_STRING=%_STRING:T=t%"
set "_STRING=%_STRING:U=u%"
set "_STRING=%_STRING:V=v%"
set "_STRING=%_STRING:W=w%"
set "_STRING=%_STRING:X=x%"
set "_STRING=%_STRING:Y=y%"
set "_STRING=%_STRING:Z=z%"

endlocal && set "%_OUTPUT%=%_STRING%"

goto :eof


:error
endlocal
exit /b %ERRORLEVEL%
