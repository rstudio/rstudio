#
# test-completions-inherit-dot-params.R
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

fixture_rd_partial <- tools::parse_Rd(
   testthat::test_path("fixtures/wrapper-inheritDotParams-partial.Rd")
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
# .rs.parseInheritDotParamsFromRd  (pure Rd logic, no help-db lookup)
# ---------------------------------------------------------------------------

test_that("parseInheritDotParamsFromRd extracts targetName from fixture Rd", {
   skip_if(!new_functions_loaded, "Source SessionRCompletions.R first")
   result <- .rs.parseInheritDotParamsFromRd(fixture_rd)
   expect_equal(result$targetName, "target")
})

test_that("parseInheritDotParamsFromRd extracts argNames from fixture Rd", {
   skip_if(!new_functions_loaded, "Source SessionRCompletions.R first")
   result <- .rs.parseInheritDotParamsFromRd(fixture_rd)
   expect_in("alpha", result$argNames)
   expect_in("beta",  result$argNames)
   expect_in("gamma", result$argNames)
   expect_in("if",    result$argNames)
})

test_that("parseInheritDotParamsFromRd respects partial @inheritDotParams", {
   skip_if(!new_functions_loaded, "Source SessionRCompletions.R first")
   result <- .rs.parseInheritDotParamsFromRd(fixture_rd_partial)
   expect_in("alpha",       result$argNames)
   expect_in("beta",        result$argNames)
   expect_disjoint("gamma", result$argNames)
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

test_that("parseInheritDotParamsFromRd returns empty argNames for empty \\describe block", {
   skip_if(!new_functions_loaded, "Source SessionRCompletions.R first")
   rd_text <- "\\name{f}\\title{F}\\description{D}\\usage{f(...)}\n\\arguments{\\item{...}{Arguments passed on to \\code{target}\\describe{}}}"
   rd <- tools::parse_Rd(textConnection(rd_text))
   result <- .rs.parseInheritDotParamsFromRd(rd)
   expect_length(result$argNames, 0L)
})

test_that("parseInheritDotParamsFromRd returns NULL when sentinel has no \\code node", {
   skip_if(!new_functions_loaded, "Source SessionRCompletions.R first")
   # Sentinel text present but no \\code node following it
   rd_text <- "\\name{f}\\title{F}\\description{D}\\usage{f(...)}\n\\arguments{\\item{...}{Arguments passed on to something\\describe{\\item{\\code{x}}{desc}}}}"
   rd <- tools::parse_Rd(textConnection(rd_text))
   expect_null(.rs.parseInheritDotParamsFromRd(rd))
})

test_that("parseInheritDotParamsFromRd returns NULL when \\arguments section is missing", {
   skip_if(!new_functions_loaded, "Source SessionRCompletions.R first")
   rd_text <- "\\name{f}\\title{A function}\\description{Does something.}"
   rd <- tools::parse_Rd(textConnection(rd_text))
   expect_null(.rs.parseInheritDotParamsFromRd(rd))
})

test_that("parseInheritDotParamsFromRd uses first block when multiple @inheritDotParams exist", {
   skip_if(!new_functions_loaded, "Source SessionRCompletions.R first")
   # Two @inheritDotParams blocks: first targets 'target1' with arg 'alpha',
   # second targets 'target2' with arg 'beta'. Only the first block is used.
   rd_text <- paste0(
      "\\name{f}\\title{F}\\description{D}\\usage{f(...)}\n",
      "\\arguments{",
      "\\item{...}{Arguments passed on to \\code{target1}\\describe{\\item{\\code{alpha}}{desc}}}",
      "\\item{...}{Arguments passed on to \\code{target2}\\describe{\\item{\\code{beta}}{desc}}}",
      "}"
   )
   rd <- tools::parse_Rd(textConnection(rd_text))
   result <- .rs.parseInheritDotParamsFromRd(rd)
   expect_false(is.null(result))
   expect_equal(result$targetName, "target1")
   expect_in("alpha", result$argNames)
   expect_false("beta" %in% result$argNames)
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
      matchedCall = make_matched_call(),
      ownFormals  = c("x", "y")
   )
   expect_length(result$results, 0L)
})

test_that("getCompletionsInheritDotParams returns empty for malformed string with multiple ::", {
   skip_if(!new_functions_loaded, "Source SessionRCompletions.R first")
   result <- .rs.getCompletionsInheritDotParams(
      token       = "",
      string      = "a::b::c",
      object      = make_wrapper_fn(c("x", "...")),
      ownFormals  = c("x = ", "... = "),
      matchedCall = make_matched_call()
   )
   expect_length(result$results, 0L)
})

test_that("getCompletionsInheritDotParams returns empty for unknown function", {
   result <- .rs.getCompletionsInheritDotParams(
      token       = "",
      string      = "____no_such_fn____",
      object      = make_wrapper_fn(),
      matchedCall = make_matched_call(),
      ownFormals  = c("x", "...")
   )
   expect_length(result$results, 0L)
})

# For the orchestrator tests we need parseInheritDotParamsFromHelp to return
# fixture data. Since mocking utils internals is not available outside pkgload,
# we temporarily replace parseInheritDotParamsFromHelp with a stub that calls
# parseInheritDotParamsFromRd on the fixture directly.
with_fixture_completions <- function(expr) {
   rsEnv <- as.environment("tools:rstudio")

   # Stub parseInheritDotParamsFromHelp to return fixture Rd data.
   original_help <- get(".rs.parseInheritDotParamsFromHelp", envir = rsEnv)
   on.exit(
      assign(".rs.parseInheritDotParamsFromHelp", original_help, envir = rsEnv),
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
         string      = "pkg::wrapper",
         object      = make_wrapper_fn(c("x", "...")),
         ownFormals  = c("x = ", "... = "),
         matchedCall = make_matched_call()
      )
      expect_length(result$results, 4L)
      expect_all_equal(result$type, .rs.acCompletionTypes$ARGUMENT)
   })
})

