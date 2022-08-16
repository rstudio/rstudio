#
# test-debugger.R
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

library(testthat)

context("debugger")

test_that("deparsing large calls is not overly expensive", {
   
   # call including large data.frame
   big <- data.frame(x = as.numeric(1:1E5))
   cl <- call("dummy", big)
   summary <- .rs.callSummary(cl)
   expect_true(nchar(summary) < 1000)
   
   # call including large vector
   data <- as.list(0:200)
   data[[1L]] <- as.name("c")
   cl <- as.call(data)
   summary <- .rs.callSummary(cl)
   expect_equal(summary, "c(...)")
   
   # call including large data.frame should be described quickly
   big <- mtcars[rep.int(1, 1E5)]
   bigcall <- call("dummy", x = big)
   time <- system.time(.rs.describeObject(environment(), "bigcall"))
   expect_true(time[1] < 1)
   
})

test_that("we successfully parse the function name from different calls", {
   
   # regular old call
   cl <- call("eval", quote(1 + 1))
   expect_equal(.rs.functionNameFromCall(cl), "eval")
   
   # function directly in call object
   cl <- quote(c(1, 2, 3))
   cl[[1L]] <- c
   expect_equal(.rs.functionNameFromCall(cl), "[Anonymous function]")
   
})

test_that("function calls are not mangled into something un-printable", {
   
   cl <- call("function", pairlist(a = 1, b = 2, c = 3), quote({}))
   sanitized <- .rs.sanitizeCall(cl)
   expect_equal(cl, sanitized)
   
})
