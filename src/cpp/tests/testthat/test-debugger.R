#
# test-debugger.R
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
   time <- system.time(.rs.describeObject("bigcall", environment()))
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

test_that("breakpoints in S7 methods for S7 generics preserve the method class", {

   skip_if_not_installed("S7")
   loadNamespace("S7")

   # define an S7 class, generic, and method in the global environment,
   # as .rs.setBreakpointImpl resolves its function names there
   code <- '
      s7bpclass <- S7::new_class("s7bpclass")
      s7bpgeneric <- S7::new_generic("s7bpgeneric", "x")
      S7::method(s7bpgeneric, s7bpclass) <- function(x, ...) {
         a <- 1
         b <- 2
         a + b
      }
   '
   eval(parse(text = code, keep.source = TRUE), envir = globalenv())
   on.exit(rm(list = c("s7bpclass", "s7bpgeneric"), envir = globalenv()), add = TRUE)

   functionName <- "S7::method(s7bpgeneric, s7bpclass)"
   methodEnvir <- attr(globalenv()$s7bpgeneric, "methods", exact = TRUE)
   original <- methodEnvir$s7bpclass

   # set a breakpoint; the method should be traced, but still look like an
   # S7 method so that the generic remains printable (#18531)
   result <- .rs.setBreakpointImpl(functionName, "", "", "2")
   expect_false(identical(result, FALSE))

   method <- methodEnvir$s7bpclass
   expect_true(.rs.isTraced(method))
   expect_true(inherits(method, "S7_method"))
   expect_true(grepl(".doTrace", paste(deparse(body(method)), collapse = ""), fixed = TRUE))
   expect_error(capture.output(print(globalenv()$s7bpgeneric)), NA)

   # the untraced copy should be a well-formed S7 method
   expect_identical(class(attr(method, "original", exact = TRUE)), class(original))

   # move the breakpoint; the method should be re-traced, not doubly traced
   result <- .rs.setBreakpointImpl(functionName, "", "", "3")
   expect_false(identical(result, FALSE))

   method <- methodEnvir$s7bpclass
   expect_true(.rs.isTraced(method))
   bodyText <- paste(deparse(body(method)), collapse = "")
   matches <- gregexpr(".doTrace", bodyText, fixed = TRUE)[[1L]]
   expect_identical(sum(matches > 0L), 1L)
   expect_error(capture.output(print(globalenv()$s7bpgeneric)), NA)

   # clear the breakpoint; the method should be restored exactly
   result <- .rs.setBreakpointImpl(functionName, "", "", "")
   expect_false(identical(result, FALSE))
   expect_identical(methodEnvir$s7bpclass, original)

   # a trace() failure should leave the method untouched
   expect_error(.rs.setBreakpointImpl(functionName, "", "", "99"))
   expect_identical(methodEnvir$s7bpclass, original)

})
