@echo off

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


:: Node version used when building the product
set RSTUDIO_NODE_VERSION=22.13.1

:: Node version installed with the product
set RSTUDIO_INSTALLED_NODE_VERSION=20.15.1

set RSTUDIO_BUILDTOOLS=https://rstudio-buildtools.s3.amazonaws.com