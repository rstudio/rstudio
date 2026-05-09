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

has_system_rd_macros <- file.exists(
   file.path(R.home("share"), "Rd", "macros", "system.Rd")
)

test_that <- function(desc, code) {
   if (!has_system_rd_macros) {
      testthat::test_that(desc, testthat::skip("system.Rd macros file not available"))
      return(invisible(NULL))
   }
   call <- sys.call()
   call[[1L]] <- quote(testthat::test_that)
   eval(call, envir = parent.frame())
}

fixture_rd <- if (has_system_rd_macros) tools::parse_Rd(
   testthat::test_path("fixtures/wrapper-inheritDotParams.Rd")
)

fixture_rd_partial <- if (has_system_rd_macros) tools::parse_Rd(
   testthat::test_path("fixtures/wrapper-inheritDotParams-partial.Rd")
)

# testthat::with_mocked_bindings / local_mocked_bindings cannot mock functions
# in the locked "tools:rstudio" environment, so we use a shared helper that
# saves, replaces, and restores bindings via assign() + on.exit().
with_rs_mock <- function(targetName, newValue, expr) {
   rsEnv <- as.environment("tools:rstudio")
   original <- get(targetName, envir = rsEnv)
   on.exit(assign(targetName, original, envir = rsEnv), add = TRUE)
   assign(targetName, newValue, envir = rsEnv)
   force(expr)
}

# Builds a minimal synthetic Rd object with the structure that roxygen2 generates
# for @inheritDotParams. Individual fields can be overridden to exercise specific
# guards in parseInheritDotParamsFromRd without needing to construct valid Rd text.
make_inherit_rd <- function(
   arg_name   = "...",
   sentinel   = "Arguments passed on to ",
   code_tag   = "\\code",
   link_text  = "target",
   link_tag   = "\\link",
   with_link  = TRUE,
   with_describe = TRUE,
   describe_items = list()
) {
   mk <- function(tag, ...) {
      node <- list(...)
      attr(node, "Rd_tag") <- tag
      node
   }

   linkNode   <- mk(link_tag, link_text)
   codeNode   <- if (with_link) mk(code_tag, linkNode) else mk(code_tag, link_text)
   textNode   <- mk("TEXT", sentinel)
   descItems  <- lapply(describe_items, function(nm) {
      mk("\\item", mk("\\code", nm), mk("TEXT", "desc"))
   })
   descNode   <- if (with_describe) do.call(mk, c(list("\\describe"), descItems)) else NULL
   dotsDesc   <- if (!is.null(descNode)) mk("RCODE", textNode, codeNode, descNode)
                 else                    mk("RCODE", textNode, codeNode)
   nameNode   <- mk("TEXT", arg_name)
   dotsItem   <- mk("\\item", nameNode, dotsDesc)
   argsNode   <- mk("\\arguments", dotsItem)
   mk("Rd", argsNode)
}


# ---------------------------------------------------------------------------
# .rs.parseInheritDotParamsFromRd  (pure Rd logic, no help-db lookup)
# ---------------------------------------------------------------------------

test_that("parseInheritDotParamsFromRd extracts targetName from fixture Rd", {
   result <- .rs.parseInheritDotParamsFromRd(fixture_rd)
   expect_equal(result$targetName, "target")
   expect_null(result$targetPkg)
})

test_that("parseInheritDotParamsFromRd returns NULL targetPkg for same-package [=fn] link form", {
   # Form 1: same-package -- \link[=fn]{fn}, no package prefix
   rd_text <- paste0(
      "\\name{f}\\title{F}\\description{D}\\usage{f(...)}\n",
      "\\arguments{\\item{...}{Arguments passed on to \\code{\\link[=target]{target}}",
      "\\describe{\\item{\\code{x}}{desc}}}}"
   )
   rd <- tools::parse_Rd(textConnection(rd_text))
   result <- .rs.parseInheritDotParamsFromRd(rd)
   expect_null(result$targetPkg)
   expect_equal(result$targetName, "target")
})

test_that("parseInheritDotParamsFromRd extracts targetPkg from cross-package [pkg:topic] link form", {
   # Form 2: cross-package -- \link[pkg:topic]{pkg::fn}
   # roxygen2 generates: dest <- gsub("::", ":", src) when src contains "::"
   rd_text <- paste0(
      "\\name{f}\\title{F}\\description{D}\\usage{f(...)}\n",
      "\\arguments{\\item{...}{Arguments passed on to \\code{\\link[dplyr:slice_min]{dplyr::slice_min}}",
      "\\describe{\\item{\\code{x}}{desc}}}}"
   )
   rd <- tools::parse_Rd(textConnection(rd_text))
   result <- .rs.parseInheritDotParamsFromRd(rd)
   expect_equal(result$targetPkg,  "dplyr")
   expect_equal(result$targetName, "slice_min")
})

