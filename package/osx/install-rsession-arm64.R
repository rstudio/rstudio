
#
# install-rsession-arm64.R
#
# Copyright (C) 2021 by RStudio, PBC
#
# Unless you have received this program directly from RStudio pursuant
# to the terms of a commercial license agreement with RStudio, then
# this program is licensed to you under the terms of version 3 of the
# GNU Affero General Public License. This program is distributed WITHOUT
# ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
# MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
# AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
#
#

# pkgDir <- getwd()
# installDir <- file.path(getwd(), "build-x86_64/_CPack_Packages/Darwin/DragNDrop/RStudio-99.9.9/RStudio.app")

# read command line arguments
args <- commandArgs(trailingOnly = TRUE)
pkgDir <- args[[1L]]
installDir <- args[[2L]]

# copy rsession binary
src <- file.path(pkgDir, "build-arm64/src/cpp/session/rsession")
tgt <- file.path(installDir, "Contents/MacOS/rsession-arm64")
file.copy(src, tgt)

# read the dependencies
system(paste("otool -L", tmp))

# copy arm64 rsession binary into install tree
src <- file.path(pkgDir, "build-arm64/src/cpp/session/rsession")
tgt <- file.path(installDir, "")
