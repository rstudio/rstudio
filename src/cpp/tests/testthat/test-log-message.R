#
# test-log-message.R
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

context("log message helpers")

# Captures arguments passed to .rs.logMessageEmit during 'expr'. The full
# .rs.log*Message -> .rs.logMessageImpl -> .rs.logMessageEmit chain runs
# normally, but the final .Call is intercepted so we can assert on what
# would have been logged.
withCapturedLogs <- function(expr) {
   captured <- list()
   rsEnv <- as.environment("tools:rstudio")
   original <- get(".rs.logMessageEmit", envir = rsEnv)
   on.exit(assign(".rs.logMessageEmit", original, envir = rsEnv), add = TRUE)
   assign(".rs.logMessageEmit", function(method, message) {
      captured[[length(captured) + 1L]] <<- list(method = method, message = message)
   }, envir = rsEnv)
   force(expr)
   captured
}

clearLogCache <- function() {
   rm(list = ls(envir = .rs.loggedMessageCache, all.names = TRUE),
      envir = .rs.loggedMessageCache)
}

test_that(".rs.log*Message passes literal text verbatim when ... is empty", {
   logs <- withCapturedLogs({
      .rs.logInfoMessage("progress: 100% complete")
   })
   expect_length(logs, 1L)
   expect_equal(logs[[1]]$method, "rs_logInfoMessage")
   expect_equal(logs[[1]]$message, "progress: 100% complete")
})

test_that(".rs.log*Message applies sprintf when ... is non-empty", {
   logs <- withCapturedLogs({
      .rs.logWarningMessage("hello %s, count = %d", "world", 42L)
   })
   expect_length(logs, 1L)
   expect_equal(logs[[1]]$method, "rs_logWarningMessage")
   expect_equal(logs[[1]]$message, "hello world, count = 42")
})

test_that(".rs.log*Message with once = TRUE suppresses repeats but lets others through", {
   clearLogCache()
   on.exit(clearLogCache(), add = TRUE)

   logs <- withCapturedLogs({
      .rs.logErrorMessage("duplicate message", once = TRUE)
      .rs.logErrorMessage("duplicate message", once = TRUE)   # suppressed
      .rs.logErrorMessage("duplicate message")                # once = FALSE, still logs
      .rs.logErrorMessage("different message", once = TRUE)   # distinct, logs
   })

   messages <- vapply(logs, `[[`, character(1L), "message")
   expect_equal(messages, c("duplicate message", "duplicate message", "different message"))
})
