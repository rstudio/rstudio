#
# test-environment.R
#
# Copyright (C) 2020 by RStudio, PBC
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
   # our test-runner sets runAllTests function in the global environment, so initial
   # length is one
   .rs.invokeRpc("set_environment", "R_GlobalEnv")
   expect_equal(length(.rs.invokeRpc("list_environment")), 1)

   # add some items to the global environment
   assign("obj1", 1, envir = globalenv())
   assign("obj2", "two", envir = globalenv())
   assign("obj3", list(1, 2, 3), envir = globalenv())

   # list the global environment
   .rs.invokeRpc("set_environment", "R_GlobalEnv")
   contents <- .rs.invokeRpc("list_environment")

   # verify contents (newly added plus the initial runAllTests function)
   expect_equal(length(contents), 4)
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

test_that("flag must be specified when removing objects", {
   expect_error(.rs.invokeRpc("remove_all_objects"))
})

test_that("all objects are removed when requested", {
   contents <- .rs.invokeRpc("list_environment")
   expect_true(length(contents) > 0)

   .rs.invokeRpc("remove_all_objects", TRUE)

   contents <- .rs.invokeRpc("list_environment")
   expect_equal(length(contents), 0)
})

test_that("functions with backslashes deparse correctly", {
   # character vector with code for a simple function
   code <- "function() { \"first line\\nsecond line\" }"

   # parse and evaluate the expression (yielding a function f)
   eval(parse(text = paste0("f <- ", code)))

   # immediately deparse f back into a string
   output <- .rs.deparseFunction(f, TRUE, TRUE)

   expect_equal(output, code)
})
