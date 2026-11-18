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

# .rs.savehistory() and .rs.loadhistory() replace utils::savehistory() and
# utils::loadhistory() on Windows; they exist on every platform, so these tests
# call them directly. the first test replaces the session's history with a
# synthetic one, so only that is ever written out. tests that change the
# session's history only run in the rstudio-tests runner's disposable session.

test_that(".rs.loadhistory() and .rs.savehistory() round-trip a history file", {
   skip_if_not(nzchar(Sys.getenv("RSTUDIO_TESTS_HELPER")), "changes the session's console history")
   skip_if(identical(trimws(Sys.getenv("R_HISTSIZE")), "0"), "R_HISTSIZE keeps no history")

   input <- tempfile("history-input-")
   output <- tempfile("history-output-")
   on.exit(unlink(c(input, output)), add = TRUE)

   writeLines("history_test_entry()", input)
   .rs.loadhistory(input)
   .rs.savehistory(output)

   expect_identical(readLines(output), "history_test_entry()")
})

test_that(".rs.savehistory() reports a file it can't write", {
   path <- file.path(tempfile("no-such-dir-"), "history.Rhistory")
   expect_error(.rs.savehistory(path))
   expect_false(file.exists(path))
})

test_that(".rs.loadhistory() reports a file it can't read", {
   # a failed load can empty the session's history (#19686)
   skip_if_not(nzchar(Sys.getenv("RSTUDIO_TESTS_HELPER")), "changes the session's console history")
   expect_error(.rs.loadhistory(tempfile("no-such-history-")))
})
