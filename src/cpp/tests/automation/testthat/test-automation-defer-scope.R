
library(testthat)

self <- remote <- .rs.automation.newRemote()
withr::defer(.rs.automation.deleteRemote())

# Regression tests for .rs.test() per-test defer scoping.
#
# .rs.test() wraps each test body in local() so that withr::defer()
# fires when the individual test exits, not when the file exits.
#
# The success path uses a test pair (register + verify) to prove
# per-test timing through .rs.test(). The failure and error paths
# exercise local() directly to confirm on.exit behavior without
# producing suite failures.

markerFile <- file.path(tempdir(), "brat-defer-scope-marker.txt")
withr::defer(unlink(markerFile))


# -- Success path --------------------------------------------------------------
# Two-test pair: the first registers a defer, the second verifies it
# fired between tests (not deferred to file exit).

.rs.test("per-test defer: register on success", {
   unlink(markerFile)
   withr::defer(writeLines("success", markerFile))
   expect_true(TRUE)
})

.rs.test("per-test defer: verify fired after success", {
   expect_true(file.exists(markerFile))
   expect_equal(readLines(markerFile), "success")
   unlink(markerFile)
})


# -- Expectation failure path --------------------------------------------------
# testthat records failures without stopping execution, so local()
# exits normally. Verify defer fires alongside a failing expectation.

.rs.test("per-test defer: fires alongside expectation failure", {
   unlink(markerFile)
   local({
      withr::defer(writeLines("failure", markerFile))
      testthat::expect_failure(expect_equal(1L, 2L))
   })
   expect_true(file.exists(markerFile))
   expect_equal(readLines(markerFile), "failure")
   unlink(markerFile)
})


# -- Error path ----------------------------------------------------------------
# stop() causes local() to exit abnormally. on.exit handlers (used by
# withr::defer) fire during stack unwinding before the error is caught.

.rs.test("per-test defer: fires when error exits local scope", {
   unlink(markerFile)
   tryCatch(
      local({
         withr::defer(writeLines("error", markerFile))
         stop("intentional error")
      }),
      error = function(e) {
         expect_match(conditionMessage(e), "intentional error")
      }
   )
   expect_true(file.exists(markerFile))
   expect_equal(readLines(markerFile), "error")
   unlink(markerFile)
})
