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

# Runs runTests() over a fresh folder holding the given fixtures. Returns the
# count it logged (NA if it wrote no log) and the run's output, so a failed
# expectation can show which fixture was miscounted.
runFixtures <- function(fixtures) {

   testDir <- tempfile("run-tests-fixtures-")
   outputDir <- tempfile("run-tests-output-")
   dir.create(testDir)
   dir.create(outputDir)
   on.exit(unlink(c(testDir, outputDir), recursive = TRUE), add = TRUE)

   for (name in names(fixtures))
      writeLines(fixtures[[name]], file.path(testDir, name))

   # runTests() resets this option on exit; keep the outer suite's value
   op <- options("rstudio.tests.running")
   on.exit(options(op), add = TRUE)

   env <- new.env(parent = globalenv())
   sys.source(test_path("run-tests.R"), envir = env)

   # an explicit empty filter keeps the outer run's TESTTHAT_FILTER out
   output <- capture.output(env$runTests(testDir = testDir, outputDir = outputDir, filter = ""))

   logFile <- file.path(outputDir, "testthat-failures.log")
   failures <- NA_character_
   if (file.exists(logFile))
      failures <- readLines(logFile, warn = FALSE)

   list(failures = failures, output = paste(output, collapse = "\n"))

}

test_that("runTests() does not count passing, skipped, or warning tests", {

   run <- runFixtures(list(
      "test-pass.R" = 'test_that("passes", expect_true(TRUE))',
      "test-skip.R" = 'test_that("skips", skip("skipped"))',
      "test-warn.R" = 'test_that("warns", { warning("warned"); expect_true(TRUE) })'
   ))

   expect_identical(run$failures, "0", info = run$output)

})

# rstudio-tests reads the count runTests() writes to testthat-failures.log to
# decide whether the 'r' scope passed, so every kind of failure must reach it
# (#13980). Each fixture runs alone, so one miscount can't hide another.
test_that("runTests() counts each failing test once", {

   fixtures <- list(
      "test-fail.R"       = 'test_that("fails", { expect_true(FALSE); expect_true(FALSE) })',
      "test-error.R"      = 'test_that("errors", stop("boom"))',
      "test-fail-error.R" = 'test_that("fails, then errors", { expect_true(FALSE); stop("boom") })',
      "test-toplevel.R"   = 'stop("boom outside test_that()")',
      "test-parse.R"      = 'test_that("does not parse", {'
   )

   for (name in names(fixtures))
   {
      run <- runFixtures(fixtures[name])
      expect_identical(run$failures, "1", info = paste0(name, "\n", run$output))
   }

})
