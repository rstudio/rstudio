#
# test-terminal.R
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

context("terminal")

test_that("terminal can be started and stopped", {
   
   terminalCountBefore <- length(.rs.api.terminalList())
   expect_gte(terminalCountBefore, 0)
   
   termId <- .rs.api.terminalCreate(caption = "Test Terminal", show = TRUE)
   while (!.rs.api.terminalRunning(termId)) {
      Sys.sleep(0.1)
   }
   expect_length(termId, 1)
   
   terminalCountAfter <- length(.rs.api.terminalList())
   expect_equal(terminalCountAfter, terminalCountBefore + 1)
   
   .rs.api.terminalKill(termId)
   terminalCountAfter <- length(.rs.api.terminalList())
   expect_equal(terminalCountAfter, terminalCountBefore)
})
