#
# test-source-progress.R
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

   sourceWithProgress(script = path, con = con)
   close(con)

   processOutput(readLines(p))
}

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

test_that("environment import/export works", {
   # create a new environment with a couple of values
   inputEnv <- new.env(parent = emptyenv())
   assign("x", 1, envir = inputEnv)
   assign("y", 2, envir = inputEnv)

   # write the environment to disk
   inputRdata <- tempfile(fileext = ".Rdata")
   on.exit(file.remove(inputRdata), add = TRUE)
   save(list = ls(inputEnv), file = inputRdata, envir = inputEnv)

   # create filename for output
   outputRdata <- tempfile(fileext = ".Rdata")
   on.exit(file.remove(outputRdata), add = TRUE)

   # source the script; it simply adds x and y to make z
   sourceWithProgress(script = "resources/source-progress/assignment.R",
                      con = NULL,
                      importRdata = inputRdata,
                      exportRdata = outputRdata)

   # load results into a new environment
   outputEnv <- new.env(parent = emptyenv())
   load(file = outputRdata, envir = outputEnv)

   # validate results
   expect_equal(3, get("z", envir = outputEnv))
})
