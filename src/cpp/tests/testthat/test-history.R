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
# call them directly. only failures are exercised, so the session's own history
# is never written out.

test_that(".rs.savehistory() reports a file it can't write", {
   path <- file.path(tempfile("no-such-dir-"), "history.Rhistory")
   expect_error(.rs.savehistory(path))
   expect_false(file.exists(path))
})

test_that(".rs.loadhistory() reports a file it can't read", {
   expect_error(.rs.loadhistory(tempfile("no-such-history-")))
})
