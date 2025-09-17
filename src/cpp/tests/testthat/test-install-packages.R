#
# test-packages
#
# Copyright (C) 2022 by Posit Software, PBC
#
# Unless you have received this program directly from Posit Software pursuant
# to the terms of a commercial license agreement with Posit Software, then
# this program is licensed to you under the terms of version 3 of the
# GNU Affero General Public License. This program is distributed WITHOUT
# ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
# MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
# AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
#
#

expect_install <- function(pkgs, ...) {
   
   # create temporary library for installation
   lib <- tempfile("library-")
   dir.create(lib, recursive = TRUE)
   on.exit(unlink(lib, recursive = TRUE), add = TRUE)
   
   # install the package
   install.packages(pkgs, lib = lib, ...)
   
   # check that the package was installed
   installedPkgs <- list.files(lib, full.names = TRUE)
   expect_length(installedPkgs, 1L)
   
   # check that we can read the package DESCRIPTION file
   pkgInfo <- .rs.readPackageDescription(installedPkgs)
   expect_true(is.list(pkgInfo))
   
}

test_that("packages can be installed", {
   
   expect_install("rlang", repos = "https://cloud.r-project.org")
   expect_install("rlang", repos = "https://packagemanager.posit.co/cran/latest")
   
   info <- download.packages("rlang", destdir = tempdir(), type = "binary")
   expect_install(info[1, 2], repos = NULL, type = "binary")

   if (!.rs.platform.isWindows)
   {
      info <- download.packages("rlang", destdir = tempdir(), type = "source")
      expect_install(info[1, 2], repos = NULL, type = "source")
   }
   
})
