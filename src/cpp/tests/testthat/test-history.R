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
   skip_if(identical(trimws(Sys.getenv("R_HISTSIZE")), "0"), "R_HISTSIZE keeps no history")

   input <- file.path("~", basename(tempfile("history-test-")))
   output <- tempfile("history-output-")
   on.exit(unlink(c(path.expand(input), output)), add = TRUE)

   writeLines("history_tilde_test_entry()", path.expand(input))
   .rs.loadhistory(input)
   .rs.savehistory(output)

   expect_identical(readLines(output), "history_tilde_test_entry()")
})
