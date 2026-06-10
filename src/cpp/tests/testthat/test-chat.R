#
# test-chat.R
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

context("chat")

test_that("manifestDownloadCommand sets a 30s timeout and downloads to a temp file", {
   cmd <- .rs.chat.manifestDownloadCommand("https://cdn.posit.co/posit-ai/manifest.json")
   expect_true(grepl("options(timeout = 30L)", cmd, fixed = TRUE))
   expect_true(grepl("tempfile()", cmd, fixed = TRUE))
   expect_true(grepl("download.file(", cmd, fixed = TRUE))
   expect_true(grepl("cat(readLines(", cmd, fixed = TRUE))
   # the URL is deparsed (quoted) into the command
   expect_true(grepl("https://cdn.posit.co/posit-ai/manifest.json", cmd, fixed = TRUE))
})

test_that("manifestDownloadCommand injects download.file.method when set", {
   withr::with_options(list(download.file.method = "libcurl"), {
      cmd <- .rs.chat.manifestDownloadCommand("https://example.test/m.json")
      expect_true(grepl("method = \"libcurl\"", cmd, fixed = TRUE))
   })
})

test_that("manifestDownloadCommand injects download.file.extra when set", {
   withr::with_options(list(download.file.extra = c("--cacert", "/etc/ca.pem")), {
      cmd <- .rs.chat.manifestDownloadCommand("https://example.test/m.json")
      expect_true(grepl("extra = ", cmd, fixed = TRUE))
      expect_true(grepl("--cacert", cmd, fixed = TRUE))
   })
})

test_that("manifestDownloadCommand omits method/extra when unset", {
   withr::with_options(list(download.file.method = NULL, download.file.extra = NULL), {
      cmd <- .rs.chat.manifestDownloadCommand("https://example.test/m.json")
      expect_false(grepl("method = ", cmd, fixed = TRUE))
      expect_false(grepl("extra = ", cmd, fixed = TRUE))
   })
})

test_that("manifestDownloadCommand errors when download.file reports a non-zero status", {
   # Some download.file methods return a non-zero status with only a warning, so
   # the command must check the status and stop() -- otherwise a failed download
   # would exit 0 with a partial/empty body and be parsed as a valid manifest.
   # Exercise the behavior (the command throws), not the command text. cmd is the
   # trusted output of the function under test, evaluated in a sandbox env whose
   # download.file is stubbed to report failure -- this is how the generated R
   # command is meant to be run.
   cmd <- .rs.chat.manifestDownloadCommand("https://example.test/m.json")

   env <- new.env()
   env$download.file <- function(...) 1L  # non-zero status, no file written
   expect_error(eval(parse(text = cmd), envir = env), "download.file failed")
})

test_that("manifestDownloadCommand emits the downloaded body with newlines intact", {
   # cat() defaults to sep = " "; the command uses sep = "\n" so a multi-line body
   # round-trips verbatim rather than having its line breaks collapsed to spaces.
   cmd <- .rs.chat.manifestDownloadCommand("https://example.test/m.json")

   env <- new.env()
   env$download.file <- function(url, destfile, ...) {
      writeLines(c("{", '  "v": 1', "}"), destfile)
      0L
   }
   out <- paste(capture.output(eval(parse(text = cmd), envir = env)), collapse = "\n")
   expect_identical(out, "{\n  \"v\": 1\n}")
})

test_that("chat.safeEval returns value and visibility with no conditions", {
   result <- .rs.chat.safeEval(quote(42))
   expect_equal(result$value, 42)
   expect_true(result$visible)
   expect_equal(result$conditions, list())

   result <- .rs.chat.safeEval(quote(invisible(42)))
   expect_false(result$visible)
})

test_that("chat.safeEval records and muffles warnings when warn < 2", {
   expect_warning(
      result <- .rs.chat.safeEval(quote(warning("beware"))),
      regexp = NA
   )
   expect_equal(result$conditions, list(list(type = "warning", text = "beware")))
})

test_that("chat.safeEval leaves warning-to-error conversion intact under warn = 2", {
   withr::with_options(list(warn = 2), {
      result <- .rs.chat.safeEval(quote(warning("beware")))
   })
   expect_true(inherits(result, "error"))
   expect_equal(
      attr(result, "assistant_conditions"),
      list(list(type = "warning", text = "beware"))
   )
})

test_that("chat.safeEval returns errors with recorded conditions attached", {
   expect_message(
      result <- .rs.chat.safeEval(quote({ message("progress"); stop("boom") })),
      "progress"
   )
   expect_true(inherits(result, "error"))
   expect_equal(conditionMessage(result), "boom")
   expect_equal(
      attr(result, "assistant_conditions"),
      list(list(type = "message", text = "progress\n"))
   )
})

test_that("chat.formatWarningMessages formats deferred warnings like the REPL", {
   expect_equal(.rs.chat.formatWarningMessages(list()), "")

   one <- list(list(type = "warning", text = "beware"))
   expect_equal(.rs.chat.formatWarningMessages(one), "Warning message:\nbeware\n")

   several <- list(
      list(type = "warning", text = "first"),
      list(type = "message", text = "not a warning\n"),
      list(type = "warning", text = "second")
   )
   expect_equal(
      .rs.chat.formatWarningMessages(several),
      "Warning messages:\n1: first\n2: second\n"
   )
})

test_that("chat.callExpressionBoundaryHook calls a hidden global hook with its arguments", {
   calls <- list()
   assign(".test_boundary_hook", function(expr, value, ok, visible, error, conditions) {
      calls[[length(calls) + 1L]] <<- list(value = value, ok = ok, visible = visible)
   }, envir = globalenv())
   on.exit(rm(".test_boundary_hook", envir = globalenv()), add = TRUE)

   .rs.chat.callExpressionBoundaryHook(".test_boundary_hook", quote(1 + 1), 2, TRUE, TRUE)
   expect_equal(calls, list(list(value = 2, ok = TRUE, visible = TRUE)))
})

test_that("chat.callExpressionBoundaryHook is a no-op for missing or invalid hooks", {
   expect_silent(.rs.chat.callExpressionBoundaryHook("", quote(1), 1, TRUE, TRUE))
   expect_silent(.rs.chat.callExpressionBoundaryHook(".no_such_hook", quote(1), 1, TRUE, TRUE))

   assign(".test_not_a_function", 42, envir = globalenv())
   on.exit(rm(".test_not_a_function", envir = globalenv()), add = TRUE)
   expect_silent(.rs.chat.callExpressionBoundaryHook(".test_not_a_function", quote(1), 1, TRUE, TRUE))
})

test_that("chat.callExpressionBoundaryHook swallows hook errors", {
   assign(".test_error_hook", function(...) stop("hook failed"), envir = globalenv())
   on.exit(rm(".test_error_hook", envir = globalenv()), add = TRUE)

   expect_silent(.rs.chat.callExpressionBoundaryHook(".test_error_hook", quote(1), 1, TRUE, TRUE))
})
