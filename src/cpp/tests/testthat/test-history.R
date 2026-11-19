#
# test-history.R
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

library(testthat)

context("history")

# .rs.savehistory() and .rs.loadhistory() expand '~' in R, and rs_saveHistory /
# rs_loadHistory take the path as given (#19684); FilePath treats "~/..." as a
# relative path. these tests write a temporary file in the home directory.

test_that(".rs.savehistory() expands '~'", {
   file <- file.path("~", basename(tempfile("history-test-")))
   on.exit(unlink(path.expand(file)), add = TRUE)

   .rs.savehistory(file)

   expect_true(file.exists(path.expand(file)))
})

test_that(".rs.loadhistory() expands '~'", {
   skip_if_not(nzchar(Sys.getenv("RSTUDIO_TESTS_HELPER")), "changes the session's console history")
   skip_if(identical(trimws(Sys.getenv("R_HISTSIZE")), "0"), "R_HISTSIZE=0 saves no history")

   input <- file.path("~", basename(tempfile("history-test-")))
   output <- tempfile("history-output-")
   on.exit(unlink(c(path.expand(input), output)), add = TRUE)

   writeLines("history_tilde_test_entry()", path.expand(input))
   .rs.loadhistory(input)
   .rs.savehistory(output)

   expect_identical(readLines(output), "history_tilde_test_entry()")
})

# savehistory() reaches Rsavehistory() on POSIX, and .rs.savehistory() and
# rs_saveHistory on Windows; both must save only the last R_HISTSIZE commands
# (#19690)
test_that("savehistory() saves only the last R_HISTSIZE commands", {
   skip_if_not(nzchar(Sys.getenv("RSTUDIO_TESTS_HELPER")), "changes the session's console history")

   input <- tempfile("history-input-")
   output <- tempfile("history-output-")
   on.exit(unlink(c(input, output)), add = TRUE)

   histsize <- Sys.getenv("R_HISTSIZE", unset = NA)
   on.exit({
      if (is.na(histsize))
         Sys.unsetenv("R_HISTSIZE")
      else
         Sys.setenv(R_HISTSIZE = histsize)
   }, add = TRUE)

   writeLines(c("first()", "second()", "third()"), input)
   loadhistory(input)
   Sys.setenv(R_HISTSIZE = "2")
   savehistory(output)

   expect_identical(readLines(output), c("second()", "third()"))
})

# .rs.savehistory() and .rs.loadhistory() replace utils::savehistory() and
# utils::loadhistory() on Windows; they exist on every platform, so these tests
# call them directly. the error is the system's message for the failure, which
# on Windows is the Win32 text rather than strerror()'s.

test_that(".rs.savehistory() reports a file it can't write", {
   path <- file.path(tempfile("no-such-dir-"), "history.Rhistory")
   expect_error(
      .rs.savehistory(path),
      "No such file or directory|cannot find the path specified"
   )
   expect_false(file.exists(path))
})

test_that(".rs.loadhistory() reports a file it can't read", {
   expect_error(
      .rs.loadhistory(tempfile("no-such-history-")),
      "No such file or directory|cannot find the file specified"
   )
})
