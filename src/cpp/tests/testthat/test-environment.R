#
# test-environment.R
#
# Copyright (C) 2009-18 by RStudio, Inc.
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

context("environment")

test_that("environment object listings are correct", {
   # add some items to the global environment
   assign("obj1", 1, envir = globalenv())
   assign("obj2", "two", envir = globalenv())
   assign("obj3", list(1, 2, 3), envir = globalenv())

   # list the global environment
   contents <- .rs.invokeRpc("list_environment")

   # verify contents
   obj1 <- contents[[1]]
   expect_equal(obj1[["name"]], "obj1")
   expect_equal(obj1[["value"]], "1")
   obj2 <- contents[[2]]
   expect_equal(obj2[["name"]], "obj2")
   expect_equal(obj2[["value"]], "\"two\"")
   obj3 <- contents[[3]]
   expect_equal(obj3[["name"]], "obj3")
   expect_equal(obj3[["length"]], 3)
})
