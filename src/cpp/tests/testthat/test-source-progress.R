#
# test-source-progress.R
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

context("source-progress")

# one-time setup; load progress script
source(file.path(.rs.sessionModulePath(), "SourceWithProgress.R"))

processOutput <- function(output) {
   values <- list()
   for (line in seq_along(output)) {
      result <- regexec("__rs_progress_0__ (\\w+) __ (.*) __rs_progress_1__", output[[line]])
      if (length(result[[1]]) > 2) {
         matches <- regmatches(output[[line]], result)[[1]]
         values[[matches[[2]]]] <- matches[[3]]
      }
   }
   values
}

test_that("statements in script are executed", {
   # create a temporary file to host the output
   p <- tempfile(fileext = ".txt")
   on.exit(file.remove(p), add = TRUE)
   con <- file(p, open = "at")

   sourceWithProgress("resources/source-progress/assignment.R", con)

   expect_equal(2, var)
})

test_that("progress reports include expected values", {
   # create a temporary file to host the output
   p <- tempfile(fileext = ".txt")
   on.exit(file.remove(p), add = TRUE)
   con <- file(p, open = "at")

   sourceWithProgress("resources/source-progress/three.R", con)
   close(con)

   output <- processOutput(readLines(p))

   expect_equal("3", output[["count"]])
   expect_equal("3", output[["statement"]])
   expect_equal("1", output[["completed"]])
})

