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

# helper method; splits a CSV line on the first comma
splitLine <- function(line) {
   idx <- regexpr(",", line, fixed = TRUE)
   if (idx < 0) {
      # no matches, just return the whole line
      line
   } else {
      # split at the first comma
      c(substring(line, 1, idx - 1), 
        substring(line, idx + 1))
   }
}

test_that("statements in script are executed", {
   p <- tempfile(fileext = ".csv")
   on.exit(file.remove(p), add = TRUE)
   sourceWithProgress("resources/source-progress/assignment.R", p)

   expect_equal(2, var)
})

test_that("progress reports include expected values", {
   p <- tempfile(fileext = ".csv")
   on.exit(file.remove(p), add = TRUE)
   sourceWithProgress("resources/source-progress/three.R", p)

   # parse output
   lines <- readLines(p)
   output <- list()
   for (line in lines) {
      contents <- splitLine(line)
      output[[contents[[1]]]] <- as.numeric(contents[[2]])
   }

   expect_equal(3, output[["count"]])
   expect_equal(3, output[["statement"]])
   expect_equal(1, output[["completed"]])
})

