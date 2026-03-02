#
# test-completions-inherit-dot-params.R
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

library(testthat)

context("inheritDotParams completions")

# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------

make_wrapper_fn <- function(own_args = c("x", "...")) {
   args_list <- setNames(vector("list", length(own_args)), own_args)
   as.function(c(args_list, list(NULL)))
}

make_matched_call <- function(named_args = character()) {
   as.call(c(
      list(as.symbol("f")),
      setNames(replicate(length(named_args), quote(x)), named_args)
   ))
}

fixture_rd <- tools::parse_Rd(
   testthat::test_path("fixtures/wrapper-inheritDotParams.Rd")
)

# These tests require the updated SessionRCompletions.R to be sourced in the
# current session. If running inside a development RStudio build (not yet
# restarted with the new code), source it manually first:
#
#   source("src/cpp/session/modules/SessionRCompletions.R")
#   testthat::test_file("src/cpp/tests/testthat/test-completions-inherit-dot-params.R")
#
new_functions_loaded <- exists(".rs.parseInheritDotParamsFromRd", mode = "function")


# ---------------------------------------------------------------------------
# .rs.extractDescribeItemNames
# ---------------------------------------------------------------------------

test_that("extractDescribeItemNames returns forwarded arg names from fixture Rd", {
   args_node <- Filter(
      function(x) identical(attr(x, "Rd_tag"), "\\arguments"),
      fixture_rd
   )[[1L]]

   dots_item <- Filter(function(x) {
      identical(attr(x, "Rd_tag"), "\\item") &&
         trimws(paste(unlist(x[[1L]]), collapse = "")) == "..."
   }, args_node)[[1L]]

   expect_equal(
      .rs.extractDescribeItemNames(dots_item[[2L]]),
      c("alpha", "beta", "gamma")
   )
})

test_that("extractDescribeItemNames returns character() for node with no \\describe", {
   node <- list()
   attr(node, "Rd_tag") <- "BLOCK"
   expect_equal(.rs.extractDescribeItemNames(node), character())
})

test_that("extractDescribeItemNames returns character() for empty \\describe", {
   node <- list()
   attr(node, "Rd_tag") <- "\\describe"
   expect_equal(.rs.extractDescribeItemNames(node), character())
})


# ---------------------------------------------------------------------------
# .rs.parseInheritDotParamsFromRd  (pure Rd logic, no help-db lookup)
# ---------------------------------------------------------------------------

test_that("parseInheritDotParamsFromRd extracts args and targetName from fixture Rd", {
   skip_if(!new_functions_loaded, "Source SessionRCompletions.R first")
   result <- .rs.parseInheritDotParamsFromRd(fixture_rd)
   expect_equal(result$argNames, c("alpha", "beta", "gamma"))
   expect_equal(result$targetName, "target")
})

test_that("parseInheritDotParamsFromRd returns NULL for Rd with no @inheritDotParams", {
   skip_if(!new_functions_loaded, "Source SessionRCompletions.R first")
   # base::mean Rd has ... but documented manually -- parse it directly
   mean_rd <- tryCatch(
      get(".getHelpFile", envir = asNamespace("utils"), mode = "function")(
         eval(.rs.makeHelpCall("mean", "base"))
      ),
      error = function(e) NULL
   )
   skip_if(is.null(mean_rd), "Could not load mean Rd")
   expect_null(.rs.parseInheritDotParamsFromRd(mean_rd))
})


# ---------------------------------------------------------------------------
# .rs.parseInheritDotParamsFromHelp  (help-db lookup layer)
# ---------------------------------------------------------------------------

test_that("parseInheritDotParamsFromHelp returns NULL for unknown function", {
   skip_if(!new_functions_loaded, "Source SessionRCompletions.R first")
   expect_null(.rs.parseInheritDotParamsFromHelp("____no_such_fn____", NULL))
})


# ---------------------------------------------------------------------------
# .rs.getCompletionsInheritDotParams
# ---------------------------------------------------------------------------

