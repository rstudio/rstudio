#
# test-notebook.R
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

test_that("#| comments are removed by .rs.extractChunkInnerCode() (#11606)", {
   code <- "```{sql}\n#| connection = db\n\nSELECT 'test'\n```"
   extract <- .rs.extractChunkInnerCode(code)
   expect_true(!any(grepl("#[|]", extract)))
})

test_that("--| cell options are removed by .rs.extractChunkInnerCode() (#18093)", {
   # SQL chunks may use the '--|' comment prefix for cell options; these
   # must be stripped before the body is sent to the database server, as
   # some backends (MySQL / MariaDB) reject '--|' as invalid SQL syntax.
   code <- "```{sql}\n--| connection: con\nSELECT 1\n```"
   extract <- .rs.extractChunkInnerCode(code)
   expect_false(any(grepl("--[|]", extract)))
   expect_true(any(grepl("SELECT 1", extract)))
})

test_that(".rs.truncateDataCapture() truncates data.table columns positionally (#18317)", {
   skip_if_not_installed("data.table")

   # [.data.table evaluates 'j' expressions in the frame of the table, so a
   # naive x[, c(1:n)] returns the integer vector 1:n rather than the columns
   dt <- data.table::as.data.table(as.list(seq_len(5)))

   data <- .rs.truncateDataCapture(dt, max.print = 10, cols.max.print = 3)

   expect_s3_class(data, "data.frame")
   expect_identical(dim(data), c(1L, 3L))
   expect_identical(names(data), c("V1", "V2", "V3"))
})

test_that(".rs.truncateDataCapture() tolerates as.data.frame methods that retain class", {
   # a data.table-like class: single-argument '[' selects rows, and
   # as.data.frame() returns the object with its class intact
   registerS3method("as.data.frame", "sticky", function(x, ...) x)
   registerS3method("[", "sticky", function(x, i, ...) {
      df <- structure(x, class = "data.frame")
      structure(df[i, , drop = FALSE], class = c("sticky", "data.frame"))
   })
   on.exit({
      table <- get(".__S3MethodsTable__.", envir = asNamespace("base"))
      rm(list = c("as.data.frame.sticky", "[.sticky"), envir = table)
   }, add = TRUE)

   x <- structure(data.frame(matrix(seq_len(10), nrow = 2)), class = c("sticky", "data.frame"))

   data <- .rs.truncateDataCapture(x, max.print = 10, cols.max.print = 3)
   expect_identical(class(data), "data.frame")
   expect_identical(dim(data), c(2L, 3L))
})

test_that(".rs.truncateDataCapture() limits rows and columns for data.frames", {
   df <- data.frame(matrix(seq_len(20), nrow = 4))

   data <- .rs.truncateDataCapture(df, max.print = 2, cols.max.print = 3)
   expect_identical(dim(data), c(2L, 3L))

   # data within the limits is returned unchanged
   data <- .rs.truncateDataCapture(df, max.print = 10, cols.max.print = 10)
   expect_identical(dim(data), c(4L, 5L))
})

test_that(".rs.rnb.extractInlineRCode() finds inline R code in prose only", {
   contents <- c(
      "---",
      "title: test",
      "output: html_notebook",
      "---",
      "",
      "```{r}",
      "x <- 42  # `r 1 + 1` within a chunk is not inline code",
      "```",
      "",
      "The answer is `r 1 + 1`.",
      "Today is `r Sys.Date()`."
   )

   code <- .rs.rnb.extractInlineRCode(contents)
   expect_equal(code, c("1 + 1", "Sys.Date()"))
})

test_that(".rs.rnb.evaluateInlineChunks() evaluates inline chunks (#17521)", {
   rmdPath <- tempfile(fileext = ".Rmd")
   writeLines(c(
      "---",
      "title: test",
      "output: html_notebook",
      "---",
      "",
      "Broken: `r .rs.test.noSuchObject`",
      "Value: `r .rs.test.inlineValue * 2`"
   ), con = rmdPath)
   on.exit(unlink(rmdPath), add = TRUE)

   # inline chunks are evaluated against the global environment
   assign(".rs.test.inlineValue", 21, envir = globalenv())
   on.exit(rm(".rs.test.inlineValue", envir = globalenv()), add = TRUE)

   cachePath <- .rs.rnb.evaluateInlineChunks(rmdPath)
   expect_true(file.exists(cachePath))
   on.exit(unlink(cachePath), add = TRUE)

   # NOTE: the broken expression comes first in the document, so this also
   # verifies that an evaluation failure does not abort later expressions
   outputs <- readRDS(cachePath)
   expect_equal(outputs[[".rs.test.inlineValue * 2"]]$text, "42")
   expect_match(outputs[[".rs.test.noSuchObject"]]$error, "not found")
})

