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
   on.exit(rm(list = paste0(".rs.", functions), envir = .rs.toolsEnv()), add = TRUE)

   writeLines(c(
      '.rs.addFunction("startupTest.derived", function() "derived")',
      '.rs.addJsonRpcHandler("startup_test_derived", function() .rs.startupTest.derived())',
      '.rs.addFunction( "startupTest.spaced",',
      '                function() "spaced")',
      '# .rs.addFunction("startupTest.comment", ...) is only mentioned here',
      'message(".rs.addFunction(\\"startupTest.quoted\\", ...) is only quoted here")'
   ), path)

   expect_setequal(.rs.lazyModuleDefinitions(path), functions)
   .rs.addLazyModule(path)
   expect_false(exists(".rs.startupTest.comment", envir = .rs.toolsEnv(), inherits = FALSE))
   expect_false(exists(".rs.startupTest.quoted", envir = .rs.toolsEnv(), inherits = FALSE))
   expect_equal(.rs.rpc.startup_test_derived(), "derived")
   expect_equal(.rs.startupTest.spaced(), "spaced")
})

test_that("lazy module proxies report definitions the module omits", {
   path <- tempfile(fileext = ".R")
   on.exit(unlink(path), add = TRUE)
   functions <- c("startupTest.present", "startupTest.omitted")
   on.exit(rm(list = paste0(".rs.", functions), envir = .rs.toolsEnv()), add = TRUE)

   writeLines('.rs.addFunction("startupTest.present", function() TRUE)', path)
   .rs.addLazyModule(path, functions)
   expect_true(.rs.isLazyModuleProxy(.rs.startupTest.omitted))
   expect_error(.rs.startupTest.omitted(), "did not define")
   expect_true(.rs.startupTest.present())
})

test_that("lazy modules do not shadow helpers defined by other modules", {
   path <- tempfile(fileext = ".R")
   on.exit(unlink(path), add = TRUE)
   functions <- c("startupTest.shared", "startupTest.own")
   on.exit(rm(list = paste0(".rs.", functions), envir = .rs.toolsEnv()), add = TRUE)

   .rs.addFunction("startupTest.shared", function() "from another module")
   writeLines(c(
      '.rs.addFunction("startupTest.shared", function() "from the lazy module")',
      '.rs.addFunction("startupTest.own", function() .rs.startupTest.shared())'
   ), path)

   .rs.addLazyModule(path)
   expect_false(.rs.isLazyModuleProxy(.rs.startupTest.shared))
   expect_equal(.rs.startupTest.shared(), "from another module")

   # loading the module still lets it redefine the helper, as sourcing would
   expect_equal(.rs.startupTest.own(), "from the lazy module")
   expect_equal(.rs.startupTest.shared(), "from the lazy module")
})

test_that("lazy module proxies can be replaced by a later lazy module", {
   first <- tempfile(fileext = ".R")
   second <- tempfile(fileext = ".R")
   on.exit(unlink(c(first, second)), add = TRUE)
   on.exit(rm(".rs.startupTest.replaced", envir = .rs.toolsEnv()), add = TRUE)

   writeLines('.rs.addFunction("startupTest.replaced", function() "first")', first)
   writeLines('.rs.addFunction("startupTest.replaced", function() "second")', second)
   .rs.addLazyModule(first)
   .rs.addLazyModule(second)
   expect_equal(.rs.startupTest.replaced(), "second")
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