test_that("getCompletionsInheritDotParams returns empty when fn has no ...", {
   result <- .rs.getCompletionsInheritDotParams(
      token       = "",
      string      = "wrapper",
      object      = function(x, y) NULL,
      matchedCall = make_matched_call()
   )
   expect_equal(length(result$results), 0L)
})

test_that("getCompletionsInheritDotParams returns empty for unknown function", {
   result <- .rs.getCompletionsInheritDotParams(
      token       = "",
      string      = "____no_such_fn____",
      object      = make_wrapper_fn(),
      matchedCall = make_matched_call()
   )
   expect_equal(length(result$results), 0L)
})

# For the orchestrator tests we need parseInheritDotParamsFromHelp to return
# fixture data. Since mocking utils internals is not available outside pkgload,
# we temporarily replace parseInheritDotParamsFromHelp with a stub that calls
# parseInheritDotParamsFromRd on the fixture directly.
with_fixture_completions <- function(expr) {
   rsEnv <- as.environment("tools:rstudio")
   original <- get(".rs.parseInheritDotParamsFromHelp", envir = rsEnv)
   on.exit(
      assign(".rs.parseInheritDotParamsFromHelp", original, envir = rsEnv),
      add = TRUE
   )
   assign(
      ".rs.parseInheritDotParamsFromHelp",
      function(fnName, pkgName) .rs.parseInheritDotParamsFromRd(fixture_rd),
      envir = rsEnv
   )
   force(expr)
}

test_that("getCompletionsInheritDotParams returns ARGUMENT completions from fixture", {
   skip_if(!new_functions_loaded, "Source SessionRCompletions.R first")
   with_fixture_completions({
      result <- .rs.getCompletionsInheritDotParams(
         token       = "",
         string      = "wrapper",
         object      = make_wrapper_fn(c("x", "...")),
         matchedCall = make_matched_call()
      )
      expect_true(length(result$results) > 0L)
      expect_true(all(result$type == .rs.acCompletionTypes$ARGUMENT))
   })
})

test_that("getCompletionsInheritDotParams excludes wrapper's own formals", {
   skip_if(!new_functions_loaded, "Source SessionRCompletions.R first")
   with_fixture_completions({
      result <- .rs.getCompletionsInheritDotParams(
         token       = "",
         string      = "wrapper",
         object      = make_wrapper_fn(c("x", "...")),
         matchedCall = make_matched_call()
      )
      returned_names <- sub(" = $", "", result$results)
      expect_false("x"    %in% returned_names)
      expect_true("alpha" %in% returned_names)
      expect_true("beta"  %in% returned_names)
      expect_true("gamma" %in% returned_names)
   })
})

test_that("getCompletionsInheritDotParams excludes already-used args", {
   skip_if(!new_functions_loaded, "Source SessionRCompletions.R first")
   with_fixture_completions({
      result <- .rs.getCompletionsInheritDotParams(
         token       = "",
         string      = "wrapper",
         object      = make_wrapper_fn(c("x", "...")),
         matchedCall = make_matched_call(named_args = "alpha")
      )
      returned_names <- sub(" = $", "", result$results)
      expect_false("alpha" %in% returned_names)
      expect_true("beta"   %in% returned_names)
      expect_true("gamma"  %in% returned_names)
   })
})

test_that("getCompletionsInheritDotParams filters by token prefix", {
   skip_if(!new_functions_loaded, "Source SessionRCompletions.R first")
   with_fixture_completions({
      result <- .rs.getCompletionsInheritDotParams(
         token       = "al",
         string      = "wrapper",
         object      = make_wrapper_fn(c("x", "...")),
         matchedCall = make_matched_call()
      )
      returned_names <- sub(" = $", "", result$results)
      expect_true("alpha"  %in% returned_names)
      expect_false("beta"  %in% returned_names)
      expect_false("gamma" %in% returned_names)
   })
})
