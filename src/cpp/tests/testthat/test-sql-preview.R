#
# test-sql-preview.R
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

context("sql preview")

# parse a single expression the same way the connection classifier does
.rs.test.parseConn <- function(text)
{
   parse(text = text, keep.source = FALSE)[[1L]]
}

test_that(".rs.preview.isSafeExpr accepts benign connection expressions", {

   safe <- c(
      "con",                                       # plain symbol lookup
      "\"con\"",                                   # string literal
      "42",                                        # numeric literal
      "DBI::dbConnect(RSQLite::SQLite())",         # allow-listed constructors
      "dbConnect(odbc::odbc())",                   # bare allow-listed call
      "dbConnect(odbc::odbc(), pwd = Sys.getenv(\"PW\"))",
      "(DBI::dbConnect(RSQLite::SQLite()))",       # parenthesized
      "DBI::dbConnect"                             # namespace access, no call
   )

   for (expr in safe)
      expect_true(
         .rs.preview.isSafeExpr(.rs.test.parseConn(expr)),
         info = expr
      )

})

test_that(".rs.preview.isSafeExpr rejects arbitrary code", {

   unsafe <- c(
      "system(\"echo pwned\")",                    # the reported payload
      "base::system(\"echo pwned\")",              # qualification is not trust
      "getNamespace(\"base\")$system(\"x\")",
      "DBI::dbConnect(RSQLite::SQLite(), x = system(\"x\"))", # unsafe argument
      "list(system(\"x\"))",                       # nested in a pure helper
      "{ system(\"x\") }",                         # braces
      "eval(parse(text = \"system('x')\"))"
   )

   for (expr in unsafe)
      expect_false(
         .rs.preview.isSafeExpr(.rs.test.parseConn(expr)),
         info = expr
      )

})

test_that("active bindings are not treated as safe symbols", {

   name <- ".rs.test.activeConn"
   on.exit(if (exists(name, envir = globalenv())) rm(list = name, envir = globalenv()))

   makeActiveBinding(name, function() stop("active binding evaluated"), globalenv())
   expect_false(.rs.preview.isSafeExpr(as.symbol(name)))

   # a normal (inactive) binding of the same name is safe
   rm(list = name, envir = globalenv())
   assign(name, 1L, envir = globalenv())
   expect_true(.rs.preview.isSafeExpr(as.symbol(name)))

})

test_that("sites can extend the allowlist via the session option", {

   call <- .rs.test.parseConn("acmedb::connect(\"prod\")")

   # not allow-listed by default
   expect_identical(.rs.preview.exprStatus(call), "unsafe")

   # the 'preview-allowed-functions' session option is surfaced into this
   # cache var; populate it directly to mirror an administrator setting it
   .rs.setVar("preview.allowedFunctions", "acmedb::connect")
   on.exit(.rs.setVar("preview.allowedFunctions", NULL), add = TRUE)

   # an allow-listed callee makes the call safe, but its arguments are still
   # validated recursively
   expect_identical(.rs.preview.exprStatus(call), "safe")
   expect_identical(
      .rs.preview.exprStatus(.rs.test.parseConn("acmedb::connect(system(\"x\"))")),
      "unsafe"
   )

})

test_that("active bindings anywhere on the search path are unsafe", {

   # eval() resolves through the search path, so an active binding in an
   # attached environment (not globalenv) must also be rejected
   e <- new.env(parent = emptyenv())
   name <- "rs_test_attached_active"
   makeActiveBinding(name, function() stop("active binding evaluated"), e)

   attach(e, name = "rs_test_active_env", warn.conflicts = FALSE)
   on.exit(detach("rs_test_active_env"))

   expect_false(.rs.preview.isSafeExpr(as.symbol(name)))

})

test_that("the per-session permitted set is honored and matches by structure", {

   expr <- .rs.test.parseConn("connectViaCustomHelper(profile = \"prod\")")
   expect_identical(.rs.preview.exprStatus(expr), "unsafe")

   .rs.preview.permitExpr(expr)
   on.exit(.rs.preview.permittedExprs$items <- NULL)

   # the same expression, parsed independently, must now be permitted
   reparsed <- .rs.test.parseConn("connectViaCustomHelper(profile = \"prod\")")
   expect_identical(.rs.preview.exprStatus(reparsed), "permitted")

   # a different expression remains unsafe
   other <- .rs.test.parseConn("connectViaCustomHelper(profile = \"dev\")")
   expect_identical(.rs.preview.exprStatus(other), "unsafe")

})

test_that("asDBIConnection refuses to evaluate unsafe connection strings", {

   skip_if_not_installed("DBI")

   marker <- "RS_SQL_PREVIEW_PWNED"
   Sys.unsetenv(marker)
   on.exit(Sys.unsetenv(marker))

   payload <- sprintf("Sys.setenv(%s = \"1\")", marker)
   expect_null(.rs.sql.asDBIConnection(payload))
   expect_identical(Sys.getenv(marker), "")

})

test_that("preview_sql blocks function-name injection in the header", {

   handler <- .rs.rpc.preview_sql

   # '-- !preview system(...)' builds a call whose function position is the
   # payload, bypassing any check that only inspects the 'conn' argument
   result <- handler("system(\"echo pwned\")")
   expect_identical(unclass(result$action), "error")
   expect_match(unclass(result$message), "Unsupported")

})

test_that("preview_sql prompts for unsafe connection expressions", {

   handler <- .rs.rpc.preview_sql

   code <- ".rs.previewSql(\"query.sql\", conn = system(\"echo pwned\"))"

   # without consent, the unsafe expression is not evaluated; the caller is
   # asked to confirm instead
   result <- handler(code, allow = FALSE)
   expect_identical(unclass(result$action), "confirm")

})

test_that("preview_sql rejects injection smuggled through extra arguments", {

   handler <- .rs.rpc.preview_sql

   code <- ".rs.previewSql(\"query.sql\", conn = con, params = system(\"echo pwned\"))"
   result <- handler(code, allow = FALSE)
   expect_identical(unclass(result$action), "error")
   expect_match(unclass(result$message), "Unsupported")

})

test_that("preview_sql does not prompt for safe connection expressions", {

   handler <- .rs.rpc.preview_sql

   # a bare symbol is a safe lookup; it should pass the consent gate and reach
   # evaluation (which here fails harmlessly, but is never a 'confirm')
   code <- ".rs.previewSql(\"query.sql\", conn = .rs.test.missingConnection)"
   result <- handler(code, allow = FALSE)
   expect_false(identical(unclass(result$action), "confirm"))

})

test_that("preview_r2d3 only consents to the r2d3::r2d3 call", {

   handler <- .rs.rpc.preview_r2d3

   # the function position must be r2d3::r2d3; anything else is rejected so the
   # callee cannot itself be used to inject code
   result <- handler("system(\"echo pwned\")")
   expect_identical(unclass(result$action), "error")
   expect_match(unclass(result$message), "Unsupported")

})

test_that("preview_r2d3 prompts for unsafe argument expressions", {

   handler <- .rs.rpc.preview_r2d3

   code <- "r2d3::r2d3(\"viz.js\", data = system(\"echo pwned\"))"
   result <- handler(code, allow = FALSE)
   expect_identical(unclass(result$action), "confirm")

})

test_that("preview_r2d3 allows statically-safe commands without prompting", {

   handler <- .rs.rpc.preview_r2d3

   code <- "r2d3::r2d3(\"viz.js\", data = c(0.3, 0.6, 0.8))"
   result <- handler(code, allow = FALSE)
   expect_identical(unclass(result$action), "ok")

})
