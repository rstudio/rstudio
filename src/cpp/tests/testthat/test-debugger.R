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

test_that("frames whose inner call is built internally keep their call site", {

   # lapply() constructs the call to FUN itself, so that call carries no
   # source reference and the lapply() frame has no location of its own.
   # It falls back to the source reference of the lapply() call, which is
   # what keeps the frame navigable from the traceback.
   file <- tempfile(pattern = "frames-", fileext = ".R")
   on.exit(unlink(file), add = TRUE)

   writeLines(c(
      "outer_fn <- function() {",
      "   lapply(1, function(i) inner_fn())",
      "}",
      "inner_fn <- function() {",
      "   .rs.callFrames(targetDepth = 1L)",
      "}"
   ), file)

   env <- new.env(parent = globalenv())
   source(file, local = env, keep.source = TRUE)

   # lapply() wraps its result, so unwrap before reading the frame list.
   # Outside a debug session rs_getBrowserEnv has no environment to report,
   # which warns on the way back through the embedding layer.
   frames <- suppressWarnings(env$outer_fn())[[1L]]$frames
   names <- vapply(frames, function(frame) as.character(frame$function_name), "")

   # Frames run innermost first, and the test harness has lapply() frames of
   # its own further out, so ours is the first match. The file name assertion
   # below confirms we picked it.
   index <- which(names == "lapply")[1L]
   expect_false(is.na(index))
   expect_true(as.logical(frames[[index]]$real_sourceref))
   expect_match(as.character(frames[[index]]$file_name), basename(file), fixed = TRUE)

})

test_that("debugSourceRef only trusts the runtime srcref for located functions", {

   runtimeRef <- c(10L, 1L, 10L, 20L, 1L, 20L)

   # A function carrying its own source may use the evaluator's position.
   located <- eval(parse(
      text = "function(ref) .rs.debugSourceRef(1L, ref, NULL)",
      keep.source = TRUE
   ))

   expect_equal(located(runtimeRef)$srcref, runtimeRef)

   # R_GetCurrentSrcref would hand a function without source references its
   # caller's location instead (#18754). With nothing left to simulate from,
   # the location is reported as unknown so that the client can leave the
   # debug highlight alone, rather than moving it to line zero.
   unlocated <- eval(parse(
      text = "function(ref) .rs.debugSourceRef(1L, ref, NULL)",
      keep.source = FALSE
   ))

   expect_null(unlocated(runtimeRef))

   # Depths past the end of the call stack resolve to nothing.
   expect_null(.rs.debugSourceRef(1000L, runtimeRef, NULL))

})