test_that("parseInheritDotParamsFromRd extracts argNames from fixture Rd", {
   result <- .rs.parseInheritDotParamsFromRd(fixture_rd)
   # Standard args
   expect_in("alpha", result$argNames)
   expect_in("beta",  result$argNames)
   expect_in("gamma", result$argNames)
   # Reserved word -- requires backtick-quoting when deparsed
   expect_in("if",    result$argNames)
   # Comma-separated args from a single \item{\code{n,prop}}{} entry
   expect_in("n",     result$argNames)
   expect_in("prop",  result$argNames)
})

test_that("parseInheritDotParamsFromRd respects partial @inheritDotParams", {
   result <- .rs.parseInheritDotParamsFromRd(fixture_rd_partial)
   expect_in("alpha",       result$argNames)
   expect_in("beta",        result$argNames)
   expect_disjoint("gamma", result$argNames)
})

test_that("parseInheritDotParamsFromRd returns NULL for Rd with no @inheritDotParams", {
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

test_that("parseInheritDotParamsFromRd uses first block when multiple @inheritDotParams exist", {
   # Two @inheritDotParams blocks: first targets 'target1' with arg 'alpha',
   # second targets 'target2' with arg 'beta'. Only the first block is used.
   rd_text <- paste0(
      "\\name{f}\\title{F}\\description{D}\\usage{f(...)}\n",
      "\\arguments{",
      "\\item{...}{Arguments passed on to \\code{\\link[=target1]{target1}}\\describe{\\item{\\code{alpha}}{desc}}}",
      "\\item{...}{Arguments passed on to \\code{\\link[=target2]{target2}}\\describe{\\item{\\code{beta}}{desc}}}",
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
# .rs.parseInheritDotParamsFromRd -- Rd not matching @inheritDotParams structure
#
# Steps through the expected structure in guard order. Valid Rd that deviates
# from the roxygen2-generated format returns NULL at the relevant guard.
# ---------------------------------------------------------------------------

# Guard 1: Find \arguments
test_that("parseInheritDotParamsFromRd returns NULL when \\arguments section is absent", {
   rd_no_args <- list(); attr(rd_no_args, "Rd_tag") <- "Rd"
   expect_null(.rs.parseInheritDotParamsFromRd(rd_no_args))
})

# Guard 2: Find \item with ... name
test_that("parseInheritDotParamsFromRd returns NULL when no ... item is found", {
   rd <- make_inherit_rd(arg_name = "x")
   expect_null(.rs.parseInheritDotParamsFromRd(rd))
})

# Guard 3: sentinel TEXT must be present and start with "Arguments passed on to"
test_that("parseInheritDotParamsFromRd returns NULL when sentinel text is absent", {
   rd <- make_inherit_rd(sentinel = "Something else entirely")
   expect_null(.rs.parseInheritDotParamsFromRd(rd))
})

test_that("parseInheritDotParamsFromRd returns NULL when sentinel text does not start the TEXT node", {
   rd <- make_inherit_rd(sentinel = "Note: Arguments passed on to ")
   expect_null(.rs.parseInheritDotParamsFromRd(rd))
})

# Guard 4: \code node must follow the sentinel
test_that("parseInheritDotParamsFromRd returns NULL when \\code node is absent", {
   # e.g. \code{target} replaced with a different tag
   rd <- make_inherit_rd(code_tag = "\\text")
   expect_null(.rs.parseInheritDotParamsFromRd(rd))
})

# Guard 5: Find \link inside \code
test_that("parseInheritDotParamsFromRd returns NULL when \\link node is absent", {
   # e.g. \code{target} without a \link node
   rd <- make_inherit_rd(with_link = FALSE)
   expect_null(.rs.parseInheritDotParamsFromRd(rd))
})

# Guard 6: \link node text must be non-empty and of the form "pkg::fn" or "fn"
test_that("parseInheritDotParamsFromRd returns NULL when \\link node text is empty", {
   rd <- make_inherit_rd(link_text = "")
   expect_null(.rs.parseInheritDotParamsFromRd(rd))
})

test_that("parseInheritDotParamsFromRd returns NULL when \\link pkg prefix is empty", {
   # "::fn" is not a valid R namespace expression -- return NULL rather than
   # silently treating it as a same-package reference.
   rd <- make_inherit_rd(link_text = "::fn")
   expect_null(.rs.parseInheritDotParamsFromRd(rd))
})

test_that("parseInheritDotParamsFromRd returns NULL when \\link text has too many separators", {
   # "a::b::c" splits into 3 parts -- not a valid namespace expression
   rd <- make_inherit_rd(link_text = "a::b::c")
   expect_null(.rs.parseInheritDotParamsFromRd(rd))
})

test_that("parseInheritDotParamsFromRd extracts targetPkg from triple-colon link text", {
   # :{2,3} split handles pkg:::fn defensively, even though roxygen2 currently
   # produces a parse error for @inheritDotParams pkg:::fn
   rd <- make_inherit_rd(link_text = "pkg:::fn")
   result <- .rs.parseInheritDotParamsFromRd(rd)
   expect_equal(result$targetPkg,  "pkg")
   expect_equal(result$targetName, "fn")
})

# Guard 7: Find \describe
test_that("parseInheritDotParamsFromRd returns NULL when \\describe node is absent", {
   rd <- make_inherit_rd(with_describe = FALSE)
   expect_null(.rs.parseInheritDotParamsFromRd(rd))
})

# Guard 8: Filter \item nodes with length >= 1. An empty \describe is a valid
# (if degenerate) @inheritDotParams -- the pattern matched, but no args were
# forwarded -- so the function returns a list with empty argNames, not NULL.
test_that("parseInheritDotParamsFromRd returns empty argNames for empty \\describe block", {
   rd <- make_inherit_rd(describe_items = list())
   result <- .rs.parseInheritDotParamsFromRd(rd)
   expect_length(result$argNames, 0L)
})

# ---------------------------------------------------------------------------
# .rs.parseInheritDotParamsFromHelp  (help-db lookup layer)
# ---------------------------------------------------------------------------

test_that("parseInheritDotParamsFromHelp returns NULL for unknown function", {
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

with_fixture_completions <- function(expr) {
   with_rs_mock(
      ".rs.parseInheritDotParamsFromHelp",
      function(fnName, pkgName) .rs.parseInheritDotParamsFromRd(fixture_rd),
      expr
   )
}

test_that("getCompletionsInheritDotParams returns ARGUMENT completions from fixture", {
   with_fixture_completions({
      result <- .rs.getCompletionsInheritDotParams(
         token       = "",
         string      = "pkg::wrapper",
         object      = make_wrapper_fn(c("x", "...")),
         ownFormals  = c("x = ", "... = "),
         matchedCall = make_matched_call()
      )
      expect_length(result$results, 6L)
      expect_all_equal(result$type, .rs.acCompletionTypes$ARGUMENT)
      # Verify comma-separated args are split correctly
      expect_in("n = ",    result$results)
      expect_in("prop = ", result$results)
   })
})

test_that("getCompletionsInheritDotParams uses target package from \\link for completionSource", {
   with_fixture_completions({
      result <- .rs.getCompletionsInheritDotParams(
         token       = "",
         string      = "pkg::wrapper",
         object      = make_wrapper_fn(c("x", "...")),
         ownFormals  = c("x = ", "... = "),
         matchedCall = make_matched_call()
      )
      # Fixture uses [=target]{target} (same-package), so completionSource
      # falls back to pkgName.
      expect_all_equal(result$packages, "pkg::target")
   })
})

test_that("getCompletionsInheritDotParams uses targetPkg over pkgName for cross-package \\link", {
   # Cross-package [pkg:topic]{pkg::fn} link -- targetPkg should be preferred
   # over pkgName (the wrapper's package), fixing the masking / cross-package case.
   rd_text <- paste0(
      "\\name{wrapper}\\title{W}\\description{D}\\usage{wrapper(...)}\n",
      "\\arguments{\\item{...}{Arguments passed on to \\code{\\link[otherpkg:target]{otherpkg::target}}",
      "\\describe{\\item{\\code{x}}{desc}}}}"
   )
   rd <- tools::parse_Rd(textConnection(rd_text))
   with_rs_mock(
      ".rs.parseInheritDotParamsFromHelp",
      function(fnName, pkgName) .rs.parseInheritDotParamsFromRd(rd),
      {
         result <- .rs.getCompletionsInheritDotParams(
            token       = "",
            string      = "otherpkg::wrapper",
            object      = make_wrapper_fn(c("...")),
            ownFormals  = c("... = "),
            matchedCall = make_matched_call()
         )
         expect_all_equal(result$packages, "otherpkg::target")
      }
   )
})

test_that("getCompletionsInheritDotParams excludes wrapper's own formals", {
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
# .rs.getCompletionsFunction  (end-to-end, no external packages)
# ---------------------------------------------------------------------------

with_mock_inherit_dot_params <- function(newValue, expr) {
   with_rs_mock(".rs.getCompletionsInheritDotParams", newValue, expr)
}

test_that("getCompletionsFunction does not call getCompletionsInheritDotParams for primitives", {
   # Primitives like list() have ... but cannot have @inheritDotParams docs.
   # Verify the is.primitive() guard fires before getCompletionsInheritDotParams is reached.
   expect_no_error(
      with_mock_inherit_dot_params(
         function(...) stop("should not be called for primitives"),
         .rs.getCompletionsFunction(
            token        = "",
            string       = "list",
            functionCall = quote(list()),
            numCommas    = 0L,
            envir        = globalenv()
         )
      )
   )
})

test_that("getCompletionsFunction returns only own formals for non-primitive base fns without @inheritDotParams", {
   # base::mean has ... but no @inheritDotParams -- getCompletionsInheritDotParams
   # is called but returns empty, so only mean's own formals are offered.
   result <- .rs.getCompletionsFunction(
      token        = "",
      string       = "mean",
      functionCall = quote(mean()),
      numCommas    = 0L,
      envir        = globalenv()
   )
   returned_names <- sub(" = $", "", result$results)
   expect_setequal(returned_names, c("x", "..."))
})


# ---------------------------------------------------------------------------
# .rs.getCompletionsFunction  (end-to-end, requires ggplot2)
# ---------------------------------------------------------------------------

test_that("getCompletionsFunction returns inherited dot args with empty token", {
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

