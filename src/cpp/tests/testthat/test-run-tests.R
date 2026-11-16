#
# test-run-tests.R
#
# Copyright (C) 2026 by Posit Software, PBC
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

context("run-tests")

# rstudio-tests reads the count runTests() writes to testthat-failures.log to
# decide whether the 'r' scope passed, so errors must be counted too (#13980).
test_that("runTests() counts tests that error as failures", {

   testDir <- tempfile("run-tests-fixtures-")
   outputDir <- tempfile("run-tests-output-")
   dir.create(testDir)
   dir.create(outputDir)
   on.exit(unlink(c(testDir, outputDir), recursive = TRUE), add = TRUE)

   fixtures <- list(
      "test-pass.R"     = 'test_that("passes", expect_true(TRUE))',
      "test-skip.R"     = 'test_that("skips", skip("skipped"))',
      "test-warn.R"     = 'test_that("warns", { warning("warned"); expect_true(TRUE) })',
      "test-fail.R"     = 'test_that("fails", expect_true(FALSE))',
      "test-error.R"    = 'test_that("errors", stop("boom"))',
      "test-toplevel.R" = 'stop("boom outside test_that()")'
   )

   for (name in names(fixtures))
      writeLines(fixtures[[name]], file.path(testDir, name))

   # runTests() resets this option on exit; keep the outer suite's value
   op <- options("rstudio.tests.running")
   on.exit(options(op), add = TRUE)

   env <- new.env(parent = globalenv())
   sys.source(test_path("run-tests.R"), envir = env)

   # an explicit empty filter keeps the outer run's TESTTHAT_FILTER out
   capture.output(env$runTests(testDir = testDir, outputDir = outputDir, filter = ""))

   # one failed expectation, one test that errors, one file that errors
   failures <- readLines(file.path(outputDir, "testthat-failures.log"), warn = FALSE)
   expect_identical(failures, "3")

})