test_that("getCompletionsInheritDotParams excludes wrapper's own formals", {
   skip_if(!new_functions_loaded, "Source SessionRCompletions.R first")
   with_fixture_completions({
      result <- .rs.getCompletionsInheritDotParams(
         token       = "",
         string      = "pkg::wrapper",
         object      = make_wrapper_fn(c("x", "...")),
         ownFormals  = c("x = ", "... = "),
         matchedCall = make_matched_call()
      )
      returned_names <- sub(" = $", "", result$results)
      expect_disjoint("x",     returned_names)
      expect_in("alpha", returned_names)
      expect_in("beta",  returned_names)
      expect_in("gamma", returned_names)
   })
})

test_that("getCompletionsInheritDotParams excludes already-used args", {
   skip_if(!new_functions_loaded, "Source SessionRCompletions.R first")
   with_fixture_completions({
      result <- .rs.getCompletionsInheritDotParams(
         token       = "",
         string      = "pkg::wrapper",
         object      = make_wrapper_fn(c("x", "...")),
         ownFormals  = c("x = ", "... = "),
         matchedCall = make_matched_call(named_args = "alpha")
      )
      returned_names <- sub(" = $", "", result$results)
      expect_disjoint("alpha", returned_names)
      expect_in("beta",   returned_names)
      expect_in("gamma",  returned_names)
   })
})

test_that("getCompletionsInheritDotParams backtick-quotes reserved word arg names", {
   skip_if(!new_functions_loaded, "Source SessionRCompletions.R first")
   with_fixture_completions({
      result <- .rs.getCompletionsInheritDotParams(
         token       = "",
         string      = "pkg::wrapper",
         object      = make_wrapper_fn(c("x", "...")),
         ownFormals  = c("x = ", "... = "),
         matchedCall = make_matched_call()
      )
      # Reserved words like `if` require backtick-quoting when deparsed
      expect_in("`if` = ", result$results)
   })
})

test_that("getCompletionsInheritDotParams filters by token prefix", {
   skip_if(!new_functions_loaded, "Source SessionRCompletions.R first")
   with_fixture_completions({
      result <- .rs.getCompletionsInheritDotParams(
         token       = "al",
         string      = "pkg::wrapper",
         object      = make_wrapper_fn(c("x", "...")),
         ownFormals  = c("x = ", "... = "),
         matchedCall = make_matched_call()
      )
      returned_names <- sub(" = $", "", result$results)
      expect_in("alpha",  returned_names)
      expect_disjoint("beta",  returned_names)
      expect_disjoint("gamma", returned_names)
   })
})

test_that("getCompletionsInheritDotParams returns empty for function defined in global env", {
   skip_if(!new_functions_loaded, "Source SessionRCompletions.R first")
   # A locally-defined function lives in R_GlobalEnv, not a package namespace.
   # The early exit should fire and return empty completions.
   local_fn <- local(function(x, ...) x, envir = globalenv())
   result <- .rs.getCompletionsInheritDotParams(
      token       = "",
      string      = "local_fn",   # no :: prefix, so package resolved from env
      object      = local_fn,
      ownFormals  = c("x = ", "... = "),
      matchedCall = make_matched_call()
   )
   expect_length(result$results, 0L)
})