test_that(".rs.rnb.extractInlineRCode() handles fence edge cases", {

   # tilde fences are recognized
   contents <- c("~~~", "`r 1 + 1`", "~~~", "Value: `r 2 + 2`")
   expect_equal(.rs.rnb.extractInlineRCode(contents), "2 + 2")

   # an unclosed fence swallows the rest of the document
   contents <- c("Before: `r 1 + 1`", "```", "`r 2 + 2`")
   expect_equal(.rs.rnb.extractInlineRCode(contents), "1 + 1")

   # blockquoted fences are recognized
   contents <- c("> ```", "> `r 1 + 1`", "> ```", "Value: `r 2 + 2`")
   expect_equal(.rs.rnb.extractInlineRCode(contents), "2 + 2")

   # a display-only block delimited by four backticks can contain inner
   # three-backtick fences; nothing within it is inline code
   contents <- c(
      "````markdown",
      "```{r}",
      "x <- 1",
      "```",
      "`r displayOnly()`",
      "````",
      "Value: `r 3 + 3`"
   )
   expect_equal(.rs.rnb.extractInlineRCode(contents), "3 + 3")

   # a tilde fence line inside an open backtick fence does not close it
   contents <- c("```", "~~~", "`r 1 + 1`", "```", "Value: `r 2 + 2`")
   expect_equal(.rs.rnb.extractInlineRCode(contents), "2 + 2")
})

test_that(".rs.rnb.evaluateInlineChunks() handles asis, multi-line, and duplicate expressions", {
   rmdPath <- tempfile(fileext = ".Rmd")
   writeLines(c(
      "---",
      "title: test",
      "output: html_notebook",
      "---",
      "",
      "Asis: `r knitr::asis_output('**bold**')`",
      "",
      "Multi: `r paste('a',",
      "   'b')`",
      "",
      "Dup: `r 6 * 7` and again `r 6 * 7`"
   ), con = rmdPath)
   on.exit(unlink(rmdPath), add = TRUE)

   cachePath <- .rs.rnb.evaluateInlineChunks(rmdPath)
   expect_true(file.exists(cachePath))
   on.exit(unlink(cachePath), add = TRUE)

   outputs <- readRDS(cachePath)

   # knit_asis results pass through knitr:::sew()
   expect_equal(outputs[["knitr::asis_output('**bold**')"]]$text, "**bold**")

   # multi-line inline expressions are evaluated
   texts <- vapply(outputs, function(output) as.character(output$text)[[1]], "")
   expect_true("a b" %in% texts)

   # duplicate expressions are evaluated (and stored) once
   expect_equal(sum(names(outputs) == "6 * 7"), 1L)
   expect_equal(outputs[["6 * 7"]]$text, "42")
})

test_that(".rs.rnb.installInlineOutputs() installs the inline output hook (#17521)", {
   outputs <- list(
      "6 * 7" = list(text = "42"),
      "stop('boom')" = list(error = "boom")
   )
   cachePath <- tempfile(fileext = ".rds")
   saveRDS(outputs, file = cachePath)

   # installation mutates knitr's global hook state; restore when done
   defaultHook <- knitr::knit_hooks$get("evaluate.inline")
   on.exit(knitr::knit_hooks$set(evaluate.inline = defaultHook), add = TRUE)

   .rs.rnb.installInlineOutputs(cachePath)

   # the cache file is consumed on installation
   expect_false(file.exists(cachePath))

   hook <- knitr::knit_hooks$get("evaluate.inline")

   # cache hit: the cached text is substituted verbatim
   expect_equal(hook("6 * 7", globalenv()), "42")

   # cached error: re-signaled loudly
   expect_error(hook("stop('boom')", globalenv()), "boom")

   # cache miss: falls back to evaluating in this process
   expect_equal(hook("2 + 2", globalenv()), 4)
})

test_that(".rs.rnb.installInlineOutputs() removes the cache file even on failure", {
   cachePath <- tempfile(fileext = ".rds")
   writeLines("not an rds file", con = cachePath)

   expect_error(.rs.rnb.installInlineOutputs(cachePath))
   expect_false(file.exists(cachePath))
})

test_that(".rs.rnb.evaluateInlineChunks() returns '' when knitr lacks the evaluate.inline hook", {
   rmdPath <- tempfile(fileext = ".Rmd")
   writeLines("Value: `r 1 + 1`", con = rmdPath)
   on.exit(unlink(rmdPath), add = TRUE)

   # simulate an older knitr without the 'evaluate.inline' hook; setting a
   # hook to NULL removes it, and we restore the original when done
   defaultHook <- knitr::knit_hooks$get("evaluate.inline")
   knitr::knit_hooks$set(evaluate.inline = NULL)
   on.exit(knitr::knit_hooks$set(evaluate.inline = defaultHook), add = TRUE)

   expect_equal(.rs.rnb.evaluateInlineChunks(rmdPath), "")
})

test_that(".rs.rnb.evaluateInlineChunks() returns '' when no inline code present", {
   rmdPath <- tempfile(fileext = ".Rmd")
   writeLines(c(
      "---",
      "title: test",
      "output: html_notebook",
      "---",
      "",
      "Just prose; the chunk below is not inline code.",
      "",
      "```{r}",
      "1 + 1",
      "```"
   ), con = rmdPath)
   on.exit(unlink(rmdPath), add = TRUE)

   expect_equal(.rs.rnb.evaluateInlineChunks(rmdPath), "")
})
