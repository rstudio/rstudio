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

# Runs the bash terminal hook script with a scratch HOME whose .bash_profile
# contains 'profile', and reports the resulting PATH, whether the helper
# variable was unset, and where 'rstudio' resolves. Only sentinel-prefixed
# lines are read back, since the host's own /etc/profile may print to stdout
# (Ubuntu's bash.bashrc sudo hint, for example).
runBashHookScript <- function(profile, path, postbackDir) {
   
   home <- tempfile("home-")
   dir.create(home)
   on.exit(unlink(home, recursive = TRUE), add = TRUE)
   writeLines(profile, file.path(home, ".bash_profile"))
   
   hookScript <- file.path(
      .Call("rs_rResourcesPath", PACKAGE = "(embedding)"),
      "terminal", "bash", ".bash_profile"
   )
   
   script <- paste(
      'echo "RS_PATH=${PATH}"',
      'echo "RS_POSTBACK_DIR=${_RS_POSTBACK_DIR-unset}"',
      'echo "RS_RESOLVED=$(command -v rstudio)"',
      sep = "; "
   )
   
   output <- system2(
      Sys.which("bash"),
      c("--rcfile", shQuote(hookScript), "-i", "-c", shQuote(script)),
      env = c(
         paste0("HOME=", shQuote(home)),
         paste0("PATH=", shQuote(path)),
         paste0("_REALENV=", shQuote("<unset>")),
         paste0("_RS_POSTBACK_DIR=", shQuote(postbackDir))
      ),
      stdout = TRUE,
      stderr = FALSE
   )
   
   keys <- c(path = "RS_PATH=", postbackDir = "RS_POSTBACK_DIR=", resolved = "RS_RESOLVED=")
   lapply(keys, function(key) {
      line <- output[startsWith(output, key)]
      substring(line, nchar(key) + 1L)
   })
   
}

countPathEntries <- function(path, dir) {
   sum(strsplit(path, ":", fixed = TRUE)[[1]] == dir)
}

test_that("the bash hook script keeps the postback dir first on the PATH", {
   
   skip_on_os("windows")
   skip_if(!nzchar(Sys.which("bash")), "bash is not available")
   
   postbackDir <- tempfile("postback-")
   dir.create(postbackDir)
   on.exit(unlink(postbackDir, recursive = TRUE), add = TRUE)
   helper <- file.path(postbackDir, "rstudio")
   writeLines("#!/bin/sh", helper)
   Sys.chmod(helper, "0755")
   
   # the session prepends the postback dir before the shell starts
   path <- paste(postbackDir, Sys.getenv("PATH"), sep = ":")
   
   # a profile that assigns PATH outright (as Debian's /etc/profile does)
   result <- runBashHookScript('PATH="/usr/bin:/bin"; export PATH', path, postbackDir)
   expect_equal(result$resolved, helper)
   expect_equal(countPathEntries(result$path, postbackDir), 1L)
   expect_true(startsWith(result$path, paste0(postbackDir, ":")))
   expect_equal(result$postbackDir, "unset")
   
   # a profile that leaves PATH alone: no second copy is added
   result <- runBashHookScript("", path, postbackDir)
   expect_equal(result$resolved, helper)
   expect_equal(countPathEntries(result$path, postbackDir), 1L)
   expect_true(startsWith(result$path, paste0(postbackDir, ":")))
   expect_equal(result$postbackDir, "unset")
   
   # a profile that moves the dir to the end (as macOS path_helper does)
   profile <- sprintf('PATH="/usr/bin:/bin:%s"; export PATH', postbackDir)
   result <- runBashHookScript(profile, path, postbackDir)
   expect_equal(result$resolved, helper)
   expect_equal(countPathEntries(result$path, postbackDir), 1L)
   expect_true(startsWith(result$path, paste0(postbackDir, ":")))
   
   # a profile that empties PATH: no trailing empty entry (the current dir)
   result <- runBashHookScript('PATH=""; export PATH', path, postbackDir)
   expect_equal(result$resolved, helper)
   expect_equal(result$path, postbackDir)
   
})