test_that("getCompletionsInheritDotParams handles triple-colon pkg:::fn syntax", {
   skip_if(!new_functions_loaded, "Source SessionRCompletions.R first")
   with_fixture_completions({
      result <- .rs.getCompletionsInheritDotParams(
         token       = "",
         string      = "pkg:::wrapper",
         object      = make_wrapper_fn(c("x", "...")),
         ownFormals  = c("x = ", "... = "),
         matchedCall = make_matched_call()
      )
      expect_gt(length(result$results), 0L)
   })
})

test_that("getCompletionsInheritDotParams returns cached result on second call", {
   skip_if(!new_functions_loaded, "Source SessionRCompletions.R first")
   with_fixture_completions({
      # Call twice with the same pkg::fn -- second call hits the cache.
      result1 <- .rs.getCompletionsInheritDotParams(
         token       = "",
         string      = "pkg::wrapper",
         object      = make_wrapper_fn(c("x", "...")),
         ownFormals  = c("x = ", "... = "),
         matchedCall = make_matched_call()
      )
      result2 <- .rs.getCompletionsInheritDotParams(
         token       = "",
         string      = "pkg::wrapper",
         object      = make_wrapper_fn(c("x", "...")),
         ownFormals  = c("x = ", "... = "),
         matchedCall = make_matched_call()
      )
      expect_equal(result1$results, result2$results)
   })
})


# ---------------------------------------------------------------------------
# .rs.getCompletionsFunction  (end-to-end, requires ggplot2)
# ---------------------------------------------------------------------------

test_that("getCompletionsFunction returns inherited dot args with empty token", {
   skip_if(!new_functions_loaded, "Source SessionRCompletions.R first")
   skip_if_not_installed("ggplot2")
   result <- .rs.getCompletionsFunction(
      token        = "",
      string       = "ggplot2::scale_color_grey",
      functionCall = quote(ggplot2::scale_color_grey()),
      numCommas    = 0L,
      envir        = globalenv()
   )
   returned_names <- sub(" = $", "", result$results)
   expect_in("breaks", returned_names)
})

test_that("getCompletionsFunction returns inherited dot args with partial token", {
   skip_if(!new_functions_loaded, "Source SessionRCompletions.R first")
   skip_if_not_installed("ggplot2")
   result <- .rs.getCompletionsFunction(
      token        = "br",
      string       = "ggplot2::scale_color_grey",
      functionCall = quote(ggplot2::scale_color_grey(br)),
      numCommas    = 0L,
      envir        = globalenv()
   )
   returned_names <- sub(" = $", "", result$results)
   expect_in("breaks",  returned_names)
   expect_disjoint("labels", returned_names)
})

test_that("getCompletionsFunction resolves package via environment when no :: used", {
   skip_if(!new_functions_loaded, "Source SessionRCompletions.R first")
   skip_if_not_installed("ggplot2")
   # Use the function object from ggplot2 but pass a bare name as string --
   # exercises the environmentName(environment(object)) fallback path.
   withr::with_package("ggplot2", {
      result <- .rs.getCompletionsFunction(
         token        = "",
         string       = "scale_color_grey",
         functionCall = quote(scale_color_grey()),
         numCommas    = 0L,
         envir        = globalenv()
      )
   })
   returned_names <- sub(" = $", "", result$results)
   expect_in("breaks", returned_names)
})

test_that("getCompletionsFunction handles triple-colon pkg:::fn syntax", {
   skip_if(!new_functions_loaded, "Source SessionRCompletions.R first")
   skip_if_not_installed("ggplot2")
   result <- .rs.getCompletionsFunction(
      token        = "",
      string       = "ggplot2:::scale_color_grey",
      functionCall = quote(ggplot2:::scale_color_grey()),
      numCommas    = 0L,
      envir        = globalenv()
   )
   returned_names <- sub(" = $", "", result$results)
   expect_in("breaks", returned_names)
})

test_that("getCompletionsFunction returns completions when token filters out ...", {
   skip_if(!new_functions_loaded, "Source SessionRCompletions.R first")
   skip_if_not_installed("ggplot2")
   # When token = "br", resolveFormals filters out "... = " from formals$formals,
   # so the hasDots fallback via formals(object) must be exercised.
   result <- .rs.getCompletionsFunction(
      token        = "br",
      string       = "ggplot2::scale_color_grey",
      functionCall = quote(ggplot2::scale_color_grey(br)),
      numCommas    = 0L,
      envir        = globalenv()
   )
   returned_names <- sub(" = $", "", result$results)
   expect_in("breaks", returned_names)
})

