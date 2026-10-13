#
# test-terminal.R
#
# Copyright (C) 2022 by Posit Software, PBC
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

test_that("the bash hook script restores the postback dir after profiles reset PATH", {
   
   # the hook script sources ~/.bash_profile, so a scratch HOME stands in for
   # a profile that assigns PATH outright (as Debian's /etc/profile does)
   skip_on_os("windows")
   bash <- Sys.which("bash")
   skip_if(!nzchar(bash), "bash is not available")
   
   home <- tempfile("home-")
   dir.create(home)
   writeLines('PATH="/usr/bin:/bin"; export PATH', file.path(home, ".bash_profile"))
   
   postbackDir <- tempfile("postback-")
   dir.create(postbackDir)
   helper <- file.path(postbackDir, "rstudio")
   writeLines("#!/bin/sh", helper)
   Sys.chmod(helper, "0755")
   
   hookScript <- file.path(
      .Call("rs_rResourcesPath", PACKAGE = "(embedding)"),
      "terminal", "bash", ".bash_profile"
   )
   
   resolved <- system2(
      bash,
      c("--rcfile", shQuote(hookScript), "-i", "-c", shQuote("command -v rstudio")),
      env = c(
         paste0("HOME=", shQuote(home)),
         paste0("PATH=", shQuote(paste(postbackDir, Sys.getenv("PATH"), sep = ":"))),
         paste0("_REALENV=", shQuote("<unset>")),
         paste0("_RS_POSTBACK_DIR=", shQuote(postbackDir))
      ),
      stdout = TRUE,
      stderr = FALSE
   )
   
   expect_equal(resolved, helper)
   
})
