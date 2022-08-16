#
# test-reticulate.R
#
# Copyright (C) 2022 by Posit, PBC
#
# Unless you have received this program directly from Posit pursuant
# to the terms of a commercial license agreement with Posit, then
# this program is licensed to you under the terms of version 3 of the
# GNU Affero General Public License. This program is distributed WITHOUT
# ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
# MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
# AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
#
#

context("reticulate")

test_that("RETICULATE_PYTHON environment variable is respected", {
   # save old value and set a dummy value
   oldPython <- Sys.getenv("RETICULATE_PYTHON")
   Sys.setenv(RETICULATE_PYTHON = "/opt/testthat/python")
   on.exit({
      # restore old value
      Sys.setenv(RETICULATE_PYTHON = oldPython)
   }, add = TRUE)

   # perform autodiscovery
   python <- .rs.inferReticulatePython()

   # we expect that since we set a custom value it'll be reflected
   expect_equal(python, Sys.getenv("RETICULATE_PYTHON"))
})