::
:: codesign.cmd
::
:: Copyright (C) 2026 by Posit Software, PBC
::
:: Unless you have received this program directly from Posit Software pursuant
:: to the terms of a commercial license agreement with Posit Software, then
:: this program is licensed to you under the terms of version 3 of the
:: GNU Affero General Public License. This program is distributed WITHOUT
:: ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
:: MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
:: AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
::
:: Usage: codesign.cmd FILE...
::
:: Signs FILE... (wildcards allowed) with Posit's code signing certificate, then
:: verifies the signatures. Runs in the signing environment the Jenkins Windows
:: build sets up: signtool.exe on PATH (VsDevCmd.bat), the DigiCert KSP
:: certificates synced, KEYPAIR_ALIAS set, and the certificate in WORKSPACE.
::
@echo off
setlocal

if "%~1" == "" (
    echo codesign.cmd: no files given. Usage: codesign.cmd FILE...
    exit /b 1
)
if not defined KEYPAIR_ALIAS (
    echo codesign.cmd: KEYPAIR_ALIAS is not set. Run this from a Jenkins build that binds the signing credentials.
    exit /b 1
)
if not exist "%WORKSPACE%\cert.pem" (
    echo codesign.cmd: "%WORKSPACE%\cert.pem" not found. The Prepare Signing Tools stage downloads it; check that it ran.
    exit /b 1
)

signtool.exe sign /csp "DigiCert Signing Manager KSP" /kc "%KEYPAIR_ALIAS%" /f "%WORKSPACE%\cert.pem" /tr http://timestamp.digicert.com /td SHA256 /fd SHA256 %* || (
    echo codesign.cmd: signing failed for %*
    exit /b 1
)
signtool.exe verify /pa %* || (
    echo codesign.cmd: signature verification failed for %*
    exit /b 1
)
