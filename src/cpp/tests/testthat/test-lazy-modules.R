#
# test-lazy-modules.R
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

test_that("lazy module helpers retain their arguments and load once", {
   path <- tempfile(fileext = ".R")
   counter <- tempfile()
   on.exit(unlink(c(path, counter)), add = TRUE)
   functions <- c("startupTest.value", "rpc.startup_test")
   on.exit(rm(list = paste0(".rs.", functions), envir = .rs.toolsEnv()), add = TRUE)

   writeLines(c(
      sprintf("cat('loaded\\n', file = %s, append = TRUE)", deparse(counter)),
      '.rs.addFunction("startupTest.value", function(value = 42) value)',
      '.rs.addJsonRpcHandler("startup_test", function(value) .rs.startupTest.value(value))'
   ), path)

   .rs.addLazyModule(path, functions)
   expect_false(file.exists(counter))
   expect_equal(.rs.startupTest.value(value = 10), 10)
   expect_equal(.rs.rpc.startup_test(20), 20)
   expect_equal(readLines(counter), "loaded")
   expect_equal(formals(.rs.startupTest.value)$value, 42)
})

test_that("lazy modules derive their proxies from the module's definitions", {
   path <- tempfile(fileext = ".R")
   on.exit(unlink(path), add = TRUE)
   functions <- c("startupTest.derived", "rpc.startup_test_derived", "startupTest.spaced")
   mentioned <- "startupTest.comment"
   on.exit(rm(list = paste0(".rs.", c(functions, mentioned)), envir = .rs.toolsEnv()), add = TRUE)

   writeLines(c(
      '.rs.addFunction("startupTest.derived", function() "derived")',
      '.rs.addJsonRpcHandler("startup_test_derived", function() .rs.startupTest.derived())',
      '.rs.addFunction( "startupTest.spaced",',
      '                function() "spaced")',
      '# .rs.addFunction("startupTest.comment", ...) is only mentioned here'
   ), path)

   expect_setequal(.rs.lazyModuleDefinitions(path), c(functions, mentioned))
   .rs.addLazyModule(path)
   expect_equal(.rs.rpc.startup_test_derived(), "derived")
   expect_equal(.rs.startupTest.spaced(), "spaced")
   # a stray mention only yields a proxy that reports the omission
   expect_error(.rs.startupTest.comment(), "did not define")
})

test_that("lazy modules can retry after a failed load", {
   path <- tempfile(fileext = ".R")
   on.exit(unlink(path), add = TRUE)
   on.exit(rm(".rs.startupTest.retry", envir = .rs.toolsEnv()), add = TRUE)
   .rs.addLazyModule(path, "startupTest.retry")
   expect_error(.rs.startupTest.retry())
   writeLines('.rs.addFunction("startupTest.retry", function() TRUE)', path)
   expect_true(.rs.startupTest.retry())
})

test_that("SQL helpers remain available through lazy loading", {
   expect_true(".rs.rpc.sql_get_completions" %in% .rs.listJsonRpcHandlers())
   expect_true(.rs.sql.isTableScopedKeyword("from"))
   expect_false(.rs.sql.isTableScopedKeyword("where"))
   expect_true(is.function(.rs.rpc.sql_get_completions))
})

test_that("Stan helpers remain available through lazy loading", {
   expect_true(".rs.rpc.stan_run_diagnostics" %in% .rs.listJsonRpcHandlers())
   expect_true("for" %in% .rs.stan.keywords())
   expect_true("real" %in% .rs.stan.types())
   expect_true(is.function(.rs.rpc.stan_get_completions))
})
