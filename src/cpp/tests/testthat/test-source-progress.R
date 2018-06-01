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

# given the output of an R script, extract the progress markers and build a list with name/value
# pairs for each one (note that this will capture only the last value emitted when multiple markers
# with the same category exist)
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

# sources the given script with progress and returns the value of all emitted progress markers
progressResult <- function(path) {
   p <- tempfile(fileext = ".txt")
   on.exit(file.remove(p), add = TRUE)
   con <- file(p, open = "at")

   sourceWithProgress(path, con)
   close(con)

   processOutput(readLines(p))
}

test_that("statements in script are executed", {
   progressResult("resources/source-progress/assignment.R")

   expect_equal(2, var)
})

test_that("progress reports include expected values", {
   output <- progressResult("resources/source-progress/three.R")

   expect_equal("3", output[["count"]])
   expect_equal("3", output[["statement"]])
   expect_equal("1", output[["completed"]])
})


test_that("sections in source are emitted as status", {
   output <- progressResult("resources/source-progress/sections.R")

   expect_equal("Compute", output[["section"]])
})
